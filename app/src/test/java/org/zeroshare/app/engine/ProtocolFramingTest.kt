package org.zeroshare.app.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.zeroshare.app.core.constants.ProtocolConstants
import org.zeroshare.app.data.engine.ProtocolPacket
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class ProtocolFramingTest {

    @Test
    fun testPacketSerializationAndDeserialization() {
        val payloadData = "Hello Shario Protocol Frame".toByteArray(Charsets.UTF_8)
        val originalPacket = ProtocolPacket(
            type = ProtocolConstants.TYPE_CHUNK_DATA,
            fileId = 42,
            chunkIndex = 7,
            payload = payloadData
        )

        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        originalPacket.writeTo(dos)

        val serializedBytes = baos.toByteArray()

        val bais = ByteArrayInputStream(serializedBytes)
        val dis = DataInputStream(bais)
        val decodedPacket = ProtocolPacket.readFrom(dis)

        assertEquals(originalPacket.type, decodedPacket.type)
        assertEquals(originalPacket.fileId, decodedPacket.fileId)
        assertEquals(originalPacket.chunkIndex, decodedPacket.chunkIndex)
        assertArrayEquals(originalPacket.payload, decodedPacket.payload)
    }

    @Test
    fun testCorruptedMagicByteThrowsException() {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Write invalid magic byte
        dos.writeByte(0x00) // Not 0x5A
        dos.writeByte(ProtocolConstants.TYPE_CHUNK_DATA.toInt())
        dos.writeInt(1)
        dos.writeInt(0)
        dos.writeInt(0)

        val dis = DataInputStream(ByteArrayInputStream(baos.toByteArray()))
        try {
            ProtocolPacket.readFrom(dis)
            org.junit.Assert.fail("Expected IOException for invalid magic byte")
        } catch (e: IOException) {
            // Expected
        }
    }
}
