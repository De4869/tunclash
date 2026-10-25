package com.mihomo.tproxy.core

import com.mihomo.tproxy.model.ProxySettings

/**
 * Turns whatever the user imported (Clash YAML, share links, several
 * subscriptions) into a single mihomo config with our runtime overrides.
 */
object ConfigComposer {

    const val TARGET = "🚀 节点选择"
    const val AUTO = "♻️ 自动选择"
    const val FALLBACK = "🔯 故障转移"
    const val BALANCE = "⚖️ 负载均衡"

    data class Node(val block: String, val name: String, val type: String, val server: String, val port: String) {
        val key: String get() = type + "|" + server + "|" + port + "|" + Yaml.field(block, "uuid") + Yaml.field(block, "password")
    }

    private data class Region(val label: String, val flag: String)

    private fun regionOf(name: String): Region? {
        val n = name.lowercase()
        fun has(vararg ks: String) = ks.any { n.contains(it) }
        return when {
            has("香港", "hong kong", "hk", "深港", "沪港") -> Region("香港", "🇭🇰")
            has("台湾", "台北", "taiwan", "tw", "新北") -> Region("台湾", "🇹🇼")
            has("日本", "东京", "大阪", "japan", "jp", "tokyo", "osaka") -> Region("日本", "🇯🇵")
            has("新加坡", "狮城", "singapore", "sg") -> Region("新加坡", "🇸🇬")
            has("美国", "洛杉矶", "圣何塞", "西雅图", "纽约", "united states", "usa", "us ", "los angeles") -> Region("美国", "🇺🇸")
            has("韩国", "首尔", "korea", "kr", "seoul") -> Region("韩国", "🇰🇷")
            has("英国", "伦敦", "united kingdom", "uk", "london") -> Region("英国", "🇬🇧")
            has("德国", "法兰克福", "germany", "de ") -> Region("德国", "🇩🇪")
            has("法国", "巴黎", "france", "fr ") -> Region("法国", "🇫🇷")
            has("荷兰", "阿姆斯特丹", "netherlands", "nl") -> Region("荷兰", "🇳🇱")
            has("加拿大", "canada", "ca ") -> Region("加拿大", "🇨🇦")
            has("澳大利亚", "悉尼", "australia", "au ") -> Region("澳大利亚", "🇦🇺")
            has("俄罗斯", "russia", "ru ") -> Region("俄罗斯", "🇷🇺")
            has("土耳其", "turkey", "tr ") -> Region("土耳其", "🇹🇷")
            has("印度", "india", "in ") -> Region("印度", "🇮🇳")
            has("马来西亚", "malaysia", "my ") -> Region("马来西亚", "🇲🇾")
            has("泰国", "thailand", "th ") -> Region("泰国", "🇹🇭")
            has("越南", "vietnam", "vn ") -> Region("越南", "🇻🇳")
            has("菲律宾", "philippines", "ph ") -> Region("菲律宾", "🇵🇭")
            has("印尼", "印度尼西亚", "indonesia", "id ") -> Region("印尼", "🇮🇩")
            has("阿根廷", "argentina") -> Region("阿根廷", "🇦🇷")
            has("巴西", "brazil") -> Region("巴西", "🇧🇷")
            has("迪拜", "dubai", "阿联酋") -> Region("阿联酋", "🇦🇪")
            has("瑞士", "swiss") -> Region("瑞士", "🇨🇭")
            has("瑞典", "sweden") -> Region("瑞典", "🇸🇪")
            has("波兰", "poland") -> Region("波兰", "🇵🇱")
            has("乌克兰", "ukraine") -> Region("乌克兰", "🇺🇦")
            has("中国", "回国", "china") -> Region("中国", "🇨🇳")
            else -> null
        }
    }

    /** The emoji a node name already starts with (handles regional-indicator flag pairs). */
    fun leadingEmoji(name: String): String {
        if (name.isEmpty()) return ""
        val cp = name.codePointAt(0)
        if (cp in 0x1F1E6..0x1F1FF && name.codePointCount(0, name.length) >= 2) {
            val cp2 = name.codePointAt(Character.charCount(cp))
            if (cp2 in 0x1F1E6..0x1F1FF) {
                return name.substring(0, Character.charCount(cp) + Character.charCount(cp2))
            }
        }
        return String(Character.toChars(cp))
    }

    fun flagOf(name: String): String {
        if (name.isBlank()) return ""
        val c = name[0]
        // already decorated with an emoji / group marker
        if (Character.isSurrogate(c)) return ""
        if (name.startsWith("♻") || name.startsWith("⚖") || name.startsWith("🔯") || name.startsWith("🚀")) return ""
        return regionOf(name)?.flag ?: "🌐"
    }

    // ---------------------------------------------------------------- header

    private fun dnsServers(s: ProxySettings): List<String> {
        val primary = when (s.dnsPreset) {
            "google" -> listOf("https://dns.google/dns-query", "8.8.8.8")
            "cf" -> listOf("https://cloudflare-dns.com/dns-query", "1.1.1.1")
            "custom" -> s.customDns.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                .ifEmpty { listOf("https://dns.alidns.com/dns-query") }
            else -> listOf("https://dns.alidns.com/dns-query", "https://doh.pub/dns-query")
        }
        // a domestic fallback keeps resolution alive when the chosen (foreign) DNS is unreachable
        return (primary + listOf("https://dns.alidns.com/dns-query", "223.5.5.5")).distinct()
    }

    private fun header(s: ProxySettings): String {
        val dns = dnsServers(s)
        val b = StringBuilder()
        b.append("mixed-port: 7890\n")
        b.append("allow-lan: false\n")
        b.append("bind-address: 127.0.0.1\n")
        b.append("mode: ").append(if (s.mode == "direct") "rule" else s.mode).append("\n")
        b.append("log-level: ").append(s.logLevel).append("\n")
        b.append("ipv6: ").append(s.ipv6).append("\n")
        b.append("unified-delay: true\n")
        b.append("tcp-concurrent: true\n")
        b.append("find-process-mode: strict\n")
        b.append("external-controller: 127.0.0.1:9090\n")
        b.append("secret: ").append(Yaml.quote(s.secret)).append("\n")
        b.append("geodata-mode: false\n")
        b.append("geo-auto-update: false\n")
        b.append("profile:\n  store-selected: true\n  store-fake-ip: true\n")
        b.append("sniffer:\n")
        b.append("  enable: true\n")
        b.append("  force-dns-mapping: true\n")
        b.append("  parse-pure-ip: true\n")
        b.append("  override-destination: true\n")
        b.append("  sniff:\n")
        b.append("    TLS:\n      ports: [443, 8443]\n")
        b.append("    HTTP:\n      ports: [80, 8080-8880]\n")
        b.append("    QUIC:\n      ports: [443, 8443]\n")
        b.append("tun:\n")
        b.append("  enable: true\n")
        b.append("  stack: ").append(s.tunStack).append("\n")
        b.append("  device: mihomo0\n")
        b.append("  auto-route: true\n")
        b.append("  auto-detect-interface: true\n")
        b.append("  dns-hijack:\n    - any:53\n")
        if (s.perAppEnabled && s.perAppUids.isNotEmpty()) {
            val key = if (s.perAppWhitelist) "include-uid" else "exclude-uid"
            b.append("  ").append(key).append(":\n")
            s.perAppUids.sorted().forEach { b.append("    - ").append(it).append("\n") }
        }
        b.append("  mtu: ").append(s.mtu).append("\n")
        b.append("dns:\n")
        b.append("  enable: true\n")
        b.append("  listen: 0.0.0.0:1053\n")
        b.append("  ipv6: ").append(s.ipv6).append("\n")
        b.append("  enhanced-mode: fake-ip\n")
        b.append("  fake-ip-range: 198.18.0.1/16\n")
        b.append("  respect-rules: true\n")
        b.append("  fake-ip-filter:\n")
        listOf(
            "geosite:geolocation-!cn",
            "*.lan", "*.local", "*.localdomain", "localhost.ptlogin2.qq.com",
            "+.msftconnecttest.com", "+.msftncsi.com", "time.*.com", "ntp.*.com",
            "+.srv.nintendo.net", "+.stun.playstation.net", "xbox.*.microsoft.com",
            "+.battlenet.com.cn", "+.wotgame.cn", "+.stun.*"
        ).forEach { b.append("    - ").append(Yaml.quote(it)).append("\n") }
        b.append("  default-nameserver:\n    - 223.5.5.5\n    - 119.29.29.29\n")
        b.append("  proxy-server-nameserver:\n    - 223.5.5.5\n    - 119.29.29.29\n")
        b.append("  nameserver:\n")
        dns.forEach { b.append("    - ").append(Yaml.quote(it)).append("\n") }
        b.append("  nameserver-policy:\n")
        b.append("    \"geosite:cn,private\": [\"https://dns.alidns.com/dns-query\", \"223.5.5.5\", \"119.29.29.29\"]\n")
        return b.toString()
    }

    // ---------------------------------------------------------------- rules

    private fun perAppRules(s: ProxySettings): List<String> = emptyList()

    private fun adProviderRules(s: ProxySettings): List<String> =
        if (!s.adBlock || s.adSources.isEmpty()) emptyList()
        else s.adSources.indices.map { "RULE-SET,ad" + it + ",REJECT" }

    private fun providerBlock(s: ProxySettings): String {
        if (!s.adBlock || s.adSources.isEmpty()) return ""
        val b = StringBuilder("rule-providers:\n")
        s.adSources.forEachIndexed { i, src ->
            b.append("  ad").append(i).append(":\n")
            b.append("    type: http\n")
            b.append("    behavior: ").append(src.behavior).append("\n")
            b.append("    format: ").append(src.format).append("\n")
            b.append("    url: ").append(Yaml.quote(src.url)).append("\n")
            b.append("    path: ./ad").append(i).append(".yaml\n")
            b.append("    interval: ").append(s.adUpdateHours.coerceIn(1, 168) * 3600).append("\n")
        }
        return b.toString()
    }

    /** Adds (or merges into) a top level rule-providers: key. */
    private fun withProviders(text: String, s: ProxySettings): String {
        if (!s.adBlock || s.adSources.isEmpty()) return text
        val block = providerBlock(s)
        val entries = block.removePrefix("rule-providers:\n").trimEnd()
        val lines = text.lines().toMutableList()
        val idx = lines.indexOfFirst { it.matches(Regex("^rule-providers:\\s*$")) }
        return if (idx >= 0) {
            lines.add(idx + 1, entries)
            lines.joinToString("\n")
        } else {
            text.trimEnd() + "\n\n" + block
        }
    }

    private fun adRules(s: ProxySettings): List<String> =
        if (!s.adBlock) emptyList()
        else listOf("GEOSITE,category-ads-all,REJECT", "DOMAIN-KEYWORD,adservice,REJECT")

    private fun rulesFor(s: ProxySettings, target: String, includeDirect: Boolean = true): List<String> {
        val rules = mutableListOf<String>()
        val perApp = perAppRules(s)
        rules += perApp
        rules += adProviderRules(s)
        rules += adRules(s)
        if (includeDirect) {
            rules += "DOMAIN-SUFFIX,lan,DIRECT"
            rules += "DOMAIN-SUFFIX,local,DIRECT"
            rules += "IP-CIDR,127.0.0.0/8,DIRECT,no-resolve"
            rules += "IP-CIDR,10.0.0.0/8,DIRECT,no-resolve"
            rules += "IP-CIDR,172.16.0.0/12,DIRECT,no-resolve"
            rules += "IP-CIDR,192.168.0.0/16,DIRECT,no-resolve"
            rules += "IP-CIDR,100.64.0.0/10,DIRECT,no-resolve"
            rules += "IP-CIDR,169.254.0.0/16,DIRECT,no-resolve"
            rules += "IP-CIDR,224.0.0.0/4,DIRECT,no-resolve"
        }
        when (s.rulePreset) {
            "global" -> rules += "MATCH," + target
            "direct" -> rules += "MATCH,DIRECT"
            else -> {
                // domain based first: domestic DNS answers for blocked domains are often polluted,
                // so GEOIP,CN alone would send google/youtube DIRECT.
                rules += "GEOSITE,geolocation-!cn," + target
                rules += "DOMAIN-SUFFIX,xn--ngstr-lra8j.com,DIRECT"
                rules += "DOMAIN-SUFFIX,clientservices.googleapis.com," + target
                rules += "DOMAIN-SUFFIX,services.googleapis.cn," + target
                rules += "DOMAIN-SUFFIX,gvt1.com,DIRECT"
                rules += "DOMAIN-SUFFIX,gvt2.com,DIRECT"
                rules += "DOMAIN-SUFFIX,googleusercontent.com,DIRECT"
                rules += "DOMAIN-SUFFIX,ggpht.com," + target
                rules += "DOMAIN,dl.google.com,DIRECT"
                rules += "DOMAIN-SUFFIX,cn,DIRECT"
                rules += "GEOSITE,cn,DIRECT"
                rules += "GEOIP,CN,DIRECT"
                rules += "MATCH," + target
            }
        }
        return rules
    }

    private fun rulesBlock(s: ProxySettings, target: String): String {
        val b = StringBuilder("rules:\n")
        rulesFor(s, target).forEach { b.append("  - ").append(it).append("\n") }
        return b.toString()
    }

    // ---------------------------------------------------------------- nodes

    private fun nodesOf(raw: String): List<Node> {
        val blocks = if (ShareLinks.looksLikeLinks(raw)) ShareLinks.toBlocks(raw) else Yaml.proxyBlocks(raw)
        return blocks.mapNotNull { block ->
            val name = Yaml.field(block, "name").ifBlank { return@mapNotNull null }
            Node(
                block = block,
                name = name,
                type = Yaml.field(block, "type"),
                server = Yaml.field(block, "server"),
                port = Yaml.field(block, "port")
            )
        }
    }

    /** The proxy group a config's final MATCH rule points to (falls back to the first selector). */
    fun mainTarget(text: String): String? {
        for (line in text.lines()) {
            val t = line.trim()
            if (!t.startsWith("-")) continue
            val rule = Yaml.unquote(t.removePrefix("-").trim())
            if (rule.startsWith("MATCH,", ignoreCase = true)) {
                val name = rule.substringAfter(',').trim()
                if (name.isNotBlank()) return name
            }
        }
        val groups = text.substringAfter("proxy-groups:", "")
        val g = Regex("""(?m)^\s*-\s*name:\s*(.+?)\s*$""").find(groups) ?: return null
        return Yaml.unquote(g.groupValues[1]).ifBlank { null }
    }

    private val JUNK = listOf("剩余流量", "距离下次重置", "套餐到期", "到期时间", "官网", "客服", "超时请重启", "订阅", "过期")
    fun isJunkNode(name: String): Boolean = JUNK.any { name.contains(it) }

    fun nodeCount(raw: String): Int = nodesOf(raw).size

    /** Deduplicate identical servers and resolve display name collisions. */
    private fun dedupe(all: List<Node>): List<Node> {
        val byKey = LinkedHashMap<String, Node>()
        val usedNames = HashSet<String>()
        for (n in all) {
            if (byKey.containsKey(n.key)) continue
            val base = Yaml.safeName(n.name)
            var finalName = base
            var i = 2
            while (!usedNames.add(finalName)) {
                finalName = base + " (" + i + ")"
                i++
            }
            byKey[n.key] = n.copy(name = finalName, block = Yaml.rename(n.block, finalName))
        }
        return byKey.values.toList()
    }

    private fun groupsFor(nodes: List<Node>, s: ProxySettings): String {
        val names = nodes.map { it.name }
        val regions = LinkedHashMap<String, MutableList<String>>()
        nodes.forEach { n ->
            val r = regionOf(n.name)
            if (r != null) regions.getOrPut(r.flag + " " + r.label) { mutableListOf() }.add(n.name)
        }
        val b = StringBuilder("proxy-groups:\n")
        fun q(s0: String) = Yaml.quote(s0)
        fun list(items: List<String>, indent: String = "      ") {
            items.forEach { b.append(indent).append("- ").append(q(it)).append("\n") }
        }
        // main selector
        b.append("  - name: ").append(q(TARGET)).append("\n    type: select\n    proxies:\n")
        list(listOf(AUTO, FALLBACK, BALANCE) + regions.keys.toList() + names)
        // url-test
        b.append("  - name: ").append(q(AUTO)).append("\n    type: url-test\n")
        b.append("    url: http://www.gstatic.com/generate_204\n    interval: 300\n    tolerance: 50\n")
        b.append("    lazy: false\n    proxies:\n")
        list(names)
        // fallback
        b.append("  - name: ").append(q(FALLBACK)).append("\n    type: fallback\n")
        b.append("    url: http://www.gstatic.com/generate_204\n    interval: 300\n")
        b.append("    proxies:\n")
        list(names)
        // load balance
        b.append("  - name: ").append(q(BALANCE)).append("\n    type: load-balance\n")
        b.append("    url: http://www.gstatic.com/generate_204\n    interval: 300\n    strategy: consistent-hashing\n")
        b.append("    proxies:\n")
        list(names)
        // region groups
        regions.forEach { (label, members) ->
            b.append("  - name: ").append(q(label)).append("\n    type: url-test\n")
            b.append("    url: http://www.gstatic.com/generate_204\n    interval: 300\n    tolerance: 50\n")
            b.append("    proxies:\n")
            list(members)
        }
        return b.toString()
    }

    // ---------------------------------------------------------------- public

    private fun buildConfig(rawNodes: List<Node>, s: ProxySettings): String {
        val nodes = dedupe(rawNodes)
        val b = StringBuilder()
        b.append(header(s)).append("\n")
        if (nodes.isEmpty()) {
            b.append("proxies: []\nproxy-groups: []\n")
            b.append("rules:\n  - MATCH,DIRECT\n")
            return b.toString()
        }
        b.append("proxies:\n")
        nodes.forEach { b.append(it.block).append("\n") }
        b.append(groupsFor(nodes, s))
        if (s.adBlock && s.adSources.isNotEmpty()) b.append(providerBlock(s))
        b.append(rulesBlock(s, TARGET))
        return b.toString()
    }

    fun defaultConfig(s: ProxySettings): String = buildConfig(emptyList(), s)

    /** One profile -> runnable config. Imported configs keep their own rules. */
    fun compose(raw: String, s: ProxySettings): String {
        val text = ShareLinks.expand(raw)
        val isLinks = ShareLinks.looksLikeLinks(text)
        val nodes = if (isLinks) nodesOf(text) else nodesOf(text)
        if (isLinks) return buildConfig(dedupe(nodes), s)
        if (!Yaml.hasProxies(text) && nodes.isEmpty()) {
            // provider based or broken config: keep verbatim but apply overrides
            return header(s) + "\n" + Yaml.stripManaged(text).trim() + "\n"
        }
        if (nodes.isEmpty()) return buildConfig(emptyList(), s)
        val hasGroups = Regex("(?m)^proxy-groups:").containsMatchIn(text)
        if (!hasGroups) {
            // no strategy groups: rebuild from parsed nodes so names stay consistent
            return buildConfig(nodes, s)
        }
        var stripped = withProviders(Yaml.stripManaged(text).trim(), s)
        val smart = mutableListOf<String>()
        val target = mainTarget(stripped)
        if (target != null) {
            // Google Play downloads resolve to CN CDN ips and would be sent DIRECT by GEOIP,CN
            smart += "DOMAIN-SUFFIX,xn--ngstr-lra8j.com,DIRECT"
            smart += "DOMAIN-SUFFIX,clientservices.googleapis.com," + target
            smart += "DOMAIN-SUFFIX,services.googleapis.cn," + target
            smart += "DOMAIN-SUFFIX,gvt1.com,DIRECT"
            smart += "DOMAIN-SUFFIX,gvt2.com,DIRECT"
            smart += "DOMAIN-SUFFIX,googleusercontent.com,DIRECT"
            smart += "DOMAIN-SUFFIX,ggpht.com," + target
            smart += "DOMAIN,dl.google.com,DIRECT"
            if (s.rulePreset == "smart") {
                smart += "GEOSITE,geolocation-!cn," + target
                smart += "GEOSITE,cn,DIRECT"
            }
        }
        val extraRules = adProviderRules(s) + adRules(s) + smart
        if (extraRules.isNotEmpty()) {
            val whitelist = s.perAppEnabled && s.perAppWhitelist && s.perAppPackages.isNotEmpty()
            stripped = Yaml.insertRules(stripped, if (whitelist) extraRules + "MATCH,DIRECT" else extraRules)
        }
        return header(s) + "\n" + stripped + "\n"
    }

    /** Merge every source into one deduplicated config with region groups. */
    fun merge(sources: List<String>, s: ProxySettings): String {
        val keys = s.mergeFilter.split(",", "，", " ", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val all = sources.filter { it.isNotBlank() }
            .flatMap { nodesOf(ShareLinks.expand(it)) }
            .filterNot { isJunkNode(it.name) }
            .filterNot { node -> keys.any { node.name.contains(it, ignoreCase = true) } }
        return buildConfig(dedupe(all), s)
    }

    /** Extract just the display names, used for "整合前" previews. */
    fun nodeNames(raw: String): List<String> = nodesOf(ShareLinks.expand(raw)).map { it.name }
}