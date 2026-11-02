@file:Suppress("DEPRECATION")

package com.temankuliah.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.temankuliah.app.ui.components.staggeredEntrance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.domain.TaskStatus
import com.temankuliah.app.ui.DialogType
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.ui.theme.TkSpace
import com.temankuliah.app.ui.theme.rememberTkTwoPane
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

/**
 * Beranda TemanKuliah sesuai desain terbaru:
 * - Header: avatar inisial, sapaan, tombol notifikasi
 * - Kartu "Pencapaian Akademik" (IPK, jalur cumlaude, IPS/SKS/Matkul)
 * - "Hari ini" dengan tab Jadwal / Tugas / Agenda
 * - "Deadline terdekat"
 * - Layar >= 840dp memakai 2 kolom
 */
@Composable
fun DashboardScreen(
    user: UserEntity,
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier,
    onOpenProfile: () -> Unit = { viewModel.setTab(4) },
    onOpenAcademic: () -> Unit = { viewModel.setTab(3) },
    onOpenScheduleTab: () -> Unit = { viewModel.setTab(1) },
    onOpenTasksTab: () -> Unit = { viewModel.setTab(2) },
    onOpenNotifications: () -> Unit = {}
) {
    val unreadNotifications by viewModel.notificationController.unreadCount.collectAsStateWithLifecycle()
    val gpaSummary by viewModel.courseController.gpaSummary.collectAsStateWithLifecycle()
    val todaySchedules by viewModel.scheduleController.todaySchedules.collectAsStateWithLifecycle()
    val schedules by viewModel.scheduleController.schedules.collectAsStateWithLifecycle()
    val academicEvents by viewModel.calendarController.academicEvents.collectAsStateWithLifecycle()
    val tasks by viewModel.taskController.tasks.collectAsStateWithLifecycle()
    val allCourses by viewModel.courseController.courses.collectAsStateWithLifecycle()
    val targetCredits by viewModel.preferencesController.targetCredits.collectAsStateWithLifecycle()
    val semester = user.activeSemester
    val currentCourses = remember(allCourses, semester) { allCourses.filter { it.semester == semester } }
    val semesterGpa = gpaSummary.semesterGpaMap[semester] ?: 0.0
    val semesterGradedCredits = gpaSummary.semesterCreditsMap[semester] ?: 0

    // Tab Tugas pada "Hari ini": tugas jatuh tempo hari ini (lebih dulu), lalu yang terlambat.
    // Deadline terdekat: tugas setelah hari ini, supaya dua bagian tidak menampilkan tugas yang sama.
    val (todayTasks, nearestDeadlines) = remember(tasks, semester) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfToday = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val open = tasks.filter { it.semester == semester && it.status != TaskStatus.SELESAI }
        val dueToday = open
            .filter { it.dueAtMillis < endOfToday }
            .sortedWith(compareBy<TaskEntity>({ it.dueAtMillis < startOfToday }, { it.dueAtMillis }))
        val later = open
            .filter { it.dueAtMillis >= endOfToday }
            .sortedBy { it.dueAtMillis }
            .take(3)
        dueToday to later
    }

    val upcomingAgenda = remember(schedules, academicEvents, semester) {
        val now = System.currentTimeMillis()
        buildList {
            schedules.asSequence()
                .filter { it.semester == semester && !it.isCompleted }
                .forEach { schedule ->
                    val timestamp = runCatching {
                        LocalDate.parse(schedule.scheduledDate)
                            .atTime(LocalTime.parse(schedule.startTime))
                            .atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                    }.getOrNull()
                    if (timestamp != null && timestamp > now) {
                        add(
                            DashboardAgendaEntry(
                                key = "schedule_${schedule.id}",
                                title = schedule.courseName,
                                subtitle = schedule.room.ifBlank { "Ruang TBA" },
                                timestamp = timestamp,
                                schedule = schedule
                            )
                        )
                    }
                }
            academicEvents.asSequence()
                .filter { it.semester == semester && it.scheduledAtMillis > now }
                .forEach { event ->
                    add(
                        DashboardAgendaEntry(
                            key = "event_${event.id}",
                            title = event.title,
                            subtitle = event.type,
                            timestamp = event.scheduledAtMillis
                        )
                    )
                }
        }.sortedBy { it.timestamp }.take(5)
    }

    val timeGreeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..10 -> "Selamat pagi"
            in 11..14 -> "Selamat siang"
            in 15..18 -> "Selamat sore"
            else -> "Selamat malam"
        }
    }

    val firstName = user.fullName.trim().substringBefore(' ').ifBlank { "Mahasiswa" }
    var todaySegmentIndex by rememberSaveable { mutableIntStateOf(0) }

    // Potongan konten yang dipakai bersama layout 1 kolom dan 2 kolom.
    val header: @Composable () -> Unit = {
        DashboardHeader(
            timeGreeting = timeGreeting,
            firstName = firstName,
            onOpenProfile = onOpenProfile,
            onOpenNotifications = onOpenNotifications,
            unreadCount = unreadNotifications
        )
    }
    val gpaCard: @Composable () -> Unit = {
        GpaHeroCard(
            cumulativeGpa = gpaSummary.cumulativeGpa,
            totalCredits = gpaSummary.totalCredits,
            targetCredits = targetCredits,
            semesterGpa = semesterGpa,
            semesterGradedCredits = semesterGradedCredits,
            semester = semester,
            courseCount = currentCourses.size,
            onOpenAcademic = onOpenAcademic,
            onAddCourse = { viewModel.openDialog(DialogType.AddCourse) }
        )
    }
    val todaySection: @Composable () -> Unit = {
        TodaySection(
            selectedSegment = todaySegmentIndex,
            onSelectSegment = { todaySegmentIndex = it },
            todaySchedules = todaySchedules,
            todayTasks = todayTasks,
            upcomingAgenda = upcomingAgenda,
            allCourses = allCourses,
            onOpenScheduleTab = onOpenScheduleTab,
            onOpenTasksTab = onOpenTasksTab,
            onAddSchedule = { viewModel.openDialog(DialogType.AddSchedule) },
            onAddTask = { viewModel.openDialog(DialogType.AddTask) },
            onEditSchedule = { viewModel.openDialog(DialogType.EditSchedule(it)) },
            onEditTask = { task -> viewModel.openDialog(DialogType.EditTask(task)) }
        )
    }
    val deadlineSection: @Composable () -> Unit = {
        NearestDeadlineSection(
            tasks = nearestDeadlines,
            allCourses = allCourses,
            onOpenTasksTab = onOpenTasksTab,
            onEditTask = { task -> viewModel.openDialog(DialogType.EditTask(task)) }
        )
    }


    Box(modifier = modifier.fillMaxSize()) {
        val isWide = rememberTkTwoPane()

        if (isWide) {
            // Layar lebar (tablet/foldable): kiri = header + kartu akademik; kanan = hari ini + deadline
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = TkSpace.page, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item(key = "header") {
                        Box(Modifier.staggeredEntrance(0)) { header() }
                    }
                    item(key = "gpa_hero") {
                        Box(Modifier.staggeredEntrance(1)) { gpaCard() }
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item(key = "today") {
                        Box(Modifier.staggeredEntrance(2)) { todaySection() }
                    }
                    item(key = "deadline") {
                        Box(Modifier.staggeredEntrance(3)) { deadlineSection() }
                    }
                }
            }
        } else {
            // Ponsel: satu kolom dengan animasi staggered entrance bertahap
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = 8.dp, bottom = 28.dp)
            ) {
                item(key = "header") {
                    Box(Modifier.staggeredEntrance(0)) { header() }
                }
                item(key = "gpa_hero") {
                    Box(Modifier.padding(top = 12.dp, bottom = 8.dp).staggeredEntrance(1)) { gpaCard() }
                }
                item(key = "today") {
                    Box(Modifier.padding(bottom = 7.dp).staggeredEntrance(2)) { todaySection() }
                }
                item(key = "deadline") {
                    Box(Modifier.staggeredEntrance(3)) { deadlineSection() }
                }
            }
        }
    }
}

internal data class DashboardAgendaEntry(
    val key: String,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val schedule: ScheduleEntity? = null
)
