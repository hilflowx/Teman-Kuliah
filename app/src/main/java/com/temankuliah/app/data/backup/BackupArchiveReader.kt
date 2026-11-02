package com.temankuliah.app.data.backup

import android.content.Context
import com.temankuliah.app.data.backup.BackupAssetFiles.validateAcademicDocumentFile
import com.temankuliah.app.data.backup.BackupAssetFiles.validateProfileImageFile
import com.temankuliah.app.data.backup.BackupSnapshotCodec.toAcademicEvents
import com.temankuliah.app.data.backup.BackupSnapshotCodec.toCourses
import com.temankuliah.app.data.backup.BackupSnapshotCodec.toDocuments
import com.temankuliah.app.data.backup.BackupSnapshotCodec.toProjectMilestones
import com.temankuliah.app.data.backup.BackupSnapshotCodec.toProjects
import com.temankuliah.app.data.backup.BackupSnapshotCodec.toSchedules
import com.temankuliah.app.data.backup.BackupSnapshotCodec.toTasks
import com.temankuliah.app.data.backup.BackupSnapshotCodec.toUsers
import com.temankuliah.app.data.backup.BackupSnapshotValidator.validateSnapshot
import com.temankuliah.app.data.backup.LocalDataArchive.BackupSettings
import com.temankuliah.app.data.backup.LocalDataArchive.FORMAT
import com.temankuliah.app.data.backup.LocalDataArchive.FORMAT_VERSION
import com.temankuliah.app.data.backup.LocalDataArchive.MAX_ARCHIVE_UNCOMPRESSED
import com.temankuliah.app.data.backup.LocalDataArchive.MAX_ENTRIES
import com.temankuliah.app.data.backup.LocalDataArchive.MAX_ENTRY_BYTES
import com.temankuliah.app.data.backup.LocalDataArchive.Snapshot
import com.temankuliah.app.data.local.DATABASE_SCHEMA_VERSION
import com.temankuliah.app.domain.LocalPasswordHasher
import com.temankuliah.app.domain.rethrowIfCancellation
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

/** Membaca dan memvalidasi arsip cadangan ZIP ke bentuk [LocalDataArchive.Snapshot]; berkas sementara dibersihkan lewat [ParsedArchive]. */
internal object BackupArchiveReader {
    /** Hasil baca arsip: lampiran diekstrak ke folder sementara yang dihapus saat [close]. */
    internal class ParsedArchive(val snapshot: Snapshot, private val workDir: File) : Closeable {
        override fun close() { workDir.deleteRecursively() }
    }

    internal fun readAndValidate(context: Context, input: InputStream): ParsedArchive {
        // Lampiran diekstrak ke file sementara di cacheDir, bukan ditahan sebagai ByteArray di RAM.
        val workDir = File(context.cacheDir, "backup_restore_${UUID.randomUUID()}")
        if (!workDir.mkdirs()) throw IOException("Tidak dapat menyiapkan folder sementara untuk membaca cadangan.")
        try {
            return ParsedArchive(readSnapshot(input, workDir), workDir)
        } catch (error: Throwable) {
            workDir.deleteRecursively()
            throw error
        }
    }

    private fun readSnapshot(input: InputStream, workDir: File): Snapshot {
        var manifestBytes: ByteArray? = null
        var payloadBytes: ByteArray? = null
        val assets = linkedMapOf<String, File>()
        var entryCount = 0
        var uncompressedBytes = 0L
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.isDirectory) {
                    zip.closeEntry()
                    entry = zip.nextEntry
                    continue
                }
                if (++entryCount > MAX_ENTRIES) throw IOException("Cadangan berisi terlalu banyak file.")
                val name = entry.name
                if (name.startsWith("/") || name.contains("..") || name.contains('\\')) throw IOException("Path di dalam cadangan tidak aman.")
                if (name != "manifest.json" && name != "data.json" && !name.startsWith("assets/")) throw IOException("Entri cadangan tidak dikenal: $name")
                val isJson = name == "manifest.json" || name == "data.json"
                val duplicate = when (name) {
                    "manifest.json" -> manifestBytes != null
                    "data.json" -> payloadBytes != null
                    else -> name in assets
                }
                if (duplicate) throw IOException("Cadangan memiliki nama file ganda: $name")
                val memorySink = if (isJson) ByteArrayOutputStream() else null
                val assetFile = if (isJson) null else File(workDir, "asset_${assets.size}")
                val sink: OutputStream = memorySink ?: BufferedOutputStream(FileOutputStream(assetFile!!))
                try {
                    sink.use {
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var entryBytes = 0L
                        var read: Int
                        while (zip.read(buffer).also { read = it } >= 0) {
                            entryBytes += read
                            uncompressedBytes += read
                            if (entryBytes > MAX_ENTRY_BYTES || uncompressedBytes > MAX_ARCHIVE_UNCOMPRESSED) throw IOException("Ukuran cadangan melampaui batas aman.")
                            sink.write(buffer, 0, read)
                        }
                    }
                } catch (error: Throwable) {
                    assetFile?.delete()
                    throw error
                }
                when (name) {
                    "manifest.json" -> manifestBytes = memorySink!!.toByteArray()
                    "data.json" -> payloadBytes = memorySink!!.toByteArray()
                    else -> assets[name] = assetFile!!
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val manifestJson = manifestBytes ?: throw IOException("Manifest cadangan tidak ditemukan.")
        val payloadJson = payloadBytes ?: throw IOException("Data cadangan tidak ditemukan.")
        val manifest = try { JSONObject(String(manifestJson, Charsets.UTF_8)) } catch (ignored: Exception) { rethrowIfCancellation(ignored); throw IOException("Manifest cadangan rusak.") }
        if (manifest.optString("format") != FORMAT) throw IOException("Format file bukan cadangan TemanKuliah.")
        val version = manifest.optInt("formatVersion", -1)
        if (version != FORMAT_VERSION) throw IOException("Versi cadangan $version tidak didukung aplikasi ini.")
        val schemaVersion = manifest.optInt("databaseSchemaVersion", -1)
        if (schemaVersion < 1) throw IOException("Versi skema database di cadangan tidak valid.")
        if (schemaVersion > DATABASE_SCHEMA_VERSION) {
            throw IOException("Cadangan dibuat oleh versi aplikasi yang lebih baru (skema database $schemaVersion; aplikasi ini mendukung sampai $DATABASE_SCHEMA_VERSION). Perbarui aplikasi lalu coba lagi.")
        }
        if (manifest.optString("payload") != "data.json") throw IOException("Referensi data pada manifest tidak valid.")
        if (manifest.optInt("attachmentCount", -1) < 0) throw IOException("Jumlah lampiran pada manifest tidak valid.")
        val payload = try { JSONObject(String(payloadJson, Charsets.UTF_8)) } catch (ignored: Exception) { rethrowIfCancellation(ignored); throw IOException("Data cadangan tidak dapat dibaca.") }
        if (payload.optInt("dataVersion", -1) != FORMAT_VERSION) throw IOException("Versi struktur data tidak didukung.")
        if (manifest.optInt("attachmentCount", -1) != assets.size) throw IOException("Jumlah lampiran tidak sesuai dengan manifest cadangan.")
        val snapshot = try {
            Snapshot(
                users = payload.getJSONArray("users").toUsers().map { user ->
                    val archivedPassword = user.password
                    if (archivedPassword.isEmpty() || LocalPasswordHasher.isEncoded(archivedPassword)) user
                    else if (archivedPassword.startsWith("pbkdf2-sha256$") || archivedPassword.startsWith("pbkdf2-sha1$")) throw IOException("Hash kata sandi dalam cadangan rusak.")
                    else user.copy(password = LocalPasswordHasher.hash(archivedPassword))
                },
                courses = payload.getJSONArray("courses").toCourses(),
                schedules = payload.getJSONArray("schedules").toSchedules(),
                tasks = payload.getJSONArray("tasks").toTasks(),
                documents = payload.getJSONArray("documents").toDocuments(),
                events = (payload.optJSONArray("events") ?: JSONArray()).toAcademicEvents(),
                projects = (payload.optJSONArray("projects") ?: JSONArray()).toProjects(),
                milestones = (payload.optJSONArray("milestones") ?: JSONArray()).toProjectMilestones(),
                assets = assets,
                settings = payload.getJSONObject("preferences").let { o ->
                    val startDates = linkedMapOf<String, String>()
                    if (o.has("startDates")) {
                        val sd = o.optJSONObject("startDates") ?: throw IllegalArgumentException("startDates harus berupa objek.")
                        sd.keys().forEach { k ->
                            // Hanya string asli yang diterima; angka/boolean tidak dipaksa menjadi teks.
                            val value = sd.opt(k) as? String
                                ?: throw IllegalArgumentException("nilai preferensi '${k.take(40)}' harus berupa teks.")
                            startDates[k] = value
                        }
                    }
                    BackupSettings(
                        targetCredits = o.getInt("targetCredits"),
                        taskNotificationsEnabled = o.getBoolean("taskNotificationsEnabled"),
                        classRemindersEnabled = o.getBoolean("classRemindersEnabled"),
                        soundEnabled = o.getBoolean("soundEnabled"),
                        vibrationEnabled = o.getBoolean("vibrationEnabled"),
                        reminderMinutes = o.getInt("reminderMinutes"),
                        semesterStartDates = startDates
                    )
                },
                createdAt = manifest.optLong("createdAt", 0L)
            )
        } catch (error: Exception) {
            rethrowIfCancellation(error)
            throw IOException("Struktur data cadangan tidak lengkap atau tidak valid: ${error.message}")
        }
        // Tidak ada perbaikan diam-diam: data yang tidak valid menggagalkan restore dengan pesan jelas.
        validateSnapshot(snapshot)
        val referenced = (snapshot.users.mapNotNull { it.profileImagePath } + snapshot.documents.map { it.storedPath }).map { "assets/$it" }.toSet()
        if (referenced != assets.keys) {
            val missing = referenced - assets.keys
            if (missing.isNotEmpty()) throw IOException("Lampiran wajib tidak ada: ${missing.first()}")
            throw IOException("Cadangan berisi lampiran yang tidak direferensikan.")
        }
        snapshot.assets.forEach { (entryName, file) ->
            val relative = entryName.removePrefix("assets/")
            if (relative.startsWith("academic_documents/")) validateAcademicDocumentFile(relative, file)
            if (relative.startsWith("profile_images/")) validateProfileImageFile(file)
        }
        return snapshot
    }
}
