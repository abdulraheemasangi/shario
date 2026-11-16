package org.zeroshare.app.data.engine

import org.zeroshare.app.core.extensions.FormatExtensions
import java.util.ArrayDeque

/**
 * Sliding-window transfer speedometer and ETA estimator.
 */
class SpeedCalculator(private val windowDurationMs: Long = 2000L) {

    private data class SpeedSample(val timestampMs: Long, val totalBytes: Long)

    private val samples = ArrayDeque<SpeedSample>()
    private var lastComputedSpeed: Long = 0L

    @Synchronized
    fun update(totalBytesTransferred: Long): Long {
        val now = System.currentTimeMillis()
        samples.addLast(SpeedSample(now, totalBytesTransferred))

        // Evict samples older than window
        while (samples.size > 2 && (now - samples.first.timestampMs) > windowDurationMs) {
            samples.removeFirst()
        }

        if (samples.size >= 2) {
            val oldest = samples.first
            val newest = samples.last
            val timeDiffSec = (newest.timestampMs - oldest.timestampMs) / 1000.0
            val bytesDiff = newest.totalBytes - oldest.totalBytes

            if (timeDiffSec > 0 && bytesDiff >= 0) {
                lastComputedSpeed = (bytesDiff / timeDiffSec).toLong()
            }
        }
        return lastComputedSpeed
    }

    fun getSpeedBytesPerSec(): Long = lastComputedSpeed

    fun getEstimatedEta(remainingBytes: Long): String {
        return FormatExtensions.formatEta(remainingBytes, lastComputedSpeed)
    }

    @Synchronized
    fun reset() {
        samples.clear()
        lastComputedSpeed = 0L
    }
}
