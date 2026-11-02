package com.temankuliah.app.data.repository

import com.temankuliah.app.data.local.AUTH_GUEST
import com.temankuliah.app.data.local.AUTH_LOCAL
import com.temankuliah.app.data.local.UserDao
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.domain.LocalPasswordHasher
import com.temankuliah.app.domain.LoginThrottle
import com.temankuliah.app.domain.normalizeNim
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Akun ditemukan tetapi belum punya sandi (hasil restore cadangan). Login tidak
 * boleh menerima sandi apa pun untuk akun ini; pemanggil mengarahkan pengguna ke layar "Buat sandi".
 */
class PasswordSetupRequiredException(val identifier: String) :
    IllegalStateException("Akun ini belum memiliki sandi. Buat sandi terlebih dahulu.")

/** Akun, masuk/keluar, profil, dan semester aktif. */
class UserRepository(
    private val userDao: UserDao,
    private val loginThrottle: LoginThrottle = LoginThrottle()
) {
    val activeUserFlow: Flow<UserEntity?> = userDao.getActiveUserFlow()

    suspend fun getActiveUser(): UserEntity? = withContext(Dispatchers.IO) { userDao.getActiveUser() }

    /** Hash sandi lama yang masih tersimpan sebagai teks biasa. */
    suspend fun upgradeLegacyPasswords() = withContext(Dispatchers.IO) {
        userDao.getAllUsersOnce().forEach { user ->
            if (user.password.isNotEmpty() && !LocalPasswordHasher.isEncoded(user.password)) {
                userDao.updateUser(user.copy(password = LocalPasswordHasher.hash(user.password)))
            }
        }
    }

    suspend fun loginWithEmailAndPassword(emailOrPhone: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanIdentifier = emailOrPhone.trim().lowercase(Locale.ROOT)
        if (cleanIdentifier.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Email, nomor HP, atau NIM wajib diisi."))
        }
        val isEmail = cleanIdentifier.contains('@')
        val isPhone = cleanIdentifier.any { it.isDigit() } && cleanIdentifier.length >= 4
        if (!isEmail && !isPhone) {
            return@withContext Result.failure(IllegalArgumentException("Format email, nomor HP, atau NIM tidak valid."))
        }
        if (password.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Kata sandi tidak boleh kosong."))
        }
        val user = findAccount(cleanIdentifier)
        // Akun yang sama bisa dimasuki lewat email dan NIM, jadi hitungan dikunci dengan id akun.
        // Identitas tak dikenal memakai teks yang diketik agar tetap terhitung.
        val throttleKey = user?.let { "id:${it.id}" } ?: cleanIdentifier
        val waitMs = loginThrottle.remainingLockMs(throttleKey)
        if (waitMs > 0) {
            return@withContext Result.failure(IllegalStateException(tooManyAttemptsMessage(waitMs)))
        }

        // Akun tanpa sandi tidak boleh menerima sandi dari percobaan masuk (siapa pun yang mengetik
        // identitasnya akan menguasai akun). Arahkan ke alur "Buat sandi" yang eksplisit. Syaratnya sengaja
        // dari sandi kosong, bukan authType: restore memulihkan authType apa adanya.
        // Bukan kegagalan kredensial, jadi tidak dihitung ke pembatas percobaan.
        if (user != null && user.hasNoPassword()) {
            return@withContext Result.failure(PasswordSetupRequiredException(cleanIdentifier))
        }

        // Akun tidak ada dan sandi salah memakai pesan dan hitungan yang sama, supaya tidak bisa dipakai menebak akun.
        if (user == null || !LocalPasswordHasher.verify(password, user.password)) {
            val delayMs = loginThrottle.recordFailure(throttleKey)
            val message = if (delayMs > 0) tooManyAttemptsMessage(delayMs) else LOGIN_FAILED_MESSAGE
            return@withContext Result.failure(IllegalArgumentException(message))
        }

        loginThrottle.recordSuccess(throttleKey)
        userDao.activateUser(user.id)
        Result.success(user.copy(isLoggedIn = true))
    }

    /**
     * Alur eksplisit "Buat sandi" untuk akun yang belum punya sandi. Hanya berlaku untuk akun tanpa sandi:
     * akun yang sudah bersandi ditolak (metode ini bukan jalur reset), begitu pula akun tamu. Penetapan
     * sandi dilakukan sebagai satu UPDATE bersyarat sehingga tidak bisa menimpa sandi yang baru saja dibuat.
     */
    suspend fun createPasswordForAccount(
        emailOrPhone: String,
        newPassword: String,
        confirmPassword: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanIdentifier = emailOrPhone.trim().lowercase(Locale.ROOT)
        if (cleanIdentifier.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Email atau nomor HP wajib diisi."))
        }
        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            return@withContext Result.failure(IllegalArgumentException("Kata sandi minimal $MIN_PASSWORD_LENGTH karakter."))
        }
        if (newPassword != confirmPassword) {
            return@withContext Result.failure(IllegalArgumentException("Konfirmasi kata sandi tidak sama."))
        }

        val user = findAccount(cleanIdentifier)
            ?: return@withContext Result.failure(IllegalArgumentException("Akun tidak ditemukan. Silakan periksa kembali email atau nomor HP Anda."))
        if (user.isGuest) {
            return@withContext Result.failure(IllegalArgumentException("Akun tamu tidak memakai kata sandi."))
        }
        if (!user.hasNoPassword()) {
            return@withContext Result.failure(IllegalStateException("Akun ini sudah memiliki kata sandi. Silakan masuk dengan kata sandi tersebut."))
        }

        val hashed = LocalPasswordHasher.hash(newPassword)
        if (userDao.setInitialPassword(user.id, hashed) != 1) {
            // Kalah balapan: sandi sudah ditetapkan di antara pemeriksaan dan penulisan.
            return@withContext Result.failure(IllegalStateException("Akun ini sudah memiliki kata sandi. Silakan masuk dengan kata sandi tersebut."))
        }
        userDao.activateUser(user.id)
        Result.success(user.copy(password = hashed, authType = AUTH_LOCAL, isLoggedIn = true))
    }

    /**
     * Langkah awal alur "Lupa kata sandi": memastikan akun ada dan bukan akun tamu sebelum UI meminta
     * verifikasi kunci layar. Tidak mengubah data dan tidak dihitung ke pembatas percobaan masuk.
     */
    suspend fun findAccountForPasswordReset(emailOrPhone: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanIdentifier = emailOrPhone.trim().lowercase(Locale.ROOT)
        if (cleanIdentifier.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Isi email atau nomor HP akunmu dulu."))
        }
        val user = findAccount(cleanIdentifier)
            ?: return@withContext Result.failure(IllegalArgumentException("Akun tidak ditemukan. Silakan periksa kembali email atau nomor HP Anda."))
        if (user.isGuest) {
            return@withContext Result.failure(IllegalArgumentException("Akun tamu tidak memakai kata sandi."))
        }
        Result.success(user)
    }

    /**
     * Menetapkan sandi baru untuk akun yang lupa sandi. **Pemanggil wajib sudah memverifikasi kunci layar
     * perangkat** (lihat AuthController.resetPassword); repositori tidak dapat memeriksanya sendiri. Hanya
     * kolom sandi yang berubah, data akademik tidak tersentuh. Penguncian percobaan masuk direset dan
     * akun langsung masuk.
     */
    suspend fun resetPasswordForAccount(
        emailOrPhone: String,
        newPassword: String,
        confirmPassword: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            return@withContext Result.failure(IllegalArgumentException("Kata sandi minimal $MIN_PASSWORD_LENGTH karakter."))
        }
        if (newPassword != confirmPassword) {
            return@withContext Result.failure(IllegalArgumentException("Konfirmasi kata sandi tidak sama."))
        }
        val user = findAccountForPasswordReset(emailOrPhone).getOrElse { return@withContext Result.failure(it) }

        val hashed = LocalPasswordHasher.hash(newPassword)
        if (userDao.replacePassword(user.id, hashed) != 1) {
            return@withContext Result.failure(IllegalStateException("Kata sandi tidak dapat diganti. Silakan coba lagi."))
        }
        loginThrottle.recordSuccess(emailOrPhone.trim().lowercase(Locale.ROOT))
        loginThrottle.recordSuccess("id:${user.id}")
        userDao.activateUser(user.id)
        Result.success(user.copy(password = hashed, authType = AUTH_LOCAL, isLoggedIn = true))
    }

    suspend fun registerUser(
        fullName: String,
        nim: String,
        studyProgram: String,
        faculty: String,
        university: String,
        semester: Int,
        email: String,
        password: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val trimmedName = fullName.trim()
        val trimmedNim = nim.trim()
        val cleanEmail = email.trim().lowercase(Locale.ROOT)

        if (trimmedName.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Nama lengkap tidak boleh kosong."))
        }
        val isEmail = cleanEmail.contains('@')
        val isPhone = cleanEmail.filter { it.isDigit() }.length >= 8
        if (!isEmail && !isPhone) {
            return@withContext Result.failure(IllegalArgumentException("Format email atau nomor HP tidak valid."))
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return@withContext Result.failure(IllegalArgumentException("Kata sandi minimal $MIN_PASSWORD_LENGTH karakter."))
        }

        userDao.getUserByEmail(cleanEmail)?.let {
            return@withContext Result.failure(IllegalArgumentException("Email atau nomor HP sudah digunakan oleh akun lain."))
        }

        if (trimmedNim.isNotEmpty()) {
            userDao.getUserByNormalizedNim(normalizeNim(trimmedNim))?.let {
                return@withContext Result.failure(IllegalArgumentException("NIM sudah terdaftar pada akun lain."))
            }
        }

        val hashedPassword = LocalPasswordHasher.hash(password)
        val activeSem = semester.coerceIn(1, 14)
        val semCount = maxOf(8, activeSem)

        val newUser = UserEntity(
            fullName = trimmedName,
            nim = trimmedNim,
            studyProgram = studyProgram.trim(),
            faculty = faculty.trim(),
            university = university.trim(),
            password = hashedPassword,
            email = cleanEmail,
            activeSemester = activeSem,
            semesterCount = semCount,
            authType = AUTH_LOCAL
        )
        val newId = userDao.insertUser(newUser)
        userDao.activateUser(newId)
        Result.success(newUser.copy(id = newId, isLoggedIn = true))
    }

    /** Mode tamu memakai satu akun lokal tetap, sehingga data tamu masih ada saat masuk sebagai tamu lagi. */
    suspend fun signInAsGuest(): Result<UserEntity> = withContext(Dispatchers.IO) {
        val existing = userDao.getGuestUser()
        if (existing != null) {
            userDao.activateUser(existing.id)
            return@withContext Result.success(existing.copy(isLoggedIn = true))
        }
        val guest = UserEntity(
            fullName = GUEST_NAME,
            nim = "",
            studyProgram = "",
            faculty = "",
            university = "",
            password = "",
            authType = AUTH_GUEST
        )
        val newId = userDao.insertUser(guest)
        userDao.activateUser(newId)
        Result.success(guest.copy(id = newId, isLoggedIn = true))
    }

    suspend fun logout() = withContext(Dispatchers.IO) { userDao.logoutAll() }

    suspend fun updateProfile(
        userId: Long,
        fullName: String,
        nim: String,
        studyProgram: String,
        faculty: String,
        university: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = userDao.getActiveUser()?.takeIf { it.id == userId }
            ?: return@withContext Result.failure(IllegalStateException("Pengguna tidak aktif."))
        val trimmedName = fullName.trim()
        val trimmedNim = nim.trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Nama wajib diisi."))
        }
        if (trimmedNim.isNotEmpty()) {
            val duplicate = userDao.getUserByNormalizedNim(normalizeNim(trimmedNim))
            if (duplicate != null && duplicate.id != userId) {
                return@withContext Result.failure(IllegalArgumentException("NIM sudah digunakan akun lain."))
            }
        }
        userDao.updateUser(
            user.copy(
                fullName = trimmedName,
                nim = trimmedNim,
                studyProgram = studyProgram.trim(),
                faculty = faculty.trim(),
                university = university.trim()
            )
        )
        Result.success(Unit)
    }

    suspend fun updateProfileImage(userId: Long, relativePath: String?): Result<String?> = withContext(Dispatchers.IO) {
        val user = userDao.getActiveUser()?.takeIf { it.id == userId }
            ?: return@withContext Result.failure(IllegalStateException("Pengguna tidak aktif."))
        userDao.updateUser(user.copy(profileImagePath = relativePath))
        Result.success(user.profileImagePath)
    }

    suspend fun updateActiveSemester(userId: Long, semester: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (semester < 1) return@withContext Result.failure(IllegalArgumentException("Semester minimal 1."))
        val user = userDao.getActiveUser()?.takeIf { it.id == userId }
            ?: return@withContext Result.failure(IllegalStateException("Pengguna tidak aktif."))
        userDao.updateUser(user.copy(activeSemester = semester))
        Result.success(Unit)
    }

    /** Menyimpan jumlah semester. Tidak pernah menghapus data; jumlah tidak boleh di bawah semester aktif. */
    suspend fun updateSemesterCount(userId: Long, count: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val user = userDao.getActiveUser()?.takeIf { it.id == userId }
            ?: return@withContext Result.failure(IllegalStateException("Pengguna tidak aktif."))
        if (count < maxOf(1, user.activeSemester)) return@withContext Result.failure(IllegalArgumentException("Jumlah semester tidak valid."))
        if (count != user.semesterCount) userDao.updateUser(user.copy(semesterCount = count))
        Result.success(Unit)
    }

    private suspend fun findAccount(cleanIdentifier: String): UserEntity? =
        userDao.getUserByEmail(cleanIdentifier) ?: userDao.getUserByNormalizedNim(normalizeNim(cleanIdentifier))

    /** Akun biasa tanpa sandi. Tamu memang tidak bersandi dan tidak termasuk. */
    private fun UserEntity.hasNoPassword(): Boolean = !isGuest && password.isEmpty()

    private fun tooManyAttemptsMessage(waitMs: Long): String =
        "Terlalu banyak percobaan masuk. Coba lagi dalam ${(waitMs + 999) / 1000} detik."

    companion object {
        const val GUEST_NAME = "Tamu"
        const val MIN_PASSWORD_LENGTH = 8
        const val LOGIN_FAILED_MESSAGE = "Akun atau kata sandi salah. Silakan periksa kembali dan coba lagi."
    }
}
