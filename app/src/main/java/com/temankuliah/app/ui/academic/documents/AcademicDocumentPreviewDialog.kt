package com.temankuliah.app.ui.academic.documents

import com.temankuliah.app.ui.theme.TemanKuliahGreenFill
import com.temankuliah.app.ui.theme.tkShadow
import com.temankuliah.app.ui.components.TkScrollableDialogText
import com.temankuliah.app.domain.rethrowIfCancellation
import com.temankuliah.app.domain.documentMimeType
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.files.AcademicDocumentProcessor
import com.temankuliah.app.data.files.StudentFileStore
import com.temankuliah.app.data.local.AcademicDocumentEntity
import com.temankuliah.app.domain.GpaCalculator
import com.temankuliah.app.ui.academic.KhsLine
import com.temankuliah.app.ui.academic.KhsRowCard
import com.temankuliah.app.ui.academic.KrsLine
import com.temankuliah.app.ui.academic.KrsRowCard
import com.temankuliah.app.ui.academic.RowIssue
import com.temankuliah.app.ui.academic.fileProviderUri
import com.temankuliah.app.ui.academic.khsRowIssue
import com.temankuliah.app.ui.academic.krsRowIssue
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.components.OutlinedButton
import com.temankuliah.app.ui.components.TkCloseHeader
import com.temankuliah.app.ui.components.TkDatePickerDialog
import com.temankuliah.app.ui.components.TkFullScreenDialog
import com.temankuliah.app.ui.icons.Add
import com.temankuliah.app.ui.icons.CalendarMonth
import com.temankuliah.app.ui.icons.Check
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.SurfaceVariantLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TemanKuliahOrangeLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TemanKuliahRedLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import com.temankuliah.app.ui.theme.TkSpace
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Baris pratinjau -> baris KRS; SKS tanpa pembagian teori/praktek dianggap teori, seperti data tersimpan. */
private fun AcademicDocumentProcessor.AcademicRow.toKrsLine(): KrsLine {
    val totalSks = if (theory + practice > 0) (theory + practice) else credits
    val hasSplit = theory + practice > 0
    val isPractice = courseName.contains("Praktik", ignoreCase = true) || courseName.contains("Praktek", ignoreCase = true)
    val parts = if (day.isBlank() && start.isBlank()) AcademicDocumentProcessor.parseScheduleParts(schedule) else null
    return KrsLine(
        code = code,
        name = courseName,
        theory = if (hasSplit) theory else if (isPractice) 0 else totalSks,
        practice = if (hasSplit) practice else if (isPractice) totalSks else 0,
        sks = totalSks,
        lecturer = lecturer,
        day = parts?.day ?: day,
        start = parts?.start ?: start,
        end = parts?.end ?: end,
        lamaMinutes = parts?.lamaMinutes ?: lama
    )
}

/** Baris pratinjau -> baris KHS; SKS kosong diisi dari teori + praktek, sama dengan aturan validasi simpan. */
private fun AcademicDocumentProcessor.AcademicRow.toKhsPreviewLine(): KhsLine =
    KhsLine(code, courseName, if (credits > 0) credits else theory + practice, grade)

/**
 * Pratinjau hasil impor KRS/KHS sebagai layar penuh: judul tetap di atas, baris tampil sebagai kartu
 * yang bergulir vertikal di tengah (baris yang mencurigakan diberi tanda), dan tombol simpan tetap di
 * dasar sehingga mudah dijangkau ibu jari.
 */
@Composable
internal fun AcademicDocumentPreviewDialog(
    preview: DocumentImportPreviewState,
    onDismiss: () -> Unit,
    onUpdateRow: (Int, AcademicDocumentProcessor.AcademicRow) -> Unit,
    onAddRow: (AcademicDocumentProcessor.AcademicRow) -> Unit,
    onDeleteRow: (Int) -> Unit,
    onSave: (replaceExisting: Boolean, startDateIso: String?) -> Unit,
    saveError: String?,
    isSaving: Boolean,
    onReparse: (String) -> Unit,
    onSemesterChange: ((Int) -> Unit)? = null
) {
    var editingRowIndex by remember { mutableStateOf<Int?>(null) }
    var showRawText by remember { mutableStateOf(false) }
    var showAddRowDialog by remember { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }
    var rawDraft by remember(preview.rawExtractedText) { mutableStateOf(preview.rawExtractedText) }
    val isKrs = preview.type == "KRS"

    var selectedSemester by remember(preview.semester) { mutableIntStateOf(preview.semester) }

    var selectedStartDate by remember(preview.initialStartDateIso) {
        mutableStateOf(preview.initialStartDateIso)
    }
    var showStartDatePicker by remember { mutableStateOf(false) }

    val krsLines = if (isKrs) preview.rows.map { it.toKrsLine() } else emptyList()
    val khsLines = if (!isKrs) preview.rows.map { it.toKhsPreviewLine() } else emptyList()
    // Baris yang kodenya kosong (dan SKS kosong pada KHS) ditolak saat disimpan; angka janggal hanya diberi tanda.
    val issues: List<RowIssue?> = if (isKrs) krsLines.map { krsRowIssue(it) } else khsLines.map { khsRowIssue(it) }
    val blockingCount = issues.count { it?.blocking == true }
    val oddCount = issues.count { it != null && !it.blocking }
    val khsTotals = if (!isKrs) GpaCalculator.semesterTotals(preview.rows.map { it.credits to it.grade }) else null

    TkFullScreenDialog(onDismissRequest = onDismiss) {
        // ---- Judul (tetap) ----
        TkCloseHeader(
            title = "Pratinjau hasil impor: ${preview.type}",
            supporting = "Semester $selectedSemester · ${preview.sourceName}",
            onClose = onDismiss
        )

        // ---- Isi (bergulir vertikal) ----
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TkSpace.page, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Pilihan Semester Sasaran ----
            Surface(
                shape = TkShape.card,
                color = SurfaceLight,
                border = BorderStroke(1.dp, DividerLight),
                modifier = Modifier.fillMaxWidth().testTag("preview_semester_card")
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                "Semester Sasaran Impor",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                "Tentukan semester tujuan data dokumen ini",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (preview.detectedSemester != null) {
                            Surface(shape = CircleShape, color = TemanKuliahGreenLight) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = TemanKuliahGreenDark, modifier = Modifier.size(13.dp))
                                    Text(
                                        "Terdeteksi Smtr ${preview.detectedSemester}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TemanKuliahGreenDark
                                    )
                                }
                            }
                        } else {
                            Surface(shape = CircleShape, color = TemanKuliahOrangeLight) {
                                Text(
                                    "Pilih Semester",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TemanKuliahOrangeDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    val maxSemester = maxOf(8, preview.semester, preview.detectedSemester ?: 0, selectedSemester)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..maxSemester).forEach { sem ->
                            val isSelected = sem == selectedSemester
                            Surface(
                                onClick = {
                                    selectedSemester = sem
                                    onSemesterChange?.invoke(sem)
                                },
                                shape = TkShape.md,
                                color = if (isSelected) TemanKuliahBlue else SurfaceVariantLight,
                                border = BorderStroke(1.dp, if (isSelected) TemanKuliahBlue else DividerLight),
                                modifier = Modifier.heightIn(min = 38.dp).testTag("preview_sem_chip_$sem")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "Semester $sem",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                "Periksa baris di bawah. Ketuk ikon pensil untuk mengoreksi sebelum disimpan.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = TemanKuliahBlueLight,
                    border = BorderStroke(1.dp, TemanKuliahBlue.copy(alpha = 0.2f))
                ) {
                    Text(
                        "${preview.rows.size} baris ditemukan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TemanKuliahBlueDark,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    onClick = { showAddRowDialog = true },
                    shape = CircleShape,
                    color = SurfaceLight,
                    border = BorderStroke(1.dp, TemanKuliahBlue),
                    modifier = Modifier.heightIn(min = 40.dp).testTag("btn_preview_add_row")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(16.dp))
                        Text("Tambah baris", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TemanKuliahBlue)
                    }
                }
            }

            if (blockingCount > 0 || oddCount > 0) {
                Surface(
                    shape = TkShape.card,
                    color = TemanKuliahOrangeLight,
                    border = BorderStroke(1.dp, TemanKuliahOrangeDark.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (blockingCount > 0) {
                            Text(
                                "• $blockingCount baris belum lengkap (kode MK${if (!isKrs) " / SKS" else ""} kosong). Ketuk ikon pensil untuk melengkapi sebelum menyimpan.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = TemanKuliahOrangeDark
                            )
                        }
                        if (oddCount > 0) {
                            Text(
                                "• $oddCount baris memuat angka yang tampak tidak wajar (kemungkinan salah baca OCR). Periksa sebelum menyimpan.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = TemanKuliahOrangeDark
                            )
                        }
                    }
                }
            }

            if (preview.rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TkShape.lg)
                        .background(SurfaceVariantLight)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tidak ada baris otomatis yang terbaca.\nTekan '+ Tambah baris' untuk mengisi manual hasil dokumen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                preview.rows.forEachIndexed { index, _ ->
                    if (isKrs) {
                        KrsRowCard(
                            line = krsLines[index],
                            issue = issues[index],
                            flagMissingSchedule = true,
                            tagKey = "preview_$index",
                            onEdit = { editingRowIndex = index },
                            onDelete = { onDeleteRow(index) }
                        )
                    } else {
                        KhsRowCard(
                            line = khsLines[index],
                            issue = issues[index],
                            tagKey = "preview_$index",
                            onEdit = { editingRowIndex = index },
                            onDelete = { onDeleteRow(index) }
                        )
                    }
                }

                if (isKrs) {
                    KrsTotalsStatus(
                        declaredSks = preview.declaredTotals?.sks,
                        readSks = krsLines.sumOf { it.sks },
                        courseCount = preview.rows.size,
                        semester = preview.semester
                    )
                    StartDateCard(selectedStartDate = selectedStartDate, onPick = { showStartDatePicker = true })
                } else {
                    KhsSummary(khsTotals!!)
                }
            }

            // Teks mentah hasil OCR: bantu cek bila ada baris yang tidak terbaca, bisa dikoreksi lalu dibaca ulang.
            Surface(
                shape = TkShape.card,
                color = SurfaceLight,
                border = BorderStroke(1.dp, DividerLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { showRawText = !showRawText },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (showRawText) "Sembunyikan teks hasil baca" else "Ada baris yang kurang? Lihat / koreksi teks hasil baca",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TemanKuliahBlue
                        )
                    }
                    if (showRawText) {
                        androidx.compose.material3.OutlinedTextField(
                            value = rawDraft,
                            onValueChange = { rawDraft = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            label = { Text("Teks hasil baca (satu baris tabel per baris)") }
                        )
                        OutlinedButton(
                            onClick = { onReparse(rawDraft) },
                            enabled = rawDraft.isNotBlank() && !isSaving,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Baca ulang dari teks (mengganti baris di atas)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // ---- Tombol simpan (tetap di dasar, dalam jangkauan ibu jari, terlindung navigasi sistem) ----
        Surface(
            color = SurfaceLight,
            shadowElevation = tkShadow(8.dp),
            border = BorderStroke(1.dp, DividerLight.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = TkSpace.page, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Galat simpan tampil di sini (snackbar tertutup layar ini).
                if (saveError != null) {
                    Text(
                        saveError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TkShape.sm)
                            .background(TemanKuliahRedLight)
                            .padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TemanKuliahRedDark
                    )
                }
                Button(
                    onClick = { onSave(false, if (isKrs) selectedStartDate else null) },
                    enabled = preview.rows.isNotEmpty() && !isSaving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("btn_preview_save"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isSaving) "Menyimpan…" else "Simpan ke Semester $selectedSemester",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = { confirmReplace = true },
                    enabled = preview.rows.isNotEmpty() && !isSaving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).testTag("btn_preview_replace"),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, DividerLight),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Ganti data Semester $selectedSemester", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                Text(
                    "Simpan akan menggabungkan dengan data yang ada tanpa duplikasi.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("Ganti seluruh data Semester $selectedSemester?") },
            text = {
                TkScrollableDialogText {
                    Text(
                    if (isKrs) "Mata kuliah dan jadwal kuliah Semester $selectedSemester yang sudah ada akan diganti dengan ${preview.rows.size} baris dari dokumen ini."
                    else "Mata kuliah dan nilai Semester $selectedSemester yang sudah ada akan diganti dengan ${preview.rows.size} baris dari dokumen ini."
                )
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmReplace = false; onSave(true, if (isKrs) selectedStartDate else null) }) {
                    Text("Ganti", color = TemanKuliahRedDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Batal") } }
        )
    }

    if (showStartDatePicker) {
        TkDatePickerDialog(
            title = "Pilih Tanggal Mulai Perkuliahan",
            initialDate = selectedStartDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now(),
            onDismiss = { showStartDatePicker = false },
            onConfirm = { date ->
                selectedStartDate = date.toString()
                showStartDatePicker = false
            }
        )
    }

    // Edit satu baris dari pratinjau
    editingRowIndex?.let { index ->
        val row = preview.rows.getOrNull(index)
        if (row != null) {
            RowEditorDialog(
                isKrs = isKrs,
                initialRow = row,
                onDismiss = { editingRowIndex = null },
                onSave = { updated ->
                    onUpdateRow(index, updated)
                    editingRowIndex = null
                }
            )
        }
    }

    if (showAddRowDialog) {
        RowEditorDialog(
            isKrs = isKrs,
            initialRow = AcademicDocumentProcessor.AcademicRow(
                courseName = "",
                credits = 0,
                grade = "-",
                no = preview.rows.size + 1,
                code = ""
            ),
            onDismiss = { showAddRowDialog = false },
            onSave = { newRow ->
                onAddRow(newRow)
                showAddRowDialog = false
            }
        )
    }
}

/** KRS: kartu tanggal mulai perkuliahan; jadwal 16 pertemuan dibuat dari tanggal ini saat disimpan. */
@Composable
private fun StartDateCard(selectedStartDate: String?, onPick: () -> Unit) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", IndonesianLocale) }
    val parsedDate = selectedStartDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    Surface(
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(TkShape.md)
                        .background(TemanKuliahBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Tanggal Mulai Perkuliahan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        parsedDate?.format(dateFormatter)?.replaceFirstChar { it.titlecase(IndonesianLocale) }
                            ?: "Belum ditentukan",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (parsedDate != null) TemanKuliahBlueDark else TemanKuliahRedDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(
                    onClick = onPick,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 40.dp).testTag("btn_preview_start_date")
                ) {
                    Text(if (parsedDate == null) "Tentukan" else "Ubah", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                if (parsedDate == null) {
                    "Tentukan tanggal mulai terlebih dahulu. Jadwal otomatis 16 pertemuan akan dibuat dari tanggal ini."
                } else {
                    "Jadwal 16 pertemuan mingguan akan dibuat otomatis mulai dari tanggal ini (bisa memilih tanggal di masa lampau)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

/** KRS: cocokkan jumlah SKS hasil baca dengan baris "Jumlah SKS" pada dokumen. */
@Composable
private fun KrsTotalsStatus(declaredSks: Int?, readSks: Int, courseCount: Int, semester: Int) {
    val matches = declaredSks != null && declaredSks == readSks
    val mismatch = declaredSks != null && declaredSks != readSks
    val background = when {
        matches -> TemanKuliahGreenLight
        mismatch -> TemanKuliahOrangeLight
        else -> SurfaceVariantLight
    }
    val border = when {
        matches -> TemanKuliahGreenDark.copy(alpha = 0.3f)
        mismatch -> TemanKuliahOrangeDark.copy(alpha = 0.3f)
        else -> DividerLight
    }
    val color = when {
        matches -> TemanKuliahGreenDark
        mismatch -> TemanKuliahOrangeDark
        else -> TextSecondary
    }
    val message = when {
        matches -> "Jumlah SKS cocok dengan dokumen ($declaredSks SKS)."
        mismatch -> "Dokumen mencantumkan $declaredSks SKS, hasil baca $readSks SKS. Periksa baris yang kurang atau salah baca."
        else -> "Baris Jumlah SKS pada dokumen tidak terbaca; total dihitung dari baris di atas ($readSks SKS)."
    }
    Surface(
        shape = TkShape.card,
        color = background,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "$courseCount mata kuliah akan disimpan ke KRS Semester $semester",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(message, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

/** KHS: ringkasan SKS, mutu, dan IPS yang dihitung dengan rumus yang sama dengan tabel. */
@Composable
private fun KhsSummary(totals: com.temankuliah.app.domain.SemesterTotals) {
    Surface(
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "SKS bernilai: ${totals.gradedCredits} dari ${totals.totalCredits}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "Total mutu: ${String.format(Locale.US, "%.2f", totals.totalQualityPoints)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Surface(
                color = TemanKuliahGreenFill,
                shape = CircleShape,
                shadowElevation = tkShadow(2.dp)
            ) {
                Text(
                    text = if (totals.gradedCredits > 0) "IPS ${String.format(Locale.US, "%.2f", totals.ips)}" else "IPS -",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/** Membuka berkas KRS/KHS yang tersimpan dengan aplikasi lain di perangkat. */
internal fun openStoredDocument(context: Context, doc: AcademicDocumentEntity) {
    val file = StudentFileStore.fileFor(context, doc.storedPath)
    if (file == null) {
        android.widget.Toast.makeText(context, "Berkas tidak ditemukan. Unggah ulang.", android.widget.Toast.LENGTH_LONG).show()
    } else try {
        val contentUri = fileProviderUri(context, file)
        val extension = file.extension.lowercase(Locale.ROOT)
        val mime = documentMimeType(extension)
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(contentUri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    } catch (ignored: Exception) {
        rethrowIfCancellation(ignored)
        android.widget.Toast.makeText(context, "Tidak ada aplikasi untuk membuka berkas ini.", android.widget.Toast.LENGTH_LONG).show()
    }
}
