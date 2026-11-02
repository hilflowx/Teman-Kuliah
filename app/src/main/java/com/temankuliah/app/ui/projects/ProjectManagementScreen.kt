package com.temankuliah.app.ui.projects

import com.temankuliah.app.ui.theme.TemanKuliahRedFill
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.data.local.ProjectEntity
import com.temankuliah.app.domain.TaskStatus
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.TkDashedEmptyCard
import com.temankuliah.app.ui.components.TkHeroStat
import com.temankuliah.app.ui.components.TkHeroStatsCard
import com.temankuliah.app.ui.components.TkListHeading
import com.temankuliah.app.ui.components.TkPillChipRow
import com.temankuliah.app.ui.components.TkSearchField
import com.temankuliah.app.ui.components.TkTitleActionHeader
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TkSpace
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ProjectManagementScreen(
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier,
    switcher: (@Composable () -> Unit)? = null
) {
    val projects by viewModel.projectController.projects.collectAsStateWithLifecycle()
    val milestones by viewModel.projectController.projectMilestones.collectAsStateWithLifecycle()
    val tasks by viewModel.taskController.tasks.collectAsStateWithLifecycle()
    val courses by viewModel.courseController.courses.collectAsStateWithLifecycle()
    val semester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val semesterProjects = projects.filter { it.semester == semester }
    val isDataEmpty = semesterProjects.isEmpty()

    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var showProjectEditor by remember { mutableStateOf(false) }
    var milestonesProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var linkingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var deleteProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var advancedProgressFilter by remember { mutableStateOf("Semua Status") }
    var advancedDeadlineFilter by remember { mutableStateOf("Semua Tenggat") }
    var advancedCourseFilterId by remember { mutableStateOf<Long?>(null) }
    val advancedFilterActive = advancedProgressFilter != "Semua Status" ||
        advancedDeadlineFilter != "Semua Tenggat" || advancedCourseFilterId != null
    val today = LocalDate.now()
    val startOfToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val startOfSevenDays = today.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val totalCount = semesterProjects.size
    val inProgressCount = semesterProjects.count { it.progress in 1..99 }
    val completedCount = semesterProjects.count { it.progress == 100 }

    val filteredProjects = semesterProjects.filter { project ->
        val matchesQuery = query.isBlank() ||
            project.title.contains(query, ignoreCase = true) ||
            project.description.contains(query, ignoreCase = true)
        val matchesFilter = when (filter) {
            "Berjalan" -> project.progress in 1..99
            "Belum Dimulai" -> project.progress == 0
            "Selesai" -> project.progress == 100
            else -> true
        }
        val matchesAdvancedStatus = when (advancedProgressFilter) {
            "Berjalan" -> project.progress in 1..99
            "Belum Dimulai" -> project.progress == 0
            "Selesai" -> project.progress == 100
            else -> true
        }
        val matchesDeadline = when (advancedDeadlineFilter) {
            "Tenggat Terlewat" -> project.deadlineAtMillis < startOfToday && project.progress < 100
            "7 Hari ke Depan" -> project.deadlineAtMillis in startOfToday until startOfSevenDays && project.progress < 100
            else -> true
        }
        val relatedCourseIds = tasks.asSequence()
            .filter { it.semester == semester && it.projectId == project.id }
            .mapNotNull { it.courseId }.toSet()
        val matchesCourse = when (advancedCourseFilterId) {
            null -> true
            -1L -> relatedCourseIds.isEmpty()
            else -> advancedCourseFilterId in relatedCourseIds
        }
        matchesQuery && matchesFilter && matchesAdvancedStatus && matchesDeadline && matchesCourse
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = TkSpace.page, top = 8.dp, end = TkSpace.page, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            TkTitleActionHeader(
                title = "Proyek kuliah",
                supporting = "Semester $semester · $inProgressCount proyek berjalan",
                onAdd = { editingProject = null; showProjectEditor = true },
                addTag = "btn_add_project",
                addDescription = "Tambah proyek kuliah"
            )
        }

        if (switcher != null) item(key = "switcher", span = { GridItemSpan(maxLineSpan) }) { switcher() }

        item(key = "stats", span = { GridItemSpan(maxLineSpan) }) {
            TkHeroStatsCard(
                stats = listOf(
                    TkHeroStat("Total", totalCount),
                    TkHeroStat("Berjalan", inProgressCount),
                    TkHeroStat("Selesai", completedCount)
                )
            )
        }

        item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
            TkSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Cari proyek kuliah",
                onFilterClick = { showFilterSheet = true },
                filterActive = advancedFilterActive,
                filterDescription = "Filter proyek",
                testTag = "project_search_field"
            )
        }

        item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
            TkPillChipRow(
                options = listOf("Semua", "Berjalan", "Belum Dimulai", "Selesai"),
                selected = filter,
                onSelect = { filter = it },
                label = { if (it == "Belum Dimulai") "Belum dimulai" else it }
            )
        }

        item(key = "heading", span = { GridItemSpan(maxLineSpan) }) { TkListHeading(title = "Daftar proyek", countText = "${filteredProjects.size} proyek") }

        if (filteredProjects.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                TkDashedEmptyCard(
                    icon = TkOutlineIcons.Kanban,
                    title = if (isDataEmpty) "Belum ada proyek" else "Tidak ada proyek pada filter ini",
                    message = if (isDataEmpty) "Tambahkan proyek pertamamu lewat tombol Tambah di kanan atas."
                    else "Coba ubah kata kunci pencarian atau pilih filter lain."
                )
            }
        } else {
            items(filteredProjects, key = { it.id }) { project ->
                // Label mata kuliah diambil dari tugas yang tertaut ke proyek; tanpa tautan, tampil label umum.
                val courseTag = remember(project.id, tasks, courses) {
                    tasks.filter { it.projectId == project.id && it.courseId != null }
                        .mapNotNull { linked -> courses.firstOrNull { it.id == linked.courseId }?.name }
                        .distinct()
                        .joinToString(" · ")
                        .ifEmpty { "Proyek kuliah" }
                }
                val linkedTasks = tasks.filter { it.projectId == project.id }

                androidx.compose.foundation.layout.Box(modifier = Modifier.animateItem()) {
                    ProjectCard(
                        project = project,
                        courseTag = courseTag,
                        milestones = milestones.filter { it.projectId == project.id },
                        relatedTaskCount = linkedTasks.size,
                        doneTaskCount = linkedTasks.count { it.status == TaskStatus.SELESAI },
                        onEdit = { editingProject = project; showProjectEditor = true },
                        onDelete = { deleteProject = project },
                        onMilestones = { milestonesProject = project },
                        onLinkTasks = { linkingProject = project }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        ProjectFilterBottomSheet(
            courses = courses.filter { it.semester == semester }.sortedBy { it.name },
            initialStatus = advancedProgressFilter,
            initialDeadline = advancedDeadlineFilter,
            initialCourseId = advancedCourseFilterId,
            onDismiss = { showFilterSheet = false },
            onApply = { status, deadline, courseId ->
                advancedProgressFilter = status
                advancedDeadlineFilter = deadline
                advancedCourseFilterId = courseId
                showFilterSheet = false
            },
            onReset = {
                advancedProgressFilter = "Semua Status"
                advancedDeadlineFilter = "Semua Tenggat"
                advancedCourseFilterId = null
                filter = "Semua"
                query = ""
                showFilterSheet = false
            }
        )
    }

    if (showProjectEditor) ProjectEditorDialog(
        initial = editingProject,
        defaultSemester = semester,
        onDismiss = { showProjectEditor = false; editingProject = null },
        onSave = { title, description, deadline, progress ->
            if (editingProject == null) viewModel.projectController.addProject(title, description, deadline, progress)
            else viewModel.projectController.updateProject(editingProject!!, title, description, deadline, progress)
            showProjectEditor = false
            editingProject = null
        }
    )
    milestonesProject?.let { project ->
        ProjectMilestonesDialog(
            project,
            milestones.filter { it.projectId == project.id },
            onDismiss = { milestonesProject = null },
            onAdd = { title, deadline -> viewModel.projectController.addProjectMilestone(project, title, deadline) },
            onToggle = { item, checked -> viewModel.projectController.updateProjectMilestone(item, item.title, item.deadlineAtMillis, checked) },
            onDelete = viewModel.projectController::deleteProjectMilestone
        )
    }
    linkingProject?.let { project ->
        LinkProjectTasksDialog(
            project,
            tasks.filter { it.semester == semester },
            onDismiss = { linkingProject = null },
            onSave = { task, linked -> viewModel.projectController.setTaskProject(task, if (linked) project.id else null) }
        )
    }
    deleteProject?.let { project ->
        AlertDialog(
            onDismissRequest = { deleteProject = null },
            title = { Text("Hapus proyek?") },
            text = { Text("${project.title} akan dihapus. Tugasnya tetap tersimpan dan tidak akan ikut terhapus.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.projectController.deleteProject(project); deleteProject = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahRedFill)
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deleteProject = null }) { Text("Batal") } }
        )
    }
}
