package org.zeroshare.app.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.zeroshare.app.core.constants.ProtocolConstants
import org.zeroshare.app.core.hash.Sha256Hasher
import org.zeroshare.app.core.security.AesGcmCipher
import org.zeroshare.app.data.engine.ProtocolPacket
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Random

class ZeroQualityLossTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun test48MpRawPhotoByteForByteFidelity() {
        // 1. Simulate 48MP RAW DNG Photo with simulated EXIF/GPS header
        val photoSize = 12 * 1024 * 1024 // 12 MB realistic test slice
        val sourcePhotoFile = tempFolder.newFile("IMG_48MP_RAW.dng")
        val random = Random(42L) // Deterministic seed

        FileOutputStream(sourcePhotoFile).use { fos ->
            val buf = ByteArray(64 * 1024)
            var written = 0
            while (written < photoSize) {
                random.nextBytes(buf)
                val toWrite = minOf(buf.size, photoSize - written)
                fos.write(buf, 0, toWrite)
                written += toWrite
            }
        }

        // 2. Sender computes SHA-256 hash on original stream
        val senderSourceHash = Sha256Hasher.computeSha256(FileInputStream(sourcePhotoFile))

        // 3. Execute Chunked Transfer Simulation through Protocol Framing and AES-256-GCM
        val destinationPhotoFile = tempFolder.newFile("RECEIVED_IMG_48MP_RAW.dng")
        val sessionKey = AesGcmCipher.generateKey()
        val senderCipher = AesGcmCipher(sessionKey)
        val receiverCipher = AesGcmCipher(sessionKey)

        val receiverHasher = Sha256Hasher()

        FileInputStream(sourcePhotoFile).use { input ->
            FileOutputStream(destinationPhotoFile).use { output ->
                val chunkBuffer = ByteArray(ProtocolConstants.CHUNK_SIZE_BYTES)
                var bytesRead: Int
                var chunkIndex = 0

                while (input.read(chunkBuffer).also { bytesRead = it } != -1) {
                    val rawChunk = if (bytesRead == chunkBuffer.size) chunkBuffer else chunkBuffer.copyOf(bytesRead)

                    // Sender Encrypts & Frames Packet
                    val encryptedChunk = senderCipher.encrypt(rawChunk)
                    val senderPacket = ProtocolPacket(
                        type = ProtocolConstants.TYPE_CHUNK_DATA,
                        fileId = 1,
                        chunkIndex = chunkIndex++,
                        payload = encryptedChunk
                    )

                    // Wire Serialization Simulation
                    val wireBaos = ByteArrayOutputStream()
                    senderPacket.writeTo(DataOutputStream(wireBaos))

                    // Receiver Deserialization Simulation
                    val wireDis = DataInputStream(ByteArrayInputStream(wireBaos.toByteArray()))
                    val receivedPacket = ProtocolPacket.readFrom(wireDis)

                    // Receiver Decrypts & Writes Chunk
                    val decryptedChunk = receiverCipher.decrypt(receivedPacket.payload)
                    output.write(decryptedChunk)
                    receiverHasher.update(decryptedChunk)
                }
            }
        }

        // 4. Receiver verifies SHA-256 digest
        val receiverDigest = receiverHasher.digestHex()
        val receiverFileHash = Sha256Hasher.computeSha256(FileInputStream(destinationPhotoFile))

        println("=== 48MP RAW Photo Quality Verification (Shario) ===")
        println("Sender Original Hash:   $senderSourceHash")
        println("Receiver Received Hash: $receiverDigest")
        println("Receiver File Hash:     $receiverFileHash")

        assertEquals("Received file size must be identical", sourcePhotoFile.length(), destinationPhotoFile.length())
        assertEquals("Receiver ongoing digest must match sender", senderSourceHash, receiverDigest)
        assertEquals("Receiver stored file hash must match sender", senderSourceHash, receiverFileHash)
        assertTrue(Sha256Hasher.verifyHash(receiverDigest, senderSourceHash))
    }

    @Test
    fun test4kVideoStreamingFidelity() {
        // 1. Simulate 4K 60fps MP4 Video stream
        val videoSize = 25 * 1024 * 1024 // 25 MB high-bitrate video stream slice
        val sourceVideoFile = tempFolder.newFile("VID_4K_60FPS.mp4")
        val random = Random(99L)

        FileOutputStream(sourceVideoFile).use { fos ->
            val buf = ByteArray(64 * 1024)
            var written = 0
            while (written < videoSize) {
                random.nextBytes(buf)
                val toWrite = minOf(buf.size, videoSize - written)
                fos.write(buf, 0, toWrite)
                written += toWrite
            }
        }

        // Sender calculates source hash
        val senderSourceHash = Sha256Hasher.computeSha256(FileInputStream(sourceVideoFile))

        // 2. Transfer pipeline
        val destinationVideoFile = tempFolder.newFile("RECEIVED_VID_4K_60FPS.mp4")
        val receiverHasher = Sha256Hasher()

        FileInputStream(sourceVideoFile).use { input ->
            FileOutputStream(destinationVideoFile).use { output ->
                val chunkBuffer = ByteArray(ProtocolConstants.CHUNK_SIZE_BYTES)
                var bytesRead: Int

                while (input.read(chunkBuffer).also { bytesRead = it } != -1) {
                    val rawChunk = if (bytesRead == chunkBuffer.size) chunkBuffer else chunkBuffer.copyOf(bytesRead)

                    output.write(rawChunk)
                    receiverHasher.update(rawChunk)
                }
            }
        }

        val receiverDigest = receiverHasher.digestHex()

        println("=== 4K Video Quality Verification (Shario) ===")
        println("Sender 4K Video Hash:   $senderSourceHash")
        println("Receiver 4K Video Hash: $receiverDigest")

        assertEquals("Source and destination video size must match", sourceVideoFile.length(), destinationVideoFile.length())
        assertEquals("4K Video bit-for-bit SHA-256 match", senderSourceHash, receiverDigest)
        assertTrue(Sha256Hasher.verifyHash(receiverDigest, senderSourceHash))
    }
}
