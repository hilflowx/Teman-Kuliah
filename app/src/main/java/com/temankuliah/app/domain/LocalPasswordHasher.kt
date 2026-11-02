package com.temankuliah.app.domain

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores locally entered passwords as salted PBKDF2 hashes.
 * The encoded format includes its algorithm and work factor for future upgrades.
 */
object LocalPasswordHasher {
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val PREFIX_SHA256 = "pbkdf2-sha256"
    private const val PREFIX_SHA1 = "pbkdf2-sha1"
    private val random = SecureRandom()

    fun isEncoded(value: String): Boolean {
        val parts = value.split('$')
        if (parts.size != 4 || parts[0] !in setOf(PREFIX_SHA256, PREFIX_SHA1)) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        if (iterations !in 10_000..1_000_000) return false
        val salt = parts[2].fromHexOrNull() ?: return false
        val derived = parts[3].fromHexOrNull() ?: return false
        return salt.size in 8..64 && derived.size in 16..64
    }

    fun hash(password: String): String {
        require(password.isNotEmpty()) { "Kata sandi tidak boleh kosong." }
        val algorithm = preferredAlgorithm()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val derived = derive(password, salt, ITERATIONS, algorithm)
        return "${prefixFor(algorithm)}\$${ITERATIONS}\$${salt.toHex()}\$${derived.toHex()}"
    }

    fun verify(password: String, encoded: String): Boolean {
        val parts = encoded.split('$')
        if (parts.size != 4) return false
        val algorithm = when (parts[0]) {
            PREFIX_SHA256 -> "PBKDF2WithHmacSHA256"
            PREFIX_SHA1 -> "PBKDF2WithHmacSHA1"
            else -> return false
        }
        val iterations = parts[1].toIntOrNull() ?: return false
        // Keep corrupted or tampered records from requesting unreasonable CPU time.
        if (iterations !in 10_000..1_000_000) return false
        val salt = parts[2].fromHexOrNull() ?: return false
        val expected = parts[3].fromHexOrNull() ?: return false
        if (salt.size !in 8..64 || expected.isEmpty()) return false
        val actual = try {
            derive(password, salt, iterations, algorithm)
        } catch (_: IllegalStateException) {
            return false
        }
        return MessageDigest.isEqual(expected, actual)
    }

    private fun preferredAlgorithm(): String = try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        "PBKDF2WithHmacSHA256"
    } catch (_: NoSuchAlgorithmException) {
        "PBKDF2WithHmacSHA1"
    }

    private fun prefixFor(algorithm: String): String =
        if (algorithm.endsWith("SHA256")) PREFIX_SHA256 else PREFIX_SHA1

    private fun derive(password: String, salt: ByteArray, iterations: Int, algorithm: String): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        } catch (error: Exception) {
            rethrowIfCancellation(error)
            throw IllegalStateException("Penyedia hashing kata sandi tidak tersedia.", error)
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }

    private fun String.fromHexOrNull(): ByteArray? {
        if (isEmpty() || length % 2 != 0 || any { it.digitToIntOrNull(16) == null }) return null
        return runCatching {
            chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }.getOrNull()
    }
}
