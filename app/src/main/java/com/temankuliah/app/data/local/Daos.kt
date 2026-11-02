package com.temankuliah.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    /** [normalizedNim] harus hasil `normalizeNim()`; kolom dibandingkan tanpa beda huruf besar/kecil dan spasi tepi. */
    @Query("SELECT * FROM users WHERE LOWER(TRIM(nim)) = :normalizedNim AND TRIM(nim) != '' LIMIT 1")
    suspend fun getUserByNormalizedNim(normalizedNim: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE AND email != '' LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE authType = 'guest' ORDER BY id ASC LIMIT 1")
    suspend fun getGuestUser(): UserEntity?

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getActiveUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getActiveUser(): UserEntity?

    /**
     * Menetapkan sandi pertama hanya bila akun (bukan tamu) memang masih tanpa sandi. Dibuat sebagai satu
     * UPDATE bersyarat supaya dua permintaan serentak tidak bisa saling menimpa sandi, dan tidak pernah
     * bisa dipakai untuk mengganti sandi yang sudah ada. Mengembalikan jumlah baris yang berubah (0 atau 1).
     */
    @Query("UPDATE users SET password = :hash, authType = 'local' WHERE id = :userId AND password = '' AND authType != 'guest'")
    suspend fun setInitialPassword(userId: Long, hash: String): Int

    /**
     * Mengganti sandi akun biasa pada alur "Lupa kata sandi" (setelah kunci layar perangkat diverifikasi di UI).
     * Akun tamu tidak pernah memakai sandi. Mengembalikan jumlah baris yang berubah (0 atau 1).
     */
    @Query("UPDATE users SET password = :hash, authType = 'local' WHERE id = :userId AND authType != 'guest'")
    suspend fun replacePassword(userId: Long, hash: String): Int

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAll()

    @Query("UPDATE users SET isLoggedIn = CASE WHEN id = :userId THEN 1 ELSE 0 END")
    suspend fun activateUser(userId: Long)

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsersOnce(): List<UserEntity>

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)
}

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE userId = :userId ORDER BY semester DESC, name ASC")
    fun getCoursesByUser(userId: Long): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteCourseById(id: Long)

    @Query("SELECT COUNT(*) FROM courses WHERE userId = :userId AND code = :code COLLATE NOCASE AND semester = :semester AND id != :excludeId")
    suspend fun countDuplicateCourse(userId: Long, code: String, semester: Int, excludeId: Long = 0): Int

    @Query("SELECT * FROM courses ORDER BY id ASC")
    suspend fun getAllCoursesOnce(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE userId = :userId AND semester = :semester ORDER BY name ASC")
    suspend fun getCoursesByUserAndSemester(userId: Long, semester: Int): List<CourseEntity>

    @Query("DELETE FROM courses WHERE userId = :userId AND semester = :semester")
    suspend fun deleteCoursesByUserAndSemester(userId: Long, semester: Int)

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE userId = :userId ORDER BY scheduledDate ASC, startTime ASC")
    fun getSchedulesByUser(userId: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE userId = :userId AND semester = :semester ORDER BY scheduledDate ASC, startTime ASC")
    suspend fun getSchedulesByUserAndSemester(userId: Long, semester: Int): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE userId = :userId AND semester = :semester AND LOWER(TRIM(courseName)) = LOWER(TRIM(:courseName))")
    suspend fun getSchedulesByCourse(userId: Long, semester: Int, courseName: String): List<ScheduleEntity>

    @Query("DELETE FROM schedules WHERE userId = :userId AND semester = :semester AND LOWER(TRIM(courseName)) = LOWER(TRIM(:courseName))")
    suspend fun deleteSchedulesByCourse(userId: Long, semester: Int, courseName: String): Int

    /** Memindahkan jadwal satu mata kuliah ke nama/semester baru (relasi hanya lewat teks `courseName`). */
    @Query("UPDATE schedules SET courseName = :newName, semester = :newSemester WHERE userId = :userId AND semester = :oldSemester AND LOWER(TRIM(courseName)) = LOWER(TRIM(:oldName))")
    suspend fun renameSchedulesByCourse(userId: Long, oldSemester: Int, oldName: String, newName: String, newSemester: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ScheduleEntity>): List<Long>

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)

    @Query("DELETE FROM schedules WHERE userId = :userId AND semester = :semester")
    suspend fun deleteSchedulesByUserAndSemester(userId: Long, semester: Int)

    /** Semua pertemuan satu seri Jadwal Berulang milik [userId]. */
    @Query("SELECT * FROM schedules WHERE userId = :userId AND recurrenceGroupId = :groupId ORDER BY scheduledDate ASC, startTime ASC")
    suspend fun getSchedulesByGroup(userId: Long, groupId: String): List<ScheduleEntity>

    @Query("DELETE FROM schedules WHERE recurrenceGroupId = :groupId AND userId = :userId AND recurrenceGroupId != ''")
    suspend fun deleteSchedulesByGroup(userId: Long, groupId: String)

    @Query("DELETE FROM schedules WHERE id IN (:ids)")
    suspend fun deleteSchedulesByIds(ids: List<Long>)

    @Query("SELECT * FROM schedules ORDER BY id ASC")
    suspend fun getAllSchedulesOnce(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE userId = :userId ORDER BY id ASC")
    suspend fun getAllSchedulesByUserOnce(userId: Long): List<ScheduleEntity>

    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()
}


@Dao
interface AcademicDocumentDao {
    @Query("SELECT * FROM academic_documents WHERE userId = :userId ORDER BY semester DESC, type ASC")
    fun observeByUser(userId: Long): Flow<List<AcademicDocumentEntity>>

    @Query("SELECT * FROM academic_documents ORDER BY id ASC")
    suspend fun getAllOnce(): List<AcademicDocumentEntity>

    @Query("SELECT * FROM academic_documents WHERE userId = :userId AND semester = :semester AND type = :type LIMIT 1")
    suspend fun find(userId: Long, semester: Int, type: String): AcademicDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: AcademicDocumentEntity): Long

    @Update
    suspend fun update(document: AcademicDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<AcademicDocumentEntity>)

    @Delete
    suspend fun delete(document: AcademicDocumentEntity)

    @Query("DELETE FROM academic_documents")
    suspend fun deleteAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueAtMillis ASC")
    fun observeByUser(userId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY id ASC")
    suspend fun getAllOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY id ASC")
    suspend fun getAllByUserOnce(userId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("UPDATE tasks SET projectId = :projectId WHERE id = :taskId")
    suspend fun setTaskProject(taskId: Long, projectId: Long?)

    @Query("UPDATE tasks SET projectId = NULL WHERE projectId = :projectId")
    suspend fun clearProjectLinks(projectId: Long)

    /** Tanpa foreign key, penghapusan mata kuliah harus melepas tautan tugasnya secara manual. */
    @Query("UPDATE tasks SET courseId = NULL WHERE userId = :userId AND courseId = :courseId")
    suspend fun clearCourseLinks(userId: Long, courseId: Long): Int

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}


@Dao
interface AcademicEventDao {
    @Query("SELECT * FROM academic_events WHERE userId = :userId ORDER BY scheduledAtMillis ASC")
    fun observeByUser(userId: Long): Flow<List<AcademicEventEntity>>
    @Query("SELECT * FROM academic_events ORDER BY id ASC")
    suspend fun getAllOnce(): List<AcademicEventEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: AcademicEventEntity): Long
    @Update
    suspend fun update(event: AcademicEventEntity)
    @Delete
    suspend fun delete(event: AcademicEventEntity)
    @Query("DELETE FROM academic_events")
    suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AcademicEventEntity>)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE userId = :userId ORDER BY deadlineAtMillis ASC")
    fun observeByUser(userId: Long): Flow<List<ProjectEntity>>
    @Query("SELECT * FROM projects ORDER BY id ASC")
    suspend fun getAllOnce(): List<ProjectEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long
    @Update
    suspend fun update(project: ProjectEntity)
    @Delete
    suspend fun delete(project: ProjectEntity)
    @Query("DELETE FROM projects")
    suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>)
}

@Dao
interface ProjectMilestoneDao {
    @Query("SELECT * FROM project_milestones WHERE userId = :userId ORDER BY deadlineAtMillis ASC")
    fun observeByUser(userId: Long): Flow<List<ProjectMilestoneEntity>>
    @Query("SELECT * FROM project_milestones ORDER BY id ASC")
    suspend fun getAllOnce(): List<ProjectMilestoneEntity>
    @Query("SELECT * FROM project_milestones WHERE projectId = :projectId ORDER BY deadlineAtMillis ASC")
    suspend fun getByProject(projectId: Long): List<ProjectMilestoneEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(milestone: ProjectMilestoneEntity): Long
    @Update
    suspend fun update(milestone: ProjectMilestoneEntity)
    @Delete
    suspend fun delete(milestone: ProjectMilestoneEntity)
    @Query("DELETE FROM project_milestones")
    suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(milestones: List<ProjectMilestoneEntity>)
}
