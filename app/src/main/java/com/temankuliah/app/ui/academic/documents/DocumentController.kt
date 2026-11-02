package com.temankuliah.app.ui.academic.documents

import com.temankuliah.app.domain.DEFAULT_REMINDER_MINUTES
import com.temankuliah.app.domain.parseSemesterStartDate
import com.temankuliah.app.domain.semesterStartDateKey
import com.temankuliah.app.domain.rethrowIfCancellation
import android.net.Uri
import android.provider.OpenableColumns
import com.temankuliah.app.data.files.AcademicDocumentProcessor
import com.temankuliah.app.data.files.StagedAcademicDocument
import com.temankuliah.app.data.files.StudentFileStore
import com.temankuliah.app.data.local.AcademicDocumentEntity
import com.temankuliah.app.data.local.AppStorage
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.ui.FeatureContext
import com.temankuliah.app.ui.FeatureController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.Locale

internal class DocumentController(
    ctx: FeatureContext,
    private val onSemesterStartDateSet: (Int, String) -> Unit
) : FeatureController(ctx) {
    val academicDocuments: StateFlow<List<AcademicDocumentEntity>> = currentUser
        .flatMapLatest { user -> if (user != null) repository.getDocumentsFlow(user.id) else flowOf(emptyList()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun storedSemesterStartDate(semester: Int): String? =
        AppStorage.getPreferences(application).getString(semesterStartDateKey(semester), null)
            ?.takeIf { value -> parseSemesterStartDate(value) != null }

    private val _documentImportPreview = MutableStateFlow<DocumentImportPreviewState?>(null)
    val documentImportPreview: StateFlow<DocumentImportPreviewState?> = _documentImportPreview.asStateFlow()

    // Galat/proses simpan impor ditampilkan DI DALAM dialog pratinjau. Snackbar tertutup oleh dialog,
    // sehingga sebelumnya penyimpanan yang gagal tidak terlihat sama sekali oleh pengguna.
    private val _importSaveError = MutableStateFlow<String?>(null)
    val importSaveError: StateFlow<String?> = _importSaveError.asStateFlow()

    private val _isSavingImport = MutableStateFlow(false)
    val isSavingImport: StateFlow<Boolean> = _isSavingImport.asStateFlow()

    /**
     * Menampilkan pratinjau. Draf dari pratinjau sebelumnya (bila ada dan berbeda) dibuang supaya tidak
     * tertinggal di penyimpanan.
     */
    private fun showPreview(state: DocumentImportPreviewState) {
        val previous = _documentImportPreview.value?.stagedDocument
        _documentImportPreview.value = state
        if (previous != null && previous.draftPath != state.stagedDocument?.draftPath) {
            scope.launch { repository.discardStagedAcademicDocument(application, previous) }
        }
    }

    fun processAcademicDocumentUri(uri: Uri, semester: Int, type: String) {
        val user = currentUser.value ?: return
        scope.launch {
            // Draf yang dibuat tetapi belum diserahkan ke pratinjau harus dibuang bila langkah berikutnya gagal.
            var pendingDraft: StagedAcademicDocument? = null
            try {
                val displayName = application.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "$type-Semester-$semester"
                val staged = repository.stageAcademicDocumentFile(
                    context = application,
                    resolver = application.contentResolver,
                    userId = user.id,
                    semester = semester.coerceAtLeast(1),
                    type = type,
                    uri = uri,
                    displayName = displayName
                )
                pendingDraft = staged
                val detectedSemester = AcademicDocumentProcessor.extractSemesterNumber(staged.extractedText)
                val detectedYear = AcademicDocumentProcessor.extractAcademicYear(staged.extractedText)
                val targetSemester = (detectedSemester ?: semester).coerceIn(1, 14)
                val rows = AcademicDocumentProcessor.findAcademicRows(staged.extractedText, isKrs = (type == "KRS"))
                showPreview(
                    DocumentImportPreviewState(
                        semester = targetSemester,
                        type = type,
                        sourceName = displayName,
                        rawExtractedText = staged.extractedText,
                        rows = rows,
                        storedRelativePath = staged.draftPath,
                        stagedDocument = staged,
                        declaredTotals = declaredKrsTotals(type, staged.extractedText),
                        initialStartDateIso = storedSemesterStartDate(targetSemester),
                        detectedSemester = detectedSemester,
                        detectedAcademicYear = detectedYear
                    )
                )
                pendingDraft = null
                if (rows.isEmpty()) {
                    snackbar.value = "Dokumen terbaca. Silakan tambahkan baris mata kuliah secara manual di tabel pratinjau."
                } else {
                    val semNotice = if (detectedSemester != null) " (Terdeteksi Semester $detectedSemester)" else ""
                    snackbar.value = "Berhasil membaca ${rows.size} baris mata kuliah dari dokumen$semNotice."
                }
            } catch (error: Exception) {
                pendingDraft?.let { repository.discardStagedAcademicDocument(application, it) }
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal memproses dokumen."
            }
        }
    }

    fun processAcademicDocumentCamera(photoFile: File, semester: Int, type: String) {
        val user = currentUser.value ?: return
        scope.launch {
            var pendingDraft: StagedAcademicDocument? = null
            try {
                val safeSemester = semester.coerceAtLeast(1)
                val staged = repository.stageAcademicPhoto(
                    context = application,
                    userId = user.id,
                    semester = safeSemester,
                    type = type,
                    photo = photoFile,
                    displayName = "Foto Kamera $type Semester $safeSemester.jpg"
                )
                pendingDraft = staged
                val detectedSemester = AcademicDocumentProcessor.extractSemesterNumber(staged.extractedText)
                val detectedYear = AcademicDocumentProcessor.extractAcademicYear(staged.extractedText)
                val targetSemester = (detectedSemester ?: safeSemester).coerceIn(1, 14)
                val rows = AcademicDocumentProcessor.findAcademicRows(staged.extractedText, isKrs = (type == "KRS"))
                showPreview(
                    DocumentImportPreviewState(
                        semester = targetSemester,
                        type = type,
                        sourceName = "Foto Kamera $type",
                        rawExtractedText = staged.extractedText,
                        rows = rows,
                        storedRelativePath = staged.draftPath,
                        stagedDocument = staged,
                        declaredTotals = declaredKrsTotals(type, staged.extractedText),
                        initialStartDateIso = storedSemesterStartDate(targetSemester),
                        detectedSemester = detectedSemester,
                        detectedAcademicYear = detectedYear
                    )
                )
                pendingDraft = null
                if (rows.isEmpty()) {
                    snackbar.value = "Foto kamera berhasil diproses. Silakan lengkapi baris pada tabel pratinjau."
                } else {
                    val semNotice = if (detectedSemester != null) " (Terdeteksi Semester $detectedSemester)" else ""
                    snackbar.value = "Berhasil membaca ${rows.size} baris mata kuliah dari kamera$semNotice."
                }
            } catch (error: Exception) {
                pendingDraft?.let { repository.discardStagedAcademicDocument(application, it) }
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal memproses foto kamera."
            }
        }
    }

    fun processAcademicDocumentText(rawText: String, semester: Int, type: String) {
        val user = currentUser.value ?: return
        scope.launch {
            try {
                val detectedSemester = AcademicDocumentProcessor.extractSemesterNumber(rawText)
                val detectedYear = AcademicDocumentProcessor.extractAcademicYear(rawText)
                val targetSemester = (detectedSemester ?: semester).coerceIn(1, 14)
                val rows = AcademicDocumentProcessor.findAcademicRows(rawText, isKrs = (type == "KRS"))
                showPreview(
                    DocumentImportPreviewState(
                        semester = targetSemester,
                        type = type,
                        sourceName = "Input Teks Langsung",
                        rawExtractedText = rawText,
                        rows = rows,
                        storedRelativePath = null,
                        declaredTotals = declaredKrsTotals(type, rawText),
                        initialStartDateIso = storedSemesterStartDate(targetSemester),
                        detectedSemester = detectedSemester,
                        detectedAcademicYear = detectedYear
                    )
                )
                if (rows.isEmpty()) {
                    snackbar.value = "Teks dimasukkan. Silakan tambahkan baris pada tabel pratinjau."
                } else {
                    val semNotice = if (detectedSemester != null) " (Terdeteksi Semester $detectedSemester)" else ""
                    snackbar.value = "Berhasil memetakan ${rows.size} mata kuliah dari teks$semNotice."
                }
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal memproses teks."
            }
        }
    }

    fun updatePreviewRow(index: Int, row: AcademicDocumentProcessor.AcademicRow) {
        val current = _documentImportPreview.value ?: return
        _importSaveError.value = null
        if (index in current.rows.indices) {
            val updatedList = current.rows.toMutableList()
            updatedList[index] = row
            _documentImportPreview.value = current.copy(rows = updatedList)
        }
    }

    fun addPreviewRow(row: AcademicDocumentProcessor.AcademicRow) {
        val current = _documentImportPreview.value ?: return
        _importSaveError.value = null
        val updatedList = current.rows.toMutableList()
        updatedList.add(row.copy(no = updatedList.size + 1))
        _documentImportPreview.value = current.copy(rows = updatedList)
    }

    fun deletePreviewRow(index: Int) {
        val current = _documentImportPreview.value ?: return
        _importSaveError.value = null
        if (index in current.rows.indices) {
            val updatedList = current.rows.toMutableList()
            updatedList.removeAt(index)
            val reindexed = updatedList.mapIndexed { idx, item -> item.copy(no = idx + 1) }
            _documentImportPreview.value = current.copy(rows = reindexed)
        }
    }

    /**
     * Pratinjau dibatalkan: draf dihapus dan dokumen lama dibiarkan. Bila penyimpanan sedang berjalan, draf
     * dikelola oleh [savePreviewCourses] (di-commit bila sukses, dibuang bila gagal), jadi tidak dihapus di sini.
     */
    fun dismissDocumentPreview() {
        val staged = _documentImportPreview.value?.stagedDocument
        _documentImportPreview.value = null
        _importSaveError.value = null
        if (staged != null && !_isSavingImport.value) {
            scope.launch { repository.discardStagedAcademicDocument(application, staged) }
        }
    }

    /** Ubah semester target untuk pratinjau impor secara manual dari UI */
    fun updatePreviewSemester(newSemester: Int) {
        val current = _documentImportPreview.value ?: return
        val safe = newSemester.coerceIn(1, 14)
        _documentImportPreview.value = current.copy(
            semester = safe,
            initialStartDateIso = storedSemesterStartDate(safe)
        )
    }

    /** Baca ulang baris dari teks OCR yang sudah dikoreksi pengguna (menggantikan baris pratinjau). */
    fun reparsePreviewText(text: String) {
        val current = _documentImportPreview.value ?: return
        val rows = AcademicDocumentProcessor.findAcademicRows(text, isKrs = current.type == "KRS")
        val detectedSemester = AcademicDocumentProcessor.extractSemesterNumber(text) ?: current.detectedSemester
        val detectedYear = AcademicDocumentProcessor.extractAcademicYear(text) ?: current.detectedAcademicYear
        _documentImportPreview.value = current.copy(
            rawExtractedText = text,
            rows = rows,
            declaredTotals = declaredKrsTotals(current.type, text),
            detectedSemester = detectedSemester,
            detectedAcademicYear = detectedYear
        )
        _importSaveError.value = null
        snackbar.value = "Teks dibaca ulang: ${rows.size} baris ditemukan."
    }

    fun savePreviewCourses(replaceExisting: Boolean, customStartDateIso: String? = null, targetSemester: Int? = null) {
        val user = currentUser.value ?: return
        val preview = _documentImportPreview.value ?: return
        if (_isSavingImport.value) return
        val staged = preview.stagedDocument
        scope.launch {
            _isSavingImport.value = true
            _importSaveError.value = null
            try {
                val semester = (targetSemester ?: preview.semester).coerceIn(1, 14)
                if (semester > user.semesterCount) {
                    repository.updateSemesterCount(user.id, semester)
                }
                val result = repository.importCourses(user.id, semester, preview.rows, replaceExisting, isKrs = preview.type == "KRS")

                var scheduleCreatedCount = 0
                var scheduleUpdatedCount = 0
                var scheduleFailedCount = 0
                val startDate: LocalDate? = if (preview.type == "KRS") {
                    val preferences = AppStorage.getPreferences(application)
                    val storedStartDate = preferences.getString(semesterStartDateKey(semester), null)
                    val requestedStartDate = customStartDateIso?.trim()?.takeIf { it.isNotBlank() } ?: storedStartDate
                    val parsedStartDate = parseSemesterStartDate(requestedStartDate)
                    if (!customStartDateIso.isNullOrBlank() && parsedStartDate != null) {
                        onSemesterStartDateSet(semester, parsedStartDate.toString())
                    }

                    if (replaceExisting) {
                        val deletedScheduleIds = repository.deleteSchedulesByUserAndSemester(user.id, semester)
                        deletedScheduleIds.forEach { ReminderScheduler.cancelClass(application, it) }
                    }
                    parsedStartDate
                } else null

                // Jangan membuat jadwal menggunakan tanggal hari ini sebagai pengganti. Pengguna
                // harus menetapkan tanggal mulai terlebih dahulu, atau memakai tanggal semester tersimpan.
                if (startDate != null) {
                    preview.rows.forEachIndexed { _, row ->
                        val parsed = parseKrsSchedule(row.schedule)
                        val rowStart = parsed.startTime.ifBlank { row.start }
                        val rowEnd = parsed.endTime.ifBlank { row.end }
                        // Tanpa hari dan jam dari dokumen, jadwal tidak dibuat (tidak menebak).
                        if (row.courseName.isBlank() || parsed.dayOfWeek !in 1..7 || rowStart.isBlank() || rowEnd.isBlank()) return@forEachIndexed
                        val delta = (parsed.dayOfWeek - startDate.dayOfWeek.value + 7) % 7
                        val firstDate = startDate.plusDays(delta.toLong()).toString()
                        val template = ScheduleEntity(
                            userId = user.id,
                            courseName = row.courseName.trim(),
                            dayOfWeek = parsed.dayOfWeek,
                            scheduledDate = firstDate,
                            startTime = rowStart,
                            endTime = rowEnd,
                            room = parsed.room.ifBlank { "" },
                            lecturer = row.lecturer.trim(),
                            type = "Kuliah Teori",
                            semester = semester,
                            alarmEnabled = true,
                            reminderMinutesBefore = DEFAULT_REMINDER_MINUTES
                        )
                        try {
                            // Idempoten: pertemuan yang sudah ada diperbarui, bukan digandakan.
                            val result = repository.upsertWeeklySchedules(template, 16)
                            // Alarm jadwal yang diganti dibatalkan lalu dipasang ulang dengan isi terbaru.
                            result.updated.forEach { ReminderScheduler.cancelClass(application, it.id) }
                            (result.updated + result.inserted).forEach { ReminderScheduler.scheduleClass(application, it) }
                            scheduleCreatedCount += result.inserted.size
                            scheduleUpdatedCount += result.updated.size
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (ignored: Exception) {
                            rethrowIfCancellation(ignored)
                            // Kegagalan satu baris jadwal tidak membatalkan penyimpanan KRS, tetapi dihitung dan dilaporkan ke pengguna.
                            scheduleFailedCount++
                        }
                    }
                }

                // Impor sudah dikonfirmasi dan berhasil: baru sekarang dokumen lama diganti dan filenya dihapus.
                var documentWarning = ""
                if (staged != null) {
                    try {
                        repository.commitStagedAcademicDocument(application, staged)
                    } catch (commitError: Exception) {
                        repository.discardStagedAcademicDocument(application, staged)
                        rethrowIfCancellation(commitError)
                        documentWarning = " Namun file dokumennya gagal disimpan (${commitError.message ?: "terjadi kesalahan"}); dokumen sebelumnya dipertahankan."
                    }
                }

                val scheduleMsg = buildString {
                    if (scheduleCreatedCount > 0) append(" & $scheduleCreatedCount jadwal kuliah otomatis dibuat")
                    if (scheduleUpdatedCount > 0) append(" ($scheduleUpdatedCount jadwal yang sudah ada diperbarui)")
                    if (scheduleFailedCount > 0) append(" ($scheduleFailedCount mata kuliah gagal dijadwalkan, tambahkan manual)")
                    if (preview.type == "KRS" && startDate == null) append(". Jadwal otomatis belum dibuat karena tanggal mulai perkuliahan belum ditentukan")
                }
                snackbar.value = if (replaceExisting) {
                    "${result.inserted} mata kuliah ${preview.type} disimpan$scheduleMsg menggantikan data Semester $semester.$documentWarning"
                } else {
                    "${preview.type} Semester $semester tersimpan: ${result.inserted} baru, ${result.updated} diperbarui$scheduleMsg.$documentWarning"
                }
                _documentImportPreview.value = null
            } catch (error: Exception) {
                // Dialog sudah ditutup selama menyimpan: tidak ada lagi yang memakai draf, jadi buang.
                // Bila dialog masih menampilkan draf ini, draf dipertahankan agar pengguna bisa mencoba menyimpan lagi.
                if (staged != null && _documentImportPreview.value?.stagedDocument?.draftPath != staged.draftPath) {
                    repository.discardStagedAcademicDocument(application, staged)
                }
                rethrowIfCancellation(error)
                // Dialog tetap terbuka dan galatnya tampil di dalamnya; data pratinjau tidak hilang.
                _importSaveError.value = error.message ?: "Gagal menyimpan data mata kuliah."
            } finally {
                _isSavingImport.value = false
            }
        }
    }

    private data class ParsedKrsSchedule(
        val dayOfWeek: Int,
        val startTime: String,
        val endTime: String,
        val room: String
    )

    private fun parseKrsSchedule(raw: String): ParsedKrsSchedule {
        val lower = raw.lowercase(Locale.ROOT)
        val dayOfWeek = when {
            lower.contains("senin") -> 1
            lower.contains("selasa") -> 2
            lower.contains("rabu") -> 3
            lower.contains("kamis") -> 4
            lower.contains("jumat") || lower.contains("jum'at") || lower.contains("jum’at") -> 5
            lower.contains("sabtu") -> 6
            lower.contains("minggu") || lower.contains("ahad") -> 7
            else -> 0 // hari tidak terbaca: jangan menebak
        }

        val timeRegex = Regex("([01]?\\d|2[0-3])[.:]([0-5]\\d)\\s*(?:-|–|—|s/d|s\\.d\\.?|sd|sampai|to)\\s*([01]?\\d|2[0-3])[.:]([0-5]\\d)")
        val match = timeRegex.find(raw)
        val (startTime, endTime) = if (match != null) {
            val h1 = match.groupValues[1].toInt()
            val m1 = match.groupValues[2].toInt()
            val h2 = match.groupValues[3].toInt()
            val m2 = match.groupValues[4].toInt()
            String.format(Locale.US, "%02d:%02d", h1, m1) to String.format(Locale.US, "%02d:%02d", h2, m2)
        } else {
            "" to ""
        }

        val roomRegex = Regex("(?i)\\b(?:Ruang|R\\.|R\\s|Gedung|Lab|Komp)\\s*([A-Za-z0-9.-]+)")
        val room = roomRegex.find(raw)?.value?.trim().orEmpty()

        return ParsedKrsSchedule(
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            room = room
        )
    }

    fun reprocessAcademicDocument(document: AcademicDocumentEntity) {
        scope.launch {
            try {
                val updated = repository.reprocessAcademicDocument(application, document)
                val rows = AcademicDocumentProcessor.findAcademicRows(updated.extractedText, isKrs = (document.type == "KRS"))
                showPreview(
                    DocumentImportPreviewState(
                        semester = document.semester,
                        type = document.type,
                        sourceName = document.displayName,
                        rawExtractedText = updated.extractedText,
                        rows = rows,
                        storedRelativePath = updated.storedPath,
                        declaredTotals = declaredKrsTotals(document.type, updated.extractedText),
                        initialStartDateIso = storedSemesterStartDate(document.semester)
                    )
                )
                snackbar.value = if (rows.isNotEmpty()) "Teks diproses ulang: ${rows.size} baris ditemukan." else updated.processingMessage
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Dokumen gagal diproses ulang."
            }
        }
    }

    fun deleteAcademicDocument(document: AcademicDocumentEntity) {
        scope.launch {
            try {
                repository.deleteAcademicDocument(document)
                StudentFileStore.deleteRelative(application, document.storedPath)
                snackbar.value = "Dokumen ${document.type} dihapus."
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Dokumen gagal dihapus."
            }
        }
    }
}

data class DocumentImportPreviewState(
    val semester: Int,
    val type: String, // "KRS" or "KHS"
    val sourceName: String,
    val rawExtractedText: String,
    val rows: List<AcademicDocumentProcessor.AcademicRow>,
    val storedRelativePath: String? = null,
    /** Draf file yang baru dipilih; baru menggantikan dokumen lama setelah impor dikonfirmasi. Null untuk input teks/proses ulang. */
    val stagedDocument: StagedAcademicDocument? = null,
    /** KRS: angka pada baris "Jumlah SKS" di dokumen, dipakai untuk memeriksa hasil baca. */
    val declaredTotals: AcademicDocumentProcessor.KrsTotals? = null,
    val initialStartDateIso: String? = null,
    val detectedSemester: Int? = null,
    val detectedAcademicYear: String? = null
)

/** Total tercetak pada dokumen KRS; KHS tidak memilikinya. */
internal fun declaredKrsTotals(type: String, text: String): AcademicDocumentProcessor.KrsTotals? =
    if (type == "KRS") AcademicDocumentProcessor.findKrsTotals(text) else null
