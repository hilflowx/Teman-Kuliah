package com.temankuliah.app.ui.projects

import com.temankuliah.app.domain.rethrowIfCancellation
import com.temankuliah.app.data.local.ProjectEntity
import com.temankuliah.app.data.local.ProjectMilestoneEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.ui.FeatureContext
import com.temankuliah.app.ui.FeatureController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class ProjectController(
    ctx: FeatureContext
) : FeatureController(ctx) {
    val projects: StateFlow<List<ProjectEntity>> = currentUser
        .flatMapLatest { user -> if (user != null) repository.getProjectsFlow(user.id) else flowOf(emptyList()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectMilestones: StateFlow<List<ProjectMilestoneEntity>> = currentUser
        .flatMapLatest { user -> if (user != null) repository.getProjectMilestonesFlow(user.id) else flowOf(emptyList()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Project Management ---
    fun addProject(title: String, description: String, deadlineAtMillis: Long, progress: Int) {
        val user = currentUser.value ?: return
        scope.launch {
            try {
                repository.addProject(ProjectEntity(userId = user.id, semester = user.activeSemester, title = title.trim(), description = description.trim(), deadlineAtMillis = deadlineAtMillis, progress = progress))
                snackbar.value = "Proyek kuliah berhasil ditambahkan."
            } catch (error: Exception) { rethrowIfCancellation(error); snackbar.value = error.message ?: "Gagal menambahkan proyek." }
        }
    }

    fun updateProject(project: ProjectEntity, title: String, description: String, deadlineAtMillis: Long, progress: Int) {
        scope.launch {
            try {
                repository.updateProject(project.copy(title = title.trim(), description = description.trim(), deadlineAtMillis = deadlineAtMillis, progress = progress))
                snackbar.value = "Proyek kuliah berhasil diperbarui."
            } catch (error: Exception) { rethrowIfCancellation(error); snackbar.value = error.message ?: "Gagal memperbarui proyek." }
        }
    }

    fun deleteProject(project: ProjectEntity) {
        scope.launch {
            try {
                repository.deleteProject(project)
                snackbar.value = "Proyek dihapus. Tugas terkait tetap disimpan tanpa relasi proyek."
            } catch (error: Exception) { rethrowIfCancellation(error); snackbar.value = error.message ?: "Gagal menghapus proyek." }
        }
    }

    fun addProjectMilestone(project: ProjectEntity, title: String, deadlineAtMillis: Long) {
        val user = currentUser.value ?: return
        scope.launch {
            try {
                repository.addProjectMilestone(ProjectMilestoneEntity(userId = user.id, projectId = project.id, title = title.trim(), deadlineAtMillis = deadlineAtMillis))
                snackbar.value = "Tahapan proyek ditambahkan."
            } catch (error: Exception) { rethrowIfCancellation(error); snackbar.value = error.message ?: "Gagal menambahkan tahapan." }
        }
    }

    fun updateProjectMilestone(milestone: ProjectMilestoneEntity, title: String, deadlineAtMillis: Long, isCompleted: Boolean) {
        scope.launch {
            try {
                repository.updateProjectMilestone(milestone.copy(title = title.trim(), deadlineAtMillis = deadlineAtMillis, isCompleted = isCompleted))
                snackbar.value = "Tahapan proyek diperbarui."
            } catch (error: Exception) { rethrowIfCancellation(error); snackbar.value = error.message ?: "Gagal memperbarui tahapan." }
        }
    }

    fun deleteProjectMilestone(milestone: ProjectMilestoneEntity) {
        scope.launch {
            runCatching { repository.deleteProjectMilestone(milestone) }
                .onFailure { snackbar.value = it.message ?: "Gagal menghapus tahapan." }
        }
    }

    fun setTaskProject(task: TaskEntity, projectId: Long?) {
        val activeUserId = currentUser.value?.id ?: return
        if (task.userId != activeUserId) return
        scope.launch {
            runCatching { repository.setTaskProject(activeUserId, task, projectId) }
                .onFailure { snackbar.value = it.message ?: "Gagal menghubungkan tugas ke proyek." }
        }
    }
}
