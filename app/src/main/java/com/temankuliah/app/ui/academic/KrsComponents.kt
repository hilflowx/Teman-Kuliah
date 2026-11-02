package com.temankuliah.app.ui.academic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.files.AcademicDocumentProcessor
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.displayClassTime
import com.temankuliah.app.data.local.practiceCredits
import com.temankuliah.app.data.local.theoryCredits
import com.temankuliah.app.ui.icons.DeleteOutline
import com.temankuliah.app.ui.icons.MenuBook
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceVariantLight
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape

/** Ubah mata kuliah tersimpan menjadi baris data KRS (hari, jam, dan lama dibaca dari jam kuliah). */
internal fun CourseEntity.toKrsLine(): KrsLine {
    val display = displayClassTime
    val parts = AcademicDocumentProcessor.parseScheduleParts(display)
    val room = Regex("\\(([^)]+)\\)").find(display)?.groupValues?.get(1).orEmpty()
    val unparsed = if (parts.day.isBlank() && parts.start.isBlank()) display.trim() else ""
    return KrsLine(
        code = code,
        name = name,
        theory = theoryCredits,
        practice = practiceCredits,
        sks = credits,
        lecturer = lecturer,
        day = parts.day.ifBlank { unparsed },
        start = parts.start,
        end = parts.end,
        lamaMinutes = parts.lamaMinutes,
        room = room
    )
}

/**
 * Kartu "Daftar mata kuliah" halaman KRS: keadaan kosong berbingkai putus-putus, atau daftar
 * [KrsRowCard] dengan ringkasan total beban studi di bawahnya.
 */
@Composable
internal fun KrsCourseListCard(
    courses: List<CourseEntity>,
    onAdd: () -> Unit,
    onEdit: (CourseEntity) -> Unit,
    onDelete: (CourseEntity) -> Unit,
    onDeleteAll: () -> Unit
) {
    val totalSks = courses.sumOf { it.credits }
    val totalTeori = courses.sumOf { it.theoryCredits }
    val totalPraktik = courses.sumOf { it.practiceCredits }

    AcademicListCard(
        title = "Daftar mata kuliah",
        subtitle = if (courses.isEmpty()) "0 mata kuliah" else "${courses.size} mata kuliah · $totalSks SKS",
        onAdd = onAdd
    ) {
        if (courses.isEmpty()) {
            AcademicEmptyList(
                icon = Icons.Default.MenuBook,
                title = "Belum ada mata kuliah",
                message = "Ketuk Impor di kanan atas untuk memasukkan berkas KRS, atau tambahkan manual."
            )
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDeleteAll, modifier = Modifier.testTag("btn_delete_all_krs")) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TemanKuliahRedDark, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Hapus semua data KRS", color = TemanKuliahRedDark, style = MaterialTheme.typography.labelMedium)
                    }
                }

                courses.forEach { course ->
                    val line = course.toKrsLine()
                    KrsRowCard(
                        line = line,
                        // Pada data tersimpan hanya angka yang tampak janggal yang ditandai; kode kosong itu wajar untuk input manual.
                        issue = krsRowIssue(line)?.takeIf { !it.blocking },
                        onEdit = { onEdit(course) },
                        onDelete = { onDelete(course) }
                    )
                }

                // Ringkasan beban studi semester
                Surface(
                    shape = TkShape.lg,
                    color = SurfaceVariantLight,
                    border = BorderStroke(1.dp, DividerLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Total beban studi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "Teori $totalTeori SKS · Praktik $totalPraktik SKS",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Surface(shape = TkShape.md, color = TemanKuliahBlueLight) {
                            Text(
                                "$totalSks SKS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TemanKuliahBlueDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
