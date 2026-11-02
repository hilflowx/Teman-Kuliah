package com.temankuliah.app.ui.academic

import androidx.compose.animation.core.snap
import com.temankuliah.app.ui.components.rememberReducedMotion
import com.temankuliah.app.ui.theme.TemanKuliahRedFill
import com.temankuliah.app.ui.theme.tkShadow
import com.temankuliah.app.ui.components.tkHeroCircles
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.temankuliah.app.ui.components.MotionTokens
import com.temankuliah.app.ui.components.pressScale
import com.temankuliah.app.ui.components.TkSemesterPickerSheet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.AcademicDocumentEntity
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.TkDashedEmptyCard
import com.temankuliah.app.ui.icons.Add
import com.temankuliah.app.ui.icons.ChevronDown
import com.temankuliah.app.ui.icons.DeleteOutline
import com.temankuliah.app.ui.icons.Description
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TextMuted
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkGradientEnd
import com.temankuliah.app.ui.theme.TkGradientStart
import com.temankuliah.app.ui.theme.TkShape

/** Ringkasan satu semester untuk daftar pilihan: "7 MK · 20 SKS" atau "Belum ada mata kuliah". */
internal fun semesterSummaryLine(courses: List<CourseEntity>): String =
    if (courses.isEmpty()) "Belum ada mata kuliah" else "${courses.size} matkul · ${courses.sumOf { it.credits }} SKS"

// ---------------------------------------------------------------------------------------------
// Pemilih semester
// ---------------------------------------------------------------------------------------------

@Composable
private fun SemesterBadge(number: Int, selected: Boolean) {
    Box(
        Modifier
            .size(44.dp)
            .clip(TkShape.md)
            .background(if (selected) TemanKuliahBlue else TemanKuliahBlueLight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$number",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else TemanKuliahBlueDark
        )
    }
}

@Composable
private fun ActivePill() {
    Box(Modifier.clip(CircleShape).background(TemanKuliahGreenLight).padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text("Aktif", color = TemanKuliahGreenDark, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/**
 * Pemilih semester bersama halaman KRS dan KHS: label "Pilih semester" lalu kartu berisi lencana
 * angka, keterangan, pil "Aktif", dan panah. Daftar pilihan terbuka sebagai menu selebar kartu.
 * Memilih semester di sini hanya mengganti semester yang dilihat, bukan semester aktif.
 */
@Composable
internal fun AcademicSemesterPicker(
    selectedSemester: Int,
    activeSemester: Int,
    semesterCount: Int,
    summaryOf: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val isActive = selectedSemester == activeSemester

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (rememberReducedMotion()) snap() else tween(durationMillis = MotionTokens.DurationShort, easing = MotionTokens.EasingStandard),
        label = "academicChevronRotation"
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pilih semester", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Box {
            Surface(
                onClick = { expanded = true },
                shape = TkShape.card,
                color = SurfaceLight,
                border = BorderStroke(if (expanded) 1.5.dp else 1.dp, if (expanded) TemanKuliahBlue else DividerLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale()
                    .testTag("academic_semester_picker")
                    .semantics {
                        contentDescription = "Semester $selectedSemester${if (isActive) ", aktif" else ""}. Ketuk untuk memilih semester"
                    }
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SemesterBadge(selectedSemester, selected = true)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Semester $selectedSemester", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                        Text(
                            if (isActive) "Semester berjalan" else summaryOf(selectedSemester),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isActive) {
                        ActivePill()
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.Default.ChevronDown,
                        contentDescription = null,
                        tint = if (expanded) TemanKuliahBlue else TextMuted,
                        modifier = Modifier.size(22.dp).rotate(chevronRotation)
                    )
                }
            }

        }
    }

    if (expanded) {
        TkSemesterPickerSheet(
            title = "Pilih semester",
            subtitle = "Ketuk untuk berpindah semester",
            selectedSemester = selectedSemester,
            activeSemester = activeSemester,
            semesterCount = semesterCount,
            summaryOf = summaryOf,
            onSelect = { sem ->
                onSelect(sem)
                expanded = false
            },
            onDismissRequest = { expanded = false }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Kartu biru
// ---------------------------------------------------------------------------------------------

/** Satu ubin angka pada kartu biru: label kecil, angka besar, keterangan. */
internal data class AcademicHeroStat(val label: String, val value: String, val caption: String)

/**
 * Kartu gradien biru KRS/KHS: ubin ikon, label dokumen, judul semester, pil status, dan dua ubin angka.
 * Gaya dan warnanya sama dengan kartu Semester Aktif di halaman Akademik.
 */
@Composable
internal fun AcademicHeroCard(
    icon: ImageVector,
    label: String,
    title: String,
    status: String,
    stats: List<AcademicHeroStat>,
    modifier: Modifier = Modifier
) {
    val shape = TkShape.xl
    val softWhite = Color.White.copy(alpha = 0.18f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = tkShadow(16.dp),
                shape = shape,
                ambientColor = TkGradientEnd.copy(alpha = 0.25f),
                spotColor = TkGradientEnd.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(TkGradientStart, TkGradientEnd)))
            .tkHeroCircles()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(TkShape.lg).background(softWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.24f)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            stats.forEach { stat ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(TkShape.lg)
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(stat.label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
                    Text(stat.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                    Text(stat.caption, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Berkas tersimpan
// ---------------------------------------------------------------------------------------------

/** Baris ringkas berkas KRS/KHS yang pernah diimpor, dengan aksi buka dan hapus. Hanya tampil bila berkas ada. */
@Composable
internal fun AcademicStoredFileRow(
    type: String,
    document: AcademicDocumentEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        modifier = modifier.fillMaxWidth().testTag("academic_stored_file_${type.lowercase()}")
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).clip(TkShape.md).background(TemanKuliahBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Berkas $type tersimpan", style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                Text(
                    document.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onOpen,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Buka", fontWeight = FontWeight.Bold, color = TemanKuliahBlue)
                }
                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = TemanKuliahRedDark)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Kartu daftar
// ---------------------------------------------------------------------------------------------

/**
 * Kerangka kartu daftar (KRS/KHS): judul, subjudul, pil "+ Tambah", lalu [content] sebagai isi
 * (keadaan kosong atau daftar kartu).
 */
@Composable
internal fun AcademicListCard(
    title: String,
    subtitle: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = TkShape.xl,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = onAdd,
                    shape = CircleShape,
                    color = TemanKuliahBlueLight,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("btn_academic_add")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(18.dp))
                        Text("Tambah", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TemanKuliahBlue)
                    }
                }
            }
            content()
        }
    }
}

/** Keadaan kosong di dalam [AcademicListCard]: kotak putus-putus dengan ikon, pesan, dan chip sumber impor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AcademicEmptyList(icon: ImageVector, title: String, message: String) {
    TkDashedEmptyCard(
        icon = icon,
        title = title,
        message = message,
        footer = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("PDF", "Gambar", "Kamera", "Galeri", "Teks").forEach { source ->
                    Surface(
                        shape = CircleShape,
                        color = SurfaceLight,
                        border = BorderStroke(1.dp, DividerLight)
                    ) {
                        Text(
                            source,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    )
}

// ---------------------------------------------------------------------------------------------
// Dialog konfirmasi
// ---------------------------------------------------------------------------------------------

/** Dialog konfirmasi hapus dokumen KRS/KHS. [keptData] menyebut data yang tetap dipertahankan. */
@Composable
internal fun AcademicDeleteDocumentDialog(
    type: String,
    document: AcademicDocumentEntity,
    keptData: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Dokumen $type?") },
        text = { Text("Dokumen \"${document.displayName}\" akan dihapus dari penyimpanan. $keptData yang sudah ada tetap dipertahankan.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahRedFill)
            ) { Text("Hapus") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

/** Dialog konfirmasi hapus satu mata kuliah dari semester terpilih. */
@Composable
internal fun AcademicDeleteCourseDialog(
    course: CourseEntity,
    semester: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Mata Kuliah?") },
        text = { Text("Hapus \"${course.name}\" dari Semester $semester?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahRedFill)
            ) { Text("Hapus") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
