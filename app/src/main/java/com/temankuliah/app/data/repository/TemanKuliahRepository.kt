package com.temankuliah.app.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.temankuliah.app.data.files.AcademicDocumentProcessor
import com.temankuliah.app.data.files.StagedAcademicDocument
import com.temankuliah.app.data.local.AcademicDocumentDao
import com.temankuliah.app.data.local.AcademicDocumentEntity
import com.temankuliah.app.data.local.AcademicEventDao
import com.temankuliah.app.data.local.AcademicEventEntity
import com.temankuliah.app.data.local.CourseDao
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.ProjectDao
import com.temankuliah.app.data.local.ProjectEntity
import com.temankuliah.app.data.local.ProjectMilestoneDao
import com.temankuliah.app.data.local.ProjectMilestoneEntity
import com.temankuliah.app.data.local.ScheduleDao
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.local.TaskDao
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.data.local.TemanKuliahDatabase
import com.temankuliah.app.data.local.UserDao
import com.temankuliah.app.data.local.UserEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Pintu masuk tunggal ke data. Logikanya sudah dipecah per fitur ([users], [documents], [courses],
 * [tasks], [events], [projects], [schedules]); kelas ini hanya merangkai dan meneruskan panggilan
 * lama supaya controller dan tes yang ada tetap jalan. Kode baru sebaiknya langsung memakai
 * repository fiturnya, mis. `repository.tasks.addTask(...)`.
 */
class TemanKuliahRepository(
    private val database: RoomDatabase,
    userDao: UserDao,
    courseDao: CourseDao,
    scheduleDao: ScheduleDao,
    academicDocumentDao: AcademicDocumentDao,
    taskDao: TaskDao,
    academicEventDao: AcademicEventDao,
    projectDao: ProjectDao,
    projectMilestoneDao: ProjectMilestoneDao
) {
    val users = UserRepository(userDao)
    val documents = AcademicDocumentRepository(academicDocumentDao)
    val courses = CourseRepository(database, courseDao, scheduleDao, taskDao)
    val tasks = TaskRepository(taskDao, courseDao)
    val events = AcademicEventRepository(academicEventDao)
    val projects = ProjectRepository(database, projectDao, projectMilestoneDao, taskDao)
    val schedules = ScheduleRepository(database, scheduleDao)

    // --- Akun ---
    val activeUserFlow: Flow<UserEntity?> get() = users.activeUserFlow
    suspend fun getActiveUser() = users.getActiveUser()
    suspend fun upgradeLegacyPasswords() = users.upgradeLegacyPasswords()
    suspend fun loginWithEmailAndPassword(email: String, password: String) =
        users.loginWithEmailAndPassword(email, password)
    suspend fun findAccountForPasswordReset(emailOrPhone: String) =
        users.findAccountForPasswordReset(emailOrPhone)
    suspend fun resetPasswordForAccount(
        emailOrPhone: String,
        newPassword: String,
        confirmPassword: String
    ) = users.resetPasswordForAccount(emailOrPhone, newPassword, confirmPassword)
    suspend fun createPasswordForAccount(emailOrPhone: String, newPassword: String, confirmPassword: String) =
        users.createPasswordForAccount(emailOrPhone, newPassword, confirmPassword)
    suspend fun registerUser(
        fullName: String, nim: String, studyProgram: String, faculty: String, university: String, semester: Int, email: String, password: String
    ) = users.registerUser(fullName, nim, studyProgram, faculty, university, semester, email, password)
    suspend fun signInAsGuest() = users.signInAsGuest()
    suspend fun logout() = users.logout()
    suspend fun updateProfile(
        userId: Long, fullName: String, nim: String, studyProgram: String, faculty: String, university: String
    ) = users.updateProfile(userId, fullName, nim, studyProgram, faculty, university)
    suspend fun updateProfileImage(userId: Long, relativePath: String?) = users.updateProfileImage(userId, relativePath)
    suspend fun updateActiveSemester(userId: Long, semester: Int) = users.updateActiveSemester(userId, semester)
    suspend fun updateSemesterCount(userId: Long, count: Int) = users.updateSemesterCount(userId, count)

    // --- Dokumen KRS/KHS ---
    fun getDocumentsFlow(userId: Long): Flow<List<AcademicDocumentEntity>> = documents.getDocumentsFlow(userId)
    suspend fun stageAcademicDocumentFile(
        context: Context, resolver: ContentResolver, userId: Long, semester: Int, type: String, uri: Uri, displayName: String
    ) = documents.stageAcademicDocumentFile(context, resolver, userId, semester, type, uri, displayName)
    suspend fun stageAcademicPhoto(
        context: Context, userId: Long, semester: Int, type: String, photo: File, displayName: String
    ) = documents.stageAcademicPhoto(context, userId, semester, type, photo, displayName)
    suspend fun commitStagedAcademicDocument(context: Context, staged: StagedAcademicDocument) =
        documents.commitStagedAcademicDocument(context, staged)
    suspend fun discardStagedAcademicDocument(context: Context, staged: StagedAcademicDocument) =
        documents.discardStagedAcademicDocument(context, staged)
    suspend fun reprocessAcademicDocument(context: Context, document: AcademicDocumentEntity) =
        documents.reprocessAcademicDocument(context, document)
    suspend fun deleteAcademicDocument(document: AcademicDocumentEntity) = documents.deleteAcademicDocument(document)

    // --- Mata kuliah ---
    suspend fun importCourses(
        userId: Long, semester: Int, rows: List<AcademicDocumentProcessor.AcademicRow>,
        replaceExisting: Boolean, isKrs: Boolean = false
    ) = courses.importCourses(userId, semester, rows, replaceExisting, isKrs)
    fun getCoursesFlow(userId: Long): Flow<List<CourseEntity>> = courses.getCoursesFlow(userId)
    suspend fun addCourse(course: CourseEntity) = courses.addCourse(course)
    suspend fun updateCourse(course: CourseEntity) = courses.updateCourse(course)

    /** Hasil [addCourseWithWeeklySchedules]: id mata kuliah baru dan baris jadwal yang ikut tersimpan. */
    data class CourseWithSchedules(val courseId: Long, val schedules: List<ScheduleEntity>)

    /**
     * Menyimpan mata kuliah dan jadwal mingguannya dalam SATU transaksi: keduanya tersimpan, atau bila
     * jadwal gagal (validasi/penyimpanan) mata kuliah ikut dibatalkan dan exception diteruskan.
     * Mengembalikan baris jadwal (dengan id) agar pemanggil dapat memasang alarm setelah commit.
     */
    suspend fun addCourseWithWeeklySchedules(
        course: CourseEntity, scheduleTemplate: ScheduleEntity, meetingCount: Int
    ): CourseWithSchedules = database.withTransaction {
        val courseId = courses.addCourse(course)
        CourseWithSchedules(courseId, schedules.addWeeklySchedules(scheduleTemplate, meetingCount))
    }
    suspend fun updateCourseGrade(course: CourseEntity, gradeLetter: String) = courses.updateCourseGrade(course, gradeLetter)
    suspend fun getCourseById(courseId: Long) = courses.getCourseById(courseId)
    suspend fun deleteCourse(courseId: Long) = courses.deleteCourse(courseId)
    suspend fun deleteCourseCascade(courseId: Long) = courses.deleteCourseCascade(courseId)

    // --- Tugas ---
    fun getTasksFlow(userId: Long): Flow<List<TaskEntity>> = tasks.getTasksFlow(userId)
    suspend fun addTask(task: TaskEntity) = tasks.addTask(task)
    suspend fun updateTask(task: TaskEntity) = tasks.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = tasks.deleteTask(task)

    // --- Kalender ---
    fun getAcademicEventsFlow(userId: Long): Flow<List<AcademicEventEntity>> = events.getAcademicEventsFlow(userId)
    suspend fun addAcademicEvent(event: AcademicEventEntity) = events.addAcademicEvent(event)
    suspend fun updateAcademicEvent(event: AcademicEventEntity) = events.updateAcademicEvent(event)
    suspend fun deleteAcademicEvent(event: AcademicEventEntity) = events.deleteAcademicEvent(event)

    // --- Proyek ---
    fun getProjectsFlow(userId: Long): Flow<List<ProjectEntity>> = projects.getProjectsFlow(userId)
    suspend fun addProject(project: ProjectEntity) = projects.addProject(project)
    suspend fun updateProject(project: ProjectEntity) = projects.updateProject(project)
    suspend fun deleteProject(project: ProjectEntity) = projects.deleteProject(project)
    suspend fun setTaskProject(userId: Long, task: TaskEntity, projectId: Long?) = projects.setTaskProject(userId, task, projectId)
    fun getProjectMilestonesFlow(userId: Long): Flow<List<ProjectMilestoneEntity>> = projects.getProjectMilestonesFlow(userId)
    suspend fun addProjectMilestone(milestone: ProjectMilestoneEntity) = projects.addProjectMilestone(milestone)
    suspend fun updateProjectMilestone(milestone: ProjectMilestoneEntity) = projects.updateProjectMilestone(milestone)
    suspend fun deleteProjectMilestone(milestone: ProjectMilestoneEntity) = projects.deleteProjectMilestone(milestone)

    // --- Jadwal ---
    fun getSchedulesFlow(userId: Long): Flow<List<ScheduleEntity>> = schedules.getSchedulesFlow(userId)
    suspend fun addSchedule(schedule: ScheduleEntity) = schedules.addSchedule(schedule)
    suspend fun addWeeklySchedules(template: ScheduleEntity, meetingCount: Int) = schedules.addWeeklySchedules(template, meetingCount)
    suspend fun upsertWeeklySchedules(template: ScheduleEntity, meetingCount: Int) = schedules.upsertWeeklySchedules(template, meetingCount)
    suspend fun updateSchedule(schedule: ScheduleEntity) = schedules.updateSchedule(schedule)
    suspend fun setScheduleCompleted(scheduleId: Long, completed: Boolean) = schedules.setScheduleCompleted(scheduleId, completed)
    suspend fun deleteSchedule(scheduleId: Long) = schedules.deleteSchedule(scheduleId)
    suspend fun deleteSchedulesByUserAndSemester(userId: Long, semester: Int) = schedules.deleteSchedulesByUserAndSemester(userId, semester)

    companion object {
        const val GUEST_NAME = UserRepository.GUEST_NAME

        /** Merangkai repository dari satu database Room, supaya pemasangan DAO tidak diulang di mana-mana. */
        fun from(database: TemanKuliahDatabase) = TemanKuliahRepository(
            database = database,
            userDao = database.userDao(),
            courseDao = database.courseDao(),
            scheduleDao = database.scheduleDao(),
            academicDocumentDao = database.academicDocumentDao(),
            taskDao = database.taskDao(),
            academicEventDao = database.academicEventDao(),
            projectDao = database.projectDao(),
            projectMilestoneDao = database.projectMilestoneDao()
        )
    }
}
