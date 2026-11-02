package com.temankuliah.app.data.files

import com.temankuliah.app.domain.rethrowIfCancellation
import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.temankuliah.app.data.local.AcademicDocumentDao
import com.temankuliah.app.data.local.AcademicDocumentEntity
import com.temankuliah.app.data.local.TemanKuliahDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Dokumen KRS/KHS yang sudah dibaca tetapi BELUM dikonfirmasi pengguna. Filenya hanya ada di folder draf
 * dan tidak pernah dirujuk Room, sehingga dokumen lama semester yang sama tidak tersentuh sampai
 * [StudentFileStore.commitStagedAcademicDocument] dipanggil.
 */
data class StagedAcademicDocument(
    val userId: Long,
    val semester: Int,
    val type: String,
    val displayName: String,
    /** Path relatif di folder draf (`academic_drafts/...`). */
    val draftPath: String,
    /** Path relatif tujuan (`academic_documents/...`) setelah dokumen dikonfirmasi. */
    val finalPath: String,
    val extractedText: String,
    val processingMessage: String
)

/** Keeps profile images and academic documents in app-private storage using relative paths. */
object StudentFileStore {
    private const val MAX_DOCUMENT_BYTES = 40L * 1024L * 1024L
    private const val DOCUMENT_DIRECTORY = "academic_documents"
    private const val DRAFT_DIRECTORY = "academic_drafts"

    /** Draf yang sedang dipakai pratinjau. Dilindungi dari [cleanupOrphans] selama masih terdaftar di sini. */
    private val activeDrafts: MutableSet<String> = ConcurrentHashMap.newKeySet<String>()

    /** Menyerialkan pemindahan draf -> dokumen final dengan [cleanupOrphans], supaya file final tidak sempat terhapus. */
    private val fileMutex = Mutex()

    suspend fun saveProfileImage(context: Context, userId: Long, uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty().lowercase()
        require(mime.startsWith("image/")) { "File yang dipilih bukan gambar." }
        val directory = File(context.filesDir, "profile_images").apply { if (!exists() && !mkdirs()) throw IOException("Tidak dapat membuat folder foto profil.") }
        val extension = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
        val name = "profile_${userId}_${UUID.randomUUID()}.$extension"
        val destination = File(directory, name)
        val temp = File(directory, "$name.tmp")
        try {
            resolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 20L * 1024L * 1024L) throw IOException("Ukuran foto maksimal 20 MB.")
                        output.write(buffer, 0, count)
                    }
                }
            } ?: throw IOException("File foto tidak dapat dibaca.")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temp.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Format gambar tidak valid atau tidak didukung perangkat." }
            require(temp.length() in 1..(20L * 1024L * 1024L)) { "Ukuran foto maksimal 20 MB." }
            if (!temp.renameTo(destination)) throw IOException("Gagal menyimpan foto profil.")
            "profile_images/$name"
        } catch (error: Exception) {
            rethrowIfCancellation(error)
            temp.delete()
            destination.delete()
            throw error
        }
    }

    /**
     * Menyimpan file yang dipilih sebagai DRAF. Dokumen lama (baris Room dan filenya) tidak diubah;
     * penggantian baru terjadi lewat [commitStagedAcademicDocument] setelah pengguna mengonfirmasi pratinjau.
     * Bila pratinjau dibatalkan, panggil [discardStagedAcademicDocument].
     */
    suspend fun stageAcademicDocument(
        context: Context,
        resolver: ContentResolver,
        userId: Long,
        semester: Int,
        type: String,
        uri: Uri,
        displayName: String
    ): StagedAcademicDocument = withContext(Dispatchers.IO) {
        requireDocumentTarget(type, semester)
        val mime = resolver.getType(uri).orEmpty().lowercase()
        val extension = supportedExtension(displayName, mime)
            ?: throw IOException("Format tidak didukung. Pilih PDF, foto, TXT, CSV, TSV, atau Markdown.")
        val name = "u${userId}_s${semester}_${type.lowercase()}_${UUID.randomUUID()}.$extension"
        val (draftPath, draft) = openDraft(context, name)
        try {
            var total = 0L
            resolver.openInputStream(uri)?.use { input ->
                draft.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_DOCUMENT_BYTES) throw IOException("Ukuran dokumen melebihi batas 40 MB.")
                        output.write(buffer, 0, count)
                    }
                }
            } ?: throw IOException("Dokumen tidak dapat dibaca.")
            require(total >= 1) { "Dokumen kosong." }
            validateDocumentFile(draft, extension)
            val extraction = try {
                AcademicDocumentProcessor.extract(draft)
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                AcademicDocumentProcessor.Extraction(
                    "",
                    "File berhasil dipilih tetapi teks belum terbaca: ${error.message ?: "proses ekstraksi gagal"}. Periksa file lalu tekan Coba proses lagi."
                )
            }
            StagedAcademicDocument(
                userId = userId,
                semester = semester,
                type = type,
                displayName = displayName.ifBlank { "$type Semester $semester.$extension" }.take(180),
                draftPath = draftPath,
                finalPath = "$DOCUMENT_DIRECTORY/$name",
                extractedText = extraction.text,
                processingMessage = extraction.message
            )
        } catch (error: Exception) {
            abandonDraft(context, draftPath)
            rethrowIfCancellation(error)
            throw error
        }
    }

    /**
     * Versi kamera dari [stageAcademicDocument]: foto sementara disalin menjadi draf, lalu dibaca. Foto sumber
     * baru dihapus setelah draf berhasil dibuat; dokumen lama tidak disentuh.
     */
    @Suppress("LongParameterList") // Sejajar dengan stageAcademicDocument; semua argumen dipakai.
    suspend fun stageAcademicPhoto(
        context: Context,
        userId: Long,
        semester: Int,
        type: String,
        photo: File,
        displayName: String,
        extractor: suspend (File) -> AcademicDocumentProcessor.Extraction = { AcademicDocumentProcessor.extract(it) }
    ): StagedAcademicDocument = withContext(Dispatchers.IO) {
        requireDocumentTarget(type, semester)
        require(photo.isFile && photo.length() > 0) { "Foto kamera tidak ditemukan." }
        val name = "u${userId}_s${semester}_${type.lowercase()}_camera_${UUID.randomUUID()}.jpg"
        val (draftPath, draft) = openDraft(context, name)
        try {
            photo.copyTo(draft, overwrite = true)
            val extraction = extractor(draft)
            photo.delete()
            StagedAcademicDocument(
                userId = userId,
                semester = semester,
                type = type,
                displayName = displayName.ifBlank { "Foto Kamera $type Semester $semester.jpg" }.take(180),
                draftPath = draftPath,
                finalPath = "$DOCUMENT_DIRECTORY/$name",
                extractedText = extraction.text,
                processingMessage = extraction.message
            )
        } catch (error: Exception) {
            abandonDraft(context, draftPath)
            rethrowIfCancellation(error)
            throw error
        }
    }

    /**
     * Dipanggil setelah pengguna mengonfirmasi pratinjau: memindahkan draf ke folder dokumen, mengganti baris
     * Room semester/jenis yang sama, lalu menghapus file dokumen lama. Bila penyimpanan gagal, draf dikembalikan
     * apa adanya dan dokumen lama tetap utuh.
     */
    suspend fun commitStagedAcademicDocument(
        context: Context,
        dao: AcademicDocumentDao,
        staged: StagedAcademicDocument
    ): AcademicDocumentEntity = withContext(Dispatchers.IO) {
        require(isDraftPath(staged.draftPath) && staged.finalPath.startsWith("$DOCUMENT_DIRECTORY/") && isSafeRelativePath(staged.finalPath)) {
            "Draf dokumen tidak valid."
        }
        fileMutex.withLock {
            val draft = fileFor(context, staged.draftPath)
                ?: throw IOException("Draf dokumen tidak ditemukan. Pilih ulang dokumen.")
            val directory = File(context.filesDir, DOCUMENT_DIRECTORY)
            if (!directory.exists() && !directory.mkdirs()) throw IOException("Tidak dapat membuat folder dokumen.")
            val destination = File(context.filesDir, staged.finalPath)
            if (!draft.renameTo(destination)) throw IOException("Penyimpanan penuh atau dokumen tidak dapat dipindahkan.")

            val (old, document, savedId) = try {
                val existing = dao.find(staged.userId, staged.semester, staged.type)
                val replacement = AcademicDocumentEntity(
                    id = existing?.id ?: 0,
                    userId = staged.userId,
                    semester = staged.semester,
                    type = staged.type,
                    displayName = staged.displayName,
                    storedPath = staged.finalPath,
                    extractedText = staged.extractedText,
                    processingMessage = staged.processingMessage
                )
                Triple(existing, replacement, dao.upsert(replacement))
            } catch (error: Exception) {
                // Kembalikan ke draf agar pengguna bisa mencoba lagi; dokumen lama belum disentuh.
                if (!destination.renameTo(draft)) destination.delete()
                rethrowIfCancellation(error)
                throw error
            }
            activeDrafts.remove(staged.draftPath)
            old?.storedPath?.takeIf { it != staged.finalPath }?.let { deleteRelative(context, it) }
            document.copy(id = savedId)
        }
    }

    /** Pratinjau dibatalkan: hapus draf. Dokumen lama (baris Room dan filenya) dibiarkan. */
    suspend fun discardStagedAcademicDocument(context: Context, staged: StagedAcademicDocument) {
        if (!isDraftPath(staged.draftPath)) return
        withContext(NonCancellable + Dispatchers.IO) { abandonDraft(context, staged.draftPath) }
    }

    private fun requireDocumentTarget(type: String, semester: Int) {
        require(type == "KRS" || type == "KHS") { "Jenis dokumen tidak dikenal." }
        require(semester >= 1) { "Semester tidak valid (minimal Semester 1)." }
    }

    private fun isDraftPath(path: String): Boolean = path.startsWith("$DRAFT_DIRECTORY/") && isSafeRelativePath(path)

    /** Mendaftarkan draf SEBELUM filenya dibuat, supaya [cleanupOrphans] tidak pernah melihatnya sebagai yatim. */
    private fun openDraft(context: Context, name: String): Pair<String, File> {
        val directory = File(context.filesDir, DRAFT_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) throw IOException("Tidak dapat membuat folder draf dokumen.")
        val relative = "$DRAFT_DIRECTORY/$name"
        activeDrafts.add(relative)
        return relative to File(directory, name)
    }

    private fun abandonDraft(context: Context, draftPath: String) {
        deleteRelative(context, draftPath)
        activeDrafts.remove(draftPath)
    }

    suspend fun reprocessAcademicDocument(
        context: Context,
        dao: AcademicDocumentDao,
        document: AcademicDocumentEntity
    ): AcademicDocumentEntity = withContext(Dispatchers.IO) {
        val file = fileFor(context, document.storedPath) ?: throw IOException("File dokumen tidak ditemukan. Unggah ulang dokumen tersebut.")
        val extraction = AcademicDocumentProcessor.extract(file)
        val updated = document.copy(extractedText = extraction.text, processingMessage = extraction.message, updatedAt = System.currentTimeMillis())
        dao.update(updated)
        updated
    }

    private fun supportedExtension(displayName: String, mime: String): String? {
        val supplied = displayName.substringAfterLast('.', "").lowercase()
        if (supplied in setOf("pdf", "jpg", "jpeg", "png", "webp", "bmp", "gif", "txt", "csv", "tsv", "md")) return supplied
        return when {
            mime == "application/pdf" -> "pdf"
            mime == "image/jpeg" -> "jpg"
            mime == "image/png" -> "png"
            mime == "image/webp" -> "webp"
            mime == "image/bmp" -> "bmp"
            mime == "image/gif" -> "gif"
            mime == "text/plain" -> "txt"
            mime == "text/csv" || mime == "application/csv" -> "csv"
            mime == "text/tab-separated-values" -> "tsv"
            mime == "text/markdown" -> "md"
            else -> null
        }
    }

    private fun validateDocumentFile(file: File, extension: String) {
        when (extension) {
            "pdf" -> {
                require(file.length() >= 8) { "File PDF kosong atau terlalu kecil." }
                val header = ByteArray(5)
                FileInputStream(file).use { if (it.read(header) != header.size) throw IOException("File PDF tidak lengkap.") }
                require(String(header, Charsets.US_ASCII) == "%PDF-") { "File tidak memiliki header PDF yang valid." }
            }
            "jpg", "jpeg", "png", "webp", "bmp", "gif" -> {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Gambar tidak valid atau formatnya tidak didukung." }
            }
        }
    }

    suspend fun cleanupOrphans(context: Context, database: TemanKuliahDatabase) = withContext(Dispatchers.IO) {
        // Mutex yang sama dengan commit: file final yang baru dipindahkan belum tentu sudah terlihat di Room.
        fileMutex.withLock {
            val referenced = mutableSetOf<String>()
            database.userDao().getAllUsersOnce().mapNotNull { it.profileImagePath }.forEach { referenced.add(it) }
            database.academicDocumentDao().getAllOnce().forEach { referenced.add(it.storedPath) }
            listOf("profile_images", DOCUMENT_DIRECTORY, DRAFT_DIRECTORY).forEach { directoryName ->
                val directory = File(context.filesDir, directoryName)
                directory.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val relative = "$directoryName/${file.name}"
                        // Draf yang sedang dipakai pratinjau dilindungi; draf sisa proses sebelumnya ikut terhapus.
                        if (relative !in referenced && relative !in activeDrafts) file.delete()
                    }
                }
            }
        }
    }

    fun fileFor(context: Context, relativePath: String): File? {
        if (!isSafeRelativePath(relativePath)) return null
        val root = context.filesDir.canonicalFile
        val file = File(root, relativePath).canonicalFile
        return file.takeIf { it.path.startsWith(root.path + File.separator) && it.isFile }
    }

    fun deleteRelative(context: Context, relativePath: String) {
        fileFor(context, relativePath)?.delete()
    }

    fun isSafeRelativePath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith("/") && !path.contains("..") && !path.contains('\\')
}
