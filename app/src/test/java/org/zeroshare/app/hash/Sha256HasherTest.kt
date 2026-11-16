package org.zeroshare.app.hash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.zeroshare.app.core.hash.Sha256Hasher
import java.io.ByteArrayInputStream

class Sha256HasherTest {

    @Test
    fun testIncrementalHashingMatchesStreamHashing() {
        val testData = "Shario — Fast Sharing. Zero Hassle. 100% bit-for-bit fidelity.".toByteArray(Charsets.UTF_8)

        // 1. One-shot stream hashing
        val expectedHash = Sha256Hasher.computeSha256(ByteArrayInputStream(testData))

        // 2. Incremental chunk hashing (e.g. 8 bytes per chunk)
        val incrementalHasher = Sha256Hasher()
        var offset = 0
        val chunkSize = 8
        while (offset < testData.size) {
            val length = minOf(chunkSize, testData.size - offset)
            incrementalHasher.update(testData, offset, length)
            offset += length
        }
        val actualHash = incrementalHasher.digestHex()

        assertEquals(expectedHash, actualHash)
        assertTrue(Sha256Hasher.verifyHash(actualHash, expectedHash))
    }

    @Test
    fun testKnownSha256Vector() {
        // "abc" SHA-256 vector is ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        val data = "abc".toByteArray(Charsets.UTF_8)
        val hash = Sha256Hasher.computeSha256(ByteArrayInputStream(data))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash)
    }
}
