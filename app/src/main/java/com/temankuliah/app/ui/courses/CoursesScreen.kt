@file:Suppress("DEPRECATION")

package com.temankuliah.app.ui.courses

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import com.temankuliah.app.ui.components.TkScrollableDialogText
import com.temankuliah.app.ui.components.staggeredEntrance
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.domain.GpaCalculator
import com.temankuliah.app.domain.TranscriptBuilder
import com.temankuliah.app.domain.TranscriptHtml
import com.temankuliah.app.domain.TranscriptStudent
import com.temankuliah.app.ui.DialogType
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.academic.documents.AcademicDocumentPreviewDialog
import com.temankuliah.app.ui.academic.documents.DocumentSourcePickerDialog
import com.temankuliah.app.ui.academic.documents.PasteDocumentTextDialog
import com.temankuliah.app.ui.academic.rememberAcademicCameraCapture
import com.temankuliah.app.ui.components.TkSubHeader
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.TkSpace
import com.temankuliah.app.ui.theme.rememberTkTwoPane
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Halaman Transkrip Nilai sesuai desain terbaru:
 * header (cetak + simpan PDF), pencarian, kartu hero IPK, satu dropdown semester, dan kartu
 * "Riwayat nilai" dengan satu tombol Tambah. Semester kosong menawarkan Impor KHS.
 */
@Composable
fun CoursesScreen(
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val allCourses by viewModel.courseController.courses.collectAsStateWithLifecycle()
    val searchQuery by viewModel.courseController.courseSearchQuery.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val semesterCount by viewModel.semesterCount.collectAsStateWithLifecycle()
    val gpaSummary by viewModel.courseController.gpaSummary.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val previewState by viewModel.documentController.documentImportPreview.collectAsStateWithLifecycle()
    val importSaveError by viewModel.documentController.importSaveError.collectAsStateWithLifecycle()
    val isSavingImport by viewModel.documentController.isSavingImport.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var selectedSemester by remember(activeSemester) { mutableIntStateOf(activeSemester.coerceAtLeast(1)) }
    var courseToDelete by remember { mutableStateOf<CourseEntity?>(null) }
    var printMessage by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadResult by remember { mutableStateOf<PdfExportResult?>(null) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf(false) }

    // Impor KHS untuk semester yang sedang dipilih (alur sama dengan halaman KHS).
    val launchCameraCapture = rememberAcademicCameraCapture(context, "khs", selectedSemester) { file, semester ->
        viewModel.documentController.processAcademicDocumentCamera(file, semester, "KHS")
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.documentController.processAcademicDocumentUri(uri, selectedSemester, "KHS")
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.documentController.processAcademicDocumentUri(uri, selectedSemester, "KHS")
    }

    // Angka kumulatif memakai GpaCalculator agar sama dengan KHS dan transkrip cetak.
    val overallTotals = remember(allCourses) {
        GpaCalculator.semesterTotals(allCourses.map { it.credits to it.gradeLetter })
    }
    val predicateBadge = remember(gpaSummary.honorsPredicate) {
        val predicate = gpaSummary.honorsPredicate
        when {
            predicate == "Belum Ada Nilai" -> null
            predicate.contains("(") -> predicate.substringAfter("(").substringBefore(")")
            else -> predicate
        }
    }

    val overviews = remember(allCourses, activeSemester, semesterCount) {
        (1..semesterCount.coerceAtLeast(selectedSemester)).map { buildSemesterOverview(allCourses, it, activeSemester) }
    }
    val selectedOverview = overviews.firstOrNull { it.semester == selectedSemester }
        ?: buildSemesterOverview(allCourses, selectedSemester, activeSemester)

    val semesterCourses = remember(allCourses, selectedSemester) {
        allCourses.filter { it.semester == selectedSemester }
    }
    val displayedCourses = remember(semesterCourses, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        val filtered = if (q.isEmpty()) {
            semesterCourses
        } else {
            semesterCourses.filter {
                it.name.lowercase(Locale.ROOT).contains(q) || it.code.lowercase(Locale.ROOT).contains(q)
            }
        }
        filtered.sortedBy { it.name }
    }

    /** Menyusun HTML transkrip dari data saat ini; null bila belum ada nilai (pesan sudah ditampilkan). */
    fun buildTranscriptHtml(emptyMessage: String): Pair<String, String>? {
        val user = currentUser
        val transcript = TranscriptBuilder.build(
            TranscriptStudent(
                fullName = user?.fullName.orEmpty(),
                nim = user?.nim.orEmpty(),
                studyProgram = user?.studyProgram.orEmpty(),
                faculty = user?.faculty.orEmpty(),
                university = user?.university.orEmpty()
            ),
            allCourses
        )
        if (transcript.isEmpty) {
            printMessage = emptyMessage
            return null
        }
        val printedOn = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID")))
        val title = listOf("Transkrip Nilai", user?.fullName.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" - ")
        return TranscriptHtml.render(transcript, printedOn) to title
    }

    fun printTranscript() {
        val (html, jobName) = buildTranscriptHtml(
            "Belum ada nilai untuk dicetak. Impor KHS atau isi nilai mata kuliah terlebih dahulu."
        ) ?: return
        if (!printHtmlDocument(context, html, jobName)) {
            printMessage = "Perangkat ini tidak dapat membuka layanan cetak."
        }
    }

    fun downloadTranscript() {
        if (isDownloading) return
        val (html, title) = buildTranscriptHtml(
            "Belum ada nilai untuk diunduh. Impor KHS atau isi nilai mata kuliah terlebih dahulu."
        ) ?: return
        isDownloading = true
        exportHtmlToPdf(context, html, title) { result ->
            isDownloading = false
            downloadResult = result
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        TkSubHeader(
            title = "Transkrip nilai",
            supporting = "Riwayat nilai semua semester",
            onBack = onBack,
            backDescription = "Kembali ke Akademik",
            trailing = {
                TranscriptHeaderActions(
                    onPrint = ::printTranscript,
                    onDownload = ::downloadTranscript,
                    isDownloading = isDownloading
                )
            }
        )

        val isWide = rememberTkTwoPane()
        var selectedCourseForDetail by remember { mutableStateOf<CourseEntity?>(null) }

        if (isWide) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(start = TkSpace.page, end = 12.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "search_bar") {
                        Box(Modifier.staggeredEntrance(0)) {
                            CoursesSearchBar(
                                searchQuery = searchQuery,
                                onQueryChange = { viewModel.courseController.setSearchQuery(it) }
                            )
                        }
                    }

                    item(key = "gpa_hero_card") {
                        Box(Modifier.staggeredEntrance(1)) {
                            GpaHeroCard(
                                cumulativeGpa = gpaSummary.cumulativeGpa,
                                totalSks = overallTotals.totalCredits,
                                totalMutu = overallTotals.totalQualityPoints,
                                predicateBadge = predicateBadge
                            )
                        }
                    }

                    item(key = "semester_dropdown") {
                        Box(Modifier.staggeredEntrance(2)) {
                            SemesterDropdown(
                                selected = selectedOverview,
                                overviews = overviews,
                                onSelect = { selectedSemester = it }
                            )
                        }
                    }

                    item(key = "history_card") {
                        Box(Modifier.staggeredEntrance(3)) {
                            HistoryCard(
                                semester = selectedSemester,
                                courseCount = semesterCourses.size,
                                courses = displayedCourses,
                                isSearching = searchQuery.isNotBlank() && semesterCourses.isNotEmpty(),
                                onAdd = { viewModel.openDialog(DialogType.AddCourseWithSemester(selectedSemester)) },
                                onImportKhs = { showSourcePicker = true },
                                onEdit = {
                                    selectedCourseForDetail = it
                                    viewModel.openDialog(DialogType.EditCourseDetails(it))
                                },
                                onDelete = { courseToDelete = it }
                            )
                        }
                    }
                }

                CourseDetailPane(
                    course = selectedCourseForDetail ?: displayedCourses.firstOrNull(),
                    onEditGrade = { viewModel.openDialog(DialogType.EditCourseGrade(it)) },
                    onEditDetails = { viewModel.openDialog(DialogType.EditCourseDetails(it)) },
                    onDelete = { courseToDelete = it },
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "search_bar") {
                    Box(Modifier.staggeredEntrance(0)) {
                        CoursesSearchBar(
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.courseController.setSearchQuery(it) }
                        )
                    }
                }

                item(key = "gpa_hero_card") {
                    Box(Modifier.staggeredEntrance(1)) {
                        GpaHeroCard(
                            cumulativeGpa = gpaSummary.cumulativeGpa,
                            totalSks = overallTotals.totalCredits,
                            totalMutu = overallTotals.totalQualityPoints,
                            predicateBadge = predicateBadge
                        )
                    }
                }

                item(key = "semester_dropdown") {
                    Box(Modifier.staggeredEntrance(2)) {
                        SemesterDropdown(
                            selected = selectedOverview,
                            overviews = overviews,
                            onSelect = { selectedSemester = it }
                        )
                    }
                }

                item(key = "history_card") {
                    Box(Modifier.staggeredEntrance(3)) {
                        HistoryCard(
                            semester = selectedSemester,
                            courseCount = semesterCourses.size,
                            courses = displayedCourses,
                            isSearching = searchQuery.isNotBlank() && semesterCourses.isNotEmpty(),
                            onAdd = { viewModel.openDialog(DialogType.AddCourseWithSemester(selectedSemester)) },
                            onImportKhs = { showSourcePicker = true },
                            onEdit = { viewModel.openDialog(DialogType.EditCourseDetails(it)) },
                            onDelete = { courseToDelete = it }
                        )
                    }
                }
            }
        }
    }

    if (showSourcePicker) {
        DocumentSourcePickerDialog(
            type = "KHS",
            semester = selectedSemester,
            onDismiss = { showSourcePicker = false },
            onTakePhoto = launchCameraCapture,
            onPickImage = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onPickFile = {
                filePicker.launch(arrayOf("application/pdf", "text/plain", "text/csv"))
            },
            onEnterText = {
                showTextInputDialog = true
                showSourcePicker = false
            }
        )
    }

    if (showTextInputDialog) {
        PasteDocumentTextDialog(
            type = "KHS",
            semester = selectedSemester,
            onDismiss = { showTextInputDialog = false },
            onProcess = { text ->
                viewModel.documentController.processAcademicDocumentText(text, selectedSemester, "KHS")
                showTextInputDialog = false
            }
        )
    }

    previewState?.let { preview ->
        AcademicDocumentPreviewDialog(
            preview = preview,
            onDismiss = { viewModel.documentController.dismissDocumentPreview() },
            onUpdateRow = { index, row -> viewModel.documentController.updatePreviewRow(index, row) },
            onAddRow = { row -> viewModel.documentController.addPreviewRow(row) },
            onDeleteRow = { index -> viewModel.documentController.deletePreviewRow(index) },
            onSave = { replaceExisting, startDate -> viewModel.documentController.savePreviewCourses(replaceExisting, startDate) },
            saveError = importSaveError,
            isSaving = isSavingImport,
            onReparse = { text -> viewModel.documentController.reparsePreviewText(text) },
            onSemesterChange = { viewModel.documentController.updatePreviewSemester(it) }
        )
    }

    printMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { printMessage = null },
            title = { Text("Cetak Transkrip") },
            text = { TkScrollableDialogText { Text(message) } },
            confirmButton = { TextButton(onClick = { printMessage = null }) { Text("Tutup") } }
        )
    }

    downloadResult?.let { result ->
        TranscriptDownloadDialog(result = result, onDismiss = { downloadResult = null })
    }

    courseToDelete?.let { course ->
        DeleteCourseDialog(
            course = course,
            onDismiss = { courseToDelete = null },
            onConfirm = {
                val id = course.id
                courseToDelete = null
                viewModel.courseController.deleteCourse(id)
            }
        )
    }
}
