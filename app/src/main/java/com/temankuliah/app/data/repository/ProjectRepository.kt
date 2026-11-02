package com.temankuliah.app.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.temankuliah.app.data.local.ProjectDao
import com.temankuliah.app.data.local.ProjectEntity
import com.temankuliah.app.data.local.ProjectMilestoneDao
import com.temankuliah.app.data.local.ProjectMilestoneEntity
import com.temankuliah.app.data.local.TaskDao
import com.temankuliah.app.data.local.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Proyek, tahapan proyek, dan tautan tugas ke proyek. */
class ProjectRepository(
    private val database: RoomDatabase,
    private val projectDao: ProjectDao,
    private val projectMilestoneDao: ProjectMilestoneDao,
    private val taskDao: TaskDao
) {
    // Proyek kuliah
    fun getProjectsFlow(userId: Long): Flow<List<ProjectEntity>> = projectDao.observeByUser(userId)

    suspend fun addProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        require(project.title.isNotBlank()) { "Nama proyek wajib diisi." }
        require(project.deadlineAtMillis > 0L) { "Tenggat proyek tidak valid." }
        require(project.progress in 0..100) { "Progres harus antara 0 dan 100%." }
        projectDao.insert(project)
    }

    suspend fun updateProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        require(project.title.isNotBlank()) { "Nama proyek wajib diisi." }
        require(project.deadlineAtMillis > 0L) { "Tenggat proyek tidak valid." }
        require(project.progress in 0..100) { "Progres harus antara 0 dan 100%." }
        projectDao.update(project)
    }

    /** Atomic: either milestones, task links, and the project all go, or nothing changes. */
    suspend fun deleteProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        database.withTransaction {
            projectMilestoneDao.getByProject(project.id).forEach { projectMilestoneDao.delete(it) }
            taskDao.clearProjectLinks(project.id)
            projectDao.delete(project)
        }
    }

    /** Links a task to a project (or unlinks with null) after checking both belong to [userId]. */
    suspend fun setTaskProject(userId: Long, task: TaskEntity, projectId: Long?) = withContext(Dispatchers.IO) {
        require(task.userId == userId) { "Tugas bukan milik akun aktif." }
        if (projectId != null) {
            val project = projectDao.getAllOnce().firstOrNull { it.id == projectId }
            require(project != null && project.userId == userId) { "Proyek tidak ditemukan." }
        }
        taskDao.setTaskProject(task.id, projectId)
    }

    // Tahapan proyek
    fun getProjectMilestonesFlow(userId: Long): Flow<List<ProjectMilestoneEntity>> = projectMilestoneDao.observeByUser(userId)

    suspend fun addProjectMilestone(milestone: ProjectMilestoneEntity) = withContext(Dispatchers.IO) {
        require(milestone.title.isNotBlank()) { "Nama tahapan wajib diisi." }
        require(milestone.deadlineAtMillis > 0L) { "Tenggat tahapan tidak valid." }
        projectMilestoneDao.insert(milestone)
    }

    suspend fun updateProjectMilestone(milestone: ProjectMilestoneEntity) = withContext(Dispatchers.IO) {
        require(milestone.title.isNotBlank()) { "Nama tahapan wajib diisi." }
        require(milestone.deadlineAtMillis > 0L) { "Tenggat tahapan tidak valid." }
        projectMilestoneDao.update(milestone)
    }

    suspend fun deleteProjectMilestone(milestone: ProjectMilestoneEntity) = withContext(Dispatchers.IO) {
        projectMilestoneDao.delete(milestone)
    }
}
