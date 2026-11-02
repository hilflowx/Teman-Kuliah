package com.temankuliah.app.ui.components

import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.TkPalette
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.tkShadow

/**
 * Bottom sheet modal untuk memilih semester sesuai desain resmi:
 * - Header: Judul, subjudul dinamis per menu, dan tombol tutup silang (X).
 * - Daftar semester dikelompokkan per tahun (Tahun 1: Sem 1–2, Tahun 2: Sem 3–4, dst.).
 * - Kartu semester grid: lencana angka putih, status aktif hijau, status terpilih biru dengan centang.
 * - Tombol aksi bawah berlabel "Pilih Semester {N}".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TkSemesterPickerSheet(
    title: String = "Pilih semester",
    subtitle: String = "Ketuk untuk berpindah semester",
    selectedSemester: Int,
    activeSemester: Int,
    semesterCount: Int = 8,
    summaryOf: (Int) -> String,
    onAddSemester: (() -> Unit)? = null,
    onSelect: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempSelected by rememberSaveable { mutableIntStateOf(selectedSemester) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = SurfaceLight,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.5.dp)
                        .clip(CircleShape)
                        .background(tkAdaptive(Color(0xFFCBD5E1), Color(0xFF3A4A66)))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    onClick = onDismissRequest,
                    shape = CircleShape,
                    color = TemanKuliahBlueLight,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_close_semester_picker")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TemanKuliahBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Grid daftar semester
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalSemesters = maxOf(8, semesterCount)
                val totalYears = (totalSemesters + 1) / 2
                for (year in 1..totalYears) {
                    val sem1 = (year - 1) * 2 + 1
                    val sem2 = (year - 1) * 2 + 2

                    item(key = "year_header_$year") {
                        Text(
                            text = "Tahun $year · Sem $sem1–$sem2",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    item(key = "year_row_$year") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SemesterGridCard(
                                sem = sem1,
                                isSelected = tempSelected == sem1,
                                isActive = sem1 == activeSemester,
                                summary = summaryOf(sem1),
                                onSelect = { tempSelected = sem1 },
                                modifier = Modifier.weight(1f)
                            )
                            if (sem2 <= totalSemesters) {
                                SemesterGridCard(
                                    sem = sem2,
                                    isSelected = tempSelected == sem2,
                                    isActive = sem2 == activeSemester,
                                    summary = summaryOf(sem2),
                                    onSelect = { tempSelected = sem2 },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (onAddSemester != null) {
                    item(key = "btn_add_semester_item") {
                        Surface(
                            onClick = onAddSemester,
                            shape = RoundedCornerShape(14.dp),
                            color = TemanKuliahBlueLight,
                            border = BorderStroke(1.dp, TemanKuliahBlue.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                                .pressScale()
                                .testTag("btn_add_semester")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = TemanKuliahBlueDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Tambah semester",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TemanKuliahBlueDark
                                )
                            }
                        }
                    }
                }
            }

            // Tombol Konfirmasi Pilihan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = {
                        onSelect(tempSelected)
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_confirm_semester")
                ) {
                    Text(
                        text = "Pilih Semester $tempSelected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SemesterGridCard(
    sem: Int,
    isSelected: Boolean,
    isActive: Boolean,
    summary: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBackground by animateColorAsState(
        targetValue = if (isSelected) TemanKuliahBlue else tkAdaptive(Color(0xFFF1F5F9), TkPalette.colors.surfaceVariant),
        label = "semCardBg"
    )
    val cardBorder = if (isSelected) null else BorderStroke(1.dp, tkAdaptive(Color(0xFFE2E8F0), TkPalette.colors.divider))

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = cardBackground,
        border = cardBorder,
        modifier = modifier
            .heightIn(min = 68.dp)
            .pressScale()
            .testTag("semester_grid_card_$sem")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lencana kotak berisi nomor semester
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceLight,
                    shadowElevation = if (isSelected) tkShadow(2.dp) else 0.dp,
                    border = if (isSelected) null else BorderStroke(1.dp, tkAdaptive(Color(0xFFE2E8F0), TkPalette.colors.divider)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$sem",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = TemanKuliahBlue
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (isActive || isSelected) 26.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Semester $sem",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (isSelected) Color.White else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summary,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.88f) else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Lencana status Aktif di kanan atas
            if (isActive) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color.White else TemanKuliahGreenLight,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "Aktif",
                        color = if (isSelected) TemanKuliahBlueDark else TemanKuliahGreenDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Ikon centang di kanan bawah saat terpilih
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = SurfaceLight,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = TemanKuliahBlue,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
