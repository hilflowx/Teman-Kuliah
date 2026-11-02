package com.temankuliah.app.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.temankuliah.app.data.files.StagedAcademicDocument
import com.temankuliah.app.data.files.StudentFileStore
import com.temankuliah.app.data.local.AcademicDocumentDao
import com.temankuliah.app.data.local.AcademicDocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/** Dokumen KRS/KHS per semester. */
class AcademicDocumentRepository(
    private val academicDocumentDao: AcademicDocumentDao
) {
    // Dokumen akademik dan tugas disimpan lokal serta dipisahkan menurut semester.
    fun getDocumentsFlow(userId: Long): Flow<List<AcademicDocumentEntity>> = academicDocumentDao.observeByUser(userId)

    /**
     * Menyimpan file pilihan (PDF/foto/teks) sebagai DRAF. Dokumen KRS/KHS lama tidak berubah sampai
     * [commitStagedAcademicDocument] dipanggil setelah pengguna mengonfirmasi pratinjau.
     */
    suspend fun stageAcademicDocumentFile(
        context: Context,
        resolver: ContentResolver,
        userId: Long,
        semester: Int,
        type: String,
        uri: Uri,
        displayName: String
    ): StagedAcademicDocument = StudentFileStore.stageAcademicDocument(
        context = context,
        resolver = resolver,
        userId = userId,
        semester = semester,
        type = type,
        uri = uri,
        displayName = displayName
    )

    /** Foto kamera sebagai DRAF; dokumen lama tidak berubah sampai [commitStagedAcademicDocument]. */
    suspend fun stageAcademicPhoto(
        context: Context,
        userId: Long,
        semester: Int,
        type: String,
        photo: File,
        displayName: String
    ): StagedAcademicDocument = StudentFileStore.stageAcademicPhoto(context, userId, semester, type, photo, displayName)

    /** Pengguna mengonfirmasi impor: draf menggantikan dokumen lama (baris Room dan filenya). */
    suspend fun commitStagedAcademicDocument(context: Context, staged: StagedAcademicDocument): AcademicDocumentEntity =
        StudentFileStore.commitStagedAcademicDocument(context, academicDocumentDao, staged)

    /** Pratinjau dibatalkan: draf dihapus, dokumen lama dibiarkan. */
    suspend fun discardStagedAcademicDocument(context: Context, staged: StagedAcademicDocument) =
        StudentFileStore.discardStagedAcademicDocument(context, staged)

    suspend fun reprocessAcademicDocument(context: Context, document: AcademicDocumentEntity): AcademicDocumentEntity =
        StudentFileStore.reprocessAcademicDocument(context, academicDocumentDao, document)

    suspend fun deleteAcademicDocument(document: AcademicDocumentEntity) = withContext(Dispatchers.IO) {
        val existing = academicDocumentDao.find(document.userId, document.semester, document.type)
        if (existing?.id == document.id) academicDocumentDao.delete(document)
    }
}
