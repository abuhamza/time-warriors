package com.timewgui.domain.idle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Detects system idle time on macOS by parsing `HIDIdleTime` from `ioreg`.
 * HIDIdleTime is reported in nanoseconds.
 */
class IdleDetector {

    suspend fun getIdleSeconds(): Long = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("ioreg", "-c", "IOHIDSystem", "-d", "4")
                .redirectErrorStream(true)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            process.waitFor()

            parseIdleNanos(output) / 1_000_000_000L
        } catch (_: Exception) {
            0L
        }
    }

    private fun parseIdleNanos(output: String): Long {
        // Matches: "HIDIdleTime" = 1234567890
        val regex = Regex(""""HIDIdleTime"\s*=\s*(\d+)""")
        val match = regex.find(output) ?: return 0L
        return match.groupValues[1].toLongOrNull() ?: 0L
    }
}
