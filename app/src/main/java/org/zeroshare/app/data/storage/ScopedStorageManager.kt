package org.zeroshare.app.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.zeroshare.app.domain.model.FileMetadata
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * Handles incoming file reception, partial chunk buffering, and zero-loss Scoped Storage publication.
 */
class ScopedStorageManager(private val context: Context) {

    private val stagingDirectory: File
        get() {
            val dir = File(context.cacheDir, "transfer_staging")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    /**
     * Resolves the temporary `.part` file used to store incoming chunks before verification.
     */
    fun getStagingPartFile(fileId: Int, fileName: String): File {
        return File(stagingDirectory, "${fileId}_${fileName}.part")
    }

    /**
     * Obtains the current size of an existing `.part` file to resume transfers.
     */
    fun getExistingPartFileSize(fileId: Int, fileName: String): Long {
        val partFile = getStagingPartFile(fileId, fileName)
        return if (partFile.exists()) partFile.length() else 0L
    }

    /**
     * Appends or writes a chunk directly at [byteOffset] into the staging `.part` file using RandomAccessFile.
     */
    fun writeChunkAtOffset(
        fileId: Int,
        fileName: String,
        byteOffset: Long,
        chunkData: ByteArray,
        length: Int
    ) {
        val partFile = getStagingPartFile(fileId, fileName)
        RandomAccessFile(partFile, "rw").use { raf ->
            raf.seek(byteOffset)
            raf.write(chunkData, 0, length)
        }
    }

    /**
     * Publishes a verified staging file into Android's public Scoped Storage (Downloads/Shario, Pictures, Movies, or Music)
     * without any re-encoding or metadata redaction.
     */
    fun finalizeAndPublishFile(metadata: FileMetadata): Uri {
        val partFile = getStagingPartFile(metadata.fileId, metadata.name)
        if (!partFile.exists()) {
            throw IllegalStateException("Staging part file not found for ${metadata.name}")
        }

        val destinationUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishToMediaStoreScoped(metadata, partFile)
        } else {
            publishToLegacyStorage(metadata, partFile)
        }

        // Clean up staging file
        partFile.delete()
        return destinationUri
    }

    private fun publishToMediaStoreScoped(metadata: FileMetadata, partFile: File): Uri {
        val (collectionUri, relativeSubdir) = determineMediaCollection(metadata.mimeType)

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, metadata.name)
            put(MediaStore.MediaColumns.MIME_TYPE, metadata.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeSubdir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (metadata.lastModified > 0) {
                put(MediaStore.MediaColumns.DATE_MODIFIED, metadata.lastModified / 1000)
            }
        }

        val uri = context.contentResolver.insert(collectionUri, values)
            ?: throw IllegalStateException("Failed to create MediaStore entry for ${metadata.name}")

        context.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(partFile).use { `in` ->
                `in`.copyTo(out, bufferSize = 64 * 1024)
            }
        } ?: throw IllegalStateException("Unable to open output stream for $uri")

        // Release IS_PENDING lock
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)

        return uri
    }

    private fun publishToLegacyStorage(metadata: FileMetadata, partFile: File): Uri {
        val sharioDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Shario")
        if (!sharioDir.exists()) sharioDir.mkdirs()

        val destFile = File(sharioDir, metadata.name)
        FileInputStream(partFile).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        if (metadata.lastModified > 0) {
            destFile.setLastModified(metadata.lastModified)
        }
        return Uri.fromFile(destFile)
    }

    private fun determineMediaCollection(mimeType: String): Pair<Uri, String> {
        return when {
            mimeType.startsWith("image/") -> {
                Pair(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "${Environment.DIRECTORY_PICTURES}/Shario")
            }
            mimeType.startsWith("video/") -> {
                Pair(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "${Environment.DIRECTORY_MOVIES}/Shario")
            }
            mimeType.startsWith("audio/") -> {
                Pair(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "${Environment.DIRECTORY_MUSIC}/Shario")
            }
            else -> {
                Pair(MediaStore.Downloads.EXTERNAL_CONTENT_URI, "${Environment.DIRECTORY_DOWNLOADS}/Shario")
            }
        }
    }
}
