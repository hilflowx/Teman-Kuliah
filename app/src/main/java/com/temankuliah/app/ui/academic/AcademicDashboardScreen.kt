package com.temankuliah.app.ui.academic

import com.temankuliah.app.ui.components.rememberEntranceProgress
import com.temankuliah.app.ui.components.TkPageHeader
import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.tkShadow
import com.temankuliah.app.ui.theme.TkGradientEnd
import com.temankuliah.app.ui.theme.TkGradientStart
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.shadow
import com.temankuliah.app.ui.components.pressScale
import com.temankuliah.app.ui.components.tkHeroCircles
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.domain.ScheduleRules
import com.temankuliah.app.domain.visibleInScheduleList
import com.temankuliah.app.ui.AcademicSection
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.components.TkIconTile
import com.temankuliah.app.ui.components.TkSectionHeader
import com.temankuliah.app.ui.icons.CalendarMonth
import com.temankuliah.app.ui.icons.ChevronRight
import com.temankuliah.app.ui.icons.Description
import com.temankuliah.app.ui.icons.MenuBook
import com.temankuliah.app.ui.icons.School
import com.temankuliah.app.ui.icons.Timeline
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.SurfaceVariantLight
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TemanKuliahOrangeLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import com.temankuliah.app.ui.theme.TkSpace

/**
 * Halaman Akademik, urutan mengikuti desain:
 * 1. Judul + program studi
 * 2. Kartu biru Semester Aktif (statistik singkat, progres, tombol buka)
 * 3. Menu akademik: kisi 2x2 (KRS, KHS, Transkrip Nilai, Jadwal Kuliah)
 * Target kelulusan ada di halaman Semester Aktif.
 */
@Composable
fun AcademicDashboardScreen(
    user: UserEntity,
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier,
    onOpenSection: (AcademicSection) -> Unit,
    onOpenSchedule: () -> Unit
) {
    val courses by viewModel.courseController.courses.collectAsStateWithLifecycle()
    val schedules by viewModel.scheduleController.schedules.collectAsStateWithLifecycle()
    val gpaSummary by viewModel.courseController.gpaSummary.collectAsStateWithLifecycle()
    val startDates by viewModel.preferencesController.semesterStartDates.collectAsStateWithLifecycle()
    val semester = user.activeSemester
    val currentCourses = remember(courses, semester) { courses.filter { it.semester == semester } }
    val scheduleCount = remember(schedules, semester) {
        ScheduleRules.buildSlots(schedules.filter { it.semester == semester }.visibleInScheduleList(), LocalDate.now()).size
    }
    val krsCredits = currentCourses.sumOf { it.credits }
    val gradedCredits = gpaSummary.semesterCreditsMap[semester] ?: 0
    val ips = gpaSummary.semesterGpaMap[semester] ?: 0.0
    val progress = remember(startDates, semester) { semesterProgressOf(startDates[semester]) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "academic_header") {
            TkPageHeader(
                title = "Akademik",
                supporting = user.studyProgram.ifBlank { "Mahasiswa Aktif" }
            )
        }

        item(key = "academic_active_semester") {
            ActiveSemesterHero(
                semester = semester,
                courseCount = currentCourses.size,
                credits = krsCredits,
                progress = progress,
                onOpen = { onOpenSection(AcademicSection.SEMESTER_ACTIVE) }
            )
        }

        item(key = "academic_menu_header") { TkSectionHeader(title = "Menu akademik") }

        item(key = "academic_menu_row") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AcademicMenuTile(
                        icon = Icons.Default.Description,
                        label = "KRS",
                        caption = "$krsCredits SKS",
                        modifier = Modifier.weight(1f).testTag("card_krs"),
                        onClick = { onOpenSection(AcademicSection.KRS) }
                    )
                    AcademicMenuTile(
                        icon = Icons.Default.Timeline,
                        label = "KHS",
                        caption = if (gradedCredits > 0) "IPS " + String.format(IndonesianLocale, "%.2f", ips) else "Menunggu",
                        waiting = gradedCredits <= 0,
                        modifier = Modifier.weight(1f).testTag("card_khs"),
                        onClick = { onOpenSection(AcademicSection.KHS) }
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AcademicMenuTile(
                        icon = Icons.Default.MenuBook,
                        label = "Transkrip Nilai",
                        caption = "${gpaSummary.totalCredits} SKS",
                        modifier = Modifier.weight(1f).testTag("card_transcript"),
                        onClick = { onOpenSection(AcademicSection.COURSES) }
                    )
                    AcademicMenuTile(
                        icon = Icons.Default.CalendarMonth,
                        label = "Jadwal Kuliah",
                        caption = "$scheduleCount aktif",
                        modifier = Modifier.weight(1f).testTag("card_schedule"),
                        onClick = onOpenSchedule
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSemesterHero(
    semester: Int,
    courseCount: Int,
    credits: Int,
    progress: SemesterProgress,
    onOpen: () -> Unit
) {
    val onBlue = Color.White
    val softWhite = Color.White.copy(alpha = 0.18f)
    Surface(
        shape = TkShape.xl,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("academic_hero_card")
            .shadow(
                elevation = tkShadow(16.dp),
                shape = TkShape.xl,
                ambientColor = TkGradientEnd.copy(alpha = 0.25f),
                spotColor = TkGradientEnd.copy(alpha = 0.45f)
            )
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(TkGradientStart, TkGradientEnd)))
                .tkHeroCircles()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(TkShape.lg).background(softWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = onBlue, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Semester aktif", style = MaterialTheme.typography.labelMedium, color = onBlue.copy(alpha = 0.9f))
                    Text(
                        "Semester $semester",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = onBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    Modifier.clip(CircleShape).background(softWhite).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(onBlue))
                    Spacer(Modifier.width(6.dp))
                    Text("Aktif", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = onBlue)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStat("Mata kuliah", "$courseCount matkul", softWhite, Modifier.weight(1f))
                HeroStat("Beban studi", "$credits SKS", softWhite, Modifier.weight(1f))
            }

            val animatedFraction by rememberEntranceProgress(progress.fraction)
            val animatedPercent = (animatedFraction * 100f).roundToInt()

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progres perkuliahan", style = MaterialTheme.typography.labelMedium, color = onBlue)
                    Text("$animatedPercent%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = onBlue)
                }
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = onBlue,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Text(progress.caption, style = MaterialTheme.typography.bodySmall, color = onBlue.copy(alpha = 0.92f))
            }

            Button(
                onClick = onOpen,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = tkAdaptive(TemanKuliahBlue, Color(0xFF1858B8))
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("btn_open_active_semester")
            ) {
                Text("Buka semester aktif", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, background: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(TkShape.lg).background(background).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
    }
}

/** Kartu menu sesuai desain: ikon di kiri atas, panah bulat di kanan atas, judul, lalu chip keterangan. */
@Composable
private fun AcademicMenuTile(
    icon: ImageVector,
    label: String,
    caption: String,
    modifier: Modifier = Modifier,
    waiting: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        modifier = modifier.heightIn(min = 150.dp).pressScale()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                TkIconTile(icon, size = 52.dp, shape = TkShape.lg)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(SurfaceVariantLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(if (waiting) TemanKuliahOrangeLight else TemanKuliahBlueLight)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (waiting) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(TemanKuliahOrangeDark))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        caption,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (waiting) TemanKuliahOrangeDark else TemanKuliahBlueDark,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
