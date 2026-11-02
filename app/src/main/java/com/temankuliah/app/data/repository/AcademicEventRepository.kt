package com.temankuliah.app.data.repository

import com.temankuliah.app.data.local.AcademicEventDao
import com.temankuliah.app.data.local.AcademicEventEntity
import com.temankuliah.app.domain.ACADEMIC_EVENT_TYPES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Ujian dan kegiatan akademik untuk kalender. */
class AcademicEventRepository(private val academicEventDao: AcademicEventDao) {
    // Kegiatan akademik (kalender)
    fun getAcademicEventsFlow(userId: Long): Flow<List<AcademicEventEntity>> = academicEventDao.observeByUser(userId)

    suspend fun addAcademicEvent(event: AcademicEventEntity) = withContext(Dispatchers.IO) {
        require(event.title.isNotBlank()) { "Nama ujian atau kegiatan wajib diisi." }
        require(event.type in ACADEMIC_EVENT_TYPES) { "Jenis kegiatan tidak valid." }
        require(event.reminderMinutesBefore in 0..180) { "Waktu pengingat tidak valid." }
        require(event.scheduledAtMillis > 0L) { "Tanggal kegiatan tidak valid." }
        academicEventDao.insert(event)
    }

    suspend fun updateAcademicEvent(event: AcademicEventEntity) = withContext(Dispatchers.IO) {
        require(event.title.isNotBlank()) { "Nama ujian atau kegiatan wajib diisi." }
        require(event.type in ACADEMIC_EVENT_TYPES) { "Jenis kegiatan tidak valid." }
        require(event.reminderMinutesBefore in 0..180) { "Waktu pengingat tidak valid." }
        require(event.scheduledAtMillis > 0L) { "Tanggal kegiatan tidak valid." }
        academicEventDao.update(event)
    }

    suspend fun deleteAcademicEvent(event: AcademicEventEntity) = withContext(Dispatchers.IO) {
        academicEventDao.delete(event)
    }
}
