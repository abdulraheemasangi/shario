package org.zeroshare.app.data.engine

import org.json.JSONObject
import org.zeroshare.app.core.constants.ProtocolConstants
import java.io.File

data class ResumeOffset(
    val fileId: Int,
    val startByteOffset: Long,
    val nextChunkIndex: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("fileId", fileId)
        put("offset", startByteOffset)
        put("chunkIndex", nextChunkIndex)
    }

    companion object {
        fun fromJson(json: JSONObject): ResumeOffset = ResumeOffset(
            fileId = json.getInt("fileId"),
            startByteOffset = json.getLong("offset"),
            nextChunkIndex = json.getInt("chunkIndex")
        )
    }
}

class ChunkResumeManager {

    /**
     * Determines the resume offset for an existing `.part` file.
     * Aligns to chunk boundaries (1MB) to ensure clean AES-GCM tag verification.
     */
    fun calculateResumeOffset(fileId: Int, partFile: File, expectedFileSize: Long): ResumeOffset {
        if (!partFile.exists() || partFile.length() <= 0L) {
            return ResumeOffset(fileId, 0L, 0)
        }

        val currentLength = partFile.length()
        if (currentLength >= expectedFileSize) {
            // Already received full size, start from 0 or verify
            return ResumeOffset(fileId, 0L, 0)
        }

        // Align to the last complete chunk boundary
        val completedChunks = (currentLength / ProtocolConstants.CHUNK_SIZE_BYTES).toInt()
        val alignedOffset = completedChunks.toLong() * ProtocolConstants.CHUNK_SIZE_BYTES

        return ResumeOffset(
            fileId = fileId,
            startByteOffset = alignedOffset,
            nextChunkIndex = completedChunks
        )
    }
}
