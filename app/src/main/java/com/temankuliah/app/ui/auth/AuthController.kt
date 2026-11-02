package com.temankuliah.app.ui.auth

import com.temankuliah.app.data.repository.PasswordSetupRequiredException
import com.temankuliah.app.domain.rethrowIfCancellation
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.ui.FeatureContext
import com.temankuliah.app.ui.FeatureController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

internal class AuthController(
    ctx: FeatureContext
) : FeatureController(ctx) {
    // Auth screen form states
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    /** Jadwalkan ulang alarm untuk akun yang baru masuk; alarm akun lain dibatalkan. */
    private suspend fun rescheduleForLogin(userId: Long) = withContext(Dispatchers.IO) {
        runCatching { ReminderScheduler.rescheduleAll(application, database, userId) }
    }

    fun showAuthError(message: String) {
        _authError.value = message
    }

    fun clearAuthError() {
        _authError.value = null
    }

    // --- Autentikasi: Email/Password ---
    /**
     * [onPasswordSetupRequired] dipanggil (dengan identitas yang diketik) bila akun belum punya sandi;
     * UI membukanya ke layar "Buat sandi". Sandi yang diketik di layar masuk tidak dipakai untuk apa pun.
     */
    fun loginWithEmail(email: String, password: String, onPasswordSetupRequired: (String) -> Unit = {}) {
        scope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                repository.loginWithEmailAndPassword(email, password)
                    .onSuccess { user ->
                        rescheduleForLogin(user.id)
                        snackbar.value = "Selamat datang kembali, ${user.fullName}!"
                    }
                    .onFailure { error ->
                        if (error is PasswordSetupRequiredException) onPasswordSetupRequired(error.identifier)
                        else _authError.value = error.message ?: "Gagal masuk."
                    }
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                _authError.value = error.message ?: "Gagal masuk."
            } finally {
                _authLoading.value = false
            }
        }
    }

    /** Alur eksplisit untuk akun lama/hasil restore yang belum punya sandi. Berhasil = langsung masuk. */
    fun createPassword(identifier: String, password: String, confirmPassword: String) {
        scope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                if (verifiedSetupIdentifier != resetKey(identifier)) {
                    _authError.value = "Verifikasi kunci layar kedaluwarsa. Ulangi dari halaman masuk."
                    return@launch
                }
                repository.createPasswordForAccount(identifier, password, confirmPassword)
                    .onSuccess { user ->
                        verifiedSetupIdentifier = null
                        rescheduleForLogin(user.id)
                        snackbar.value = "Kata sandi dibuat. Selamat datang, ${user.fullName}!"
                    }
                    .onFailure { _authError.value = it.message ?: "Gagal membuat kata sandi." }
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                _authError.value = error.message ?: "Gagal membuat kata sandi."
            } finally {
                _authLoading.value = false
            }
        }
    }

    // --- Buat sandi untuk akun tanpa sandi (BL-14): kunci layar perangkat wajib diverifikasi, sama seperti reset ---
    /** Identitas akun tanpa sandi yang kunci layar perangkatnya baru saja diverifikasi. */
    private var verifiedSetupIdentifier: String? = null

    /** Dipanggil UI hanya setelah kunci layar perangkat berhasil dikonfirmasi untuk alur "Buat sandi". */
    fun markDeviceVerifiedForSetup(identifier: String) {
        verifiedSetupIdentifier = resetKey(identifier)
        _authError.value = null
    }

    fun cancelPasswordSetup() {
        verifiedSetupIdentifier = null
        _authError.value = null
    }

    // --- Lupa kata sandi ---
    /** Identitas akun yang kunci layar perangkatnya baru saja diverifikasi; hanya ini yang boleh direset. */
    private var verifiedResetIdentifier: String? = null

    private fun resetKey(identifier: String) = identifier.trim().lowercase(Locale.ROOT)

    /**
     * Langkah 1: cek akun ada. Bila ya, [onReady] dipanggil dan UI meminta verifikasi kunci layar.
     * Verifikasi sebelumnya dibatalkan agar tidak terbawa ke akun lain.
     */
    fun beginPasswordReset(identifier: String, onReady: () -> Unit) {
        verifiedResetIdentifier = null
        scope.launch {
            _authError.value = null
            try {
                repository.findAccountForPasswordReset(identifier)
                    .onSuccess { onReady() }
                    .onFailure { _authError.value = it.message ?: "Akun tidak dapat direset." }
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                _authError.value = error.message ?: "Akun tidak dapat direset."
            }
        }
    }

    /** Langkah 2: dipanggil UI hanya setelah kunci layar perangkat berhasil dikonfirmasi. */
    fun markDeviceVerifiedForReset(identifier: String) {
        verifiedResetIdentifier = resetKey(identifier)
        _authError.value = null
    }

    fun cancelPasswordReset() {
        verifiedResetIdentifier = null
        _authError.value = null
    }

    /** Langkah 3: menetapkan sandi baru. Ditolak bila kunci layar untuk akun ini belum diverifikasi. */
    fun resetPassword(identifier: String, password: String, confirmPassword: String) {
        scope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                if (verifiedResetIdentifier != resetKey(identifier)) {
                    _authError.value = "Verifikasi kunci layar kedaluwarsa. Ulangi dari \"Lupa kata sandi?\"."
                    return@launch
                }
                repository.resetPasswordForAccount(identifier, password, confirmPassword)
                    .onSuccess { user ->
                        verifiedResetIdentifier = null
                        rescheduleForLogin(user.id)
                        snackbar.value = "Kata sandi diganti. Selamat datang kembali, ${user.fullName}!"
                    }
                    .onFailure { _authError.value = it.message ?: "Gagal mengganti kata sandi." }
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                _authError.value = error.message ?: "Gagal mengganti kata sandi."
            } finally {
                _authLoading.value = false
            }
        }
    }

    fun register(
        fullName: String,
        nim: String,
        studyProgram: String,
        faculty: String,
        university: String,
        semester: Int,
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        scope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                repository.registerUser(
                    fullName = fullName,
                    nim = nim,
                    studyProgram = studyProgram,
                    faculty = faculty,
                    university = university,
                    semester = semester,
                    email = email,
                    password = password
                ).onSuccess { user ->
                    rescheduleForLogin(user.id)
                    snackbar.value = "Akun berhasil dibuat. Selamat datang di Teman Kuliah, ${user.fullName}!"
                    onSuccess()
                }.onFailure { _authError.value = it.message ?: "Gagal mendaftarkan akun." }
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                _authError.value = error.message ?: "Gagal mendaftarkan akun."
            } finally {
                _authLoading.value = false
            }
        }
    }

    fun signInAsGuest() {
        scope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                repository.signInAsGuest()
                    .onSuccess { user ->
                        rescheduleForLogin(user.id)
                        snackbar.value = "Masuk sebagai tamu. Data tersimpan di perangkat ini saja."
                    }
                    .onFailure { _authError.value = it.message ?: "Gagal masuk sebagai tamu." }
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                _authError.value = error.message ?: "Gagal masuk sebagai tamu."
            } finally {
                _authLoading.value = false
            }
        }
    }

    fun logout() {
        scope.launch {
            closeDialog()
            _authError.value = null
            verifiedSetupIdentifier = null
            verifiedResetIdentifier = null
            repository.logout()
            // Tanpa ini alarm akun yang keluar tetap berbunyi di perangkat bersama.
            withContext(Dispatchers.IO) {
                runCatching { ReminderScheduler.cancelAllAlarms(application, database) }
            }
            snackbar.value = "Anda telah keluar."
        }
    }
}
