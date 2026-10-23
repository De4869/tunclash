package com.mihomo.tproxy.core

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Mihomo external-controller client. Everything runs on loopback, so no auth
 * prompt is needed beyond the bearer secret written into the config.
 */
class MihomoApi(var secret: String = "mihomo-tproxy", var port: Int = 9090) {

    private val base get() = "http://127.0.0.1:$port"

    private fun open(path: String, method: String = "GET", timeout: Int = 4000): HttpURLConnection {
        val c = URL(base + path).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = timeout
        c.readTimeout = timeout
        c.setRequestProperty("Authorization", "Bearer " + secret)
        if (method != "GET") {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
        }
        return c
    }

    fun get(path: String, timeout: Int = 4000): JSONObject? = runCatching {
        val c = open(path, "GET", timeout)
        try {
            if (c.responseCode !in 200..299) return null
            val text = c.inputStream.bufferedReader().readText()
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally { c.disconnect() }
    }.getOrNull()

    fun request(method: String, path: String, body: String?, timeout: Int = 4000): Boolean = runCatching {
        val c = open(path, method, timeout)
        try {
            if (body != null) c.outputStream.use { it.write(body.toByteArray()) }
            c.responseCode in 200..299
        } finally { c.disconnect() }
    }.getOrDefault(false)

    fun version(): String? = get("/version")?.optString("version")?.takeIf { it.isNotBlank() }

    fun configs(): JSONObject? = get("/configs")

    fun proxies(): JSONObject? = get("/proxies")

    fun connections(): JSONObject? = get("/connections", 6000)

    fun setMode(mode: String): Boolean =
        request("PATCH", "/configs", "{\"mode\":\"" + mode + "\"}")

    fun select(group: String, node: String): Boolean =
        request("PUT", "/proxies/" + enc(group), "{\"name\":\"" + esc(node) + "\"}")

    fun delay(node: String, url: String = "http://www.gstatic.com/generate_204", timeoutMs: Int = 3000): Int {
        val j = get("/proxies/" + enc(node) + "/delay?timeout=" + timeoutMs + "&url=" + enc(url), timeoutMs + 2500)
        return j?.optInt("delay", 0) ?: 0
    }

    /** Server side parallel latency test for every member of a group. */
    fun groupDelay(group: String, url: String = "http://www.gstatic.com/generate_204", timeoutMs: Int = 3000): org.json.JSONObject? =
        get("/group/" + enc(group) + "/delay?url=" + enc(url) + "&timeout=" + timeoutMs, timeoutMs + 20000)

    fun reloadConfig(path: String): Boolean =
        request("PUT", "/configs?force=true", "{\"path\":\"" + esc(path) + "\"}", 15000)

    /** Streams newline delimited JSON from /traffic, /logs or /connections. */
    fun stream(path: String, onLine: (String) -> Boolean) {
        val c = open(path, "GET", 5000)
        c.readTimeout = 0
        try {
            if (c.responseCode !in 200..299) return
            BufferedReader(InputStreamReader(c.inputStream)).use { r ->
                while (true) {
                    val line = r.readLine() ?: break
                    if (!onLine(line)) break
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { c.disconnect() }
        }
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}