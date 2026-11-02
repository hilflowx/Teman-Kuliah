package com.temankuliah.app.domain

import java.util.Locale

/**
 * Membatasi percobaan kata sandi per email. Kegagalan ke-[maxFailures] mengunci akun selama [baseDelayMs];
 * setiap kegagalan berikutnya menggandakan jeda, dibatasi [maxDelayMs]. Masuk yang berhasil menghapus hitungan.
 *
 * Status hanya di memori, sehingga direset saat proses aplikasi berhenti.
 */
class LoginThrottle(
    private val maxFailures: Int = 5,
    private val baseDelayMs: Long = 30_000L,
    private val maxDelayMs: Long = 15 * 60_000L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private class Attempts(var failures: Int = 0, var lockedUntilMs: Long = 0L)

    private val attempts = mutableMapOf<String, Attempts>()

    /** Sisa jeda dalam milidetik; 0 bila boleh mencoba. */
    @Synchronized
    fun remainingLockMs(email: String): Long {
        val state = attempts[key(email)] ?: return 0L
        return (state.lockedUntilMs - clock()).coerceAtLeast(0L)
    }

    /** Mencatat satu kegagalan dan mengembalikan jeda yang berlaku sekarang (0 bila belum terkunci). */
    @Synchronized
    fun recordFailure(email: String): Long {
        val state = attempts.getOrPut(key(email)) { Attempts() }
        state.failures++
        if (state.failures < maxFailures) return 0L
        val exponent = (state.failures - maxFailures).coerceAtMost(30)
        val delay = (baseDelayMs shl exponent).coerceAtMost(maxDelayMs)
        state.lockedUntilMs = clock() + delay
        return delay
    }

    @Synchronized
    fun recordSuccess(email: String) {
        attempts.remove(key(email))
    }

    private fun key(email: String): String = email.trim().lowercase(Locale.ROOT)
}
