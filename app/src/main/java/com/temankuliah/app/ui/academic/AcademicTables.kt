package com.temankuliah.app.ui.academic

import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.TkPalette
import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.temankuliah.app.domain.GpaCalculator
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
import com.temankuliah.app.ui.theme.TextMuted
import java.util.Locale

/*
 * Tabel KRS/KHS yang muat di satu layar HP tanpa geser horizontal.
 *
 * Kolom pertama (Kode MK) memakai weight(1f) dan kolom angka berlebar kecil tetap, sehingga lebar
 * apa pun yang tersedia dipakai penuh. Teks panjang (nama mata kuliah, dosen, jadwal) tidak diberi
 * kolom sendiri: ia turun ke baris kedua/ketiga dalam sel yang sama dan boleh membungkus.
 * Header, baris, dan footer berbagi lebar kolom yang sama agar angka tetap lurus.
 */

/** Satu baris tabel KRS, dipakai baik oleh data tersimpan maupun pratinjau impor. */
internal data class KrsLine(
    val code: String,
    val name: String,
    val theory: Int,
    val practice: Int,
    val sks: Int,
    val lecturer: String,
    val day: String,
    val start: String,
    val end: String,
    val lamaMinutes: Int,
    /** Ruang kuliah bila ada (jadwal yang diisi manual); KRS hasil impor tidak memuatnya. */
    val room: String = ""
)

/** Satu baris tabel KHS. */
internal data class KhsLine(
    val code: String,
    val name: String,
    val sks: Int,
    val grade: String
)

private val KrsTheoryWidth = 46.dp
private val KrsPracticeWidth = 56.dp
private val KrsSksWidth = 38.dp

private val KhsSksWidth = 36.dp
private val KhsGradeWidth = 42.dp
private val KhsBobotWidth = 46.dp
private val KhsMutuWidth = 50.dp

private val RowHorizontalPadding = 10.dp
private val ActionButtonSize = 48.dp

// ---------------------------------------------------------------------------------------------
// Bagian generik
// ---------------------------------------------------------------------------------------------

/** Skala font yang dipakai kolom angka bertabel; lebar kolom mengikutinya agar angka tidak terpotong. */
@Composable
private fun cellFontScale(): Float = LocalDensity.current.fontScale.coerceIn(1f, MAX_CELL_FONT_SCALE)

private const val MAX_CELL_FONT_SCALE = 1.4f

@Composable
private fun TableNumberCell(
    text: String,
    width: Dp,
    bold: Boolean = false,
    color: Color = TextPrimary,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall
) {
    val density = LocalDensity.current
    val scale = cellFontScale()
    CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
        Text(
            text = text,
            modifier = Modifier.width(width * scale),
            style = style,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun TableHeader(first: String, columns: List<Pair<String, Dp>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariantLight)
            .padding(horizontal = RowHorizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            first,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        columns.forEach { (label, width) ->
            TableNumberCell(label, width, bold = true, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TableFooter(
    label: String,
    values: List<Pair<String, Dp>>,
    background: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = RowHorizontalPadding, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        values.forEach { (value, width) ->
            TableNumberCell(value, width, bold = true, color = contentColor, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Baris judul mata kuliah beserta tombol edit/hapus di ujung kanan. */
@Composable
private fun NameAndActions(
    number: Int,
    name: String,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$number. ${name.ifBlank { "(tanpa nama)" }}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (onEdit != null) {
            IconButton(onClick = onEdit, modifier = Modifier.size(ActionButtonSize)) {
                Icon(Icons.Default.Edit, contentDescription = "Ubah baris $number", tint = TemanKuliahBlue, modifier = Modifier.size(17.dp))
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(ActionButtonSize)) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus baris $number", tint = TemanKuliahRedDark, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun CodeText(code: String, color: Color, modifier: Modifier) {
    if (code.isBlank()) {
        Text("Kode kosong", modifier = modifier, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TemanKuliahOrangeDark, maxLines = 1)
    } else {
        Text(code, modifier = modifier, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------------------------------------
// KRS
// ---------------------------------------------------------------------------------------------

/** "Kamis · 08:00–10:30 · 150 menit"; bagian yang kosong dilewati. */
internal fun scheduleSummary(day: String, start: String, end: String, lamaMinutes: Int, room: String = ""): String {
    val time = when {
        start.isNotBlank() && end.isNotBlank() -> "$start–$end"
        else -> start
    }
    val lama = if (lamaMinutes > 0) "$lamaMinutes menit" else ""
    return listOf(day, time, lama, room).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable
internal fun KrsTableHeader() = TableHeader(
    first = "Kode MK",
    columns = listOf("Teori" to KrsTheoryWidth, "Praktik" to KrsPracticeWidth, "SKS" to KrsSksWidth)
)

@Composable
internal fun KrsTableRow(
    line: KrsLine,
    number: Int,
    highlight: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlight) TemanKuliahOrangeLight else SurfaceLight)
            .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier)
            .padding(start = RowHorizontalPadding, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CodeText(line.code, TemanKuliahBlueDark, Modifier.weight(1f))
            TableNumberCell(line.theory.toString(), KrsTheoryWidth)
            TableNumberCell(line.practice.toString(), KrsPracticeWidth)
            TableNumberCell(line.sks.toString(), KrsSksWidth, bold = true)
        }
        NameAndActions(number, line.name, onEdit, onDelete)
        Column(Modifier.padding(end = 6.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "Dosen: ${line.lecturer.ifBlank { "-" }}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            val schedule = scheduleSummary(line.day, line.start, line.end, line.lamaMinutes, line.room)
            Text(
                text = if (schedule.isBlank()) "Jadwal belum terbaca" else schedule,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (schedule.isBlank()) FontWeight.Normal else FontWeight.Medium,
                color = if (schedule.isBlank()) TemanKuliahOrangeDark else TemanKuliahBlueDark,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Baris "Jumlah SKS" di dasar tabel KRS: total teori, total praktek, dan total SKS. */
@Composable
internal fun KrsTableFooter(totalTheory: Int, totalPractice: Int, totalSks: Int) = TableFooter(
    label = "Jumlah SKS",
    values = listOf(
        totalTheory.toString() to KrsTheoryWidth,
        totalPractice.toString() to KrsPracticeWidth,
        totalSks.toString() to KrsSksWidth
    ),
    background = TemanKuliahBlueLight,
    contentColor = TemanKuliahBlueDark
)

// ---------------------------------------------------------------------------------------------
// KHS
// ---------------------------------------------------------------------------------------------

internal fun gradeBadgeColors(letter: String): Pair<Color, Color> = when (letter.trim().uppercase(Locale.ROOT)) {
    "A", "A-" -> TemanKuliahGreenLight to TemanKuliahGreenDark
    "AB", "B+", "B", "B-" -> TemanKuliahBlueLight to TemanKuliahBlueDark
    "BC", "C+", "C" -> TemanKuliahOrangeLight to TemanKuliahOrangeDark
    "D", "E" -> TemanKuliahRedLight to TemanKuliahRedDark
    else -> tkAdaptive(Color(0xFFEEF4FC), TkPalette.colors.surfaceVariant) to TextSecondary
}

@Composable
internal fun KhsTableHeader() = TableHeader(
    first = "Kode MK",
    columns = listOf(
        "SKS" to KhsSksWidth,
        "Nilai" to KhsGradeWidth,
        "Bobot" to KhsBobotWidth,
        "Mutu" to KhsMutuWidth
    )
)

@Composable
internal fun KhsTableRow(
    line: KhsLine,
    number: Int,
    highlight: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val graded = GpaCalculator.isGraded(line.grade)
    val point = GpaCalculator.gradeLetterToPoint(line.grade)
    // Mutu = SKS x bobot, dihitung di satu tempat (GpaCalculator) agar sama dengan total dan IPS.
    val mutu = GpaCalculator.qualityPoints(line.sks, line.grade)
    val (badgeBg, badgeText) = gradeBadgeColors(line.grade)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlight) TemanKuliahOrangeLight else SurfaceLight)
            .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier)
            .padding(start = RowHorizontalPadding, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CodeText(line.code, TemanKuliahGreenDark, Modifier.weight(1f))
            TableNumberCell(if (line.sks > 0) line.sks.toString() else "-", KhsSksWidth, bold = true)
            Box(modifier = Modifier.width(KhsGradeWidth * cellFontScale()), contentAlignment = Alignment.Center) {
                Surface(shape = TkShape.sm, color = badgeBg) {
                    Text(
                        text = if (graded) line.grade.uppercase(Locale.ROOT) else "-",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TableNumberCell(if (graded) String.format(Locale.US, "%.2f", point) else "-", KhsBobotWidth)
            TableNumberCell(
                if (graded && line.sks > 0) String.format(Locale.US, "%.2f", mutu) else "-",
                KhsMutuWidth,
                bold = true,
                color = if (graded && line.sks > 0) TemanKuliahGreenDark else TextMuted
            )
        }
        NameAndActions(number, line.name, onEdit, onDelete)
    }
}

/** Footer tabel KHS: total SKS dan total mutu (2 desimal, sama dengan jumlah mutu tiap baris). */
@Composable
internal fun KhsTableFooter(totalSks: Int, totalMutu: Double) = TableFooter(
    label = "Total",
    values = listOf(
        totalSks.toString() to KhsSksWidth,
        "" to KhsGradeWidth,
        "" to KhsBobotWidth,
        String.format(Locale.US, "%.2f", totalMutu) to KhsMutuWidth
    ),
    background = TemanKuliahGreenLight,
    contentColor = TemanKuliahGreenDark
)
