package com.temankuliah.app.ui.profile

import android.net.Uri
import androidx.core.content.edit
import com.temankuliah.app.data.backup.BackupCrypto
import com.temankuliah.app.data.backup.LocalDataArchive
import com.temankuliah.app.data.local.AppStorage
import com.temankuliah.app.domain.rethrowIfCancellation
import com.temankuliah.app.ui.FeatureContext
import com.temankuliah.app.ui.FeatureController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream

internal class BackupController(
    ctx: FeatureContext,
    private val onRestored: suspend () -> Unit
) : FeatureController(ctx) {
    // --- Cadangan Lokal ---
    private val _backupPreview = MutableStateFlow<PendingBackupPreview?>(null)
    val backupPreview: StateFlow<PendingBackupPreview?> = _backupPreview.asStateFlow()

    private val preferences = AppStorage.getPreferences(application)

    /** Waktu (epoch ms) cadangan terakhir yang berhasil diekspor di perangkat ini; null bila belum pernah. */
    private val _lastBackupAt = MutableStateFlow(
        if (preferences.contains(KEY_LAST_BACKUP_AT)) preferences.getLong(KEY_LAST_BACKUP_AT, 0L).takeIf { it > 0L } else null
    )
    val lastBackupAt: StateFlow<Long?> = _lastBackupAt.asStateFlow()

    /** Uri cadangan terenkripsi yang menunggu sandi dari pengguna; null bila tidak ada. */
    private val _backupPasswordRequest = MutableStateFlow<Uri?>(null)
    val backupPasswordRequest: StateFlow<Uri?> = _backupPasswordRequest.asStateFlow()

    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    /** Cadangan selalu dienkripsi dengan [password] (BL-19); sandi tidak disimpan. */
    fun exportBackup(uri: Uri, password: CharArray) {
        scope.launch(Dispatchers.IO) {
            _backupBusy.value = true
            val temp = File.createTempFile("export_", ".tkbackup", application.cacheDir)
            try {
                val summary = LocalDataArchive.exportToFile(application, database, temp)
                val output = application.contentResolver.openOutputStream(uri)
                    ?: error("Lokasi cadangan tidak dapat ditulis.")
                output.use { out -> temp.inputStream().use { BackupCrypto.encrypt(it, out, password) } }
                val now = System.currentTimeMillis()
                preferences.edit { putLong(KEY_LAST_BACKUP_AT, now) }
                _lastBackupAt.value = now
                snackbar.value = "Cadangan lokal berhasil dibuat: ${summary.courses} mata kuliah, ${summary.tasks} tugas, ${summary.projects} proyek, ${summary.academicEvents} kegiatan, ${summary.documents} dokumen." +
                    skippedWarning(summary)
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = "Ekspor gagal: ${error.message ?: "kesalahan tidak diketahui"}"
            } finally {
                password.fill('\u0000')
                temp.delete()
                _backupBusy.value = false
            }
        }
    }

    private fun skippedWarning(summary: LocalDataArchive.Summary): String = buildString {
        if (summary.skippedDocuments > 0) append(" Peringatan: ${summary.skippedDocuments} dokumen dilewati karena file-nya tidak ditemukan.")
        if (summary.skippedProfileImages > 0) append(" Peringatan: ${summary.skippedProfileImages} foto profil dilewati karena file-nya tidak ditemukan.")
    }

    /**
     * Membuka cadangan dan memvalidasinya. Cadangan terenkripsi meminta sandi lebih dulu lewat
     * [backupPasswordRequest]; cadangan lama (ZIP polos) tetap diterima tanpa sandi.
     */
    fun previewBackup(uri: Uri, password: CharArray? = null) {
        scope.launch(Dispatchers.IO) {
            _backupBusy.value = true
            try {
                val input = application.contentResolver.openInputStream(uri)
                    ?: error("File cadangan tidak dapat dibaca.")
                val buffered = BufferedInputStream(input)
                val summary = buffered.use { stream ->
                    if (!BackupCrypto.isEncrypted(stream)) {
                        LocalDataArchive.preview(application, stream)
                    } else if (password == null) {
                        _backupPasswordRequest.value = uri
                        return@launch
                    } else {
                        withDecrypted(stream, password) { plain -> LocalDataArchive.preview(application, plain) }
                    }
                }
                _backupPasswordRequest.value = null
                _backupPreview.value = PendingBackupPreview(uri, summary, password)
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                _backupPreview.value = null
                snackbar.value = "Cadangan ditolak: ${error.message ?: "format tidak valid"}"
            } finally { _backupBusy.value = false }
        }
    }

    /** Mendekripsi ke berkas sementara agar sandi salah terdeteksi sebelum data apa pun dibaca. */
    private suspend fun <T> withDecrypted(input: InputStream, password: CharArray, block: suspend (InputStream) -> T): T {
        val temp = File.createTempFile("import_", ".tkbackup", application.cacheDir)
        try {
            temp.outputStream().use { out -> BackupCrypto.decrypt(input, out, password) }
            return temp.inputStream().use { block(it) }
        } finally {
            temp.delete()
        }
    }

    fun cancelBackupPassword() { _backupPasswordRequest.value = null }

    fun cancelBackupRestore() { _backupPreview.value = null }

    fun restorePendingBackup() {
        val pending = _backupPreview.value ?: return
        scope.launch(Dispatchers.IO) {
            _backupBusy.value = true
            try {
                val input = application.contentResolver.openInputStream(pending.uri)
                    ?: error("File cadangan tidak dapat dibaca lagi. Pilih ulang file tersebut.")
                val password = pending.password
                val summary = BufferedInputStream(input).use { stream ->
                    if (password != null && BackupCrypto.isEncrypted(stream)) {
                        withDecrypted(stream, password) { plain -> LocalDataArchive.restore(application, database, plain) }
                    } else {
                        LocalDataArchive.restore(application, database, stream)
                    }
                }
                onRestored()
                _backupPreview.value = null
                snackbar.value = "Pemulihan berhasil: ${summary.users} akun, ${summary.courses} mata kuliah, ${summary.tasks} tugas, ${summary.projects} proyek, ${summary.academicEvents} kegiatan, ${summary.documents} dokumen."
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = "Pemulihan dibatalkan tanpa menerapkan perubahan: ${error.message ?: "kesalahan tidak diketahui"}"
            } finally { _backupBusy.value = false }
        }
    }
}

private const val KEY_LAST_BACKUP_AT = "last_backup_at"

data class PendingBackupPreview(val uri: Uri, val summary: LocalDataArchive.Summary, val password: CharArray? = null)
