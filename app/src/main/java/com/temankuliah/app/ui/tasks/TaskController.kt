package com.temankuliah.app.ui.tasks

import com.temankuliah.app.domain.rethrowIfCancellation
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.ui.FeatureContext
import com.temankuliah.app.ui.FeatureController
import com.temankuliah.app.ui.profile.ReminderPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class TaskController(
    ctx: FeatureContext,
    private val reminderPreferences: StateFlow<ReminderPreferences>
) : FeatureController(ctx) {
    val tasks: StateFlow<List<TaskEntity>> = currentUser
        .flatMapLatest { user -> if (user != null) repository.getTasksFlow(user.id) else flowOf(emptyList()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Task Actions ---
    fun addTask(title: String, description: String, dueAtMillis: Long, priority: String, courseId: Long? = null) {
        val user = currentUser.value ?: return
        scope.launch {
            try {
                val task = TaskEntity(userId = user.id, semester = user.activeSemester, title = title, description = description, dueAtMillis = dueAtMillis, priority = priority, courseId = courseId)
                val id = repository.addTask(task)
                ReminderScheduler.scheduleTask(application, task.copy(id = id), reminderPreferences.value.reminderMinutes, notifyIfWindowOpen = true)
                closeDialog()
                snackbar.value = "Tugas berhasil ditambahkan."
            } catch (error: Exception) { rethrowIfCancellation(error); snackbar.value = error.message ?: "Gagal menambahkan tugas." }
        }
    }

    /** [courseId] bawaan = tautan lama, jadi pemanggil yang tidak menyebutnya tidak ikut menghapus tautan. */
    fun updateTask(task: TaskEntity, title: String, description: String, dueAtMillis: Long, priority: String, courseId: Long? = task.courseId) {
        scope.launch {
            try {
                val updated = task.copy(title = title, description = description, dueAtMillis = dueAtMillis, priority = priority, courseId = courseId)
                repository.updateTask(updated)
                ReminderScheduler.scheduleTask(application, updated, reminderPreferences.value.reminderMinutes, notifyIfWindowOpen = task.dueAtMillis != dueAtMillis)
                closeDialog()
                snackbar.value = "Tugas berhasil diperbarui."
            } catch (error: Exception) { rethrowIfCancellation(error); snackbar.value = error.message ?: "Gagal memperbarui tugas." }
        }
    }

    fun updateTaskStatus(task: TaskEntity, status: String) {
        scope.launch {
            try {
                val updated = task.copy(status = status)
                repository.updateTask(updated)
                ReminderScheduler.scheduleTask(application, updated, reminderPreferences.value.reminderMinutes)
                snackbar.value = "Status tugas diperbarui."
            } catch (error: Exception) { rethrowIfCancellation(error); snackbar.value = error.message ?: "Gagal memperbarui status tugas." }
        }
    }

    fun deleteTask(task: TaskEntity) {
        scope.launch {
            try { repository.deleteTask(task); ReminderScheduler.cancelTask(application, task.id); snackbar.value = "Tugas dihapus." }
            catch (error: Exception) { rethrowIfCancellation(error); snackbar.value = error.message ?: "Gagal menghapus tugas." }
        }
    }
}
