package com.mihomo.tproxy.core

/** Small, dependency free helpers for the handful of YAML shapes Clash uses. */
object Yaml {

    val MANAGED_KEYS = setOf(
        "port", "socks-port", "mixed-port", "redir-port", "tproxy-port",
        "allow-lan", "bind-address", "mode", "log-level", "ipv6",
        "external-controller", "external-ui", "secret", "tun", "dns", "profile",
        "geodata-mode", "geo-auto-update", "find-process-mode", "unified-delay",
        "tcp-concurrent", "interface-name", "routing-mark", "geox-url",
        "global-client-fingerprint", "keep-alive-interval", "keep-alive-idle"
    )

    private val topKey = Regex("""^([A-Za-z0-9_-]+):""")

    /** Removes every top level key we generate ourselves, keeps everything else. */
    fun stripManaged(yaml: String): String {
        val out = StringBuilder()
        var skipping = false
        for (line in yaml.lines()) {
            val m = topKey.find(line)
            if (m != null && !line.startsWith(" ") && !line.startsWith("\t")) {
                skipping = m.groupValues[1] in MANAGED_KEYS
                if (skipping) continue
            } else if (skipping) {
                if (line.isNotBlank() && !line.startsWith(" ") && !line.startsWith("\t") && !line.startsWith("#")) {
                    skipping = false
                } else {
                    continue
                }
            }
            out.append(line).append('\n')
        }
        return out.toString()
    }

    /** Inserts rules at the very top of the rules: list. */
    fun insertRules(yaml: String, rules: List<String>): String {
        if (rules.isEmpty()) return yaml
        val lines = yaml.lines().toMutableList()
        val idx = lines.indexOfFirst { it.matches(Regex("""^rules:\s*$""")) }
        if (idx < 0) {
            return yaml.trimEnd() + "\n\nrules:\n" + rules.joinToString("\n") { "  - " + it } + "\n"
        }
        // match the indentation of the existing rule items, otherwise YAML nests them
        var indent = "  "
        for (i in idx + 1 until lines.size) {
            val l = lines[i]
            if (l.isBlank()) continue
            if (!l.startsWith(" ")) break
            val m = Regex("""^(\s+)-\s""").find(l)
            if (m != null) { indent = m.groupValues[1]; break }
        }
        lines.addAll(idx + 1, rules.map { indent + "- " + it })
        return lines.joinToString("\n")
    }

    fun hasProxies(yaml: String): Boolean =
        yaml.lines().any { it.matches(Regex("""^proxies:\s*$""")) }

    /**
     * Returns the raw text blocks of every item under proxies:. Each block keeps
     * its original indentation so it can be re-emitted verbatim.
     */
    fun proxyBlocks(yaml: String): List<String> {
        val lines = yaml.lines()
        val start = lines.indexOfFirst { it.matches(Regex("""^proxies:\s*$""")) }
        if (start < 0) return emptyList()
        var end = lines.size
        for (i in start + 1 until lines.size) {
            val l = lines[i]
            if (l.isNotBlank() && !l.startsWith(" ") && !l.startsWith("#")) { end = i; break }
        }
        val body = lines.subList(start + 1, end)
        val dash = Regex("""^(\s*)-\s+""")
        val items = mutableListOf<MutableList<String>>()
        var current: MutableList<String>? = null
        for (l in body) {
            val m = dash.find(l)
            if (m != null) {
                current = mutableListOf(l)
                items.add(current)
            } else {
                val cur = current
                if (cur != null) {
                    cur.add(l)
                } else if (l.isNotBlank()) {
                    val nl = mutableListOf(l)
                    items.add(nl)
                    current = nl
                }
            }
        }
        return items.map { it.joinToString("\n") }.filter { it.isNotBlank() }
    }

    /** Clash configs from Chinese airports often use flow style: "- { name: x, type: ss, ... }". */
    fun isFlow(block: String): Boolean = block.trimStart().startsWith("- {")

    fun flowFields(block: String): Map<String, String>? {
        val start = block.indexOf('{')
        if (start < 0) return null
        val end = block.lastIndexOf('}')
        if (end <= start) return null
        val body = block.substring(start + 1, end)
        val parts = mutableListOf<String>()
        val sb = StringBuilder()
        var quote = ' '
        for (c in body) {
            if (quote != ' ') {
                if (c == quote) quote = ' ' else sb.append(c)
            } else when (c) {
                '\'' , '"' -> quote = c
                ',' -> { parts.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(c)
            }
        }
        if (sb.isNotBlank()) parts.add(sb.toString())
        val map = LinkedHashMap<String, String>()
        for (p in parts) {
            val idx = p.indexOf(':')
            if (idx <= 0) continue
            val k = p.substring(0, idx).trim()
            if (k.isNotEmpty() && k.none { it == ' ' || it == '{' }) map[k] = p.substring(idx + 1).trim()
        }
        return map
    }

    fun field(block: String, key: String): String {
        if (isFlow(block)) {
            flowFields(block)?.get(key)?.let { return unquote(it.trim()) }
        }
        val re = Regex("""^[ \t]*(?:-[ \t]+)?""" + Regex.escape(key) + """:[ \t]*(.*?)[ \t]*$""", RegexOption.MULTILINE)
        val m = re.find(block) ?: return ""
        return unquote(m.groupValues[1].substringBefore(" #").trim())
    }

    fun rename(block: String, newName: String): String {
        if (isFlow(block)) {
            val re = Regex("""(name[ \t]*:[ \t]*)('[^']*'|"[^"]*"|[^,}]*)""")
            return re.replace(block, "$1" + quote(newName))
        }
        val re = Regex("""^([ \t]*(?:-[ \t]+)?)name:[ \t]*.*$""", RegexOption.MULTILINE)
        val m = re.find(block) ?: return block
        return block.replaceRange(m.range, m.groupValues[1] + "name: " + quote(newName))
    }

    /** Airports love invalid escapes like "\_" inside double quotes; normalise them. */
    fun safeName(raw: String): String {
        var v = raw.replace("\\", "").replace("\"", "").trim()
        v = v.replace(Regex("\\s+"), " ")
        return v.ifBlank { "node" }
    }

    fun quote(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    fun unquote(s: String): String {
        var v = s.trim()
        if (v.length >= 2 && ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'")))) {
            v = v.substring(1, v.length - 1)
        }
        return v
    }
}