package org.zeroshare.app.domain.model

data class TransferRecord(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val sha256: String,
    val isVerified: Boolean,
    val isIncoming: Boolean,
    val peerName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val localUriString: String? = null
)
