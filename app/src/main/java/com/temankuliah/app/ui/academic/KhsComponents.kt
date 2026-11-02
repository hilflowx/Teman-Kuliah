package com.temankuliah.app.ui.academic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.domain.GpaCalculator
import com.temankuliah.app.ui.icons.Description
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceVariantLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TextMuted
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import java.util.Locale

/**
 * Kartu "Daftar nilai" halaman KHS: keadaan kosong berbingkai putus-putus, atau daftar
 * [KhsRowCard] dengan ringkasan SKS, angka mutu, dan IPS di bawahnya.
 */
@Composable
internal fun KhsGradeListCard(
    courses: List<CourseEntity>,
    onAdd: () -> Unit,
    onEdit: (CourseEntity) -> Unit,
    onDelete: (CourseEntity) -> Unit
) {
    val totals = GpaCalculator.semesterTotals(courses.map { it.credits to it.gradeLetter })
    val hasScores = totals.gradedCredits > 0
    val ipsLabel = if (hasScores) String.format(Locale.US, " · IPS %.2f", totals.ips) else ""

    AcademicListCard(
        title = "Daftar nilai",
        subtitle = if (courses.isEmpty()) "0 mata kuliah" else "${courses.size} mata kuliah · ${totals.totalCredits} SKS$ipsLabel",
        onAdd = onAdd
    ) {
        if (courses.isEmpty()) {
            AcademicEmptyList(
                icon = Icons.Default.Description,
                title = "Belum ada nilai",
                message = "Ketuk Impor di kanan atas untuk memasukkan berkas KHS, atau masukkan nilai manual."
            )
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                courses.forEach { course ->
                    val line = course.toKhsLine()
                    KhsRowCard(
                        line = line,
                        issue = khsRowIssue(line)?.takeIf { !it.blocking },
                        onEdit = { onEdit(course) },
                        onDelete = { onDelete(course) }
                    )
                }

                // Ringkasan SKS, mutu, dan IPS semester
                Surface(
                    shape = TkShape.lg,
                    color = SurfaceVariantLight,
                    border = BorderStroke(1.dp, DividerLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryLine("Total SKS dinilai", "${totals.gradedCredits} / ${totals.totalCredits} SKS")
                        SummaryLine("Total angka mutu", String.format(Locale.US, "%.2f", totals.totalQualityPoints))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(DividerLight))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Indeks Prestasi Semester (IPS)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = TkShape.md,
                                color = if (hasScores) TemanKuliahGreenLight else DividerLight
                            ) {
                                Text(
                                    if (hasScores) String.format(Locale.US, "%.2f / 4.00", totals.ips) else "Belum dinilai",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasScores) TemanKuliahGreenDark else TextMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
