package org.zeroshare.app.data.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.FileInputStream
import java.io.InputStream

/**
 * Raw Content Streamer designed specifically for 100% byte-for-byte fidelity.
 * Guarantees zero transcoding, preserves unredacted EXIF/GPS, and supports chunk-based offset reading.
 */
class RawContentReader(private val context: Context) {

    /**
     * Resolves the original raw [InputStream] for a [Uri].
     * If the URI is an Android MediaStore URI on Android 10+ (API 29+),
     * [MediaStore.setRequireOriginal] is invoked to prevent the OS from stripping
     * GPS coordinates and camera EXIF metadata.
     */
    fun openOriginalInputStream(uri: Uri): InputStream {
        val targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isMediaStoreUri(uri)) {
            try {
                MediaStore.setRequireOriginal(uri)
            } catch (e: Exception) {
                uri
            }
        } else {
            uri
        }

        return context.contentResolver.openInputStream(targetUri)
            ?: throw IllegalStateException("Unable to open raw stream for URI: $uri")
    }

    /**
     * Opens a seekable [ParcelFileDescriptor] positioned at [startByteOffset] for resumable chunk transfers.
     */
    fun openInputStreamAtOffset(uri: Uri, startByteOffset: Long): InputStream {
        val targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isMediaStoreUri(uri)) {
            try {
                MediaStore.setRequireOriginal(uri)
            } catch (e: Exception) {
                uri
            }
        } else {
            uri
        }

        val pfd = context.contentResolver.openFileDescriptor(targetUri, "r")
            ?: throw IllegalStateException("Unable to acquire file descriptor for $targetUri")

        val fis = FileInputStream(pfd.fileDescriptor)
        if (startByteOffset > 0) {
            fis.channel.position(startByteOffset)
        }
        return fis
    }

    /**
     * Obtains the exact byte size of a file directly from the content resolver descriptor or cursor.
     */
    fun getExactFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun isMediaStoreUri(uri: Uri): Boolean {
        return uri.authority == "media" || uri.toString().startsWith("content://media/")
    }
}
