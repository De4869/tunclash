package com.mihomo.tproxy.model

/** A subscription / configuration source owned by the user. */
data class Profile(
    val id: String,
    val name: String,
    val kind: String,          // url | links | yaml | merge
    val url: String = "",
    val content: String = "",
    val updatedAt: Long = 0L,
    val upload: Long = 0L,
    val download: Long = 0L,
    val total: Long = 0L,
    val expire: Long = 0L,
    val nodeCount: Int = 0
) {
    val isMerge: Boolean get() = kind == "merge"
}

data class RuleSource(
    val name: String,
    val url: String,
    val behavior: String = "domain",
    val format: String = "yaml"
)

data class ProxySettings(
    val themeMode: String = "system",   // system | dark | light
    val mode: String = "rule",              // rule | global | direct
    val rulePreset: String = "smart",       // smart | global | direct
    val dnsPreset: String = "cn",           // cn | google | cf | custom
    val customDns: String = "",
    val ipv6: Boolean = false,
    val tunStack: String = "mixed",         // mixed | gvisor | system
    val mtu: Int = 9000,
    val logLevel: String = "info",
    val perAppEnabled: Boolean = false,
    val perAppWhitelist: Boolean = false,
    val perAppPackages: Set<String> = emptySet(),
    val perAppUids: Set<Int> = emptySet(),
    val adBlock: Boolean = false,
    val adSources: List<RuleSource> = emptyList(),
    val bootStart: Boolean = false,
    val secret: String = "mihomo-tproxy"
)

data class NodeInfo(
    val name: String,
    val type: String,
    val delay: Int = 0,
    val alive: Boolean = true,
    val udp: Boolean = false,
    val group: String = ""
)

data class GroupInfo(
    val name: String,
    val type: String,
    val now: String = "",
    val nodes: List<String> = emptyList()
)

data class TrafficSample(val up: Long = 0, val down: Long = 0)