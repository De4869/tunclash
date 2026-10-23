package com.mihomo.tproxy

import com.mihomo.tproxy.core.ConfigComposer
import com.mihomo.tproxy.core.ShareLinks
import com.mihomo.tproxy.model.ProxySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64

class ConfigComposerTest {

    private val settings = ProxySettings(secret = "testsecret123")

    private val clashSub = """
proxies:
  - name: "HK 香港 01"
    type: ss
    server: hk1.example.com
    port: 443
    cipher: aes-256-gcm
    password: "pass1"
    udp: true
  - name: "JP 日本 01"
    type: vmess
    server: jp1.example.com
    port: 443
    uuid: 11111111-2222-3333-4444-555555555555
    alterId: 0
    cipher: auto
    tls: true
    network: ws
    ws-opts:
      path: "/ws"
      headers:
        Host: jp1.example.com
proxy-groups:
  - name: "PROXY"
    type: select
    proxies:
      - "HK 香港 01"
      - "JP 日本 01"
rules:
  - GEOIP,CN,DIRECT
  - MATCH,PROXY
""".trimIndent()

    private fun vmessLink(name: String, host: String, port: Int): String {
        val json = """{"v":"2","ps":"$name","add":"$host","port":"$port","id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","aid":"0","scy":"auto","net":"ws","type":"none","host":"$host","path":"/ray","tls":"tls","sni":"$host"}"""
        return "vmess://" + Base64.getEncoder().encodeToString(json.toByteArray())
    }

    private val links = listOf(
        vmessLink("🇸🇬 新加坡 01", "sg1.example.com", 443),
        "vless://aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee@us1.example.com:443?encryption=none&security=reality&sni=www.microsoft.com&fp=chrome&pbk=abc123&sid=01&type=tcp&flow=xtls-rprx-vision#%F0%9F%87%BA%F0%9F%87%B8%20%E7%BE%8E%E5%9B%BD%2001",
        "trojan://mypassword@hk2.example.com:443?sni=hk2.example.com&type=ws&path=%2Ftrojan#HK2",
        "ss://" + Base64.getEncoder().encodeToString("aes-128-gcm:sspass".toByteArray()) + "@jp2.example.com:8388#JP2",
        "hysteria2://hypass@hy.example.com:443?sni=hy.example.com&insecure=1#HY2"
    ).joinToString("\n")

    @Test
    fun clashConfigKeepsProviderRulesAndOverridesRuntime() {
        val out = ConfigComposer.compose(clashSub, settings)
        assertTrue(out.contains("tun:"))
        assertTrue(out.contains("external-controller: 127.0.0.1:9090"))
        assertTrue(out.contains("GEOIP,CN,DIRECT"))
        assertTrue(out.contains("MATCH,PROXY"))
        assertEquals(1, Regex("(?m)^rules:").findAll(out).count())
        assertEquals(2, ConfigComposer.nodeCount(clashSub))
        dump("01-clash.yaml", out)
    }

    @Test
    fun shareLinksProduceUsableConfig() {
        val out = ConfigComposer.compose(links, settings)
        assertTrue(out.contains("proxy-groups:"))
        assertTrue(out.contains("新加坡"))
        assertTrue(out.contains("美国"))
        assertTrue(out.contains("MATCH," + ConfigComposer.TARGET))
        assertEquals(5, ConfigComposer.nodeCount(links))
        dump("02-links.yaml", out)
    }

    @Test
    fun mergeDeduplicatesAndGroupsByRegion() {
        val merged = ConfigComposer.merge(listOf(clashSub, links), settings)
        assertEquals(7, ConfigComposer.nodeCount(merged))
        val again = ConfigComposer.merge(listOf(clashSub, links, links), settings)
        assertEquals(7, ConfigComposer.nodeCount(again))
        assertTrue(again.contains("🇸🇬 新加坡") || again.contains("新加坡"))
        dump("03-merge.yaml", merged)
    }

    @Test
    fun perAppRulesAreInjected() {
        val s = settings.copy(perAppEnabled = true, perAppPackages = setOf("com.tencent.mm", "com.eg.android.AlipayGphone"))
        val out = ConfigComposer.compose(clashSub, s)
        assertTrue(out.contains("PROCESS-NAME,com.tencent.mm,DIRECT"))
        assertTrue(out.contains("PROCESS-NAME,com.eg.android.AlipayGphone,DIRECT"))
        dump("04-perapp.yaml", out)
    }

    @Test
    fun whitelistModeSwitchesDefault() {
        val s = settings.copy(perAppEnabled = true, perAppWhitelist = true, perAppPackages = setOf("com.tencent.mm"))
        val out = ConfigComposer.compose(clashSub, s)
        assertTrue(out.contains("PROCESS-NAME,com.tencent.mm," + ConfigComposer.TARGET))
        assertTrue(out.contains("MATCH,DIRECT"))
        dump("05-whitelist.yaml", out)
    }

    @Test
    fun adBlockRule() {
        val out = ConfigComposer.compose(clashSub, settings.copy(adBlock = true))
        assertTrue(out.contains("GEOSITE,category-ads-all,REJECT"))
        dump("06-adblock.yaml", out)
    }

    @Test
    fun expandBase64Subscription() {
        val raw = Base64.getEncoder().encodeToString(links.toByteArray())
        val expanded = ShareLinks.expand(raw)
        assertEquals(5, ConfigComposer.nodeCount(expanded))
    }

    @Test
    fun flowStyleSubscription() {
        val flow = """
proxies:
    - { name: '🇭🇰香港 01 | 高级专线', type: vless, server: hk1.example.com, port: 443, uuid: 1111, udp: true, tls: true, servername: update.microsoft.com }
    - { name: '🇯🇵日本 01 | 高级专线', type: trojan, server: jp1.example.com, port: 443, password: pwd, sni: jp1.example.com }
    - { name: '🇸🇬新加坡 01', type: ss, server: sg1.example.com, port: 8388, cipher: aes-128-gcm, password: p2 }
proxy-groups:
    - { name: '🚀 节点选择', type: select, proxies: ['🇭🇰香港 01 | 高级专线', '🇯🇵日本 01 | 高级专线', '🇸🇬新加坡 01'] }
rules:
    - MATCH,🚀 节点选择
""".trimIndent()
        assertEquals(3, ConfigComposer.nodeCount(flow))
        val out = ConfigComposer.compose(flow, settings)
        assertTrue(out.contains("hk1.example.com"))
        assertTrue(out.contains("name: '🚀 节点选择'"))
        assertTrue(out.contains("MATCH,🚀 节点选择"))
        dump("07-flow.yaml", out)
    }

    @Test
    fun brokenEscapeNamesAreNormalized() {
        val cfg = """
proxies:
    - { name: "US\\_节点 01", type: ss, server: a.example.com, port: 443, cipher: aes-128-gcm, password: p }
    - { name: "US\\_节点 02", type: ss, server: b.example.com, port: 443, cipher: aes-128-gcm, password: p }
rules:
    - MATCH,DIRECT
""".trimIndent()
        assertEquals(2, ConfigComposer.nodeCount(cfg))
        val out = ConfigComposer.merge(listOf(cfg), settings)
        assertTrue(!out.contains("\\_"))
        assertTrue(out.contains("US_节点 01"))
        dump("08-escape.yaml", out)
    }

    @Test
    fun debugEachLink() {
        links.lines().forEachIndexed { i, l ->
            val n = com.mihomo.tproxy.core.ShareLinks.toBlocks(l).size
            println("LINK" + i + " -> " + n + " : " + l.take(48))
        }
    }

    private fun dump(name: String, content: String) {
        val dir = File("build/test-configs").apply { mkdirs() }
        File(dir, name).writeText(content)
    }
}