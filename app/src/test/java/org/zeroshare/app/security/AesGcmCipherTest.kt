package org.zeroshare.app.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.zeroshare.app.core.security.AesGcmCipher
import javax.crypto.AEADBadTagException

class AesGcmCipherTest {

    @Test
    fun testEncryptionAndDecryptionRoundtrip() {
        val originalSecretKey = AesGcmCipher.generateKey()
        val cipher = AesGcmCipher(originalSecretKey)

        val samplePlaintext = "Zero-loss 48MP DNG original image payload chunks".toByteArray(Charsets.UTF_8)
        val encryptedPayload = cipher.encrypt(samplePlaintext)

        assertFalse(samplePlaintext.contentEquals(encryptedPayload))

        val decryptedPlaintext = cipher.decrypt(encryptedPayload)
        assertArrayEquals(samplePlaintext, decryptedPlaintext)
    }

    @Test
    fun testTamperingThrowsAuthenticationException() {
        val secretKey = AesGcmCipher.generateKey()
        val cipher = AesGcmCipher(secretKey)

        val samplePlaintext = "Sensitive transfer payload".toByteArray(Charsets.UTF_8)
        val encryptedPayload = cipher.encrypt(samplePlaintext)

        // Flip a bit in the ciphertext to simulate tampering or bit-flip corruption
        encryptedPayload[encryptedPayload.size - 1] = (encryptedPayload[encryptedPayload.size - 1].toInt() xor 0x01).toByte()

        try {
            cipher.decrypt(encryptedPayload)
            org.junit.Assert.fail("Expected Exception when decrypting tampered payload")
        } catch (e: Exception) {
            // Expected
        }
    }

    @Test
    fun testKeyBase64SerializationRoundtrip() {
        val originalKey = AesGcmCipher.generateKey()
        val base64Key = AesGcmCipher.keyToBase64(originalKey)

        val restoredKey = AesGcmCipher.keyFromBase64(base64Key)
        assertArrayEquals(originalKey.encoded, restoredKey.encoded)
        assertEquals(originalKey.algorithm, restoredKey.algorithm)
    }
}
