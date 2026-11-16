package org.zeroshare.app.core.constants

object ProtocolConstants {
    const val PROTOCOL_VERSION: Int = 1
    const val DEFAULT_PORT: Int = 8988
    const val DEFAULT_NSD_SERVICE_TYPE: String = "_shario._tcp."

    // Protocol Frame Header Magic Byte: 'S' (Shario)
    const val MAGIC_BYTE: Byte = 0x53

    // Packet Types
    const val TYPE_HANDSHAKE: Byte = 0x01
    const val TYPE_MANIFEST: Byte = 0x02
    const val TYPE_RESUME_REQUEST: Byte = 0x03
    const val TYPE_RESUME_RESPONSE: Byte = 0x04
    const val TYPE_CHUNK_DATA: Byte = 0x05
    const val TYPE_FILE_COMPLETE: Byte = 0x06
    const val TYPE_VERIFICATION_ACK: Byte = 0x07
    const val TYPE_CANCEL: Byte = 0x08
    const val TYPE_HEARTBEAT: Byte = 0x09

    // Chunk size: 1 MB (1024 * 1024) - optimal for high throughput and resume granularity
    const val CHUNK_SIZE_BYTES: Int = 1024 * 1024 // 1 MB
    const val IO_BUFFER_SIZE_BYTES: Int = 64 * 1024 // 64 KB for underlying stream pumps

    // Verification Statuses
    const val STATUS_VERIFIED = "VERIFIED"
    const val STATUS_MISMATCH = "MISMATCH"
}
