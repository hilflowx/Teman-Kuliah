package com.temankuliah.app.data.backup

import android.os.Build
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enkripsi cadangan lokal dengan sandi cadangan, memakai JCA bawaan (tanpa dependency baru).
 *
 * Format berkas: `TKBKENC1` + 1 byte jenis KDF + salt (16 byte) + IV (12 byte) + ciphertext AES-256-GCM
 * dari seluruh ZIP cadangan. Kunci diturunkan dari sandi lewat PBKDF2. Cadangan lama (ZIP polos) tetap
 * bisa dipulihkan; [isEncrypted] membedakannya lewat penanda di awal berkas.
 */
object BackupCrypto {
    private val MAGIC = "TKBKENC1".toByteArray(Charsets.US_ASCII)
    private const val KDF_SHA256: Int = 1
    private const val KDF_SHA1: Int = 2
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val ITERATIONS_SHA256 = 210_000
    private const val ITERATIONS_SHA1 = 600_000

    /** Sandi cadangan minimal; sama dengan batas sandi akun. */
    const val MIN_PASSWORD_LENGTH = 8

    /** Gagal dekripsi karena sandi salah atau berkas rusak. */
    class WrongPasswordException : IllegalArgumentException("Sandi cadangan salah atau file cadangan rusak.")

    /** Memeriksa penanda tanpa menghabiskan stream; [input] dikembalikan ke posisi awal. */
    fun isEncrypted(input: BufferedInputStream): Boolean {
        input.mark(MAGIC.size)
        val head = ByteArray(MAGIC.size)
        var read = 0
        while (read < head.size) {
            val n = input.read(head, read, head.size - read)
            if (n < 0) break
            read += n
        }
        input.reset()
        return read == MAGIC.size && head.contentEquals(MAGIC)
    }

    fun encrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val kdf = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) KDF_SHA256 else KDF_SHA1
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, kdf), GCMParameterSpec(GCM_TAG_BITS, iv))
        output.write(MAGIC)
        output.write(kdf)
        output.write(salt)
        output.write(iv)
        CipherOutputStream(output, cipher).use { encrypted -> input.copyTo(encrypted) }
    }

    /** [input] harus berada di awal berkas (sebelum penanda). Melempar [WrongPasswordException] bila gagal. */
    fun decrypt(input: InputStream, output: OutputStream, password: CharArray) {
        try {
            val marker = input.readExactly(MAGIC.size)
            if (!marker.contentEquals(MAGIC)) throw WrongPasswordException()
            val kdf = input.read()
            val salt = input.readExactly(SALT_BYTES)
            val iv = input.readExactly(IV_BYTES)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, kdf), GCMParameterSpec(GCM_TAG_BITS, iv))
            CipherInputStream(input, cipher).use { plain -> plain.copyTo(output) }
        } catch (error: IOException) {
            throw WrongPasswordException().also { it.initCause(error) }
        } catch (error: GeneralSecurityException) {
            throw WrongPasswordException().also { it.initCause(error) }
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, kdf: Int): SecretKeySpec {
        val (algorithm, iterations) = when (kdf) {
            KDF_SHA256 -> "PBKDF2WithHmacSHA256" to ITERATIONS_SHA256
            KDF_SHA1 -> "PBKDF2WithHmacSHA1" to ITERATIONS_SHA1
            else -> throw WrongPasswordException()
        }
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun InputStream.readExactly(count: Int): ByteArray {
        val buffer = ByteArray(count)
        var read = 0
        while (read < count) {
            val n = read(buffer, read, count - read)
            if (n < 0) throw IOException("Berkas terpotong.")
            read += n
        }
        return buffer
    }
}
