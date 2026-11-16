package org.zeroshare.app.domain.model

import org.json.JSONArray
import org.json.JSONObject
import org.zeroshare.app.core.constants.ProtocolConstants

data class FileMetadata(
    val fileId: Int,
    val name: String,
    val size: Long,
    val mimeType: String,
    val sha256: String,
    val chunkSize: Int = ProtocolConstants.CHUNK_SIZE_BYTES,
    val totalChunks: Int = if (size == 0L) 0 else ((size + ProtocolConstants.CHUNK_SIZE_BYTES - 1) / ProtocolConstants.CHUNK_SIZE_BYTES).toInt(),
    val lastModified: Long = System.currentTimeMillis(),
    val isAppBundle: Boolean = false,
    val relativePath: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("fileId", fileId)
        put("name", name)
        put("size", size)
        put("mimeType", mimeType)
        put("sha256", sha256)
        put("chunkSize", chunkSize)
        put("totalChunks", totalChunks)
        put("lastModified", lastModified)
        put("isAppBundle", isAppBundle)
        put("relativePath", relativePath ?: "")
    }

    companion object {
        fun fromJson(json: JSONObject): FileMetadata = FileMetadata(
            fileId = json.getInt("fileId"),
            name = json.getString("name"),
            size = json.getLong("size"),
            mimeType = json.getString("mimeType"),
            sha256 = json.getString("sha256"),
            chunkSize = json.optInt("chunkSize", ProtocolConstants.CHUNK_SIZE_BYTES),
            totalChunks = json.optInt("totalChunks", 0),
            lastModified = json.optLong("lastModified", System.currentTimeMillis()),
            isAppBundle = json.optBoolean("isAppBundle", false),
            relativePath = json.optString("relativePath").takeIf { it.isNotEmpty() }
        )
    }
}

data class TransferManifest(
    val transferId: String,
    val senderDeviceName: String,
    val totalFiles: Int,
    val totalBytes: Long,
    val files: List<FileMetadata>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("transferId", transferId)
        put("senderDeviceName", senderDeviceName)
        put("totalFiles", totalFiles)
        put("totalBytes", totalBytes)
        val filesArray = JSONArray()
        files.forEach { filesArray.put(it.toJson()) }
        put("files", filesArray)
    }

    companion object {
        fun fromJson(json: JSONObject): TransferManifest {
            val filesArray = json.getJSONArray("files")
            val fileList = mutableListOf<FileMetadata>()
            for (i in 0 until filesArray.length()) {
                fileList.add(FileMetadata.fromJson(filesArray.getJSONObject(i)))
            }
            return TransferManifest(
                transferId = json.getString("transferId"),
                senderDeviceName = json.optString("senderDeviceName", "Unknown Sender"),
                totalFiles = json.getInt("totalFiles"),
                totalBytes = json.getLong("totalBytes"),
                files = fileList
            )
        }
    }
}
