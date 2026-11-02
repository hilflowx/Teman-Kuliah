package com.temankuliah.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.temankuliah.app.data.local.AUTH_GUEST
import com.temankuliah.app.data.local.TemanKuliahDatabase
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.data.repository.UserRepository
import com.temankuliah.app.domain.LoginThrottle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Alur "Lupa kata sandi" di lapisan repositori (verifikasi kunci layar dilakukan di UI dan dijaga AuthController). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ForgotPasswordRepositoryTest {
    private lateinit var db: TemanKuliahDatabase
    private var now = 0L
    private lateinit var repo: UserRepository

    private val email = "budi@example.com"
    private val oldPassword = "rahasia-123"
    private val newPassword = "sandi-baru-456"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TemanKuliahDatabase::class.java).allowMainThreadQueries().build()
        repo = UserRepository(db.userDao(), LoginThrottle(maxFailures = 5, baseDelayMs = 30_000L) { now })
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun registerAccount() = runBlocking {
        repo.registerUser("Budi", "", "", "", "", 1, email, oldPassword).getOrThrow()
        repo.logout()
    }

    @Test
    fun `reset mengganti sandi dan sandi lama tidak berlaku lagi`() = runBlocking {
        registerAccount()
        val user = repo.resetPasswordForAccount(email, newPassword, newPassword).getOrThrow()
        assertTrue(user.isLoggedIn)
        repo.logout()
        assertTrue(repo.loginWithEmailAndPassword(email, oldPassword).isFailure)
        assertTrue(repo.loginWithEmailAndPassword(email, newPassword).isSuccess)
    }

    @Test
    fun `reset tidak mengubah data akun selain sandi`() = runBlocking {
        registerAccount()
        val before = repo.findAccountForPasswordReset(email).getOrThrow()
        val after = repo.resetPasswordForAccount(email, newPassword, newPassword).getOrThrow()
        assertEquals(before.id, after.id)
        assertEquals(before.fullName, after.fullName)
        assertEquals(before.email, after.email)
    }

    @Test
    fun `reset menghapus penguncian percobaan masuk`() = runBlocking {
        registerAccount()
        repeat(5) { repo.loginWithEmailAndPassword(email, "salah-salah") }
        assertTrue(repo.loginWithEmailAndPassword(email, oldPassword).isFailure) // masih terkunci
        repo.resetPasswordForAccount(email, newPassword, newPassword).getOrThrow()
        repo.logout()
        assertTrue(repo.loginWithEmailAndPassword(email, newPassword).isSuccess)
    }

    @Test
    fun `sandi pendek atau konfirmasi tidak cocok ditolak dan sandi lama tetap`() = runBlocking {
        registerAccount()
        assertTrue(repo.resetPasswordForAccount(email, "pendek", "pendek").isFailure)
        assertTrue(repo.resetPasswordForAccount(email, newPassword, "beda-sandi-1").isFailure)
        assertTrue(repo.loginWithEmailAndPassword(email, oldPassword).isSuccess)
    }

    @Test
    fun `akun tidak ada atau tamu ditolak`() = runBlocking {
        assertTrue(repo.findAccountForPasswordReset("").isFailure)
        assertTrue(repo.findAccountForPasswordReset("tidak-ada@example.com").isFailure)
        assertTrue(repo.resetPasswordForAccount("tidak-ada@example.com", newPassword, newPassword).isFailure)
        // Akun tamu yang beremail sengaja dibuat langsung, agar penolakan memang berasal dari status tamu.
        runBlocking {
            db.userDao().insertUser(
                UserEntity(
                    fullName = "Tamu", nim = "", studyProgram = "", faculty = "", university = "",
                    password = "", email = "tamu@example.com", authType = AUTH_GUEST
                )
            )
        }
        assertTrue(repo.findAccountForPasswordReset("tamu@example.com").isFailure)
        assertTrue(repo.resetPasswordForAccount("tamu@example.com", newPassword, newPassword).isFailure)
    }
}
