@file:Suppress("DEPRECATION")

package com.temankuliah.app.ui.academic

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.temankuliah.app.ui.components.TkScrollableDialogText
import com.temankuliah.app.ui.components.staggeredEntrance
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.AcademicDocumentEntity
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.ui.DialogType
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.academic.documents.AcademicDocumentPreviewDialog
import com.temankuliah.app.ui.academic.documents.DocumentSourcePickerDialog
import com.temankuliah.app.ui.academic.documents.PasteDocumentTextDialog
import com.temankuliah.app.ui.academic.documents.openStoredDocument
import com.temankuliah.app.ui.components.TkAddPillButton
import com.temankuliah.app.ui.components.TkSubHeader
import com.temankuliah.app.ui.icons.Description
import com.temankuliah.app.ui.icons.Upload
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TkSpace
import com.temankuliah.app.ui.theme.rememberTkTwoPane

/**
 * Halaman Kartu Rencana Studi (KRS), sesuai desain "Redesain KRS & KHS":
 * 1. Header: tombol kembali, judul, keterangan, dan tombol "Impor" (pil biru) di kanan atas.
 * 2. "Pilih semester" dengan kartu pemilih (lencana angka, keterangan, pil Aktif, panah).
 * 3. Kartu biru "Rencana semester N" berisi status (Draft/Tersimpan), jumlah mata kuliah, dan total SKS.
 * 4. Kartu "Daftar mata kuliah": tombol "+ Tambah", keadaan kosong berbingkai putus-putus, atau daftar kartu.
 * Berkas KRS yang pernah diimpor muncul sebagai baris ringkas (buka/hapus) di antara kartu biru dan daftar.
 */
@Composable
fun KrsScreen(
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val semesterCount by viewModel.semesterCount.collectAsStateWithLifecycle()
    var selectedSemester by remember(activeSemester) { mutableIntStateOf(activeSemester.coerceAtLeast(1)) }

    val allDocs by viewModel.documentController.academicDocuments.collectAsStateWithLifecycle()
    val currentDoc = remember(allDocs, selectedSemester) {
        allDocs.firstOrNull { it.semester == selectedSemester && it.type == "KRS" }
    }

    val allCourses by viewModel.courseController.courses.collectAsStateWithLifecycle()
    val courses = remember(allCourses, selectedSemester) {
        allCourses.filter { it.semester == selectedSemester }
            .filter { course -> course.lecturer.isNotBlank() || course.classTime.isNotBlank() || course.gradeLetter == "-" }
            .sortedBy { it.name }
    }

    val semesterSummary = remember(allCourses) {
        { semester: Int -> semesterSummaryLine(allCourses.filter { it.semester == semester }) }
    }
    val totalSks = remember(courses) { courses.sumOf { it.credits } }

    val previewState by viewModel.documentController.documentImportPreview.collectAsStateWithLifecycle()
    val importSaveError by viewModel.documentController.importSaveError.collectAsStateWithLifecycle()
    val isSavingImport by viewModel.documentController.isSavingImport.collectAsStateWithLifecycle()

    var showSourcePicker by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var deleteDocumentConfirm by remember { mutableStateOf<AcademicDocumentEntity?>(null) }
    var courseToDelete by remember { mutableStateOf<CourseEntity?>(null) }
    var confirmDeleteAllKrs by remember { mutableStateOf(false) }
    val launchCameraCapture = rememberAcademicCameraCapture(context, "krs", selectedSemester) { file, semester ->
        viewModel.documentController.processAcademicDocumentCamera(file, semester, "KRS")
    }

    // Launchers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.documentController.processAcademicDocumentUri(uri, selectedSemester, "KRS")
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.documentController.processAcademicDocumentUri(uri, selectedSemester, "KRS")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        TkSubHeader(
            title = "Kartu Rencana Studi",
            supporting = "Kelola mata kuliah per semester",
            onBack = onBack,
            backDescription = "Kembali ke Akademik",
            trailing = {
                TkAddPillButton(
                    onClick = { showSourcePicker = true },
                    label = "Impor",
                    icon = Icons.Default.Upload,
                    contentDescription = "Impor KRS Semester $selectedSemester",
                    modifier = Modifier.padding(start = 8.dp).testTag("btn_import_krs")
                )
            }
        )

        val isWide = rememberTkTwoPane()

        if (isWide) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = TkSpace.page, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item(key = "semester_selector") {
                        Box(Modifier.staggeredEntrance(0)) {
                            AcademicSemesterPicker(
                                selectedSemester = selectedSemester,
                                activeSemester = activeSemester,
                                semesterCount = semesterCount,
                                summaryOf = semesterSummary,
                                onSelect = { selectedSemester = it }
                            )
                        }
                    }

                    item(key = "krs_hero") {
                        Box(Modifier.staggeredEntrance(1)) {
                            AcademicHeroCard(
                                icon = Icons.Default.Description,
                                label = "KRS",
                                title = "Rencana semester $selectedSemester",
                                status = if (courses.isEmpty()) "Draft" else "Tersimpan",
                                stats = listOf(
                                    AcademicHeroStat("Mata kuliah", "${courses.size}", "semester ini"),
                                    AcademicHeroStat("Total SKS", "$totalSks", "direncanakan")
                                )
                            )
                        }
                    }

                    if (currentDoc != null) {
                        item(key = "dokumen_krs") {
                            Box(Modifier.staggeredEntrance(2)) {
                                AcademicStoredFileRow(
                                    type = "KRS",
                                    document = currentDoc,
                                    onOpen = { openStoredDocument(context, currentDoc) },
                                    onDelete = { deleteDocumentConfirm = currentDoc }
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item(key = "daftar_mata_kuliah") {
                        Box(Modifier.staggeredEntrance(2)) {
                            KrsCourseListCard(
                                courses = courses,
                                onAdd = { viewModel.openDialog(DialogType.AddCourseWithSemester(selectedSemester)) },
                                onEdit = { viewModel.openDialog(DialogType.EditCourseDetails(it)) },
                                onDelete = { courseToDelete = it },
                                onDeleteAll = { confirmDeleteAllKrs = true }
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "semester_selector") {
                    Box(Modifier.staggeredEntrance(0)) {
                        AcademicSemesterPicker(
                            selectedSemester = selectedSemester,
                            activeSemester = activeSemester,
                            semesterCount = semesterCount,
                            summaryOf = semesterSummary,
                            onSelect = { selectedSemester = it }
                        )
                    }
                }

                item(key = "krs_hero") {
                    Box(Modifier.staggeredEntrance(1)) {
                        AcademicHeroCard(
                            icon = Icons.Default.Description,
                            label = "KRS",
                            title = "Rencana semester $selectedSemester",
                            status = if (courses.isEmpty()) "Draft" else "Tersimpan",
                            stats = listOf(
                                AcademicHeroStat("Mata kuliah", "${courses.size}", "semester ini"),
                                AcademicHeroStat("Total SKS", "$totalSks", "direncanakan")
                            )
                        )
                    }
                }

                if (currentDoc != null) {
                    item(key = "dokumen_krs") {
                        Box(Modifier.staggeredEntrance(2)) {
                            AcademicStoredFileRow(
                                type = "KRS",
                                document = currentDoc,
                                onOpen = { openStoredDocument(context, currentDoc) },
                                onDelete = { deleteDocumentConfirm = currentDoc }
                            )
                        }
                    }
                }

                item(key = "daftar_mata_kuliah") {
                    Box(Modifier.staggeredEntrance(3)) {
                        KrsCourseListCard(
                            courses = courses,
                            onAdd = { viewModel.openDialog(DialogType.AddCourseWithSemester(selectedSemester)) },
                            onEdit = { viewModel.openDialog(DialogType.EditCourseDetails(it)) },
                            onDelete = { courseToDelete = it },
                            onDeleteAll = { confirmDeleteAllKrs = true }
                        )
                    }
                }
            }
        }
    }

    // Dialogs & Pickers
    if (showSourcePicker) {
        DocumentSourcePickerDialog(
            type = "KRS",
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
            type = "KRS",
            semester = selectedSemester,
            onDismiss = { showTextInputDialog = false },
            onProcess = { text ->
                viewModel.documentController.processAcademicDocumentText(text, selectedSemester, "KRS")
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

    deleteDocumentConfirm?.let { doc ->
        AcademicDeleteDocumentDialog(
            type = "KRS",
            document = doc,
            keptData = "Daftar mata kuliah",
            onDismiss = { deleteDocumentConfirm = null },
            onConfirm = {
                viewModel.documentController.deleteAcademicDocument(doc)
                deleteDocumentConfirm = null
            }
        )
    }

    courseToDelete?.let { course ->
        AcademicDeleteCourseDialog(
            course = course,
            semester = selectedSemester,
            onDismiss = { courseToDelete = null },
            onConfirm = {
                viewModel.courseController.deleteCourse(course.id)
                courseToDelete = null
            }
        )
    }

    if (confirmDeleteAllKrs) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAllKrs = false },
            title = { Text("Hapus semua data KRS Semester $selectedSemester?") },
            text = {
                TkScrollableDialogText {
                    Text("${courses.size} mata kuliah yang tampil di KRS beserta jadwal terkait akan dihapus. File dokumen KRS tetap tersimpan.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.courseController.deleteAllKrsCourses(courses.map { it.id })
                    confirmDeleteAllKrs = false
                }) { Text("Hapus semua", color = TemanKuliahRedDark) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAllKrs = false }) { Text("Batal") }
            }
        )
    }
}
