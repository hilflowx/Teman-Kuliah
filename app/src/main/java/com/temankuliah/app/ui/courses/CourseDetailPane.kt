package com.temankuliah.app.ui.courses

import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.TkPalette
import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.ui.components.pressScale
import com.temankuliah.app.ui.components.staggeredEntrance
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkSpace

/**
 * Komponen rincian mata kuliah untuk panel detail pada tablet / foldable,
 * atau panel kedua saat menggunakan ListDetailPaneScaffold.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CourseDetailPane(
    course: CourseEntity?,
    onEditGrade: (CourseEntity) -> Unit,
    onEditDetails: (CourseEntity) -> Unit,
    onDelete: (CourseEntity) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    if (course == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = TkShape.card,
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = TkOutlineIcons.BookOpen,
                        contentDescription = null,
                        tint = TemanKuliahBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Pilih Mata Kuliah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Ketuk mata kuliah dari daftar di samping untuk melihat rincian SKS, nilai, dosen, dan opsi pengeditan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(horizontal = TkSpace.page, vertical = 16.dp)
        ) {
            if (onBack != null) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text("← Kembali ke Daftar")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(0),
                shape = TkShape.card,
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.code.ifBlank { "KODE TBA" },
                                style = MaterialTheme.typography.labelLarge,
                                color = TemanKuliahBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = course.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Surface(
                            shape = TkShape.md,
                            color = tkAdaptive(Color(0xFFE8F1FC), TkPalette.colors.surfaceVariant),
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Text(
                                text = "${course.credits} SKS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = TemanKuliahBlue,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = DividerLight)

                    // Nilai dan Bobot Mutu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = TkShape.lg,
                            colors = CardDefaults.cardColors(containerColor = tkAdaptive(Color(0xFFF7FAFD), TkPalette.colors.surfaceVariant)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DividerLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Nilai Huruf",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = course.gradeLetter.ifBlank { "Belum ada" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (course.gradeLetter.isNotBlank()) TemanKuliahBlue else TextSecondary
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = TkShape.lg,
                            colors = CardDefaults.cardColors(containerColor = tkAdaptive(Color(0xFFF7FAFD), TkPalette.colors.surfaceVariant)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DividerLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Semester",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Semester ${course.semester}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Dosen dan Kelas
                    if (course.lecturer.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TkOutlineIcons.Person,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Dosen: ${course.lecturer}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    if (course.classTime.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TkOutlineIcons.Calendar,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Waktu Kuliah: ${course.classTime}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = DividerLight)

                    // Aksi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onEditGrade(course) },
                            modifier = Modifier.weight(1f).pressScale()
                        ) {
                            Text("Ubah Nilai")
                        }

                        FilledTonalButton(
                            onClick = { onEditDetails(course) },
                            modifier = Modifier.weight(1f).pressScale()
                        ) {
                            Text("Ubah Info")
                        }

                        OutlinedButton(
                            onClick = { onDelete(course) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TemanKuliahRedDark),
                            modifier = Modifier.pressScale()
                        ) {
                            Text("Hapus")
                        }
                    }
                }
            }
        }
    }
}
