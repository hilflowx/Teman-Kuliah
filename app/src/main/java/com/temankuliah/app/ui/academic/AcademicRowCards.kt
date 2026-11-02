package com.temankuliah.app.ui.academic

import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.TkPalette
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.domain.GpaCalculator
import com.temankuliah.app.ui.icons.Delete
import com.temankuliah.app.ui.icons.Edit
import com.temankuliah.app.ui.icons.Warning
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TemanKuliahOrangeLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TemanKuliahRedLight
import com.temankuliah.app.ui.theme.TextMuted
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import java.util.Locale

/*
 * Kartu baris KRS dan KHS (sesuai desain "Redesain alur impor KRS & KHS"): kode kapsul dan tombol
 * edit/hapus di kepala, nama tebal, lalu ubin angka. Dipakai bersama oleh daftar mata kuliah
 * tersimpan dan pratinjau hasil impor, sehingga tampilannya sama di kedua tempat. Baris yang
 * mencurigakan diberi latar kuning dan satu baris keterangan.
 */

/**
 * SKS (teori, praktik, atau total) per mata kuliah di atas angka ini ditandai "tidak wajar". Biasanya itu salah baca
 * OCR, mis. "2" terbaca "32". Ini hanya tanda untuk diperiksa; batas keras penyimpanan ada di MAX_SKS_PER_COURSE.
 */
internal const val UNUSUAL_SKS_ABOVE = 8

/** Masalah pada satu baris. [blocking] berarti baris tidak bisa disimpan sebelum dilengkapi. */
internal data class RowIssue(val message: String, val blocking: Boolean)

private fun joinNatural(items: List<String>): String = when (items.size) {
    0 -> ""
    1 -> items[0]
    else -> items.dropLast(1).joinToString(", ") + " dan " + items.last()
}

/**
 * Masalah pada baris KRS: data wajib kosong (menghalangi simpan) atau angka SKS yang tampak tidak wajar.
 */
internal fun krsRowIssue(line: KrsLine): RowIssue? {
    if (line.name.isBlank()) return RowIssue("Nama mata kuliah belum diisi", blocking = true)
    if (line.code.isBlank()) return RowIssue("Kode MK belum diisi", blocking = true)
    val odd = buildList {
        if (line.theory > UNUSUAL_SKS_ABOVE) add("Teori ${line.theory}")
        if (line.practice > UNUSUAL_SKS_ABOVE) add("Praktik ${line.practice}")
        if (line.sks > UNUSUAL_SKS_ABOVE) add("SKS ${line.sks}")
    }
    return if (odd.isEmpty()) null else RowIssue("${joinNatural(odd)} tampak tidak wajar", blocking = false)
}

/** Masalah pada baris KHS, dengan aturan yang sama seperti [krsRowIssue] ditambah SKS kosong. */
internal fun khsRowIssue(line: KhsLine): RowIssue? {
    if (line.name.isBlank()) return RowIssue("Nama mata kuliah belum diisi", blocking = true)
    if (line.code.isBlank()) return RowIssue("Kode MK belum diisi", blocking = true)
    if (line.sks <= 0) return RowIssue("SKS belum diisi", blocking = true)
    if (line.sks > UNUSUAL_SKS_ABOVE) return RowIssue("SKS ${line.sks} tampak tidak wajar", blocking = false)
    return null
}

/** Ubah mata kuliah tersimpan menjadi baris data KHS. */
internal fun CourseEntity.toKhsLine(): KhsLine = KhsLine(code = code, name = name, sks = credits, grade = gradeLetter)

private val IssueBackground: Color get() = tkAdaptive(Color(0xFFFFF9EE), TkPalette.colors.orangeLight)
private val IssueBorder: Color get() = tkAdaptive(Color(0xFFF1C27A), Color(0xFF7A4A1E))

@Composable
private fun RowCardFrame(
    issue: RowIssue?,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = TkShape.card,
        color = if (issue != null) IssueBackground else SurfaceLight,
        border = BorderStroke(1.dp, if (issue != null) IssueBorder else DividerLight),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) { content() }
    }
}

@Composable
private fun CodeCapsule(code: String, modifier: Modifier = Modifier) {
    val missing = code.isBlank()
    Surface(
        shape = TkShape.sm,
        color = if (missing) tkAdaptive(Color(0xFFFFEBD0), TkPalette.colors.orangeLight) else TemanKuliahBlueLight,
        modifier = modifier
    ) {
        Text(
            text = if (missing) "Kode kosong" else code,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (missing) TemanKuliahOrangeDark else TemanKuliahBlueDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun RowActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    background: Color,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp).testTag(tag)
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(TkShape.md).background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun RowHeader(
    code: String,
    name: String,
    tagKey: String,
    tagPrefix: String,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CodeCapsule(code)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onEdit != null) {
                RowActionButton(Icons.Default.Edit, TemanKuliahBlue, TemanKuliahBlueLight, "Ubah ${name.ifBlank { "baris" }}", "btn_edit_${tagPrefix}_$tagKey", onEdit)
            }
            if (onDelete != null) {
                RowActionButton(Icons.Default.Delete, TemanKuliahRedDark, TemanKuliahRedLight, "Hapus ${name.ifBlank { "baris" }}", "btn_delete_${tagPrefix}_$tagKey", onDelete)
            }
        }
    }
}

@Composable
private fun RowTitle(name: String) {
    Text(
        text = name.ifBlank { "(tanpa nama)" },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StatTile(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    valueColor: Color = TextPrimary,
    content: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clip(TkShape.md)
            .background(tkAdaptive(Color(0xFFF1F6FD), TkPalette.colors.surfaceVariant))
            .border(BorderStroke(0.5.dp, DividerLight), TkShape.md)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
        if (content != null) {
            content()
        } else {
            Text(
                value.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IssueLine(issue: RowIssue, modifier: Modifier = Modifier) {
    Surface(
        shape = TkShape.sm,
        color = TemanKuliahOrangeLight,
        border = BorderStroke(1.dp, TemanKuliahOrangeDark.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = TemanKuliahOrangeDark, modifier = Modifier.size(15.dp))
            Text(
                issue.message,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TemanKuliahOrangeDark
            )
        }
    }
}

/**
 * Kartu satu mata kuliah KRS: kode, nama, ubin Teori/Praktik/SKS, dosen, dan jadwal.
 * [flagMissingSchedule] menandai jadwal yang tidak terbaca (dipakai pada pratinjau impor).
 */
@Composable
internal fun KrsRowCard(
    line: KrsLine,
    modifier: Modifier = Modifier,
    issue: RowIssue? = null,
    flagMissingSchedule: Boolean = false,
    tagKey: String = line.code,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    RowCardFrame(issue, modifier) {
        RowHeader(line.code, line.name, tagKey, "krs", onEdit, onDelete)
        RowTitle(line.name)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Teori", Modifier.weight(1f), value = line.theory.toString())
            StatTile("Praktik", Modifier.weight(1f), value = line.practice.toString())
            StatTile("SKS", Modifier.weight(1f), value = line.sks.toString(), valueColor = TemanKuliahBlue)
        }
        if (line.lecturer.isNotBlank()) {
            Text(
                "Dosen: ${line.lecturer}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
        val schedule = scheduleSummary(line.day, line.start, line.end, line.lamaMinutes, line.room)
        if (schedule.isNotBlank()) {
            Text(
                schedule,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = TemanKuliahBlue,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (flagMissingSchedule) {
            Text(
                "Jadwal belum terbaca",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = TemanKuliahOrangeDark,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (issue != null) IssueLine(issue)
    }
}

/** Kartu satu nilai KHS: kode, nama, ubin SKS/Nilai/Bobot/Mutu. */
@Composable
internal fun KhsRowCard(
    line: KhsLine,
    modifier: Modifier = Modifier,
    issue: RowIssue? = null,
    tagKey: String = line.code,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val graded = GpaCalculator.isGraded(line.grade)
    val bobot = if (graded) String.format(Locale.US, "%.2f", GpaCalculator.gradeLetterToPoint(line.grade)) else "–"
    val mutu = if (graded && line.sks > 0) String.format(Locale.US, "%.2f", GpaCalculator.qualityPoints(line.sks, line.grade)) else "–"
    val (badgeBg, badgeText) = gradeBadgeColors(line.grade)

    RowCardFrame(issue, modifier) {
        RowHeader(line.code, line.name, tagKey, "khs", onEdit, onDelete)
        RowTitle(line.name)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("SKS", Modifier.weight(1f), value = if (line.sks > 0) line.sks.toString() else "–")
            StatTile("Nilai", Modifier.weight(1f)) {
                if (graded) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            line.grade.trim().uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeText,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text("–", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextMuted)
                }
            }
            StatTile("Bobot", Modifier.weight(1f), value = bobot, valueColor = if (graded) TextPrimary else TextMuted)
            StatTile("Mutu", Modifier.weight(1f), value = mutu, valueColor = if (graded) TextPrimary else TextMuted)
        }
        if (issue != null) IssueLine(issue)
    }
}
