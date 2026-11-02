@file:Suppress("DEPRECATION")

package com.temankuliah.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.items
import com.temankuliah.app.ui.components.MotionTokens
import com.temankuliah.app.ui.components.rememberReducedMotion
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.domain.TaskStatus
import com.temankuliah.app.ui.DialogType
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.components.TkDashedEmptyCard
import com.temankuliah.app.ui.components.TkHeroStat
import com.temankuliah.app.ui.components.TkHeroStatsCard
import com.temankuliah.app.ui.components.TkIconSwitch
import com.temankuliah.app.ui.components.TkListHeading
import com.temankuliah.app.ui.components.TkSearchField
import com.temankuliah.app.ui.components.TkSwitchOption
import com.temankuliah.app.ui.components.TkTitleActionHeader
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.projects.ProjectManagementScreen
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkSpace
import com.temankuliah.app.ui.theme.rememberTkTwoPane
import java.util.Calendar

@Composable
fun TasksScreen(
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier,
    onShowProjectsChange: (Boolean) -> Unit = {}
) {
    var selectedContent by remember { mutableStateOf("Tugas") }

    androidx.activity.compose.BackHandler(enabled = selectedContent == "Proyek Kuliah") {
        selectedContent = "Tugas"
        onShowProjectsChange(false)
    }

    val switcher: @Composable () -> Unit = {
        TkIconSwitch(
            options = listOf(
                TkSwitchOption("Tugas", "Tugas", TkOutlineIcons.Clipboard),
                TkSwitchOption("Proyek Kuliah", "Proyek Kuliah", TkOutlineIcons.Kanban)
            ),
            selectedKey = selectedContent,
            onSelect = { tabName ->
                selectedContent = tabName
                onShowProjectsChange(tabName == "Proyek Kuliah")
            }
        )
    }

    val reducedMotion = rememberReducedMotion()
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = selectedContent,
            transitionSpec = {
                // Crossfade murni: layar penuh (termasuk pengalih Tugas/Proyek) tidak digeser,
                // jadi tidak ada konten yang bergoyang saat berpindah.
                (fadeIn(animationSpec = tween(durationMillis = if (reducedMotion) 100 else 180, delayMillis = if (reducedMotion) 0 else 60, easing = MotionTokens.EasingDecelerate)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = if (reducedMotion) 100 else 120, easing = MotionTokens.EasingAccelerate)))
                    .using(SizeTransform(clip = false))
            },
            label = "tasksContentTransition",
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { content ->
            if (content == "Tugas") {
                TasksListScreen(viewModel, Modifier.fillMaxSize(), switcher)
            } else {
                ProjectManagementScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    switcher = switcher
                )
            }
        }
    }
}

@Composable
private fun TasksListScreen(
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier,
    switcher: @Composable () -> Unit = {}
) {
    val tasks by viewModel.taskController.tasks.collectAsStateWithLifecycle()
    val courses by viewModel.courseController.courses.collectAsStateWithLifecycle()
    val projects by viewModel.projectController.projects.collectAsStateWithLifecycle()
    val semester by viewModel.activeSemester.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var advancedStatusFilter by remember { mutableStateOf("Semua Status") }
    var advancedPriorityFilter by remember { mutableStateOf("Semua Prioritas") }
    var advancedCourseFilterId by remember { mutableStateOf<Long?>(null) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showDeleteTask by remember { mutableStateOf<TaskEntity?>(null) }
    val advancedFilterActive = advancedStatusFilter != "Semua Status" ||
        advancedPriorityFilter != "Semua Prioritas" || advancedCourseFilterId != null

    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val dayStart = calendar.timeInMillis
    val tomorrow = dayStart + 86_400_000L

    val semTasks = tasks.filter { it.semester == semester }
    val isDataEmpty = semTasks.isEmpty()

    val overdueCount = semTasks.count { it.status != TaskStatus.SELESAI && it.dueAtMillis < now }
    val todayCount = semTasks.count { it.status != TaskStatus.SELESAI && it.dueAtMillis in dayStart until tomorrow }
    val upcomingCount = semTasks.count { it.status != TaskStatus.SELESAI && it.dueAtMillis >= tomorrow }
    val activeCount = semTasks.count { it.status != TaskStatus.SELESAI }

    val shownTasks = filterTasks(
        tasks = semTasks,
        courses = courses,
        query = query,
        filter = filter,
        statusFilter = advancedStatusFilter,
        priorityFilter = advancedPriorityFilter,
        courseFilterId = advancedCourseFilterId,
        now = now,
        dayStart = dayStart,
        tomorrow = tomorrow
    )

    val controlsBlock: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TkTitleActionHeader(
                title = "Tugas",
                supporting = "Semester $semester · $activeCount tugas aktif",
                onAdd = { viewModel.openDialog(DialogType.AddTask) },
                addTag = "fab_add_task",
                addDescription = "Tambah tugas"
            )

            switcher()

            TkHeroStatsCard(
                stats = listOf(
                    TkHeroStat("Terlambat", overdueCount, key = "Terlambat"),
                    TkHeroStat("Hari ini", todayCount, key = "Hari Ini"),
                    TkHeroStat("Mendatang", upcomingCount, key = "Mendatang")
                ),
                selectedKey = filter.takeIf { it != "Semua" },
                onSelect = { selected -> filter = if (filter == selected) "Semua" else selected }
            )

            TkSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Cari tugas",
                onFilterClick = { showFilterSheet = true },
                filterActive = advancedFilterActive,
                filterDescription = "Filter tugas",
                testTag = "task_search_field"
            )

            TaskQuickFilterRow(current = filter, onSelect = { filter = it })
        }
    }

    val taskListItems: LazyListScope.() -> Unit = {
        item(key = "list_heading") {
            TkListHeading(
                title = "Daftar tugas",
                countText = "${shownTasks.size} tugas"
            )
        }

        if (shownTasks.isEmpty()) {
            item(key = "empty_state") {
                TkDashedEmptyCard(
                    icon = TkOutlineIcons.Clipboard,
                    title = if (isDataEmpty) "Belum ada tugas" else "Tidak ada tugas pada filter ini",
                    message = if (isDataEmpty) "Tugas yang kamu buat akan tampil di sini. Ketuk Tambah di kanan atas untuk memulai."
                    else "Coba pilih filter lain atau hapus kata kunci pencarian."
                )
            }
        } else {
            val grouped = shownTasks.groupBy { task ->
                when {
                    task.status == TaskStatus.SELESAI -> TaskGroup.DONE
                    task.dueAtMillis < now -> TaskGroup.OVERDUE
                    task.dueAtMillis < tomorrow -> TaskGroup.TODAY
                    else -> TaskGroup.UPCOMING
                }
            }
            TaskGroup.values().forEach { group ->
                val groupTasks = grouped[group].orEmpty()
                if (groupTasks.isNotEmpty()) {
                    item(key = "group_${group.name}") { TaskGroupHeader(group) }
                    items(groupTasks, key = { it.id }) { task ->
                        Box(
                            modifier = Modifier.animateItem()
                        ) {
                            TaskCard(
                                task = task,
                                courses = courses,
                                projects = projects,
                                now = now,
                                dayStart = dayStart,
                                tomorrow = tomorrow,
                                onStatusChange = { status -> viewModel.taskController.updateTaskStatus(task, status) },
                                onEdit = { editingTask = task },
                                onDelete = { showDeleteTask = task }
                            )
                        }
                    }
                }
            }
        }
    }


    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWide = rememberTkTwoPane()
        if (isWide) {
            // Layar lebar: kontrol di kiri, daftar tugas di kanan.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = TkSpace.page, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    controlsBlock()
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    taskListItems()
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = TkSpace.page, top = 8.dp, end = TkSpace.page, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "controls") {
                    controlsBlock()
                }
                taskListItems()
            }
        }
    }

    if (editingTask != null) AddTaskDialog(
        onDismiss = { editingTask = null },
        initialTask = editingTask,
        courses = courses,
        semester = editingTask?.semester ?: semester,
        onConfirm = { title, desc, due, priority, courseId ->
            editingTask?.let { target -> viewModel.taskController.updateTask(target, title, desc, due, priority, courseId) }
            editingTask = null
        }
    )
    showDeleteTask?.let { task ->
        com.temankuliah.app.ui.components.TkAlertDialog(
            onDismissRequest = { showDeleteTask = null },
            title = "Hapus tugas?",
            text = "Tugas ‘${task.title}’ akan dihapus.",
            confirmLabel = "Hapus",
            isDestructive = true,
            onConfirm = {
                viewModel.taskController.deleteTask(task)
                showDeleteTask = null
            }
        )
    }
    if (showFilterSheet) {
        TaskFilterBottomSheet(
            courses = courses.filter { it.semester == semester }.sortedBy { it.name },
            initialStatus = advancedStatusFilter,
            initialPriority = advancedPriorityFilter,
            initialCourseId = advancedCourseFilterId,
            onDismiss = { showFilterSheet = false },
            onApply = { status, priority, courseId ->
                advancedStatusFilter = status
                advancedPriorityFilter = priority
                advancedCourseFilterId = courseId
                showFilterSheet = false
            },
            onReset = {
                advancedStatusFilter = "Semua Status"
                advancedPriorityFilter = "Semua Prioritas"
                advancedCourseFilterId = null
                filter = "Semua"
                query = ""
                showFilterSheet = false
            }
        )
    }
}

/** Kelompok tugas pada daftar: Terlambat, Hari ini, Mendatang, lalu Selesai. */
private enum class TaskGroup(val title: String) {
    OVERDUE("Terlambat"),
    TODAY("Hari ini"),
    UPCOMING("Mendatang"),
    DONE("Selesai")
}

@Composable
private fun TaskGroupHeader(group: TaskGroup) {
    val color = when (group) {
        TaskGroup.OVERDUE -> TemanKuliahOrangeDark
        TaskGroup.TODAY -> TemanKuliahBlue
        TaskGroup.UPCOMING -> TextSecondary
        TaskGroup.DONE -> TemanKuliahGreenDark
    }
    Row(
        modifier = Modifier.padding(start = 2.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(TkOutlineIcons.Flag, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = group.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
