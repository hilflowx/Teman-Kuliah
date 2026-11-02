package com.temankuliah.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.temankuliah.app.data.local.TemanKuliahDatabase
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

/** BL-21: pesan gagal masuk tidak boleh membedakan akun yang ada dari yang tidak ada. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LoginMessageTest {
    private lateinit var db: TemanKuliahDatabase
    private lateinit var repo: UserRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TemanKuliahDatabase::class.java).allowMainThreadQueries().build()
        repo = UserRepository(db.userDao(), LoginThrottle(maxFailures = 5, baseDelayMs = 30_000L) { 0L })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `akun tidak ada dan sandi salah memberi pesan yang sama`() = runBlocking {
        repo.registerUser("Budi", "", "", "", "", 1, "budi@example.com", "rahasia-123").getOrThrow()
        repo.logout()

        val wrongPassword = repo.loginWithEmailAndPassword("budi@example.com", "salah").exceptionOrNull()!!.message
        val unknownAccount = repo.loginWithEmailAndPassword("tidak.ada@example.com", "salah").exceptionOrNull()!!.message

        assertEquals(UserRepository.LOGIN_FAILED_MESSAGE, wrongPassword)
        assertEquals(wrongPassword, unknownAccount)
    }

    @Test
    fun `akun yang tidak ada ikut dibatasi percobaannya`() = runBlocking {
        repeat(4) { repo.loginWithEmailAndPassword("tidak.ada@example.com", "salah") }

        val fifth = repo.loginWithEmailAndPassword("tidak.ada@example.com", "salah").exceptionOrNull()!!.message!!

        assertTrue(fifth, fifth.contains("Coba lagi dalam 30 detik"))
    }
}
