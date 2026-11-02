package com.temankuliah.app

import com.temankuliah.app.data.backup.BackupCrypto
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCryptoTest {
    private val plain = "isi cadangan: hash sandi dan dokumen KRS".repeat(200).toByteArray()
    private val password = "sandi-cadangan-1".toCharArray()

    private fun encrypt(data: ByteArray = plain, pw: CharArray = password): ByteArray =
        ByteArrayOutputStream().also { BackupCrypto.encrypt(ByteArrayInputStream(data), it, pw) }.toByteArray()

    @Test
    fun `round trip returns the original bytes`() {
        val out = ByteArrayOutputStream()
        BackupCrypto.decrypt(ByteArrayInputStream(encrypt()), out, password)
        assertArrayEquals(plain, out.toByteArray())
    }

    @Test
    fun `ciphertext does not contain the plaintext`() {
        val text = String(encrypt(), Charsets.ISO_8859_1)
        assertFalse(text.contains("hash sandi dan dokumen KRS"))
    }

    @Test
    fun `wrong password is rejected`() {
        try {
            BackupCrypto.decrypt(ByteArrayInputStream(encrypt()), ByteArrayOutputStream(), "salah-sekali".toCharArray())
            fail("Sandi salah harus ditolak")
        } catch (expected: BackupCrypto.WrongPasswordException) {
            assertTrue(expected.message!!.contains("Sandi cadangan salah"))
        }
    }

    @Test
    fun `tampered ciphertext is rejected`() {
        val data = encrypt()
        data[data.size - 5] = (data[data.size - 5].toInt() xor 0x01).toByte()
        try {
            BackupCrypto.decrypt(ByteArrayInputStream(data), ByteArrayOutputStream(), password)
            fail("Berkas yang diubah harus ditolak")
        } catch (expected: BackupCrypto.WrongPasswordException) {
            // diharapkan
        }
    }

    @Test
    fun `isEncrypted detects the marker and leaves the stream at the start`() {
        val encrypted = BufferedInputStream(ByteArrayInputStream(encrypt()))
        assertTrue(BackupCrypto.isEncrypted(encrypted))
        assertTrue(encrypted.read() == 'T'.code)

        val zipLike = BufferedInputStream(ByteArrayInputStream(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 1, 2, 3, 4, 5)))
        assertFalse(BackupCrypto.isEncrypted(zipLike))
        assertTrue(zipLike.read() == 0x50)
    }
}
