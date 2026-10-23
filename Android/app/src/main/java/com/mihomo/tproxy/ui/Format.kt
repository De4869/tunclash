package com.mihomo.tproxy.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun fmtBytes(v: Long): String {
    if (v < 1024) return v.toString() + " B"
    val kb = v / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}

fun fmtSpeed(v: Long): String = fmtBytes(v) + "/s"

fun fmtUptime(startedAt: Long): String {
    if (startedAt <= 0) return "--:--:--"
    val s = ((System.currentTimeMillis() - startedAt) / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

fun fmtDate(ts: Long): String {
    if (ts <= 0) return "—"
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ts * 1000))
}
fun prettyName(s: String): String {
    if (!s.contains("%")) return s
    return runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)
}
