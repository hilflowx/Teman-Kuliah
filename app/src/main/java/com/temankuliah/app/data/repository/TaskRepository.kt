package com.temankuliah.app.data.repository

import com.temankuliah.app.domain.TaskStatus

import com.temankuliah.app.data.local.CourseDao
import com.temankuliah.app.data.local.TaskDao
import com.temankuliah.app.data.local.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Tugas kuliah. */
class TaskRepository(private val taskDao: TaskDao, private val courseDao: CourseDao? = null) {
    fun getTasksFlow(userId: Long): Flow<List<TaskEntity>> = taskDao.observeByUser(userId)

    suspend fun addTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        validateTask(task)
        requireCourseOwnedByUser(task)
        taskDao.insert(task.copy(title = task.title.trim(), description = task.description.trim()))
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        validateTask(task)
        // Hanya diperiksa bila tautan berubah, supaya tugas lama dengan courseId yatim tetap bisa diubah statusnya.
        if (task.courseId != taskDao.getById(task.id)?.courseId) requireCourseOwnedByUser(task)
        taskDao.update(task.copy(title = task.title.trim(), description = task.description.trim()))
    }

    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) { taskDao.delete(task) }

    /** Tanpa foreign key, relasi tugas -> mata kuliah dijaga di sini: id harus milik pengguna yang sama. */
    private suspend fun requireCourseOwnedByUser(task: TaskEntity) {
        val courseId = task.courseId ?: return
        val dao = courseDao ?: return
        require(dao.getCourseById(courseId)?.userId == task.userId) { "Mata kuliah tugas tidak ditemukan." }
    }

    private fun validateTask(task: TaskEntity) {
        require(task.userId > 0) { "Akun pengguna tidak valid." }
        require(task.title.isNotBlank()) { "Judul tugas wajib diisi." }
        require(task.semester >= 1) { "Semester minimal 1." }
        require(task.priority in setOf("Rendah", "Sedang", "Tinggi")) { "Prioritas tugas tidak valid." }
        require(task.status in setOf(TaskStatus.BELUM, TaskStatus.SEDANG, TaskStatus.SELESAI)) { "Status tugas tidak valid." }
    }
}
