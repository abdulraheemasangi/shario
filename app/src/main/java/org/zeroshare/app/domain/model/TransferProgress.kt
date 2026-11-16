package org.zeroshare.app.domain.model

enum class TransferStatus {
    IDLE,
    CONNECTING,
    HANDSHAKE,
    TRANSFERRING,
    VERIFYING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class FileTransferItemState(
    val fileId: Int,
    val name: String,
    val size: Long,
    val bytesTransferred: Long = 0,
    val sha256Expected: String,
    val sha256Actual: String? = null,
    val isVerified: Boolean = false,
    val isFailed: Boolean = false,
    val currentChunkIndex: Int = 0,
    val totalChunks: Int = 0
)

data class TransferSessionState(
    val transferId: String = "",
    val status: TransferStatus = TransferStatus.IDLE,
    val isSender: Boolean = true,
    val remoteDeviceName: String = "",
    val totalFiles: Int = 0,
    val currentFileIndex: Int = 0,
    val currentFileName: String = "",
    val totalBytes: Long = 0,
    val bytesTransferred: Long = 0,
    val speedBytesPerSec: Long = 0,
    val etaFormatted: String = "--:--",
    val fileStates: List<FileTransferItemState> = emptyList(),
    val errorMessage: String? = null
)
