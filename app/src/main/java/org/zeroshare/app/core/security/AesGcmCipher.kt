package org.zeroshare.app.core.security

import java.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-End Encryption engine using AES-256-GCM (Galois/Counter Mode).
 * Provides authenticated encryption with associated data (AEAD) guaranteeing confidentiality
 * and tamper detection for all payload chunks.
 */
class AesGcmCipher(private val secretKey: SecretKey) {

    private val secureRandom = SecureRandom()

    /**
     * Encrypts plaintext data using AES-256-GCM.
     * Returns: [12-byte Nonce / IV] + [Ciphertext + 16-byte Auth Tag]
     */
    fun encrypt(plaintext: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val ciphertextWithTag = cipher.doFinal(plaintext)
        val combined = ByteArray(iv.size + ciphertextWithTag.size)

        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertextWithTag, 0, combined, iv.size, ciphertextWithTag.size)
        return combined
    }

    /**
     * Decrypts combined [IV + Ciphertext + Tag]. Throws [SecurityException] or [javax.crypto.AEADBadTagException] if tampered.
     */
    fun decrypt(combinedPayload: ByteArray): ByteArray {
        if (combinedPayload.size < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
            throw IllegalArgumentException("Payload too short for AES-GCM")
        }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(combinedPayload, 0, iv, 0, iv.size)

        val ciphertextLength = combinedPayload.size - GCM_IV_LENGTH_BYTES
        val ciphertextWithTag = ByteArray(ciphertextLength)
        System.arraycopy(combinedPayload, GCM_IV_LENGTH_BYTES, ciphertextWithTag, 0, ciphertextLength)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(ciphertextWithTag)
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128

        /**
         * Generates a cryptographically strong 256-bit AES key.
         */
        fun generateKey(): SecretKey {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256, SecureRandom())
            return keyGen.generateKey()
        }

        /**
         * Encodes key to Base64 for transmission inside QR codes.
         */
        fun keyToBase64(key: SecretKey): String {
            return Base64.getEncoder().encodeToString(key.encoded)
        }

        /**
         * Decodes Base64 string back to AES [SecretKey].
         */
        fun keyFromBase64(base64: String): SecretKey {
            val decoded = Base64.getDecoder().decode(base64)
            return SecretKeySpec(decoded, "AES")
        }
    }
}
