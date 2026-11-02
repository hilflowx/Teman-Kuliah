package com.temankuliah.app.ui.courses

import com.temankuliah.app.domain.DEFAULT_REMINDER_MINUTES
import com.temankuliah.app.domain.parseSemesterStartDate
import com.temankuliah.app.domain.semesterStartDateKey
import com.temankuliah.app.domain.rethrowIfCancellation
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.AppStorage
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.domain.GpaCalculator
import com.temankuliah.app.domain.GpaSummary
import com.temankuliah.app.ui.FeatureContext
import com.temankuliah.app.ui.FeatureController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

internal class CourseController(
    ctx: FeatureContext
) : FeatureController(ctx) {
    private val _courseSearchQuery = MutableStateFlow("")
    val courseSearchQuery: StateFlow<String> = _courseSearchQuery.asStateFlow()

    // Courses flow mapped to current user
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val courses: StateFlow<List<CourseEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getCoursesFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Instant memoized GPA Summary
    val gpaSummary: StateFlow<GpaSummary> = courses.map { list ->
        GpaCalculator.calculateSummary(list)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GpaSummary()
    )

    fun setSearchQuery(query: String) {
        _courseSearchQuery.value = query
    }

    // --- Course Actions ---
    fun addCourse(
        code: String,
        name: String,
        credits: Int,
        gradeLetter: String,
        semester: Int,
        lecturer: String = "",
        classTime: String = "",
        autoCreateSchedule: Boolean = false,
        scheduleDayOfWeek: Int = 0,
        scheduleStartTime: String = "",
        scheduleEndTime: String = "",
        scheduleRoom: String = ""
    ) {
        val user = currentUser.value ?: return
        scope.launch {
            try {
                val course = CourseEntity(
                    userId = user.id,
                    code = code.trim().uppercase(Locale.ROOT),
                    name = name.trim(),
                    credits = credits,
                    gradeLetter = gradeLetter.trim().uppercase(Locale.ROOT),
                    gradePoint = GpaCalculator.gradeLetterToPoint(gradeLetter),
                    semester = semester,
                    lecturer = lecturer.trim(),
                    classTime = classTime.trim()
                )
                val hasScheduleInput = autoCreateSchedule && scheduleDayOfWeek in 1..7 &&
                    scheduleStartTime.isNotBlank() && scheduleEndTime.isNotBlank()
                val semesterStartDate = parseSemesterStartDate(
                    AppStorage.getPreferences(application).getString(semesterStartDateKey(semester), null)
                )

                if (hasScheduleInput && semesterStartDate != null) {
                    val delta = (scheduleDayOfWeek.coerceIn(1, 7) - semesterStartDate.dayOfWeek.value + 7) % 7
                    val template = ScheduleEntity(
                        userId = user.id,
                        courseName = course.name,
                        dayOfWeek = scheduleDayOfWeek,
                        scheduledDate = semesterStartDate.plusDays(delta.toLong()).toString(),
                        startTime = scheduleStartTime.trim(),
                        endTime = scheduleEndTime.trim(),
                        room = scheduleRoom.trim(),
                        lecturer = course.lecturer,
                        type = "Kuliah Teori",
                        semester = semester,
                        alarmEnabled = true,
                        reminderMinutesBefore = DEFAULT_REMINDER_MINUTES
                    )
                    // Atomik: bila jadwal gagal, mata kuliah ikut dibatalkan (exception ditangkap di bawah,
                    // dialog tetap terbuka supaya pengguna bisa memperbaiki isiannya).
                    val saved = repository.addCourseWithWeeklySchedules(course, template, 16).schedules
                    saved.forEach { runCatching { ReminderScheduler.scheduleClass(application, it) } }
                    snackbar.value = "Mata kuliah '${course.name}' & jadwal perkuliahan (${saved.size} pertemuan) berhasil ditambahkan."
                } else {
                    repository.addCourse(course)
                    snackbar.value = when {
                        hasScheduleInput -> "Mata kuliah '${course.name}' berhasil ditambahkan. Tentukan tanggal mulai perkuliahan sebelum membuat jadwal."
                        autoCreateSchedule -> "Mata kuliah '${course.name}' berhasil ditambahkan. Jadwal kuliah belum dibuat karena hari dan jam belum diisi."
                        else -> "Mata kuliah '${course.name}' berhasil ditambahkan."
                    }
                }
                closeDialog()
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal menambahkan mata kuliah."
            }
        }
    }

    fun updateCourse(
        course: CourseEntity,
        code: String,
        name: String,
        credits: Int,
        gradeLetter: String,
        semester: Int,
        lecturer: String = course.lecturer,
        classTime: String = course.classTime
    ) {
        scope.launch {
            try {
                val updatedSchedules = repository.updateCourse(
                    course.copy(
                        code = code.trim().uppercase(Locale.ROOT),
                        name = name.trim(),
                        credits = credits,
                        gradeLetter = gradeLetter.trim().uppercase(Locale.ROOT),
                        gradePoint = GpaCalculator.gradeLetterToPoint(gradeLetter),
                        semester = semester,
                        lecturer = lecturer.trim(),
                        classTime = classTime.trim()
                    )
                )
                // Nama/semester berubah -> teks alarm (judul notifikasi) ikut diperbarui.
                updatedSchedules.forEach { runCatching { ReminderScheduler.scheduleClass(application, it) } }
                closeDialog()
                snackbar.value = "Mata kuliah berhasil diperbarui."
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal memperbarui mata kuliah."
            }
        }
    }

    fun updateCourseGrade(course: CourseEntity, newGradeLetter: String) {
        scope.launch {
            try {
                repository.updateCourseGrade(course, newGradeLetter)
                snackbar.value = "Nilai ${course.code} diperbarui ke ${newGradeLetter.uppercase(Locale.ROOT)}."
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal memperbarui nilai."
            }
        }
    }

    fun deleteCourse(courseId: Long) {
        deleteCourses(listOf(courseId), singleItem = true)
    }

    fun deleteAllKrsCourses(courseIds: List<Long>) {
        if (courseIds.isEmpty()) return
        deleteCourses(courseIds, singleItem = false)
    }

    private fun deleteCourses(courseIds: List<Long>, singleItem: Boolean) {
        scope.launch {
            try {
                val deletedScheduleIds = repository.courses.deleteCoursesCascade(courseIds)
                deletedScheduleIds.forEach { scheduleId ->
                    ReminderScheduler.cancelClass(application, scheduleId)
                }
                snackbar.value = when {
                    !singleItem -> "${courseIds.distinct().size} mata kuliah KRS dan ${deletedScheduleIds.size} jadwal terkait berhasil dihapus."
                    deletedScheduleIds.isNotEmpty() -> "Mata kuliah dan ${deletedScheduleIds.size} jadwal perkuliahan terkait berhasil dihapus."
                    else -> "Mata kuliah telah dihapus."
                }
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal menghapus mata kuliah."
            }
        }
    }
}
