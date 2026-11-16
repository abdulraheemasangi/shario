package org.zeroshare.app.data.engine

import org.zeroshare.app.core.constants.ProtocolConstants
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException

data class ProtocolPacket(
    val type: Byte,
    val fileId: Int = 0,
    val chunkIndex: Int = 0,
    val payload: ByteArray = ByteArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProtocolPacket
        if (type != other.type) return false
        if (fileId != other.fileId) return false
        if (chunkIndex != other.chunkIndex) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.toInt()
        result = 31 * result + fileId
        result = 31 * result + chunkIndex
        result = 31 * result + payload.contentHashCode()
        return result
    }

    fun writeTo(out: DataOutputStream) {
        out.writeByte(ProtocolConstants.MAGIC_BYTE.toInt())
        out.writeByte(type.toInt())
        out.writeInt(fileId)
        out.writeInt(chunkIndex)
        out.writeInt(payload.size)
        if (payload.isNotEmpty()) {
            out.write(payload)
        }
        out.flush()
    }

    companion object {
        fun readFrom(inStream: DataInputStream): ProtocolPacket {
            val magic = inStream.readByte()
            if (magic != ProtocolConstants.MAGIC_BYTE) {
                throw IOException("Invalid protocol magic byte: $magic (expected ${ProtocolConstants.MAGIC_BYTE})")
            }

            val type = inStream.readByte()
            val fileId = inStream.readInt()
            val chunkIndex = inStream.readInt()
            val length = inStream.readInt()

            if (length < 0 || length > 16 * 1024 * 1024) { // Safety ceiling: 16 MB max per packet
                throw IOException("Packet payload length out of bounds: $length")
            }

            val payload = ByteArray(length)
            if (length > 0) {
                inStream.readFully(payload)
            }

            return ProtocolPacket(
                type = type,
                fileId = fileId,
                chunkIndex = chunkIndex,
                payload = payload
            )
        }
    }
}
