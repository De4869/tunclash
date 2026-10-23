package com.mihomo.tproxy.core

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

/** Checks GitHub releases for a newer mihomo core and downloads it. */
object CoreUpdater {

    data class Release(val version: String, val url: String, val size: Long)

    private fun open(url: String, useProxy: Boolean, timeout: Int): HttpURLConnection {
        val u = URL(url)
        val c = (if (useProxy)
            u.openConnection(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 7890)))
        else u.openConnection()) as HttpURLConnection
        c.connectTimeout = timeout
        c.readTimeout = timeout
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "MihomoTProxy/1.0")
        return c
    }

    data class AppRelease(val tag: String, val notes: String, val apkUrl: String)

    /** Reads the tag from the /releases/latest redirect (works even when the API is rate-limited). */
    fun tagFromRedirect(repo: String, useProxy: Boolean): String? {
        for (base in listOf("https://github.com/", "https://gh-proxy.com/https://github.com/")) {
            val url = base + repo + "/releases/latest"
            val loc = runCatching {
                val c = open(url, useProxy, 12000)
                try {
                    c.instanceFollowRedirects = false
                    c.responseCode
                    c.getHeaderField("Location")
                } finally { c.disconnect() }
            }.getOrNull() ?: continue
            if (!loc.isNullOrBlank()) {
                val tag = loc.substringAfterLast("/")
                if (tag.startsWith("v")) return tag
            }
        }
        return null
    }

    /** Latest release of an arbitrary repo, including notes and the APK asset link. */
    fun latestRelease(repo: String, useProxy: Boolean): AppRelease? {
        val apis = listOf(
            "https://api.github.com/repos/" + repo + "/releases/latest",
            "https://gh-proxy.com/https://api.github.com/repos/" + repo + "/releases/latest"
        )
        for (api in apis) {
            val body = runCatching {
                val c = open(api, useProxy, 12000)
                try {
                    if (c.responseCode !in 200..299) null else c.inputStream.bufferedReader().readText()
                } finally { c.disconnect() }
            }.getOrNull() ?: continue
            val j = runCatching { JSONObject(body) }.getOrNull() ?: continue
            val tag = j.optString("tag_name")
            if (tag.isBlank()) continue
            val notes = j.optString("body", "")
            var apk = ""
            val assets = j.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.optJSONObject(i) ?: continue
                    val name = a.optString("name")
                    if (name.endsWith(".apk", true)) { apk = a.optString("browser_download_url"); break }
                }
            }
            return AppRelease(tag, notes, apk)
        }
        return null
    }

    /** Latest release tag of an arbitrary repo (used by the app's own update check). */
    fun latestTag(repo: String, useProxy: Boolean): String? {
        val apis = listOf(
            "https://api.github.com/repos/" + repo + "/releases/latest",
            "https://gh-proxy.com/https://api.github.com/repos/" + repo + "/releases/latest"
        )
        for (api in apis) {
            val body = runCatching {
                val c = open(api, useProxy, 12000)
                try {
                    if (c.responseCode !in 200..299) null else c.inputStream.bufferedReader().readText()
                } finally { c.disconnect() }
            }.getOrNull() ?: continue
            val tag = runCatching { JSONObject(body).optString("tag_name") }.getOrNull()
            if (!tag.isNullOrBlank()) return tag
        }
        return null
    }

    fun check(useProxy: Boolean): Release? {
        val apis = listOf(
            "https://api.github.com/repos/MetaCubeX/mihomo/releases/latest",
            "https://gh-proxy.com/https://api.github.com/repos/MetaCubeX/mihomo/releases/latest"
        )
        for (api in apis) {
            val body = runCatching {
                val c = open(api, useProxy, 12000)
                try {
                    if (c.responseCode !in 200..299) null else c.inputStream.bufferedReader().readText()
                } finally { c.disconnect() }
            }.getOrNull() ?: continue
            val j = runCatching { JSONObject(body) }.getOrNull() ?: continue
            val tag = j.optString("tag_name")
            val assets = j.optJSONArray("assets") ?: continue
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                val name = a.optString("name")
                if (name.startsWith("mihomo-android-arm64-v8-") && name.endsWith(".gz")) {
                    return Release(tag, a.optString("browser_download_url"), a.optLong("size", 0))
                }
            }
        }
        return null
    }

    /** Downloads the .gz asset, falling back to a mirror prefix. */
    fun download(url: String, useProxy: Boolean, onProgress: (Int) -> Unit): ByteArray? {
        val candidates = listOf(url, "https://gh-proxy.com/" + url)
        for (candidate in candidates) {
            val bytes = runCatching {
                val c = open(candidate, useProxy, 20000)
                c.readTimeout = 30000
                try {
                    if (c.responseCode !in 200..299) return@runCatching null
                    val total = c.contentLength.toLong()
                    val out = ByteArrayOutputStream()
                    c.inputStream.use { input ->
                        val buf = ByteArray(64 * 1024)
                        var sum = 0L
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            out.write(buf, 0, read)
                            sum += read
                            if (total > 0) onProgress((sum * 100 / total).toInt().coerceIn(0, 100))
                        }
                    }
                    out.toByteArray()
                } finally { c.disconnect() }
            }.getOrNull()
            if (bytes != null && bytes.isNotEmpty()) return bytes
        }
        return null
    }

    fun isNewer(latest: String, current: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }
        val a = parts(latest)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
