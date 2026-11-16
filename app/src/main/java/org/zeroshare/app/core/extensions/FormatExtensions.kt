package org.zeroshare.app.core.extensions

import java.text.DecimalFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object FormatExtensions {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.lastIndex)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[index]
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        val mbPerSec = bytesPerSecond.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.2f MB/s", mbPerSec)
    }

    fun formatEta(remainingBytes: Long, bytesPerSecond: Long): String {
        if (bytesPerSecond <= 0 || remainingBytes <= 0) return "--:--"
        val seconds = remainingBytes / bytesPerSecond
        val hours = TimeUnit.SECONDS.toHours(seconds)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, secs)
        }
    }
}
