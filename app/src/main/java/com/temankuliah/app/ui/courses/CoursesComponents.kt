package com.temankuliah.app.ui.courses

import androidx.compose.animation.core.snap
import com.temankuliah.app.ui.components.rememberReducedMotion
import com.temankuliah.app.ui.components.rememberEntranceProgress
import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.TkPalette
import com.temankuliah.app.ui.theme.TkShape
import com.temankuliah.app.ui.components.tkHeroCircles
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import com.temankuliah.app.ui.components.MotionTokens
import com.temankuliah.app.ui.components.pressScale
import com.temankuliah.app.ui.components.TkSemesterPickerSheet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.domain.GpaCalculator
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.icons.Add
import com.temankuliah.app.ui.icons.Delete
import com.temankuliah.app.ui.icons.Edit
import com.temankuliah.app.ui.icons.PrintOutline
import com.temankuliah.app.ui.icons.SaveAlt
import com.temankuliah.app.ui.icons.KeyboardArrowDown
import com.temankuliah.app.ui.icons.MenuBook
import com.temankuliah.app.ui.icons.Clear
import com.temankuliah.app.ui.icons.Search
import com.temankuliah.app.ui.icons.Tune
import com.temankuliah.app.ui.icons.Upload
import com.temankuliah.app.ui.theme.BackgroundLight
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
import java.util.Locale
import com.temankuliah.app.ui.theme.tkShadow

/** Skala IPK maksimum yang ditampilkan di kartu hero. */
private const val GPA_SCALE_LABEL = "4,00"

/** Angka dua desimal berkoma (3,86), sesuai desain. */
internal fun formatDecimal(value: Double): String = String.format(IndonesianLocale, "%.2f", value)

/** Ringkasan satu semester untuk dropdown: jumlah matkul, SKS, IPS (bila sudah bernilai), dan status aktif. */
internal data class SemesterOverview(
    val semester: Int,
    val courseCount: Int,
    val totalSks: Int,
    val ips: Double?,
    val isActive: Boolean
) {
    val summary: String get() = "$courseCount matkul · $totalSks SKS"
}

internal fun buildSemesterOverview(courses: List<CourseEntity>, semester: Int, activeSemester: Int): SemesterOverview {
    val inSemester = courses.filter { it.semester == semester }
    val totals = GpaCalculator.semesterTotals(inSemester.map { it.credits to it.gradeLetter })
    return SemesterOverview(
        semester = semester,
        courseCount = inSemester.size,
        totalSks = inSemester.sumOf { it.credits },
        ips = if (totals.gradedCredits > 0) totals.ips else null,
        isActive = semester == activeSemester
    )
}

/** Tombol ikon bulat di kanan header. [filled] true = tombol utama biru; false = tombol tonal lembut. */
@Composable
internal fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    loading: Boolean = false
) {
    val container = if (filled) {
        Modifier.background(
            Brush.linearGradient(listOf(TkGradientStart, TkGradientEnd)),
            TkShape.lg
        )
    } else {
        Modifier.background(TemanKuliahBlueLight, TkShape.lg)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(TkShape.lg)
            .then(container)
            .clickable(enabled = !loading, role = Role.Button, onClick = onClick)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.4.dp,
                color = Color.White
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (filled) Color.White else TemanKuliahBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Dua aksi header: cetak lewat dialog cetak Android, dan unduh PDF langsung ke folder Download. */
@Composable
internal fun TranscriptHeaderActions(onPrint: () -> Unit, onDownload: () -> Unit, isDownloading: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderIconButton(
            icon = Icons.Filled.PrintOutline,
            contentDescription = "Cetak transkrip nilai",
            onClick = onPrint,
            modifier = Modifier.testTag("btn_print_transcript")
        )
        HeaderIconButton(
            icon = Icons.Filled.SaveAlt,
            contentDescription = "Unduh transkrip nilai sebagai PDF",
            onClick = onDownload,
            modifier = Modifier.testTag("btn_download_transcript"),
            filled = true,
            loading = isDownloading
        )
    }
}

/** Kolom pencarian mata kuliah berdasarkan nama atau kode. */
@Composable
internal fun CoursesSearchBar(searchQuery: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "Cari mata kuliah atau kode",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TemanKuliahBlue,
                modifier = Modifier.size(22.dp)
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Hapus pencarian",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        singleLine = true,
        shape = TkShape.xl,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceLight,
            unfocusedContainerColor = SurfaceLight,
            focusedBorderColor = TemanKuliahBlue,
            unfocusedBorderColor = DividerLight
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_search_bar")
    )
}

/** Kartu hero gradien biru: IPK kumulatif, lencana predikat, total SKS, dan total mutu. */
@Composable
internal fun GpaHeroCard(
    cumulativeGpa: Double,
    totalSks: Int,
    totalMutu: Double,
    predicateBadge: String?
) {
    val animatedGpa by rememberEntranceProgress(cumulativeGpa.toFloat())
    val animatedSksValue by rememberEntranceProgress(totalSks.toFloat())
    val animatedSks = (animatedSksValue + 0.5f).toInt()
    val shape = TkShape.xl
    Column(
        modifier = Modifier
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "IPK kumulatif · seluruh semester",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp)
            )
            if (predicateBadge != null) HeroBadge(predicateBadge)
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatDecimal(animatedGpa.toDouble()),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp, lineHeight = 56.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "/ $GPA_SCALE_LABEL",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroStatTile(label = "Total SKS", value = "$animatedSks SKS", modifier = Modifier.weight(1f))
            HeroStatTile(label = "Total mutu", value = formatDecimal(totalMutu), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroBadge(text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeroStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = TkShape.card,
        color = Color.White.copy(alpha = 0.16f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Pil status semester: "Aktif" (hijau) bila belum bernilai, atau "IPS x,xx" (biru) bila sudah. */
@Composable
internal fun SemesterStatusPill(overview: SemesterOverview, modifier: Modifier = Modifier) {
    val (text, container, content) = when {
        overview.ips != null -> Triple("IPS ${formatDecimal(overview.ips)}", TemanKuliahBlueLight, TemanKuliahBlueDark)
        overview.isActive -> Triple("Aktif", TemanKuliahGreenLight, TemanKuliahGreenDark)
        else -> return
    }
    Surface(shape = CircleShape, color = container, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SemesterNumberBadge(semester: Int, selected: Boolean, size: Dp, corner: Dp, fontSize: Int) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(if (selected) TemanKuliahBlue else TemanKuliahBlueLight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = semester.toString(),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else TemanKuliahBlueDark
        )
    }
}

/** Satu dropdown untuk memilih semester 1..N (menggantikan grid 8 tombol). */
@Composable
internal fun SemesterDropdown(
    selected: SemesterOverview,
    overviews: List<SemesterOverview>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (rememberReducedMotion()) snap() else tween(durationMillis = MotionTokens.DurationShort, easing = MotionTokens.EasingStandard),
        label = "coursesChevronRotation"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Pilih semester",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { expanded = true },
                shape = TkShape.card,
                color = SurfaceLight,
                border = BorderStroke(1.dp, DividerLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale()
                    .testTag("semester_dropdown")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SemesterNumberBadge(selected.semester, selected = true, size = 48.dp, corner = 14.dp, fontSize = 20)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Semester ${selected.semester}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selected.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    SemesterStatusPill(selected)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Pilih semester",
                        tint = TemanKuliahBlue,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

        }
    }

    if (expanded) {
        TkSemesterPickerSheet(
            title = "Pilih semester",
            subtitle = "Ketuk untuk berpindah semester",
            selectedSemester = selected.semester,
            activeSemester = overviews.firstOrNull { it.isActive }?.semester ?: 1,
            semesterCount = overviews.maxOfOrNull { it.semester } ?: 8,
            summaryOf = { sem ->
                overviews.firstOrNull { it.semester == sem }?.summary ?: "0 matkul · 0 SKS"
            },
            onSelect = { sem ->
                onSelect(sem)
                expanded = false
            },
            onDismissRequest = { expanded = false }
        )
    }
}

/**
 * Kartu "Riwayat nilai": judul semester, satu tombol "Tambah", lalu daftar nilai atau keadaan kosong.
 * Tombol tambah hanya ada di sini (tidak ada lagi FAB ganda).
 */
@Composable
internal fun HistoryCard(
    semester: Int,
    courseCount: Int,
    courses: List<CourseEntity>,
    isSearching: Boolean,
    onAdd: () -> Unit,
    onImportKhs: () -> Unit,
    onEdit: (CourseEntity) -> Unit,
    onDelete: (CourseEntity) -> Unit
) {
    Surface(
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = if (rememberReducedMotion()) snap() else spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Riwayat nilai",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (courseCount == 0) "Semester $semester" else "Semester $semester · $courseCount mata kuliah",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(12.dp))
                AddGradeButton(onClick = onAdd)
            }

            if (courses.isEmpty()) {
                HistoryEmptyState(
                    semester = semester,
                    isSearching = isSearching,
                    onImportKhs = onImportKhs,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                )
            } else {
                courses.forEach { course ->
                    HorizontalDivider(color = DividerLight, thickness = 1.dp)
                    CourseGradeRow(
                        course = course,
                        onEdit = { onEdit(course) },
                        onDelete = { onDelete(course) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddGradeButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = TemanKuliahBlueLight,
        modifier = Modifier.testTag("btn_add_course")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = TemanKuliahBlueDark,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Tambah",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TemanKuliahBlueDark
            )
        }
    }
}

/** Keadaan kosong berbingkai putus-putus; tombol Impor KHS hanya muncul bila bukan hasil pencarian. */
@Composable
private fun HistoryEmptyState(
    semester: Int,
    isSearching: Boolean,
    onImportKhs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashColor = tkAdaptive(Color(0xFFB9D3F2), Color(0xFF2C4268))
    val shape = TkShape.card
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(BackgroundLight)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawRoundRect(
                    color = dashColor,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                )
            }
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = TkShape.lg,
            color = SurfaceLight,
            shadowElevation = tkShadow(6.dp),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = TemanKuliahBlue,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Text(
            text = if (isSearching) "Tidak ada yang cocok" else "Belum ada data nilai",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isSearching) {
                "Coba kata kunci lain atau hapus pencarian."
            } else {
                "Nilai semester $semester akan tampil di sini setelah KHS tersimpan."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        if (!isSearching) {
            Spacer(Modifier.size(4.dp))
            Surface(
                onClick = onImportKhs,
                shape = CircleShape,
                color = SurfaceLight,
                border = BorderStroke(1.5.dp, TemanKuliahBlue),
                modifier = Modifier.testTag("btn_import_khs")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        tint = TemanKuliahBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Impor KHS",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TemanKuliahBlue
                    )
                }
            }
        }
    }
}

private fun gradeBadgeColors(letter: String): Pair<Color, Color> = when (letter.trim().uppercase(Locale.ROOT)) {
    "A", "A-" -> tkAdaptive(Color(0xFFDDF5E6), Color(0xFF12352A)) to tkAdaptive(Color(0xFF1B7A3E), Color(0xFF5FE0A8))
    "B+", "B", "B-" -> tkAdaptive(Color(0xFFEAF3FF), Color(0xFF1A2A48)) to tkAdaptive(Color(0xFF1858B8), Color(0xFF8CC4FF))
    "C+", "C" -> tkAdaptive(Color(0xFFFFF0E3), Color(0xFF3A2412)) to tkAdaptive(Color(0xFF9A3D00), Color(0xFFFFA566))
    "D", "E" -> tkAdaptive(Color(0xFFFFEBEE), Color(0xFF3D1A20)) to tkAdaptive(Color(0xFFB91C1C), Color(0xFFFF8A8A))
    else -> tkAdaptive(Color(0xFFEEF4FC), TkPalette.colors.surfaceVariant) to TextSecondary
}

/**
 * Satu baris mata kuliah: lencana nilai, nama, kode dan SKS, serta mutu di kanan.
 * Mengetuk baris membuka menu Edit/Hapus supaya desain tetap bersih tanpa ikon aksi.
 */
@Composable
private fun CourseGradeRow(course: CourseEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val graded = GpaCalculator.isGraded(course.gradeLetter)
    val (badgeBg, badgeFg) = gradeBadgeColors(course.gradeLetter)
    val mutu = if (graded) {
        String.format(Locale.US, "%.2f", GpaCalculator.qualityPoints(course.credits, course.gradeLetter.trim().uppercase(Locale.ROOT)))
    } else {
        "-"
    }
    val meta = listOf(course.code.takeIf { it.isNotBlank() }, "${course.credits} SKS").filterNotNull().joinToString(" · ")

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = "Buka menu ${course.name}") { menuOpen = true }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("course_row_${course.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(TkShape.md)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = course.gradeLetter.ifBlank { "-" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = badgeFg,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Mutu",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = mutu,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier.background(SurfaceLight)
        ) {
            DropdownMenuItem(
                text = { Text("Ubah", color = TextPrimary) },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                onClick = {
                    menuOpen = false
                    onEdit()
                },
                modifier = Modifier.testTag("course_menu_edit")
            )
            DropdownMenuItem(
                text = { Text("Hapus", color = TemanKuliahRedDark) },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = TemanKuliahRedDark, modifier = Modifier.size(18.dp))
                },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
                modifier = Modifier.testTag("course_menu_delete")
            )
        }
    }
}

/** Dialog konfirmasi hapus mata kuliah dari transkrip. */
@Composable
internal fun DeleteCourseDialog(course: CourseEntity, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus mata kuliah?", fontWeight = FontWeight.Bold) },
        text = { Text("Mata kuliah ${course.name} (${course.code}) pada Semester ${course.semester} akan dihapus dari transkrip.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Hapus", color = TemanKuliahRedDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

/** Dialog hasil unduh PDF transkrip: menawarkan membuka (Download) atau membagikan (Android 8 ke bawah). */
@Composable
internal fun TranscriptDownloadDialog(result: PdfExportResult, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    when (result) {
        is PdfExportResult.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Unduhan gagal") },
            text = { Text(result.message) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
        )
        is PdfExportResult.Saved -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (result.inDownloads) "Transkrip tersimpan" else "Transkrip siap disimpan") },
            text = {
                Text(
                    if (result.inDownloads) "${result.fileName} tersimpan di folder Download."
                    else "Pilih tujuan untuk menyimpan ${result.fileName}."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = if (result.inDownloads) {
                        android.content.Intent(android.content.Intent.ACTION_VIEW)
                            .setDataAndType(result.uri, "application/pdf")
                    } else {
                        android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_SEND)
                                .setType("application/pdf")
                                .putExtra(android.content.Intent.EXTRA_STREAM, result.uri),
                            result.fileName
                        )
                    }
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    try {
                        context.startActivity(intent)
                    } catch (_: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(context, "Tidak ada aplikasi untuk membuka PDF.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }) { Text(if (result.inDownloads) "Buka" else "Simpan") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
        )
    }
}
