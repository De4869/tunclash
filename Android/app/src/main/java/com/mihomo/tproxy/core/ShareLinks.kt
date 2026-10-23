package com.mihomo.tproxy.core

import org.json.JSONObject
import java.net.URLDecoder
import java.util.Base64

/**
 * Converts vmess / vless / trojan / ss / hysteria2 / tuic share links into
 * Clash (mihomo) proxy YAML blocks, so every airport link actually works.
 */
object ShareLinks {

    fun looksLikeLinks(text: String): Boolean =
        text.lineSequence().map { it.trim() }.any { isLink(it) }

    fun isLink(s: String): Boolean =
        s.startsWith("vmess://") || s.startsWith("vless://") || s.startsWith("trojan://") ||
            s.startsWith("ss://") || s.startsWith("hysteria2://") || s.startsWith("hy2://") ||
            s.startsWith("tuic://") || s.startsWith("hysteria://")

    /** Some airports hand out a base64 blob of share links. Unwrap it. */
    fun expand(text: String): String {
        val t = text.trim()
        if (looksLikeLinks(t)) return t
        if (t.contains("proxies:") || t.contains("proxy-groups:")) return t
        val decoded = decodeBase64(t)
        return if (decoded != null && looksLikeLinks(decoded)) decoded else t
    }

    private fun decodeBase64(s: String): String? = runCatching {
        val clean = s.replace("\n", "").replace("\r", "").replace(" ", "").trim()
        val padded = clean + "=".repeat((4 - clean.length % 4) % 4)
        val bytes = Base64.getUrlDecoder().decode(padded)
        String(bytes, Charsets.UTF_8)
    }.recoverCatching {
        val clean = s.replace("\n", "").replace("\r", "").replace(" ", "").trim()
        val padded = clean + "=".repeat((4 - clean.length % 4) % 4)
        String(Base64.getDecoder().decode(padded), Charsets.UTF_8)
    }.getOrNull()

    private fun b64(s: String): ByteArray? = runCatching {
        val clean = s.trim()
        val padded = clean + "=".repeat((4 - clean.length % 4) % 4)
        Base64.getUrlDecoder().decode(padded)
    }.recoverCatching {
        val clean = s.trim()
        val padded = clean + "=".repeat((4 - clean.length % 4) % 4)
        Base64.getDecoder().decode(padded)
    }.getOrNull()

    private fun q(s: String, key: String): String {
        return runCatching {
            java.net.URI(s).rawQuery?.split("&")?.firstOrNull { it.startsWith(key + "=") }
                ?.substringAfter("=")?.let { URLDecoder.decode(it, "UTF-8") } ?: ""
        }.getOrDefault("")
    }

    fun toBlocks(text: String): List<String> {
        val a = if (text.contains("1;") || text.contains("é") || text.contains("¤")) text else text
        val src = if (looksLikeLinks(a)) a else expand(a)
        return src.lineSequence().map { it.trim() }.filter { isLink(it) }.mapNotNull { parseOne(it) }.toList()
    }

    private fun parseOne(line: String): String? = runCatching {
        when {
            line.startsWith("vmess://") -> vmess(line.removePrefix("vmess://"))
            line.startsWith("vless://") -> vless(line.removePrefix("vless://"))
            line.startsWith("trojan://") -> trojan(line.removePrefix("trojan://"))
            line.startsWith("ss://") -> shadowsocks(line.removePrefix("ss://"))
            line.startsWith("hysteria2://") -> hysteria2(line.removePrefix("hysteria2://"))
            line.startsWith("hy2://") -> hysteria2(line.removePrefix("hy2://"))
            line.startsWith("hysteria://") -> hysteria(line.removePrefix("hysteria://"))
            line.startsWith("tuic://") -> tuic(line.removePrefix("tuic://"))
            else -> null
        }
    }.getOrNull()

    private fun nameAndFragment(raw: String): Pair<String, String> {
        val idx = raw.indexOf('#')
        val body = if (idx >= 0) raw.substring(0, idx) else raw
        val name = if (idx >= 0) URLDecoder.decode(raw.substring(idx + 1), "UTF-8") else ""
        return body to name
    }

    private fun sb(name: String, type: String, server: String, port: Int, extra: List<String>): String {
        val b = StringBuilder()
        b.append("  - name: ").append(Yaml.quote(name)).append("\n")
        b.append("    type: ").append(type).append("\n")
        b.append("    server: ").append(Yaml.quote(server)).append("\n")
        b.append("    port: ").append(port).append("\n")
        extra.forEach { b.append("    ").append(it).append("\n") }
        return b.toString().trimEnd()
    }

    private fun vmess(b64s: String): String? {
        val json = runCatching { JSONObject(String(b64(b64s) ?: return null)) }.getOrNull() ?: return null
        val name = json.optString("ps").ifBlank { json.optString("add") }
        val host = json.optString("add")
        val port = json.optString("port").toIntOrNull() ?: 443
        if (host.isBlank()) return null
        val net = json.optString("net", "tcp").ifBlank { "tcp" }
        val tls = json.optString("tls", "")
        val extra = mutableListOf<String>()
        extra += "uuid: " + Yaml.quote(json.optString("id"))
        extra += "alterId: " + (json.optString("aid", "0").toIntOrNull() ?: 0)
        extra += "cipher: " + (json.optString("scy", "auto").ifBlank { "auto" })
        extra += "udp: true"
        if (tls == "tls" || tls == "reality") {
            extra += "tls: true"
            val sni = json.optString("sni").ifBlank { json.optString("host") }.ifBlank { host }
            extra += "servername: " + Yaml.quote(sni)
        }
        when (net) {
            "ws" -> {
                extra += "network: ws"
                val path = json.optString("path").ifBlank { "/" }
                val h = json.optString("host")
                extra += "ws-opts:"
                extra += "  path: " + Yaml.quote(path)
                if (h.isNotBlank()) {
                    extra += "  headers:"
                    extra += "    Host: " + Yaml.quote(h)
                }
            }
            "grpc" -> {
                extra += "network: grpc"
                extra += "grpc-opts:"
                extra += "  grpc-service-name: " + Yaml.quote(json.optString("path"))
            }
            "h2" -> extra += "network: h2"
            else -> extra += "network: tcp"
        }
        return sb(name, "vmess", host, port, extra)
    }

    private fun vless(rest: String): String? {
        val (body, frag) = nameAndFragment(rest)
        val at = body.lastIndexOf('@')
        if (at < 0) return null
        val uuid = body.substring(0, at)
        val hostport = body.substring(at + 1).substringBefore('/')
        val host = hostport.substringBeforeLast(':')
        val port = hostport.substringAfterLast(':').toIntOrNull() ?: 443
        val query = "https://x/?" + body.substringAfter('?', "")
        val net = q(query, "type").ifBlank { "tcp" }
        val security = q(query, "security")
        val sni = q(query, "sni").ifBlank { q(query, "peer") }
        val extra = mutableListOf<String>()
        extra += "uuid: " + Yaml.quote(uuid)
        extra += "udp: true"
        val flow = q(query, "flow")
        if (flow.isNotBlank()) extra += "flow: " + Yaml.quote(flow)
        if (security == "tls" || security == "reality") {
            extra += "tls: true"
            if (sni.isNotBlank()) extra += "servername: " + Yaml.quote(sni)
            val fp = q(query, "fp")
            if (fp.isNotBlank()) extra += "client-fingerprint: " + Yaml.quote(fp)
            if (security == "reality") {
                extra += "reality-opts:"
                extra += "  public-key: " + Yaml.quote(q(query, "pbk"))
                extra += "  short-id: " + Yaml.quote(q(query, "sid"))
            }
        }
        when (net) {
            "ws" -> {
                extra += "network: ws"
                extra += "ws-opts:"
                extra += "  path: " + Yaml.quote(q(query, "path").ifBlank { "/" })
                val h = q(query, "host")
                if (h.isNotBlank()) {
                    extra += "  headers:"
                    extra += "    Host: " + Yaml.quote(h)
                }
            }
            "grpc" -> {
                extra += "network: grpc"
                extra += "grpc-opts:"
                extra += "  grpc-service-name: " + Yaml.quote(q(query, "serviceName"))
            }
            "tcp" -> extra += "network: tcp"
            else -> extra += "network: " + net
        }
        return sb(frag.ifBlank { host }, "vless", host, port, extra)
    }

    private fun trojan(rest: String): String? {
        val (body, frag) = nameAndFragment(rest)
        val at = body.lastIndexOf('@')
        if (at < 0) return null
        val pwd = URLDecoder.decode(body.substring(0, at), "UTF-8")
        val hostport = body.substring(at + 1).substringBefore('/')
        val host = hostport.substringBeforeLast(':')
        val port = hostport.substringAfterLast(':').toIntOrNull() ?: 443
        val query = "https://x/?" + body.substringAfter('?', "")
        val sni = q(query, "sni").ifBlank { q(query, "peer") }.ifBlank { host }
        val net = q(query, "type").ifBlank { "tcp" }
        val extra = mutableListOf<String>()
        extra += "password: " + Yaml.quote(pwd)
        extra += "udp: true"
        extra += "sni: " + Yaml.quote(sni)
        if (q(query, "allowInsecure") == "1" || q(query, "insecure") == "1") extra += "skip-cert-verify: true"
        when (net) {
            "ws" -> {
                extra += "network: ws"
                extra += "ws-opts:"
                extra += "  path: " + Yaml.quote(q(query, "path").ifBlank { "/" })
                val h = q(query, "host")
                if (h.isNotBlank()) {
                    extra += "  headers:"
                    extra += "    Host: " + Yaml.quote(h)
                }
            }
            "grpc" -> {
                extra += "network: grpc"
                extra += "grpc-opts:"
                extra += "  grpc-service-name: " + Yaml.quote(q(query, "serviceName"))
            }
            else -> {}
        }
        return sb(frag.ifBlank { host }, "trojan", host, port, extra)
    }

    private fun shadowsocks(rest: String): String? {
        val (body, frag) = nameAndFragment(rest)
        var method = ""
        var password = ""
        var host = ""
        var port = 443
        var plugin = ""
        var pluginOpts = ""
        val noQuery = body.substringBefore('?')
        if (body.contains('?')) {
            val query = body.substringAfter('?')
            query.split("&").forEach { kv ->
                when {
                    kv.startsWith("plugin=") -> plugin = URLDecoder.decode(kv.removePrefix("plugin="), "UTF-8")
                    kv.startsWith("plugin-opts=") -> pluginOpts = URLDecoder.decode(kv.removePrefix("plugin-opts="), "UTF-8")
                }
            }
        }
        val at = noQuery.lastIndexOf('@')
        if (at >= 0) {
            val userInfo = noQuery.substring(0, at)
            val hostport = noQuery.substring(at + 1)
            val decoded = String(b64(userInfo) ?: ByteArray(0))
            if (decoded.contains(':')) {
                method = decoded.substringBefore(':')
                password = decoded.substringAfter(':')
            } else {
                method = URLDecoder.decode(userInfo.substringBefore(':'), "UTF-8")
                password = URLDecoder.decode(userInfo.substringAfter(':'), "UTF-8")
            }
            host = hostport.substringBeforeLast(':')
            port = hostport.substringAfterLast(':').toIntOrNull() ?: 443
        } else {
            val decoded = String(b64(noQuery) ?: return null)
            if (!decoded.contains('@')) return null
            val userInfo = decoded.substringBefore('@')
            val hostport = decoded.substringAfter('@')
            method = userInfo.substringBefore(':')
            password = userInfo.substringAfter(':')
            host = hostport.substringBeforeLast(':')
            port = hostport.substringAfterLast(':').toIntOrNull() ?: 443
        }
        if (method.isBlank() || host.isBlank()) return null
        val extra = mutableListOf<String>()
        extra += "cipher: " + Yaml.quote(method)
        extra += "password: " + Yaml.quote(password)
        extra += "udp: true"
        if (plugin.isNotBlank()) {
            if (plugin.contains("obfs")) {
                extra += "plugin: obfs"
                extra += "plugin-opts:"
                extra += "  mode: " + Yaml.quote(if (pluginOpts.contains("obfs=http")) "http" else "tls")
                val h = Regex("obfs-host=([^;]+)").find(pluginOpts)?.groupValues?.get(1) ?: ""
                if (h.isNotBlank()) extra += "  host: " + Yaml.quote(h)
            } else if (plugin.contains("v2ray")) {
                extra += "plugin: v2ray-plugin"
                extra += "plugin-opts:"
                extra += "  mode: websocket"
                extra += "  tls: " + (pluginOpts.contains("tls") && !pluginOpts.contains("tls=false"))
                val h = Regex("host=([^;]+)").find(pluginOpts)?.groupValues?.get(1) ?: ""
                if (h.isNotBlank()) extra += "  host: " + Yaml.quote(h)
                val p = Regex("path=([^;]+)").find(pluginOpts)?.groupValues?.get(1) ?: ""
                if (p.isNotBlank()) extra += "  path: " + Yaml.quote(p)
            }
        }
        return sb(frag.ifBlank { host }, "ss", host, port, extra)
    }

    private fun hysteria2(rest: String): String? {
        val (body, frag) = nameAndFragment(rest)
        val at = body.lastIndexOf('@')
        if (at < 0) return null
        val pwd = URLDecoder.decode(body.substring(0, at), "UTF-8")
        val hostport = body.substring(at + 1).substringBefore('/')
        val host = hostport.substringBeforeLast(':')
        val port = hostport.substringAfterLast(':').toIntOrNull() ?: 443
        val query = "https://x/?" + body.substringAfter('?', "")
        val extra = mutableListOf<String>()
        extra += "password: " + Yaml.quote(pwd)
        val sni = q(query, "sni").ifBlank { q(query, "peer") }
        if (sni.isNotBlank()) extra += "sni: " + Yaml.quote(sni)
        if (q(query, "insecure") == "1") extra += "skip-cert-verify: true"
        val obfs = q(query, "obfs")
        if (obfs.isNotBlank()) {
            extra += "obfs: " + Yaml.quote(obfs)
            extra += "obfs-password: " + Yaml.quote(q(query, "obfs-password"))
        }
        val up = q(query, "up")
        if (up.isNotBlank()) extra += "up: " + Yaml.quote(up)
        val down = q(query, "down")
        if (down.isNotBlank()) extra += "down: " + Yaml.quote(down)
        return sb(frag.ifBlank { host }, "hysteria2", host, port, extra)
    }

    private fun hysteria(rest: String): String? {
        val (body, frag) = nameAndFragment(rest)
        val at = body.lastIndexOf('@')
        if (at < 0) return null
        val hostport = body.substring(at + 1).substringBefore('/')
        val host = hostport.substringBeforeLast(':')
        val port = hostport.substringAfterLast(':').toIntOrNull() ?: 443
        val query = "https://x/?" + body.substringAfter('?', "")
        val extra = mutableListOf<String>()
        val auth = q(query, "auth")
        if (auth.isNotBlank()) extra += "auth-str: " + Yaml.quote(auth)
        val sni = q(query, "peer").ifBlank { q(query, "sni") }
        if (sni.isNotBlank()) extra += "sni: " + Yaml.quote(sni)
        val up = q(query, "upmbps")
        if (up.isNotBlank()) extra += "up: " + Yaml.quote(up + " Mbps")
        val down = q(query, "downmbps")
        if (down.isNotBlank()) extra += "down: " + Yaml.quote(down + " Mbps")
        if (q(query, "insecure") == "1") extra += "skip-cert-verify: true"
        return sb(frag.ifBlank { host }, "hysteria", host, port, extra)
    }

    private fun tuic(rest: String): String? {
        val (body, frag) = nameAndFragment(rest)
        val at = body.lastIndexOf('@')
        if (at < 0) return null
        val userInfo = body.substring(0, at)
        val uuid = URLDecoder.decode(userInfo.substringBefore(':'), "UTF-8")
        val pwd = URLDecoder.decode(userInfo.substringAfter(':', ""), "UTF-8")
        val hostport = body.substring(at + 1).substringBefore('/')
        val host = hostport.substringBeforeLast(':')
        val port = hostport.substringAfterLast(':').toIntOrNull() ?: 443
        val query = "https://x/?" + body.substringAfter('?', "")
        val extra = mutableListOf<String>()
        extra += "uuid: " + Yaml.quote(uuid)
        extra += "password: " + Yaml.quote(pwd)
        extra += "udp-relay-mode: native"
        extra += "congestion-controller: bbr"
        val sni = q(query, "sni")
        if (sni.isNotBlank()) extra += "sni: " + Yaml.quote(sni)
        if (q(query, "allow_insecure") == "1" || q(query, "insecure") == "1") extra += "skip-cert-verify: true"
        return sb(frag.ifBlank { host }, "tuic", host, port, extra)
    }
}
