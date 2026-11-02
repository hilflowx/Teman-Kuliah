package com.temankuliah.app.data.repository

import androidx.room.RoomDatabase
import com.temankuliah.app.domain.MAX_SKS_PER_COURSE
import androidx.room.withTransaction
import com.temankuliah.app.data.files.AcademicDocumentProcessor
import com.temankuliah.app.data.local.CourseDao
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.ScheduleDao
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.local.TaskDao
import com.temankuliah.app.domain.GpaCalculator
import com.temankuliah.app.domain.Grades
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Aturan SKS tunggal untuk repository, export, dan validasi restore: SKS 1..[MAX_SKS_PER_COURSE], atau SKS 0
 * ("belum diisi") yang hanya sah selama nilai huruf masih "-". [grade] harus sudah dinormalkan.
 */
internal fun isValidCourseCredits(credits: Int, grade: String): Boolean =
    credits in 1..MAX_SKS_PER_COURSE || (credits == 0 && grade == "-")

/** Mata kuliah, nilai, dan impor baris KRS/KHS. */
class CourseRepository(
    private val database: RoomDatabase,
    private val courseDao: CourseDao,
    private val scheduleDao: ScheduleDao? = null,
    private val taskDao: TaskDao? = null
) {
    /** Result of importing KRS/KHS rows into a semester. */
    data class ImportResult(val inserted: Int, val updated: Int)

    /**
     * Imports reviewed KRS/KHS rows into [semester] in one transaction, so a failure in the middle
     * can never leave the semester half-replaced. All validation lives here.
     *
     * KRS ([isKrs] = true) memuat kode, nama, dosen, dan jam kuliah tanpa SKS: SKS yang sudah ada
     * dipertahankan, dan mata kuliah baru tanpa SKS disimpan dengan SKS 0 ("belum diisi") selama
     * nilainya masih "-" (dapat diisi kemudian). SKS 0 dengan nilai huruf ditolak. KHS wajib memiliki SKS.
     */
    suspend fun importCourses(
        userId: Long,
        semester: Int,
        rows: List<AcademicDocumentProcessor.AcademicRow>,
        replaceExisting: Boolean,
        isKrs: Boolean = false
    ): ImportResult = withContext(Dispatchers.IO) {
        require(userId > 0) { "Akun pengguna tidak valid." }
        require(semester >= 1) { "Semester minimal 1." }

        fun CourseEntity.fromRow(row: AcademicDocumentProcessor.AcademicRow, fallbackCredits: Int?): CourseEntity {
            val rawGrade = row.grade.trim().uppercase(Locale.ROOT)
            require(rawGrade in Grades.VALID) { "Nilai '${row.grade}' tidak valid." }
            val name = row.courseName.trim()
            require(name.isNotBlank()) { "Nama mata kuliah pada baris ${row.no} wajib diisi." }
            val credits = when {
                row.credits > 0 -> row.credits.coerceIn(1, MAX_SKS_PER_COURSE)
                fallbackCredits != null -> fallbackCredits.coerceIn(0, MAX_SKS_PER_COURSE)
                isKrs -> 0
                else -> throw IllegalArgumentException("SKS wajib diisi untuk KHS.")
            }
            require(isValidCourseCredits(credits, rawGrade)) {
                "SKS untuk '$name' belum diisi; SKS wajib diisi bila nilai bukan '-'."
            }
            return copy(
                code = row.code.trim().uppercase(Locale.ROOT),
                name = name,
                credits = credits,
                gradeLetter = rawGrade,
                gradePoint = GpaCalculator.gradeLetterToPoint(rawGrade),
                lecturer = row.lecturer.trim(),
                classTime = AcademicDocumentProcessor.classTimeForStorage(row)
            )
        }

        database.withTransaction {
            val blank = CourseEntity(userId = userId, code = "", name = "", credits = 1, gradeLetter = "-", gradePoint = 0.0, semester = semester)
            val usedCodes = mutableSetOf<String>()
            if (replaceExisting) {
                val entities = rows.map { row ->
                    require(row.code.isNotBlank()) { "Kode mata kuliah wajib diisi." }
                    val candidate = blank.fromRow(row, null)
                    var uniqueCode = candidate.code
                    while (uniqueCode in usedCodes) {
                        uniqueCode = "${uniqueCode}_${row.no}"
                    }
                    usedCodes.add(uniqueCode)
                    candidate.copy(code = uniqueCode)
                }
                courseDao.deleteCoursesByUserAndSemester(userId, semester)
                courseDao.insertCourses(entities)
                ImportResult(inserted = entities.size, updated = 0)
            } else {
                val existingCourses = courseDao.getCoursesByUserAndSemester(userId, semester)
                existingCourses.forEach { usedCodes.add(it.code.uppercase(Locale.ROOT)) }
                var inserted = 0
                var updated = 0
                rows.forEach { row ->
                    val rowCode = row.code.trim().uppercase(Locale.ROOT)
                    val rowName = row.courseName.trim()
                    val existing = existingCourses.firstOrNull {
                        (rowCode.isNotBlank() && it.code.equals(rowCode, ignoreCase = true)) ||
                            it.name.equals(rowName, ignoreCase = true)
                    }
                    if (existing != null) {
                        val candidate = blank.fromRow(row, existing.credits)
                        val keepGrade = candidate.gradeLetter == "-" && existing.gradeLetter != "-"
                        val mergedCredits = if (candidate.credits > 0) candidate.credits else existing.credits
                        val mergedGrade = if (keepGrade) existing.gradeLetter else candidate.gradeLetter
                        require(isValidCourseCredits(mergedCredits, mergedGrade)) {
                            "SKS untuk '${candidate.name}' belum diisi; SKS wajib diisi bila nilai bukan '-'."
                        }
                        courseDao.updateCourse(existing.copy(
                            code = candidate.code.ifBlank { existing.code },
                            name = candidate.name,
                            credits = mergedCredits,
                            gradeLetter = mergedGrade,
                            gradePoint = if (keepGrade) existing.gradePoint else candidate.gradePoint,
                            lecturer = candidate.lecturer.ifBlank { existing.lecturer },
                            classTime = candidate.classTime.ifBlank { existing.classTime }
                        ))
                        updated++
                    } else {
                        require(row.code.isNotBlank()) { "Kode mata kuliah wajib diisi." }
                        val candidate = blank.fromRow(row, null)
                        var uniqueCode = candidate.code
                        while (uniqueCode in usedCodes) {
                            uniqueCode = "${uniqueCode}_${row.no}"
                        }
                        usedCodes.add(uniqueCode)
                        courseDao.insertCourse(candidate.copy(code = uniqueCode))
                        inserted++
                    }
                }
                ImportResult(inserted, updated)
            }
        }
    }

    // Mata kuliah dan nilai. Pengulangan pada semester berbeda dihitung sebagai percobaan
    // terpisah karena skema lama belum menyimpan penanda nilai pengganti/ulang.
    fun getCoursesFlow(userId: Long): Flow<List<CourseEntity>> = courseDao.getCoursesByUser(userId)

    suspend fun addCourse(course: CourseEntity): Long = withContext(Dispatchers.IO) {
        validateCourse(course)
        require(courseDao.countDuplicateCourse(course.userId, course.code.trim(), course.semester) <= 0) {
            "Kode mata kuliah sudah digunakan pada semester ini."
        }
        courseDao.insertCourse(course.copy(
            code = course.code.trim().uppercase(Locale.ROOT),
            name = course.name.trim(),
            gradeLetter = course.gradeLetter.trim().uppercase(Locale.ROOT),
            gradePoint = GpaCalculator.gradeLetterToPoint(course.gradeLetter)
        ))
    }

    /**
     * Memperbarui mata kuliah. Jadwal terhubung ke mata kuliah hanya lewat teks `courseName` + semester,
     * jadi bila nama atau semester berubah, jadwal lama (userId + semester lama + nama lama) dipindahkan
     * ke nama/semester baru dalam transaksi yang sama. Jadwal yang berubah dikembalikan agar pemanggil
     * menjadwalkan ulang alarmnya (judul notifikasi memuat nama mata kuliah).
     */
    suspend fun updateCourse(course: CourseEntity): List<ScheduleEntity> = withContext(Dispatchers.IO) {
        validateCourse(course)
        require(courseDao.countDuplicateCourse(course.userId, course.code.trim(), course.semester, course.id) <= 0) {
            "Kode mata kuliah sudah digunakan pada semester ini."
        }
        val normalized = course.copy(
            code = course.code.trim().uppercase(Locale.ROOT),
            name = course.name.trim(),
            gradeLetter = course.gradeLetter.trim().uppercase(Locale.ROOT),
            gradePoint = GpaCalculator.gradeLetterToPoint(course.gradeLetter)
        )
        database.withTransaction {
            val previous = courseDao.getCourseById(course.id)
            courseDao.updateCourse(normalized)
            if (scheduleDao == null || previous == null || previous.userId != normalized.userId) return@withTransaction emptyList()
            val identityChanged = previous.name.trim() != normalized.name || previous.semester != normalized.semester
            if (!identityChanged) return@withTransaction emptyList()
            val related = scheduleDao.getSchedulesByCourse(previous.userId, previous.semester, previous.name)
            if (related.isEmpty()) return@withTransaction emptyList()
            scheduleDao.renameSchedulesByCourse(previous.userId, previous.semester, previous.name, normalized.name, normalized.semester)
            related.map { it.copy(courseName = normalized.name, semester = normalized.semester) }
        }
    }

    suspend fun updateCourseGrade(course: CourseEntity, gradeLetter: String) = withContext(Dispatchers.IO) {
        val normalized = gradeLetter.trim().uppercase(Locale.ROOT)
        require(normalized in Grades.VALID) {
            "Pilih nilai huruf yang valid."
        }
        courseDao.updateCourse(course.copy(gradeLetter = normalized, gradePoint = GpaCalculator.gradeLetterToPoint(normalized)))
    }

    suspend fun getCourseById(courseId: Long): CourseEntity? = withContext(Dispatchers.IO) {
        courseDao.getCourseById(courseId)
    }

    suspend fun deleteCourse(courseId: Long) = withContext(Dispatchers.IO) { courseDao.deleteCourseById(courseId) }

    /** Menghapus mata kuliah beserta jadwal terkait secara atomik; ID jadwal dikembalikan agar alarm dibatalkan. */
    suspend fun deleteCourseCascade(courseId: Long): List<Long> = deleteCoursesCascade(listOf(courseId))

    /**
     * Menghapus pilihan mata kuliah beserta jadwal terkait dalam satu transaksi. `courseId` tugas yang
     * menunjuk mata kuliah itu di-set null (tugas tetap ada sebagai tugas mandiri).
     */
    suspend fun deleteCoursesCascade(courseIds: List<Long>): List<Long> = withContext(Dispatchers.IO) {
        database.withTransaction {
            val deletedScheduleIds = mutableListOf<Long>()
            courseIds.distinct().forEach { courseId ->
                val course = courseDao.getCourseById(courseId) ?: return@forEach
                if (scheduleDao != null) {
                    val related = scheduleDao.getSchedulesByCourse(course.userId, course.semester, course.name)
                    deletedScheduleIds.addAll(related.map { it.id })
                    scheduleDao.deleteSchedulesByCourse(course.userId, course.semester, course.name)
                }
                taskDao?.clearCourseLinks(course.userId, courseId)
                courseDao.deleteCourseById(courseId)
            }
            deletedScheduleIds.distinct()
        }
    }

    private fun validateCourse(course: CourseEntity) {
        require(course.userId > 0) { "Akun pengguna tidak valid." }
        require(course.code.isNotBlank()) { "Kode mata kuliah wajib diisi." }
        require(course.name.isNotBlank()) { "Nama mata kuliah wajib diisi." }
        val grade = course.gradeLetter.trim().uppercase(Locale.ROOT)
        require(isValidCourseCredits(course.credits, grade)) {
            "SKS harus antara 1 dan $MAX_SKS_PER_COURSE, atau belum diisi jika nilai masih kosong."
        }
        require(course.semester >= 1) { "Semester minimal 1." }
        require(grade in Grades.VALID) { "Nilai huruf tidak valid." }
    }
}
