package com.temankuliah.app.ui.tasks

import com.temankuliah.app.domain.TaskStatus

import androidx.compose.runtime.Composable
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.ui.components.TkPillChipRow

/**
 * Menyaring dan mengurutkan tugas: pencarian teks, filter cepat (Hari Ini/Mendatang/Terlambat),
 * lalu filter lanjutan (status, prioritas, mata kuliah). Tugas selesai diletakkan di bawah.
 */
internal fun filterTasks(
    tasks: List<TaskEntity>,
    courses: List<CourseEntity>,
    query: String,
    filter: String,
    statusFilter: String,
    priorityFilter: String,
    courseFilterId: Long?,
    now: Long,
    dayStart: Long,
    tomorrow: Long
): List<TaskEntity> {
    return tasks.filter { task ->
        val linkedCourse = courses.firstOrNull { it.id == task.courseId }?.name.orEmpty()
        val matchesQuery = query.isBlank() ||
                task.title.contains(query, ignoreCase = true) ||
                task.description.contains(query, ignoreCase = true) ||
                linkedCourse.contains(query, ignoreCase = true)
        val matchesFilter = when (filter) {
            "Hari Ini" -> task.dueAtMillis in dayStart until tomorrow && task.status != TaskStatus.SELESAI
            "Mendatang" -> task.dueAtMillis >= tomorrow && task.status != TaskStatus.SELESAI
            "Terlambat" -> task.dueAtMillis < now && task.status != TaskStatus.SELESAI
            else -> true
        }
        val matchesStatus = when (statusFilter) {
            "Belum Dikerjakan" -> task.status == TaskStatus.BELUM
            "Sedang Dikerjakan" -> task.status == "Sedang Dikerjakan"
            "Selesai" -> task.status == TaskStatus.SELESAI
            else -> true
        }
        val matchesPriority = priorityFilter == "Semua Prioritas" || task.priority == priorityFilter
        val matchesCourse = when (courseFilterId) {
            null -> true
            -1L -> task.courseId == null
            else -> task.courseId == courseFilterId
        }
        matchesQuery && matchesFilter && matchesStatus && matchesPriority && matchesCourse
    }.sortedWith(compareBy<TaskEntity> { it.status == TaskStatus.SELESAI }.thenBy { it.dueAtMillis })
}

/** Baris chip filter cepat: Semua, Terlambat, Hari ini, Mendatang (kunci internal tetap "Hari Ini"). */
@Composable
internal fun TaskQuickFilterRow(current: String, onSelect: (String) -> Unit) {
    TkPillChipRow(
        options = listOf("Semua", "Terlambat", "Hari Ini", "Mendatang"),
        selected = current,
        onSelect = onSelect,
        label = { if (it == "Hari Ini") "Hari ini" else it }
    )
}
