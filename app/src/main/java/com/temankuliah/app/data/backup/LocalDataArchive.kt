package com.temankuliah.app.data.backup

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import com.temankuliah.app.data.backup.BackupArchiveReader.readAndValidate
import com.temankuliah.app.data.backup.BackupAssetFiles.safeDocumentExtension
import com.temankuliah.app.data.backup.BackupAssetFiles.safeImageExtension
import com.temankuliah.app.data.backup.BackupSnapshotCodec.encodeSnapshot
import com.temankuliah.app.data.backup.BackupSnapshotCodec.readSettings
import com.temankuliah.app.data.backup.BackupSnapshotValidator.semesterStartDateProblem
import com.temankuliah.app.data.backup.BackupSnapshotValidator.validateSnapshot
import com.temankuliah.app.data.files.StudentFileStore
import com.temankuliah.app.data.local.AUTH_GUEST
import com.temankuliah.app.data.local.AUTH_LOCAL
import com.temankuliah.app.data.local.AcademicDocumentEntity
import com.temankuliah.app.data.local.AcademicEventEntity
import com.temankuliah.app.data.local.AppStorage
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.DATABASE_SCHEMA_VERSION
import com.temankuliah.app.data.local.ProjectEntity
import com.temankuliah.app.data.local.ProjectMilestoneEntity
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.data.local.TemanKuliahDatabase
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.domain.LocalPasswordHasher
import com.temankuliah.app.domain.rethrowIfCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Versioned, offline-only ZIP backup. Restore intentionally replaces the full local dataset. */
object LocalDataArchive {
    internal const val FORMAT = "com.temankuliah.app.backup"
    internal const val FORMAT_VERSION = 1

    /**
     * Batas total byte tak terkompresi (manifest + data.json + semua lampiran). Dokumen boleh diunggah
     * sampai 40 MB per file dan satu semester bisa punya KRS + KHS, jadi batas lama 120 MB terlalu
     * cepat tercapai. Karena ZIP kini ditulis/dibaca lewat file sementara (bukan RAM), batas ini
     * hanya menjaga ruang penyimpanan dan melindungi dari ZIP bomb.
     */
    internal const val MAX_ARCHIVE_UNCOMPRESSED = 256L * 1024L * 1024L

    /** Satu entri harus muat dokumen terbesar yang boleh diunggah (40 MB) plus sedikit ruang. */
    internal const val MAX_ENTRY_BYTES = 45L * 1024L * 1024L
    internal const val MAX_ENTRIES = 2_000

    data class Summary(
        val users: Int,
        val courses: Int,
        val schedules: Int,
        val tasks: Int,
        val documents: Int,
        val attachments: Int,
        val createdAt: Long,
        val academicEvents: Int = 0,
        val projects: Int = 0,
        val milestones: Int = 0,
        /** Dokumen yang filenya hilang sehingga dilewati (metadata dan file sama-sama tidak diekspor). */
        val skippedDocuments: Int = 0,
        /** Foto profil yang filenya hilang; akun tetap diekspor tanpa foto. */
        val skippedProfileImages: Int = 0,
        /** SHA-256 (hex) isi data.json; dipakai auto-sync untuk melewati unggahan bila data tidak berubah. */
        val payloadSha256: String = ""
    ) {
        val hasWarnings: Boolean get() = skippedDocuments > 0 || skippedProfileImages > 0
    }

    /** Lampiran disimpan sebagai File (bukan ByteArray) agar memori tidak membengkak. */
    internal data class Snapshot(
        val users: List<UserEntity>,
        val courses: List<CourseEntity>,
        val schedules: List<ScheduleEntity>,
        val tasks: List<TaskEntity>,
        val documents: List<AcademicDocumentEntity>,
        val events: List<AcademicEventEntity>,
        val projects: List<ProjectEntity>,
        val milestones: List<ProjectMilestoneEntity>,
        val assets: Map<String, File>,
        val settings: BackupSettings,
        val createdAt: Long
    )

    internal data class BackupSettings(
        val targetCredits: Int,
        val taskNotificationsEnabled: Boolean,
        val classRemindersEnabled: Boolean,
        val soundEnabled: Boolean,
        val vibrationEnabled: Boolean,
        val reminderMinutes: Int,
        val semesterStartDates: Map<String, String> = emptyMap()
    )

    /**
     * Mengekspor ke [output]. ZIP ditulis lebih dulu ke file sementara di cacheDir, jadi kegagalan
     * (mis. melewati batas ukuran) tidak meninggalkan arsip setengah jadi di tujuan.
     */
    suspend fun export(
        context: Context,
        database: TemanKuliahDatabase,
        output: OutputStream,
        maxUncompressedBytes: Long = MAX_ARCHIVE_UNCOMPRESSED
    ): Summary = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("backup_export_", ".tmp", context.cacheDir)
        try {
            val summary = exportToFile(context, database, temp, maxUncompressedBytes)
            FileInputStream(temp).use { it.copyTo(output) }
            output.flush()
            summary
        } finally {
            temp.delete()
        }
    }

    /** Hasil tahap persiapan ekspor: semua yang dibutuhkan untuk menulis ZIP atau menghitung hash isinya. */
    private class PreparedExport(
        val snapshot: Snapshot,
        val assets: Map<String, File>,
        val payloadBytes: ByteArray,
        val manifestBytes: ByteArray,
        val skippedDocuments: Int,
        val skippedProfileImages: Int
    )

    /**
     * Membaca snapshot, memvalidasi, meng-encode data.json, dan memeriksa semua batas ukuran. Belum menulis
     * apa pun ke disk. Gagal lebih awal bila total byte tak terkompresi melewati [maxUncompressedBytes].
     */
    private suspend fun prepareExport(
        context: Context,
        database: TemanKuliahDatabase,
        maxUncompressedBytes: Long
    ): PreparedExport {
        val raw = database.withTransaction {
            Snapshot(
                users = database.userDao().getAllUsersOnce().map { it.forExport() },
                courses = database.courseDao().getAllCoursesOnce(),
                schedules = database.scheduleDao().getAllSchedulesOnce(),
                tasks = database.taskDao().getAllOnce(),
                documents = database.academicDocumentDao().getAllOnce(),
                events = database.academicEventDao().getAllOnce(),
                projects = database.projectDao().getAllOnce(),
                milestones = database.projectMilestoneDao().getAllOnce(),
                assets = emptyMap(),
                settings = readSettings(context),
                createdAt = System.currentTimeMillis()
            )
        }

        // Temukan file lampiran. Yang hilang dilewati (bukan menggagalkan export), dan metadatanya
        // tidak ikut diekspor supaya arsip tidak berisi dokumen tanpa file.
        val sources = linkedMapOf<String, File>()
        val missing = mutableSetOf<String>()
        fun locate(path: String): File? {
            sources[path]?.let { return it }
            if (path in missing) return null
            val file = StudentFileStore.fileFor(context, path)
            if (file == null) {
                missing += path
                return null
            }
            if (file.length() > MAX_ENTRY_BYTES) {
                throw IOException("Lampiran terlalu besar untuk dicadangkan (${formatMb(file.length())}, maksimal ${formatMb(MAX_ENTRY_BYTES)}): $path")
            }
            sources[path] = file
            return file
        }
        var skippedProfileImages = 0
        val users = raw.users.map { user ->
            val path = user.profileImagePath
            if (path != null && locate(path) == null) {
                skippedProfileImages++
                user.copy(profileImagePath = null)
            } else user
        }
        val documents = raw.documents.filter { locate(it.storedPath) != null }
        val skippedDocuments = raw.documents.size - documents.size
        val assets: Map<String, File> = sources.mapKeys { "assets/${it.key}" }

        // Tidak ada sanitasi diam-diam: data yang tidak valid ditolak dengan pesan jelas.
        val snapshot = raw.copy(users = users, documents = documents, assets = assets)
        validateSnapshot(snapshot)

        val payloadBytes = encodeSnapshot(snapshot).toByteArray(Charsets.UTF_8)
        val manifestBytes = JSONObject()
            .put("format", FORMAT)
            .put("formatVersion", FORMAT_VERSION)
            .put("databaseSchemaVersion", DATABASE_SCHEMA_VERSION)
            .put("createdAt", snapshot.createdAt)
            .put("payload", "data.json")
            .put("attachmentCount", assets.size)
            .toString(2)
            .toByteArray(Charsets.UTF_8)

        // Hitung total sebelum menulis apa pun supaya gagal lebih awal dengan pesan yang jelas.
        if (payloadBytes.size > MAX_ENTRY_BYTES) {
            throw IOException("Data teks cadangan terlalu besar (${formatMb(payloadBytes.size.toLong())}, maksimal ${formatMb(MAX_ENTRY_BYTES)}).")
        }
        if (assets.size + 2 > MAX_ENTRIES) {
            throw IOException("Terlalu banyak lampiran untuk dicadangkan (${assets.size}, maksimal ${MAX_ENTRIES - 2}).")
        }
        val totalBytes = manifestBytes.size.toLong() + payloadBytes.size.toLong() + assets.values.sumOf { it.length() }
        if (totalBytes > maxUncompressedBytes) {
            throw IOException("Total data cadangan ${formatMb(totalBytes)} melebihi batas ${formatMb(maxUncompressedBytes)}. Hapus sebagian dokumen atau foto, lalu coba lagi.")
        }
        return PreparedExport(snapshot, assets, payloadBytes, manifestBytes, skippedDocuments, skippedProfileImages)
    }

    /**
     * SHA-256 (hex) isi data.json yang AKAN diekspor, tanpa menulis ZIP. Metadata dokumen/foto (path unik,
     * updatedAt) ikut di dalamnya, jadi penggantian lampiran juga mengubah hash.
     */
    suspend fun payloadSha256(
        context: Context,
        database: TemanKuliahDatabase,
        maxUncompressedBytes: Long = MAX_ARCHIVE_UNCOMPRESSED
    ): String = withContext(Dispatchers.IO) {
        sha256Hex(prepareExport(context, database, maxUncompressedBytes).payloadBytes)
    }

    /**
     * Mengekspor langsung ke [target] lewat stream (tanpa ByteArrayOutputStream). Gagal lebih awal,
     * sebelum menulis apa pun, bila total byte tak terkompresi melewati [maxUncompressedBytes].
     * Dokumen yang filenya hilang dilewati dan dihitung di [Summary.skippedDocuments].
     */
    suspend fun exportToFile(
        context: Context,
        database: TemanKuliahDatabase,
        target: File,
        maxUncompressedBytes: Long = MAX_ARCHIVE_UNCOMPRESSED
    ): Summary = withContext(Dispatchers.IO) {
        val prepared = prepareExport(context, database, maxUncompressedBytes)
        val snapshot = prepared.snapshot
        val assets = prepared.assets
        val payloadBytes = prepared.payloadBytes
        val manifestBytes = prepared.manifestBytes
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(target))).use { zip ->
                var written = 0L
                fun account(count: Int) {
                    written += count
                    // Jaga-jaga bila ukuran file berubah setelah dihitung di atas.
                    if (written > maxUncompressedBytes) throw IOException("Total data cadangan melebihi batas ${formatMb(maxUncompressedBytes)}.")
                }
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifestBytes)
                account(manifestBytes.size)
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(payloadBytes)
                account(payloadBytes.size)
                zip.closeEntry()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                for ((name, file) in assets) {
                    zip.putNextEntry(ZipEntry(name))
                    var entryBytes = 0L
                    FileInputStream(file).use { input ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            if (entryBytes > MAX_ENTRY_BYTES) throw IOException("Lampiran terlalu besar untuk dicadangkan: $name")
                            account(read)
                            zip.write(buffer, 0, read)
                        }
                    }
                    zip.closeEntry()
                }
            }
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
        Summary(
            users = snapshot.users.size,
            courses = snapshot.courses.size,
            schedules = snapshot.schedules.size,
            tasks = snapshot.tasks.size,
            documents = snapshot.documents.size,
            attachments = assets.size,
            createdAt = snapshot.createdAt,
            academicEvents = snapshot.events.size,
            projects = snapshot.projects.size,
            milestones = snapshot.milestones.size,
            skippedDocuments = prepared.skippedDocuments,
            skippedProfileImages = prepared.skippedProfileImages,
            payloadSha256 = sha256Hex(payloadBytes)
        )
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    suspend fun preview(context: Context, input: InputStream): Summary = withContext(Dispatchers.IO) {
        readAndValidate(context, input).use { it.snapshot.toSummary() }
    }

    suspend fun restore(
        context: Context,
        database: TemanKuliahDatabase,
        input: InputStream,
        activeUserEmail: String? = null
    ): Summary = withContext(Dispatchers.IO) {
        readAndValidate(context, input).use { parsed ->
            applyRestore(context, database, parsed.snapshot, activeUserEmail)
        }
    }

    private fun Snapshot.toSummary() = Summary(
        users.size, courses.size, schedules.size, tasks.size, documents.size, assets.size,
        createdAt, events.size, projects.size, milestones.size
    )

    private suspend fun applyRestore(
        context: Context,
        database: TemanKuliahDatabase,
        snapshot: Snapshot,
        activeUserEmail: String?
    ): Summary {
        // Attachments are copied to unique staging destinations before changing Room.
        // This avoids overwriting any file still referenced by the live database.
        val oldPaths = database.withTransaction {
            database.userDao().getAllUsersOnce().mapNotNull { it.profileImagePath } +
                database.academicDocumentDao().getAllOnce().map { it.storedPath }
        }.toSet()
        // Kumpulkan ID lama sebelum data dihapus, supaya alarmnya bisa dibatalkan walau barisnya sudah hilang.
        val (oldTaskIds, oldScheduleIds, oldEventIds) = database.withTransaction {
            Triple(
                database.taskDao().getAllOnce().map { it.id },
                database.scheduleDao().getAllSchedulesOnce().map { it.id },
                database.academicEventDao().getAllOnce().map { it.id }
            )
        }
        val rewrittenUsers = mutableListOf<UserEntity>()
        val rewrittenDocuments = mutableListOf<AcademicDocumentEntity>()
        val copiedTargets = mutableListOf<File>()
        val pathMap = mutableMapOf<String, String>()
        try {
            // Isi lampiran sudah divalidasi di readAndValidate; di sini cukup disalin dari file sementara.
            for ((archiveName, source) in snapshot.assets) {
                val oldRelative = archiveName.removePrefix("assets/")
                if (!StudentFileStore.isSafeRelativePath(oldRelative)) throw IOException("Path lampiran tidak aman.")
                val destinationRelative = when {
                    oldRelative.startsWith("profile_images/") -> "profile_images/restored_${UUID.randomUUID()}${safeImageExtension(oldRelative)}"
                    oldRelative.startsWith("academic_documents/") -> "academic_documents/restored_${UUID.randomUUID()}${safeDocumentExtension(oldRelative)}"
                    else -> throw IOException("Jenis lampiran tidak didukung: $oldRelative")
                }
                val target = File(context.filesDir, destinationRelative)
                if (!target.parentFile!!.exists() && !target.parentFile!!.mkdirs()) throw IOException("Tidak dapat menyiapkan folder lampiran.")
                copiedTargets += target
                source.copyTo(target, overwrite = false)
                pathMap[oldRelative] = destinationRelative
            }
            val targetUser = if (activeUserEmail != null) {
                snapshot.users.firstOrNull { it.email.equals(activeUserEmail, ignoreCase = true) }
                    ?: snapshot.users.firstOrNull { it.isLoggedIn && it.authType != AUTH_GUEST }
                    ?: snapshot.users.firstOrNull { it.nim.isNotBlank() }
                    ?: snapshot.users.firstOrNull { it.isLoggedIn }
                    ?: snapshot.users.firstOrNull()
            } else null

            snapshot.users.forEach { user ->
                val newPath = user.profileImagePath?.let { pathMap[it] ?: throw IOException("Foto profil tidak ada di arsip: $it") }
                val makeLoggedIn = if (activeUserEmail != null) {
                    user.id == targetUser?.id
                } else false
                val resolvedUser = if (makeLoggedIn) {
                    user.copy(
                        profileImagePath = newPath,
                        isLoggedIn = true,
                        email = activeUserEmail ?: user.email
                    )
                } else {
                    user.copy(profileImagePath = newPath, isLoggedIn = false)
                }
                rewrittenUsers += resolvedUser
            }
            snapshot.documents.forEach { document ->
                val newPath = pathMap[document.storedPath] ?: throw IOException("File ${document.type} tidak ada di arsip.")
                rewrittenDocuments += document.copy(storedPath = newPath)
            }
            database.withTransaction {
                database.academicDocumentDao().deleteAll()
                database.academicEventDao().deleteAll()
                database.projectMilestoneDao().deleteAll()
                database.projectDao().deleteAll()
                database.taskDao().deleteAll()
                database.scheduleDao().deleteAllSchedules()
                database.courseDao().deleteAllCourses()
                database.userDao().deleteAllUsers()
                database.userDao().insertUsers(rewrittenUsers)
                database.courseDao().insertCourses(snapshot.courses)
                database.scheduleDao().insertSchedules(snapshot.schedules)
                database.taskDao().insertAll(snapshot.tasks)
                database.academicDocumentDao().insertAll(rewrittenDocuments)
                database.academicEventDao().insertAll(snapshot.events)
                database.projectDao().insertAll(snapshot.projects)
                database.projectMilestoneDao().insertAll(snapshot.milestones)
            }
        } catch (error: Exception) {
            rethrowIfCancellation(error)
            copiedTargets.forEach { it.delete() }
            throw error
        }
        AppStorage.getPreferences(context).edit {
            if (snapshot.settings.targetCredits in 1..360) putInt("target_sks", snapshot.settings.targetCredits) else remove("target_sks")
            putBoolean("notifications_tasks_enabled", snapshot.settings.taskNotificationsEnabled)
            putBoolean("notifications_classes_enabled", snapshot.settings.classRemindersEnabled)
            putBoolean("notifications_sound_enabled", snapshot.settings.soundEnabled)
            putBoolean("notifications_vibration_enabled", snapshot.settings.vibrationEnabled)
            putInt("notifications_reminder_minutes", snapshot.settings.reminderMinutes)
            // Sudah divalidasi di validateSnapshot; dicek ulang di sini sebagai pagar terakhir supaya
            // tidak ada kunci selain semester_start_date_<n> (mis. target_sks) yang bisa tertimpa.
            snapshot.settings.semesterStartDates.forEach { (key, date) ->
                if (semesterStartDateProblem(key, date) == null) putString(key, date)
            }
        }
        // Alarm milik tugas/jadwal lama dibatalkan dulu (setelah restore berhasil, supaya restore yang gagal
        // tidak meninggalkan data lama tanpa alarm), baru alarm dijadwalkan ulang dari data hasil restore.
        runCatching { com.temankuliah.app.data.notifications.ReminderScheduler.cancelAlarms(context, oldTaskIds, oldScheduleIds, oldEventIds) }
        runCatching { com.temankuliah.app.data.notifications.ReminderScheduler.rescheduleForActiveUser(context, database) }
        oldPaths.forEach { old ->
            if (StudentFileStore.isSafeRelativePath(old)) File(context.filesDir, old).delete()
        }
        return snapshot.toSummary()
    }

    private fun formatMb(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))

    /**
     * Hanya akun lokal (email + sandi) yang membawa hash sandi, supaya tetap bisa masuk setelah dipulihkan.
     * Akun tamu tidak punya sandi.
     */
    private fun UserEntity.forExport(): UserEntity {
        val exportedHash = when {
            authType != AUTH_LOCAL || password.isEmpty() -> ""
            LocalPasswordHasher.isEncoded(password) -> password
            else -> LocalPasswordHasher.hash(password)
        }
        return copy(password = exportedHash)
    }
}
