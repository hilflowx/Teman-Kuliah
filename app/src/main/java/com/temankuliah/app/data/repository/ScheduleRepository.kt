package com.temankuliah.app.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.temankuliah.app.data.local.SCHEDULE_MODE_OFFLINE
import com.temankuliah.app.data.local.SCHEDULE_MODE_ONLINE
import com.temankuliah.app.data.local.SCHEDULE_SOURCE_KRS
import com.temankuliah.app.data.local.SCHEDULE_SOURCE_MANUAL
import com.temankuliah.app.data.local.ScheduleDao
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.domain.ScheduleRecurrence
import com.temankuliah.app.domain.ScheduleRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Jadwal kuliah (kejadian pada tanggal tertentu). */
class ScheduleRepository(private val database: RoomDatabase, private val scheduleDao: ScheduleDao) {
    // Jadwal kuliah adalah kejadian pada tanggal tertentu (tidak berulang otomatis).
    fun getSchedulesFlow(userId: Long): Flow<List<ScheduleEntity>> = scheduleDao.getSchedulesByUser(userId)

    suspend fun addSchedule(schedule: ScheduleEntity): Long = withContext(Dispatchers.IO) {
        validateSchedule(schedule)
        scheduleDao.insertSchedule(schedule.normalized())
    }

    /**
     * Menyimpan jadwal mingguan: satu baris per pertemuan, dimulai dari `template.scheduledDate`
     * lalu setiap 7 hari. Semua pertemuan divalidasi dulu dan disimpan dalam satu transaksi,
     * sehingga hasilnya semua tersimpan atau tidak ada yang tersimpan.
     * Mengembalikan baris yang tersimpan (dengan id) sesuai urutan pertemuan.
     */
    suspend fun addWeeklySchedules(template: ScheduleEntity, meetingCount: Int): List<ScheduleEntity> =
        withContext(Dispatchers.IO) {
            val rows = buildWeeklyRows(template, meetingCount)
            database.withTransaction {
                val ids = scheduleDao.insertSchedules(rows)
                rows.mapIndexed { index, row -> row.copy(id = ids[index]) }
            }
        }

    /** Hasil [upsertWeeklySchedules]: pertemuan baru, dan pertemuan lama yang diperbarui di tempat (id tetap). */
    data class UpsertResult(val inserted: List<ScheduleEntity>, val updated: List<ScheduleEntity>)

    /**
     * Seperti [addWeeklySchedules] tetapi idempoten: pertemuan yang sudah ada untuk
     * (userId, semester, courseName, scheduledDate, startTime) diperbarui di tempat (ruang, dosen, jam selesai,
     * jenis, alarm) dengan id, status selesai, dan seri yang lama dipertahankan; sisanya ditambahkan.
     * Menyimpan KRS yang sama berulang kali tidak menggandakan jadwal. Semua dalam satu transaksi.
     */
    suspend fun upsertWeeklySchedules(template: ScheduleEntity, meetingCount: Int): UpsertResult =
        withContext(Dispatchers.IO) {
            val rows = buildWeeklyRows(template, meetingCount)
            database.withTransaction {
                val existing = scheduleDao.getSchedulesByCourse(template.userId, template.semester, template.courseName)
                    .groupBy { it.scheduledDate.trim() to it.startTime.trim() }
                val inserted = mutableListOf<ScheduleEntity>()
                val updated = mutableListOf<ScheduleEntity>()
                val toInsert = mutableListOf<ScheduleEntity>()
                // Pertemuan baru ikut seri yang sudah ada (bukan membuat seri kedua), supaya satu mata kuliah tetap satu kartu.
                val existingSeriesId = rows
                    .mapNotNull { existing[it.scheduledDate to it.startTime]?.minByOrNull { e -> e.id } }
                    .firstNotNullOfOrNull { it.recurrenceGroupId.takeIf { group -> group.isNotBlank() } }
                rows.forEach { row ->
                    val match = existing[row.scheduledDate to row.startTime]?.minByOrNull { it.id }
                    if (match != null) {
                        val merged = row.copy(id = match.id, isCompleted = match.isCompleted, recurrenceGroupId = match.recurrenceGroupId)
                        scheduleDao.updateSchedule(merged)
                        updated.add(merged)
                    } else toInsert.add(if (existingSeriesId != null) row.copy(recurrenceGroupId = existingSeriesId) else row)
                }
                if (toInsert.isNotEmpty()) {
                    val ids = scheduleDao.insertSchedules(toInsert)
                    toInsert.forEachIndexed { index, row -> inserted.add(row.copy(id = ids[index])) }
                }
                UpsertResult(inserted, updated)
            }
        }

    private fun buildWeeklyRows(template: ScheduleEntity, meetingCount: Int): List<ScheduleEntity> {
        require(meetingCount in 1..ScheduleRecurrence.MAX_MEETINGS) {
            "Jumlah pertemuan harus 1 sampai ${ScheduleRecurrence.MAX_MEETINGS}."
        }
        val firstDate = runCatching { java.time.LocalDate.parse(template.scheduledDate.trim()) }.getOrNull()
            ?: throw IllegalArgumentException("Tanggal kuliah tidak valid.")
        // Satu seri berbagi satu id; satu pertemuan saja diperlakukan sebagai Jadwal Sekali (id kosong).
        val groupId = if (meetingCount > 1) java.util.UUID.randomUUID().toString() else ""
        return ScheduleRecurrence.weeklyDates(firstDate, meetingCount).map { date ->
            template.copy(id = 0, scheduledDate = date.toString(), dayOfWeek = date.dayOfWeek.value, isCompleted = false, recurrenceGroupId = groupId)
                .normalized()
                .also(::validateSchedule)
        }
    }

    /**
     * Menyimpan beberapa pertemuan sekaligus (satu transaksi): semua tersimpan atau tidak ada.
     * Id pada [rows] diabaikan; hasilnya baris dengan id baru sesuai urutan masuk.
     */
    suspend fun addSchedules(rows: List<ScheduleEntity>): List<ScheduleEntity> = withContext(Dispatchers.IO) {
        require(rows.isNotEmpty()) { "Tidak ada pertemuan yang dibuat." }
        val prepared = rows.map { it.copy(id = 0).normalized().also(::validateSchedule) }
        database.withTransaction {
            val ids = scheduleDao.insertSchedules(prepared)
            prepared.mapIndexed { index, row -> row.copy(id = ids[index]) }
        }
    }

    /** Hasil [replaceRows]: baris baru, baris yang diperbarui di tempat (id dan status selesai tetap), dan baris yang dihapus. */
    data class ReplaceResult(val inserted: List<ScheduleEntity>, val updated: List<ScheduleEntity>, val deleted: List<ScheduleEntity>)

    /**
     * Mengganti pertemuan [existingIds] dengan [newRows] dalam satu transaksi. Pertemuan baru yang tanggalnya
     * sama dengan pertemuan lama memperbarui baris lama (id dan status selesai dipertahankan), tanggal baru
     * ditambahkan, dan pertemuan lama yang tidak lagi termasuk dihapus.
     */
    suspend fun replaceRows(existingIds: Collection<Long>, newRows: List<ScheduleEntity>): ReplaceResult = withContext(Dispatchers.IO) {
        require(newRows.isNotEmpty()) { "Tidak ada pertemuan yang dibuat." }
        val prepared = newRows.map { it.normalized().also(::validateSchedule) }
        database.withTransaction {
            val current = existingIds.distinct().mapNotNull { scheduleDao.getById(it) }
            val byDate = current.groupBy { it.scheduledDate }
            val matched = HashSet<Long>()
            val updated = mutableListOf<ScheduleEntity>()
            val toInsert = mutableListOf<ScheduleEntity>()
            for (row in prepared) {
                val match = byDate[row.scheduledDate].orEmpty().firstOrNull { it.id !in matched }
                if (match != null) {
                    val merged = row.copy(id = match.id, isCompleted = match.isCompleted)
                    scheduleDao.updateSchedule(merged)
                    matched.add(match.id)
                    updated.add(merged)
                } else {
                    toInsert.add(row.copy(id = 0))
                }
            }
            val deleted = current.filter { it.id !in matched }
            if (deleted.isNotEmpty()) scheduleDao.deleteSchedulesByIds(deleted.map { it.id })
            val inserted = if (toInsert.isEmpty()) emptyList() else {
                val ids = scheduleDao.insertSchedules(toInsert)
                toInsert.mapIndexed { index, row -> row.copy(id = ids[index]) }
            }
            ReplaceResult(inserted, updated, deleted)
        }
    }

    /** Menghapus satu pertemuan dan mengembalikan barisnya (untuk Urungkan); kosong bila sudah tidak ada. */
    suspend fun deleteAndReturn(scheduleId: Long): List<ScheduleEntity> = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = scheduleDao.getById(scheduleId)
            if (existing != null) scheduleDao.deleteScheduleById(scheduleId)
            listOfNotNull(existing)
        }
    }

    /** Menghapus seluruh pertemuan satu seri dan mengembalikan barisnya (untuk Urungkan). */
    suspend fun deleteGroupAndReturn(userId: Long, groupId: String): List<ScheduleEntity> = withContext(Dispatchers.IO) {
        if (groupId.isBlank()) return@withContext emptyList()
        database.withTransaction {
            val rows = scheduleDao.getSchedulesByGroup(userId, groupId)
            scheduleDao.deleteSchedulesByGroup(userId, groupId)
            rows
        }
    }

    /** Menghapus semua jadwal satu semester dan mengembalikan barisnya (untuk Urungkan). */
    suspend fun deleteSemesterAndReturn(userId: Long, semester: Int): List<ScheduleEntity> = withContext(Dispatchers.IO) {
        database.withTransaction {
            val rows = scheduleDao.getSchedulesByUserAndSemester(userId, semester)
            scheduleDao.deleteSchedulesByUserAndSemester(userId, semester)
            rows
        }
    }

    /** Mengembalikan baris yang baru dihapus dengan id aslinya. */
    suspend fun restore(rows: List<ScheduleEntity>) = withContext(Dispatchers.IO) {
        if (rows.isNotEmpty()) database.withTransaction { scheduleDao.insertSchedules(rows) }
    }

    suspend fun updateSchedule(schedule: ScheduleEntity) = withContext(Dispatchers.IO) {
        validateSchedule(schedule)
        scheduleDao.updateSchedule(schedule.normalized())
    }

    suspend fun setScheduleCompleted(scheduleId: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        val existing = scheduleDao.getById(scheduleId) ?: return@withContext
        scheduleDao.updateSchedule(existing.copy(isCompleted = completed))
    }

    suspend fun deleteSchedule(scheduleId: Long) = withContext(Dispatchers.IO) { scheduleDao.deleteScheduleById(scheduleId) }

    suspend fun deleteSchedulesByUserAndSemester(userId: Long, semester: Int): List<Long> = withContext(Dispatchers.IO) {
        database.withTransaction {
            val deletedIds = scheduleDao.getSchedulesByUserAndSemester(userId, semester).map { it.id }
            scheduleDao.deleteSchedulesByUserAndSemester(userId, semester)
            deletedIds
        }
    }

    private fun validateSchedule(schedule: ScheduleEntity) {
        require(schedule.userId > 0) { "Akun pengguna tidak valid." }
        require(schedule.semester >= 1) { "Semester minimal 1." }
        require(schedule.reminderMinutesBefore in 0..180) { "Waktu pengingat tidak valid." }
        require(schedule.courseName.isNotBlank()) { "Nama mata kuliah wajib diisi." }
        require(schedule.dayOfWeek in 1..7) { "Hari kuliah tidak valid." }
        val date = runCatching { java.time.LocalDate.parse(schedule.scheduledDate) }.getOrNull()
            ?: throw IllegalArgumentException("Tanggal kuliah tidak valid.")
        require(date.dayOfWeek.value == schedule.dayOfWeek) { "Tanggal dan hari kuliah tidak sesuai." }
        val start = schedule.startTime.toMinutesOrNull()
            ?: throw IllegalArgumentException("Jam mulai harus menggunakan format HH:mm.")
        val end = schedule.endTime.toMinutesOrNull()
            ?: throw IllegalArgumentException("Jam selesai harus menggunakan format HH:mm.")
        require(end > start) { "Jam selesai harus setelah jam mulai." }
        require(schedule.mode == SCHEDULE_MODE_OFFLINE || schedule.mode == SCHEDULE_MODE_ONLINE) { "Mode kuliah tidak valid." }
        require(schedule.sumber == SCHEDULE_SOURCE_KRS || schedule.sumber == SCHEDULE_SOURCE_MANUAL) { "Sumber mata kuliah tidak valid." }
        require(schedule.sks in 0..ScheduleRules.MAX_SKS) { "SKS harus antara 0 dan ${ScheduleRules.MAX_SKS}." }
        val link = schedule.linkMeeting
        require(link.isNullOrBlank() || ScheduleRules.isValidMeetingUrl(link)) { "Format tautan tidak valid (contoh: https://meet.google.com/...)" }
    }

    private fun ScheduleEntity.normalized(): ScheduleEntity = copy(
        courseName = courseName.trim(),
        startTime = startTime.trim(),
        endTime = endTime.trim(),
        room = room.trim(),
        lecturer = lecturer.trim(),
        type = type.trim().ifEmpty { "Kuliah Teori" },
        scheduledDate = scheduledDate.trim(),
        platform = platform?.trim()?.ifEmpty { null },
        linkMeeting = linkMeeting?.trim()?.ifEmpty { null },
        catatan = catatan?.trim()?.ifEmpty { null }
    )

    private fun String.toMinutesOrNull(): Int? {
        if (!TIME_PATTERN.matches(this)) return null
        val parts = split(':')
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private companion object {
        val TIME_PATTERN = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
    }
}
