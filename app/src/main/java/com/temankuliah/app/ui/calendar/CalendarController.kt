package com.temankuliah.app.ui.calendar

import com.temankuliah.app.domain.rethrowIfCancellation
import com.temankuliah.app.data.local.AcademicEventEntity
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.domain.DEFAULT_REMINDER_MINUTES
import com.temankuliah.app.ui.FeatureContext
import com.temankuliah.app.ui.FeatureController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class CalendarController(
    ctx: FeatureContext
) : FeatureController(ctx) {
    val academicEvents: StateFlow<List<AcademicEventEntity>> = currentUser
        .flatMapLatest { user -> if (user != null) repository.getAcademicEventsFlow(user.id) else flowOf(emptyList()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Academic Calendar Events ---
    fun addAcademicEvent(
        title: String,
        description: String,
        type: String,
        scheduledAtMillis: Long,
        alarmEnabled: Boolean = false,
        reminderMinutes: Int = DEFAULT_REMINDER_MINUTES
    ) {
        val user = currentUser.value ?: return
        scope.launch {
            try {
                val event = AcademicEventEntity(
                    userId = user.id, semester = user.activeSemester, title = title.trim(), description = description.trim(),
                    type = type, scheduledAtMillis = scheduledAtMillis, alarmEnabled = alarmEnabled, reminderMinutesBefore = reminderMinutes
                )
                val id = repository.addAcademicEvent(event)
                ReminderScheduler.scheduleEvent(application, event.copy(id = id))
                snackbar.value = "$type berhasil ditambahkan ke kalender."
            } catch (error: Exception) { rethrowIfCancellation(error); snackbar.value = error.message ?: "Gagal menyimpan kegiatan akademik." }
        }
    }

    fun updateAcademicEvent(
        event: AcademicEventEntity,
        title: String,
        description: String,
        type: String,
        scheduledAtMillis: Long,
        alarmEnabled: Boolean = event.alarmEnabled,
        reminderMinutes: Int = event.reminderMinutesBefore
    ) {
        scope.launch {
            try {
                val updated = event.copy(
                    title = title.trim(), description = description.trim(), type = type, scheduledAtMillis = scheduledAtMillis,
                    alarmEnabled = alarmEnabled, reminderMinutesBefore = reminderMinutes
                )
                repository.updateAcademicEvent(updated)
                ReminderScheduler.scheduleEvent(application, updated)
                snackbar.value = "Kegiatan kalender berhasil diperbarui."
            } catch (error: Exception) { rethrowIfCancellation(error); snackbar.value = error.message ?: "Gagal memperbarui kegiatan." }
        }
    }

    fun deleteAcademicEvent(event: AcademicEventEntity) {
        scope.launch {
            runCatching { repository.deleteAcademicEvent(event); ReminderScheduler.cancelEvent(application, event.id) }
                .onSuccess { snackbar.value = "Kegiatan dihapus dari kalender." }
                .onFailure { snackbar.value = it.message ?: "Gagal menghapus kegiatan." }
        }
    }
}
