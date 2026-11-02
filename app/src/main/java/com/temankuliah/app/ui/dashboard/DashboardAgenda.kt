package com.temankuliah.app.ui.dashboard

import com.temankuliah.app.ui.theme.TkPalette
import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.domain.isOnlineSchedule
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.components.TkEmptyState
import com.temankuliah.app.ui.components.formatClock
import com.temankuliah.app.ui.icons.Assignment
import com.temankuliah.app.ui.icons.CalendarMonth
import com.temankuliah.app.ui.icons.EventAvailable
import com.temankuliah.app.ui.icons.TkOutlineIcons
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

// -------------------------------------------------------------------------------------------------
// Bagian "Hari ini"
// -------------------------------------------------------------------------------------------------

@Composable
internal fun TodaySection(
    selectedSegment: Int,
    onSelectSegment: (Int) -> Unit,
    todaySchedules: List<ScheduleEntity>,
    todayTasks: List<TaskEntity>,
    upcomingAgenda: List<DashboardAgendaEntry>,
    allCourses: List<CourseEntity>,
    onOpenScheduleTab: () -> Unit,
    onOpenTasksTab: () -> Unit,
    onAddSchedule: () -> Unit,
    onAddTask: () -> Unit,
    onEditSchedule: (ScheduleEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit
) {
    val now = System.currentTimeMillis()
    // Penanda "Sedang" ikut bergerak tanpa menunggu data berubah.
    val nowTime by produceState(initialValue = LocalTime.now()) {
        while (true) {
            delay(30_000)
            value = LocalTime.now()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_command_card")
    ) {
        DashSectionHeader(title = "Hari ini", actionLabel = "Kalender", onAction = onOpenScheduleTab)
        Spacer(Modifier.height(3.dp))
        DashTabs(
            items = listOf("Jadwal", "Tugas", "Agenda"),
            selectedIndex = selectedSegment,
            onSelect = onSelectSegment
        )
        Spacer(Modifier.height(9.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (selectedSegment) {
                0 -> {
                    if (todaySchedules.isEmpty()) {
                        DashCard {
                            TkEmptyState(
                                icon = Icons.Default.EventAvailable,
                                compact = true,
                                title = "Tidak ada kuliah hari ini",
                                message = "Nikmati waktu istirahat atau persiapan studi.",
                                actionLabel = "+ Jadwal",
                                onAction = onAddSchedule
                            )
                        }
                    } else {
                        todaySchedules.forEach { schedule ->
                            ScheduleItem(
                                schedule = schedule,
                                ongoing = isOngoing(schedule, nowTime),
                                onClick = { onEditSchedule(schedule) }
                            )
                        }
                    }
                }

                1 -> {
                    if (todayTasks.isEmpty()) {
                        DashCard {
                            TkEmptyState(
                                icon = Icons.Default.Assignment,
                                compact = true,
                                title = "Tidak ada tugas hari ini",
                                message = "Tidak ada tugas yang jatuh tempo atau terlambat.",
                                actionLabel = "+ Tugas",
                                onAction = onAddTask
                            )
                        }
                    } else {
                        val visible = todayTasks.take(MAX_TODAY_TASKS)
                        visible.forEach { task ->
                            val overdue = task.dueAtMillis < now
                            TaskItem(
                                task = task,
                                courseName = courseNameOf(task, allCourses),
                                pill = if (overdue) DashPill("Terlambat", DashErrSoft, DashErrInk)
                                else DashPill("Hari ini", DashOrangeSoft, DashOrange),
                                onClick = { onEditTask(task) }
                            )
                        }
                        val extra = todayTasks.size - visible.size
                        if (extra > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .clickable(onClick = onOpenTasksTab),
                                contentAlignment = Alignment.Center
                            ) {
                                DashText(
                                    text = "+$extra lainnya, lihat semua",
                                    color = DashBlue,
                                    size = 12.5.sp,
                                    weight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                else -> {
                    if (upcomingAgenda.isEmpty()) {
                        DashCard {
                            TkEmptyState(
                                icon = Icons.Default.CalendarMonth,
                                compact = true,
                                title = "Belum ada agenda mendatang",
                                message = "Jadwal dan kegiatan akademik akan tampil di sini.",
                                actionLabel = "Lihat Kalender",
                                onAction = onOpenScheduleTab
                            )
                        }
                    } else {
                        upcomingAgenda.forEach { agenda ->
                            AgendaItem(
                                agenda = agenda,
                                onClick = agenda.schedule?.let { schedule -> { onEditSchedule(schedule) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_TODAY_TASKS = 5

// -------------------------------------------------------------------------------------------------
// Bagian "Deadline terdekat"
// -------------------------------------------------------------------------------------------------

@Composable
internal fun NearestDeadlineSection(
    tasks: List<TaskEntity>,
    allCourses: List<CourseEntity>,
    onOpenTasksTab: () -> Unit,
    onEditTask: (TaskEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_nearest_deadlines")
    ) {
        DashSectionHeader(title = "Tenggat terdekat", actionLabel = "Semua", onAction = onOpenTasksTab)
        Spacer(Modifier.height(3.dp))
        if (tasks.isEmpty()) {
            DashCard {
                DashText(
                    text = "Belum ada tenggat mendatang.",
                    color = DashMuted,
                    size = 12.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tasks.forEach { task ->
                    TaskItem(
                        task = task,
                        courseName = courseNameOf(task, allCourses),
                        pill = deadlinePill(daysFromToday(task.dueAtMillis)),
                        onClick = { onEditTask(task) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Item daftar
// -------------------------------------------------------------------------------------------------

private class DashPill(val text: String, val background: Color, val content: Color)

private fun deadlinePill(days: Long): DashPill {
    val text = if (days <= 0L) "Hari ini" else "$days hari"
    return if (days <= 1L) DashPill(text, DashOrangeSoft, DashOrange) else DashPill(text, DashTint, DashBlue)
}

private fun courseNameOf(task: TaskEntity, courses: List<CourseEntity>): String =
    courses.firstOrNull { it.id == task.courseId }?.name ?: "Tugas mandiri"

/** "08:00" menjadi "08.00", seperti pada desain. */
private fun dotted(clock: String): String = clock.replace(':', '.')

private fun isOngoing(schedule: ScheduleEntity, now: LocalTime): Boolean = runCatching {
    val start = LocalTime.parse(schedule.startTime)
    val end = LocalTime.parse(schedule.endTime)
    !schedule.isCompleted && !now.isBefore(start) && now.isBefore(end)
}.getOrDefault(false)

@Composable
private fun ScheduleItem(
    schedule: ScheduleEntity,
    ongoing: Boolean,
    onClick: () -> Unit
) {
    val online = schedule.isOnlineSchedule()
    val place = if (online) "Daring" else schedule.room.ifBlank { "Ruang TBA" }
    val pill = when {
        schedule.isCompleted -> DashPill("Selesai", DashOkSoft, DashOkInk)
        ongoing -> DashPill("Sedang", DashTint, DashBlue)
        else -> null
    }
    TimelineCard(
        timeBig = dotted(schedule.startTime),
        timeSmall = dotted(schedule.endTime),
        barColor = when {
            schedule.isCompleted -> DashOk
            ongoing -> DashSky
            else -> DashSkySoft
        },
        title = schedule.courseName,
        subtitle = place,
        pill = pill,
        onClick = onClick
    )
}

@Composable
private fun AgendaItem(
    agenda: DashboardAgendaEntry,
    onClick: (() -> Unit)?
) {
    val isClass = agenda.schedule != null
    TimelineCard(
        timeBig = formatClock(agenda.timestamp).replace(':', '.'),
        timeSmall = dayLabel(agenda.timestamp),
        barColor = if (isClass) DashSky else DashSkySoft,
        title = agenda.title,
        subtitle = agenda.subtitle,
        pill = null,
        onClick = onClick
    )
}

/** Kartu jadwal: kolom jam, batang warna 3dp, judul + keterangan, pil status opsional di kanan. */
@Composable
private fun TimelineCard(
    timeBig: String,
    timeSmall: String,
    barColor: Color,
    title: String,
    subtitle: String,
    pill: DashPill?,
    onClick: (() -> Unit)?
) {
    DashCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.widthIn(min = 52.dp)) {
                DashText(timeBig, DashInk, 11.5.sp, weight = FontWeight.Bold, lineHeight = 16.sp, maxLines = 1)
                DashText(
                    text = timeSmall,
                    color = DashMuted,
                    size = 10.5.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .heightIn(min = 37.dp)
                    .clip(CircleShape)
                    .background(barColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                DashText(
                    text = title,
                    color = DashInk,
                    size = 14.sp,
                    weight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                DashText(
                    text = subtitle,
                    color = DashMuted,
                    size = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (pill != null) {
                Spacer(Modifier.width(8.dp))
                DashPillView(pill)
            }
        }
    }
}

/** Kartu tugas/deadline: ubin ikon papan klip, judul, "Mata kuliah, besok 23.59", dan pil sisa hari. */
@Composable
private fun TaskItem(
    task: TaskEntity,
    courseName: String,
    pill: DashPill,
    onClick: () -> Unit
) {
    DashCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 57.dp)
                .padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(TkShape.md)
                    .background(DashTile),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TkOutlineIcons.Clipboard,
                    contentDescription = null,
                    tint = DashBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                DashText(
                    text = task.title,
                    color = DashInk,
                    size = 14.sp,
                    weight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    DashText(
                        text = courseName,
                        color = DashMuted,
                        size = 12.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    DashText(
                        text = ", ${dueLabel(task.dueAtMillis)}",
                        color = DashMuted,
                        size = 12.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            DashPillView(pill)
        }
    }
}

@Composable
private fun DashPillView(pill: DashPill) {
    Box(
        modifier = Modifier
            .heightIn(min = 27.dp)
            .clip(CircleShape)
            .background(pill.background)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        DashText(
            text = pill.text,
            color = pill.content,
            size = 11.sp,
            weight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Pembungkus umum
// -------------------------------------------------------------------------------------------------

@Composable
private fun DashSectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DashText(
            text = title,
            color = DashInk,
            size = 16.sp,
            weight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(onClick = onAction)
                .padding(start = 12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            DashText(
                text = actionLabel,
                color = DashBlue,
                size = 12.5.sp,
                weight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** Kartu putih bergaris biru muda dengan bayangan kebiruan yang lembut. */
@Composable
private fun DashCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = TkShape.card
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                // Di tema gelap kartu rata berbingkai tipis, seperti desain; bayangan biru hanya di tema terang.
                elevation = if (TkPalette.isDark) 0.dp else 6.dp,
                shape = shape,
                ambientColor = DashBlue.copy(alpha = 0.10f),
                spotColor = DashBlue.copy(alpha = 0.16f)
            )
            .clip(shape)
            .background(DashSurface)
            .border(BorderStroke(1.dp, DashLine), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

// -------------------------------------------------------------------------------------------------
// Pelabelan tanggal
// -------------------------------------------------------------------------------------------------

private fun daysFromToday(millis: Long): Long {
    val zone = ZoneId.systemDefault()
    return ChronoUnit.DAYS.between(LocalDate.now(zone), Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())
}

/** "hari ini 23.59", "besok 23.59", "kemarin 08.00", atau "Rabu 28 Okt". */
private fun dueLabel(millis: Long): String {
    val clock = formatClock(millis).replace(':', '.')
    return when (daysFromToday(millis)) {
        0L -> "hari ini $clock"
        1L -> "besok $clock"
        -1L -> "kemarin $clock"
        else -> SimpleDateFormat("EEEE d MMM", IndonesianLocale).format(Date(millis))
    }
}

/** "Hari ini", "Besok", nama hari (dalam seminggu), atau "5 Okt". */
private fun dayLabel(millis: Long): String {
    val days = daysFromToday(millis)
    return when {
        days == 0L -> "Hari ini"
        days == 1L -> "Besok"
        days in 2..6 -> SimpleDateFormat("EEEE", IndonesianLocale).format(Date(millis))
        else -> SimpleDateFormat("d MMM", IndonesianLocale).format(Date(millis))
    }
}
