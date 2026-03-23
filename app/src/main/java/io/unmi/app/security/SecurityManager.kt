package io.unmi.app.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security manager using password-derived keys (PBKDF2 + AES-GCM).
 * No Android Keystore dependency - encryption key is derived from user's password,
 * so data cannot be decrypted without the password even if the database is extracted.
 */
@Singleton
class SecurityManager @Inject constructor() {

    private companion object {
        const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH = 128
        const val GCM_IV_LENGTH = 12
        const val SALT_LENGTH = 32
        const val PBKDF2_ITERATIONS = 100_000
        const val KEY_LENGTH = 256
    }

    // Session encryption key - set after successful login
    @Volatile
    private var sessionKey: SecretKey? = null
    @Volatile
    private var currentAccountId: Long? = null

    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    /**
     * Hash password for storage/verification using PBKDF2-SHA256.
     */
    fun hashPassword(password: String, salt: String): String {
        val saltBytes = Base64.getDecoder().decode(salt)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    fun verifyPassword(password: String, salt: String, storedHash: String): Boolean {
        return hashPassword(password, salt) == storedHash
    }

    /**
     * Derive AES encryption key from password. Called on successful login.
     * Uses a different salt derivation to separate auth hash from encryption key.
     */
    fun initSession(password: String, salt: String, accountId: Long) {
        val saltBytes = Base64.getDecoder().decode(salt)
        // Use "encrypt" prefix to derive a different key than the auth hash
        val encSalt = "encrypt".toByteArray() + saltBytes
        val spec = PBEKeySpec(password.toCharArray(), encSalt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        sessionKey = SecretKeySpec(keyBytes, "AES")
        currentAccountId = accountId
    }

    fun clearSession() {
        sessionKey = null
        currentAccountId = null
    }

    fun getSessionAccountId(): Long? = currentAccountId

    fun isSessionActive(): Boolean = sessionKey != null

    /**
     * Encrypt plaintext using session key (AES-256-GCM).
     */
    fun encrypt(plainText: String): String {
        val key = sessionKey ?: throw SecurityException("未登录，无法加密")
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypt ciphertext using session key (AES-256-GCM).
     */
    fun decrypt(encryptedText: String): String {
        val key = sessionKey ?: throw SecurityException("未登录，无法解密")
        val combined = Base64.getDecoder().decode(encryptedText)
        val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
        val encrypted = combined.sliceArray(GCM_IV_LENGTH until combined.size)
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    /**
     * Encrypt sensitive domain fields into a single encrypted payload.
     */
    fun encryptDomainData(
        domainName: String,
        registrar: String?,
        note: String?,
        nameservers: String?
    ): String {
        val payload = buildString {
            append(domainName)
            append("\u0000") // null separator
            append(registrar ?: "")
            append("\u0000")
            append(note ?: "")
            append("\u0000")
            append(nameservers ?: "")
        }
        return encrypt(payload)
    }

    /**
     * Decrypt domain sensitive fields from encrypted payload.
     * Returns: [domainName, registrar, note, nameservers]
     */
    fun decryptDomainData(encryptedPayload: String): List<String> {
        val decrypted = decrypt(encryptedPayload)
        return decrypted.split("\u0000").let { parts ->
            listOf(
                parts.getOrElse(0) { "" },
                parts.getOrElse(1) { "" },
                parts.getOrElse(2) { "" },
                parts.getOrElse(3) { "" }
            )
        }
    }
}
