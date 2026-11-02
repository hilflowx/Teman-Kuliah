package com.temankuliah.app.ui.academic

import androidx.compose.animation.core.snap
import com.temankuliah.app.ui.components.rememberReducedMotion
import com.temankuliah.app.ui.components.rememberEntranceProgress
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.temankuliah.app.ui.components.MotionTokens
import com.temankuliah.app.ui.components.pressScale
import com.temankuliah.app.ui.components.TkSemesterPickerSheet
import com.temankuliah.app.ui.theme.tkShadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.ui.DialogType
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.components.TkDatePickerDialog
import com.temankuliah.app.ui.components.TkIconTile
import com.temankuliah.app.ui.components.TkSectionHeader
import com.temankuliah.app.ui.components.TkSubHeader
import com.temankuliah.app.ui.icons.CalendarMonth
import com.temankuliah.app.ui.icons.ChevronDown
import com.temankuliah.app.ui.icons.Edit
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.SurfaceVariantLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TextMuted
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import com.temankuliah.app.ui.theme.TkSpace
import java.time.LocalDate

/** Ringkasan data nyata satu semester, diambil dari mata kuliah tersimpan. */
private data class SemesterSummary(val courses: Int, val credits: Int, val ips: Double?)

private fun summaryOf(semester: Int, courses: List<CourseEntity>, ipsMap: Map<Int, Double>): SemesterSummary {
    val c = courses.filter { it.semester == semester }
    return SemesterSummary(
        courses = c.size,
        credits = c.sumOf { it.credits },
        ips = ipsMap[semester]?.takeIf { value -> c.any { it.gradeLetter.trim() != "-" } && value > 0.0 }
    )
}

/** Estimasi jam tatap muka per minggu: (nilai, keterangan). */
private fun weeklyHoursOf(schedules: List<ScheduleEntity>, credits: Int): Pair<String, String> {
    val unique = schedules.distinctBy { "${it.dayOfWeek}_${it.startTime}_${it.endTime}_${it.courseName}" }
    val minutes = unique.sumOf { s ->
        val start = runCatching { val p = s.startTime.split(':'); p[0].toInt() * 60 + p[1].toInt() }.getOrDefault(0)
        val end = runCatching { val p = s.endTime.split(':'); p[0].toInt() * 60 + p[1].toInt() }.getOrDefault(0)
        (end - start).coerceAtLeast(0)
    }
    return when {
        minutes > 0 -> {
            val h = minutes / 60
            val m = minutes % 60
            (if (m == 0) "$h jam" else "${h}j ${m}m") to "per minggu"
        }
        credits > 0 -> "~${(credits * 50) / 60} jam" to "estimasi per minggu"
        else -> "0 jam" to "per minggu"
    }
}

/**
 * Semester Aktif, urutan mengikuti desain:
 * pemilih semester (dropdown) → progres perkuliahan → statistik → target kelulusan.
 */
@Composable
fun SemesterActiveScreen(
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val semesterCount by viewModel.semesterCount.collectAsStateWithLifecycle()
    val courses by viewModel.courseController.courses.collectAsStateWithLifecycle()
    val schedules by viewModel.scheduleController.schedules.collectAsStateWithLifecycle()
    val gpaSummary by viewModel.courseController.gpaSummary.collectAsStateWithLifecycle()
    val startDates by viewModel.preferencesController.semesterStartDates.collectAsStateWithLifecycle()
    val targetCredits by viewModel.preferencesController.targetCredits.collectAsStateWithLifecycle()

    var expanded by rememberSaveable { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }

    val activeCourses = remember(courses, activeSemester) { courses.filter { it.semester == activeSemester } }
    val activeSchedules = remember(schedules, activeSemester) { schedules.filter { it.semester == activeSemester } }
    val activeCredits = remember(activeCourses) { activeCourses.sumOf { it.credits } }
    val (hoursValue, hoursCaption) = remember(activeSchedules, activeCredits) { weeklyHoursOf(activeSchedules, activeCredits) }
    val progress = remember(startDates, activeSemester) { semesterProgressOf(startDates[activeSemester]) }
    val summaries = remember(courses, gpaSummary, semesterCount) {
        (1..semesterCount).map { summaryOf(it, courses, gpaSummary.semesterGpaMap) }
    }
    val activeSummary = summaries.getOrNull(activeSemester - 1) ?: SemesterSummary(0, 0, null)

    BackHandler(enabled = expanded) { expanded = false }

    Box(
        modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(Modifier.fillMaxSize()) {
            TkSubHeader(
                title = "Semester aktif",
                supporting = "Ringkasan & kendali semester",
                onBack = onBack,
                backDescription = "Kembali ke Akademik"
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "semester_picker") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pilih semester", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        SemesterPickerCard(
                            semester = activeSemester,
                            subtitle = "${activeSummary.courses} matkul · ${activeSummary.credits} SKS",
                            expanded = false,
                            onClick = { expanded = true },
                            modifier = Modifier.testTag("semester_picker")
                        )
                    }
                }

                item(key = "semester_progress") {
                    ProgressCard(
                        progress = progress,
                        onPickStartDate = { showStartDatePicker = true }
                    )
                }

                item(key = "semester_stats") {
                    val cumulative = gpaSummary.cumulativeGpa
                    val hasGrades = gpaSummary.totalCredits > 0
                    val isCumLaude = gpaSummary.honorsPredicate.contains("Cum Laude")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            title = "Beban studi",
                            value = "$activeCredits SKS",
                            caption = "${activeCourses.size} mata kuliah",
                            captionColor = TemanKuliahBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Tatap muka",
                            value = hoursValue,
                            caption = hoursCaption,
                            captionColor = TemanKuliahBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Performa IPK",
                            value = if (hasGrades) String.format(IndonesianLocale, "%.2f", cumulative) else "–",
                            caption = when {
                                !hasGrades -> "Belum ada nilai"
                                isCumLaude -> "Cum Laude"
                                else -> "IPK kumulatif"
                            },
                            captionColor = if (isCumLaude) TemanKuliahGreenDark else TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item(key = "target_header") {
                    TkSectionHeader(title = "Target kelulusan", modifier = Modifier.padding(top = 6.dp))
                }

                item(key = "target_card") {
                    GraduationTargetCard(
                        targetCredits = targetCredits,
                        passedCredits = gpaSummary.passedCredits,
                        onEditTarget = { viewModel.openDialog(DialogType.StudyTarget) }
                    )
                }
            }
        }

        if (expanded) {
            TkSemesterPickerSheet(
                title = "Pilih semester",
                subtitle = "Ketuk untuk berpindah semester",
                selectedSemester = activeSemester,
                activeSemester = activeSemester,
                semesterCount = maxOf(8, semesterCount),
                summaryOf = { sem ->
                    val semCourses = courses.filter { it.semester == sem }
                    "${semCourses.size} matkul · ${semCourses.sumOf { it.credits }} SKS"
                },
                onAddSemester = { viewModel.addSemester() },
                onSelect = { sem ->
                    if (sem != activeSemester) viewModel.setActiveSemester(sem)
                    expanded = false
                },
                onDismissRequest = { expanded = false }
            )
        }
    }

    if (showStartDatePicker) {
        val initialDate = runCatching {
            startDates[activeSemester]?.let { LocalDate.parse(it) }
        }.getOrNull() ?: LocalDate.now()
        TkDatePickerDialog(
            title = "Mulai Kuliah Semester $activeSemester",
            initialDate = initialDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { date ->
                viewModel.preferencesController.setSemesterStartDate(activeSemester, date.toString())
                showStartDatePicker = false
            }
        )
    }
}

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
private fun StatusPill(text: String) {
    Box(Modifier.clip(CircleShape).background(TemanKuliahGreenLight).padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(text, color = TemanKuliahGreenDark, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/** Kartu pemilih semester. Dipakai dua kali: keadaan tertutup di daftar dan keadaan terbuka di atas scrim. */
@Composable
private fun SemesterPickerCard(
    semester: Int,
    subtitle: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (rememberReducedMotion()) snap() else tween(durationMillis = MotionTokens.DurationShort, easing = MotionTokens.EasingStandard),
        label = "chevronRotation"
    )
    Surface(
        onClick = onClick,
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(if (expanded) 1.5.dp else 1.dp, if (expanded) TemanKuliahBlue else DividerLight),
        shadowElevation = if (expanded) tkShadow(6.dp) else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .pressScale()
            .semantics { contentDescription = "Semester $semester, aktif. ${if (expanded) "Ketuk untuk menutup" else "Ketuk untuk memilih semester"}" }
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SemesterBadge(semester, selected = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Semester $semester", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusPill("Aktif")
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronDown,
                contentDescription = null,
                tint = if (expanded) TemanKuliahBlue else TextMuted,
                modifier = Modifier.size(22.dp).rotate(chevronRotation)
            )
        }
    }
}

@Composable
private fun ProgressCard(progress: SemesterProgress, onPickStartDate: () -> Unit) {
    Surface(
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progres perkuliahan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${progress.percent}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TemanKuliahBlue)
            }
            val animatedCourseProgress by rememberEntranceProgress(progress.fraction)
            LinearProgressIndicator(
                progress = { animatedCourseProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = TemanKuliahBlue,
                trackColor = SurfaceVariantLight
            )
            if (progress.formattedStartDate != null) {
                Text(progress.caption, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(TkShape.lg)
                    .background(SurfaceVariantLight.copy(alpha = 0.6f))
                    .clickable(onClick = onPickStartDate)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TkIconTile(Icons.Default.CalendarMonth, size = 40.dp, background = SurfaceLight, shape = TkShape.md)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tanggal mulai kuliah", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                    Text(
                        progress.formattedStartDate ?: "Belum diatur",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onPickStartDate,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, TemanKuliahBlue),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("btn_set_start_date")
                ) {
                    Text(
                        if (progress.formattedStartDate != null) "Ubah" else "Atur",
                        color = TemanKuliahBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, caption: String, captionColor: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = TkShape.lg,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        modifier = modifier.heightIn(min = 96.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = captionColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TargetGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val stroke = Stroke(width = size.minDimension * 0.09f)
        drawCircle(color, radius = size.minDimension * 0.46f, center = c, style = stroke)
        drawCircle(color, radius = size.minDimension * 0.28f, center = c, style = stroke)
        drawCircle(color, radius = size.minDimension * 0.09f, center = c)
    }
}

@Composable
private fun GraduationTargetCard(targetCredits: Int?, passedCredits: Int, onEditTarget: () -> Unit) {
    val hasTarget = targetCredits != null && targetCredits > 0
    Surface(
        shape = TkShape.card,
        color = SurfaceVariantLight.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, DividerLight),
        modifier = Modifier.fillMaxWidth().testTag("study_target_card")
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(TkShape.lg).background(SurfaceLight).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(44.dp).clip(TkShape.md).background(TemanKuliahBlueLight), contentAlignment = Alignment.Center) {
                    TargetGlyph(TemanKuliahBlue, Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Target SKS kelulusan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (hasTarget) "$passedCredits dari $targetCredits SKS tercapai" else "Belum diatur",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onEditTarget,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("btn_edit_study_target")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (hasTarget) "Ubah" else "Atur", fontWeight = FontWeight.Bold)
                }
            }

            if (targetCredits != null && targetCredits > 0) {
                val targetFraction = (passedCredits.toFloat() / targetCredits.toFloat()).coerceIn(0f, 1f)
                val animatedProgress by rememberEntranceProgress(targetFraction)
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = TemanKuliahBlue,
                    trackColor = TemanKuliahBlueLight
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TargetFigure(
                    label = "Sisa SKS",
                    main = if (hasTarget) "${(targetCredits!! - passedCredits).coerceAtLeast(0)}" else "–",
                    suffix = if (hasTarget) " dari $targetCredits" else "",
                    modifier = Modifier.weight(1f)
                )
                TargetFigure(label = "Batas cumlaude", main = "3,50", suffix = "", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TargetFigure(label: String, main: String, suffix: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(TkShape.lg).background(SurfaceLight).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(main, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
            if (suffix.isNotEmpty()) {
                Text(suffix, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}
