package org.zeroshare.app.core.hash

import java.io.InputStream
import java.security.MessageDigest

/**
 * High-performance streaming SHA-256 hasher.
 * Never loads whole files into memory; processes arbitrary streams (10GB+) in chunks.
 */
class Sha256Hasher {

    private val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")

    fun update(data: ByteArray, offset: Int, length: Int) {
        messageDigest.update(data, offset, length)
    }

    fun update(data: ByteArray) {
        messageDigest.update(data)
    }

    fun digest(): ByteArray {
        return messageDigest.digest()
    }

    fun digestHex(): String {
        val hashBytes = digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun reset() {
        messageDigest.reset()
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024 // 64 KB streaming buffer

        /**
         * Computes the SHA-256 checksum of an [InputStream] incrementally without holding the stream in RAM.
         */
        fun computeSha256(
            inputStream: InputStream,
            onProgress: ((bytesProcessed: Long) -> Unit)? = null
        ): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var totalProcessed: Long = 0

            inputStream.use { stream ->
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                    totalProcessed += bytesRead
                    onProgress?.invoke(totalProcessed)
                }
            }

            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        /**
         * Verifies whether [computedHash] matches [expectedHash] case-insensitively.
         */
        fun verifyHash(computedHash: String, expectedHash: String): Boolean {
            return computedHash.trim().equals(expectedHash.trim(), ignoreCase = true)
        }
    }
}
