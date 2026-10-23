package com.mihomo.tproxy.core

import android.util.Log
import java.util.concurrent.TimeUnit

data class ShellResult(val code: Int, val out: String, val err: String) {
    val ok: Boolean get() = code == 0
    val message: String get() = (out.trim() + " " + err.trim()).trim()
}

/**
 * Thin wrapper around KernelSU / Magisk su.
 *
 * Note: on SukiSU + SUSFS setups /system/bin/su may be hidden from the app mount
 * namespace, so we always try the canonical path first (the kernel hook resolves
 * it by path string) and only then fall back to PATH lookup.
 */
object RootShell {

    private const val TAG = "MihomoRoot"

    private val candidates = listOf("/system/bin/su", "/debug_ramdisk/su", "/sbin/su", "su")

    @Volatile
    private var cached: Boolean? = null

    @Volatile
    private var working: String? = null

    fun exec(cmd: String, timeoutSec: Long = 20, stdin: ByteArray? = null): ShellResult {
        working?.let { w ->
            val r = run(w, cmd, timeoutSec, stdin)
            if (r.code != -127) return r
            working = null
        }
        var last = ShellResult(-1, "", "su not found")
        for (cand in candidates) {
            val r = run(cand, cmd, timeoutSec, stdin)
            if (r.code != -127) {
                working = cand
                return r
            }
            last = r
        }
        Log.e(TAG, "no usable su. last=" + last.err)
        return last
    }

    private fun run(su: String, cmd: String, timeoutSec: Long, stdin: ByteArray?): ShellResult {
        var process: Process? = null
        return try {
            val pb = ProcessBuilder(su, "-c", cmd)
            pb.redirectErrorStream(false)
            process = pb.start()
            if (stdin != null) {
                process.outputStream.use { it.write(stdin); it.flush() }
            } else {
                process.outputStream.close()
            }
            val out = StringBuilder()
            val err = StringBuilder()
            val tOut = Thread {
                runCatching { process.inputStream.bufferedReader().forEachLine { out.append(it).append('\n') } }
            }
            val tErr = Thread {
                runCatching { process.errorStream.bufferedReader().forEachLine { err.append(it).append('\n') } }
            }
            tOut.start(); tErr.start()
            val done = process.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!done) {
                runCatching { process.destroyForcibly() }
                return ShellResult(-1, out.toString(), err.toString() + " (timeout)")
            }
            tOut.join(800); tErr.join(800)
            val code = process.exitValue()
            if (code != 0) Log.w(TAG, "su[" + su + "] exit=" + code + " err=" + err.toString().trim())
            ShellResult(code, out.toString(), err.toString())
        } catch (e: Exception) {
            runCatching { process?.destroyForcibly() }
            // -127 marks "binary could not be executed", so we try the next candidate
            ShellResult(-127, "", (e.message ?: e.javaClass.simpleName))
        }
    }

    fun hasRoot(): Boolean {
        cached?.let { return it }
        val r = exec("id", 10)
        val ok = r.ok && r.out.contains("uid=0")
        Log.i(TAG, "hasRoot=" + ok + " via=" + working + " out=" + r.out.trim() + " err=" + r.err.trim())
        cached = ok
        return ok
    }

    fun invalidate() {
        cached = null
        working = null
    }
}
