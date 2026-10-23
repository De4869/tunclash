package com.mihomo.tproxy.data

import java.net.HttpURLConnection
import java.net.URL

data class SubResult(
    val content: String,
    val upload: Long = 0,
    val download: Long = 0,
    val total: Long = 0,
    val expire: Long = 0,
    val title: String = ""
)

object SubFetcher {

    private const val UA = "clash-verge/v2.0.3"

    fun fetch(url: String, timeoutMs: Int = 20000): SubResult {
        var current = url.trim()
        var redirects = 0
        while (redirects < 6) {
            val c = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "*/*")
            }
            try {
                val code = c.responseCode
                if (code in 300..399) {
                    val loc = c.getHeaderField("Location") ?: error("重定向地址缺失")
                    c.disconnect()
                    current = if (loc.startsWith("http")) loc else URL(URL(current), loc).toString()
                    redirects++
                    continue
                }
                if (code !in 200..299) error("HTTP " + code)
                val info = c.getHeaderField("subscription-userinfo") ?: c.getHeaderField("Subscription-Userinfo") ?: ""
                val rawTitle = c.getHeaderField("content-disposition")?.let { cd ->
                    Regex("""filename\*?=(?:UTF-8''|")?([^";]+)""").find(cd)?.groupValues?.get(1)
                } ?: ""
                val title = runCatching { java.net.URLDecoder.decode(rawTitle, "UTF-8") }.getOrDefault(rawTitle)
                val text = c.inputStream.bufferedReader().use { it.readText() }
                var up = 0L; var down = 0L; var total = 0L; var expire = 0L
                info.split(";").forEach { kv ->
                    val k = kv.substringBefore("=").trim().lowercase()
                    val v = kv.substringAfter("=", "0").trim().toLongOrNull() ?: 0L
                    when (k) {
                        "upload" -> up = v
                        "download" -> down = v
                        "total" -> total = v
                        "expire" -> expire = v
                    }
                }
                return SubResult(text, up, down, total, expire, title)
            } finally {
                runCatching { c.disconnect() }
            }
        }
        error("重定向次数过多")
    }
}