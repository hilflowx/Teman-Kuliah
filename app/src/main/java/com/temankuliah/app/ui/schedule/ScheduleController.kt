package com.temankuliah.app.ui.schedule

import com.temankuliah.app.domain.ScheduleDraft
import com.temankuliah.app.domain.ScheduleRules
import com.temankuliah.app.domain.parseSemesterStartDate
import com.temankuliah.app.domain.isRecurring
import com.temankuliah.app.domain.rethrowIfCancellation
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.ui.FeatureContext
import com.temankuliah.app.ui.FeatureController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Cakupan perubahan untuk jadwal berulang: hanya satu pertemuan, atau seluruh seri. */
internal enum class ScheduleEditScope { ONLY_THIS, ALL }

/** Baris yang baru dihapus, disimpan sementara agar Snackbar "Urungkan" bisa mengembalikannya. */
internal data class UndoDelete(val token: Long, val message: String, val rows: List<ScheduleEntity>)

internal class ScheduleController(
    ctx: FeatureContext,
    private val semesterStartDates: StateFlow<Map<Int, String>>
) : FeatureController(ctx) {
    // Data mentah tetap tersedia untuk operasi hapus, sedangkan tampilan hanya menerima jadwal
    // semester yang tanggal mulai perkuliahannya sudah ditentukan.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allSchedules: StateFlow<List<ScheduleEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getSchedulesFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val schedules: StateFlow<List<ScheduleEntity>> = combine(allSchedules, semesterStartDates) { all, dates ->
        all.filter { schedule ->
            parseSemesterStartDate(dates[schedule.semester]) != null
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Semester yang sedang dilihat di halaman Jadwal; null berarti memakai semester aktif. */
    val viewedSemester = MutableStateFlow<Int?>(null)

    // Next upcoming / today's class on Dashboard
    val todaySchedules: StateFlow<List<ScheduleEntity>> = combine(schedules, activeSemester) { all, semester ->
        val today = LocalDate.now().toString()
        all.filter { it.scheduledDate == today && it.semester == semester }.sortedBy { it.startTime }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Schedule Actions ---
    fun setScheduleCompleted(scheduleId: Long, completed: Boolean) {
        scope.launch {
            try {
                val schedule = schedules.value.firstOrNull { it.id == scheduleId }
                repository.setScheduleCompleted(scheduleId, completed)
                snackbar.value = when {
                    !completed -> "Status perkuliahan diubah menjadi belum selesai."
                    schedule != null && schedule.recurrenceGroupId.isBlank() ->
                        "Jadwal sekali ditandai selesai: disembunyikan dari daftar Jadwal dan tetap tersimpan di Kalender."
                    else -> "Perkuliahan ditandai selesai dan tetap tersimpan di kalender."
                }
                if (schedule != null) ReminderScheduler.scheduleClass(application, schedule.copy(isCompleted = completed))
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal mengubah status perkuliahan."
            }
        }
    }

    // --- Dialog Tambah/Edit dan penghapusan (halaman Jadwal kuliah) ---

    private val _pendingUndo = MutableStateFlow<UndoDelete?>(null)

    /** Penghapusan terakhir yang masih bisa diurungkan; layar Jadwal menampilkannya sebagai Snackbar. */
    val pendingUndo: StateFlow<UndoDelete?> = _pendingUndo
    private var undoCounter = 0L

    private fun semesterStartOf(semester: Int): LocalDate? =
        parseSemesterStartDate(semesterStartDates.value[semester])

    private fun scheduleReminders(rows: List<ScheduleEntity>) {
        rows.forEach { runCatching { ReminderScheduler.scheduleClass(application, it) } }
    }

    private fun cancelReminders(rows: List<ScheduleEntity>) {
        rows.forEach { runCatching { ReminderScheduler.cancelClass(application, it.id) } }
    }

    /** Menyimpan jadwal baru dari dialog. Jadwal Berulang menjadi satu seri per hari yang dipilih. */
    fun saveNew(draft: ScheduleDraft) {
        val user = currentUser.value ?: return
        scope.launch {
            try {
                val semester = viewedSemester.value ?: user.activeSemester
                val semesterStart = semesterStartOf(semester)
                if (semesterStart == null) {
                    snackbar.value = "Tentukan tanggal mulai perkuliahan Semester $semester sebelum menambahkan jadwal."
                    return@launch
                }
                ScheduleRules.validate(draft, semesterStart)?.let { snackbar.value = it; return@launch }
                val rows = ScheduleRules.buildRows(draft, user.id, semester, semesterStart)
                val saved = repository.schedules.addSchedules(rows)
                scheduleReminders(saved)
                closeDialog()
                snackbar.value = if (saved.size == 1) "Jadwal '${draft.courseName.trim()}' berhasil ditambahkan."
                else "${saved.size} pertemuan '${draft.courseName.trim()}' berhasil ditambahkan."
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal menambahkan jadwal."
            }
        }
    }

    /**
     * Menyimpan hasil edit. [ScheduleEditScope.ONLY_THIS] hanya mengubah pertemuan [original]; [ScheduleEditScope.ALL]
     * membuat ulang seluruh seri dari isian (pertemuan bertanggal sama tetap memakai baris lamanya).
     */
    fun saveEdit(original: ScheduleEntity, editScope: ScheduleEditScope, draft: ScheduleDraft) {
        scope.launch {
            try {
                val semester = original.semester
                val semesterStart = semesterStartOf(semester)
                ScheduleRules.validate(draft, semesterStart)?.let { snackbar.value = it; return@launch }
                val classType = original.type.replace("Online · ", "").replace("Offline · ", "").trim()
                    .ifBlank { ScheduleRules.DEFAULT_CLASS_TYPE }
                val singleEdit = !draft.repeating && (editScope == ScheduleEditScope.ONLY_THIS || !original.isRecurring)
                if (singleEdit) {
                    val built = ScheduleRules.buildRows(draft, original.userId, semester, semesterStart, classType).single()
                    val updated = built.copy(id = original.id, isCompleted = original.isCompleted, recurrenceGroupId = original.recurrenceGroupId)
                    repository.schedules.updateSchedule(updated)
                    runCatching { ReminderScheduler.scheduleClass(application, updated) }
                } else {
                    val seriesRows = if (original.isRecurring && editScope == ScheduleEditScope.ALL) {
                        allSchedules.value.filter { it.userId == original.userId && it.recurrenceGroupId == original.recurrenceGroupId }
                    } else listOf(original)
                    val rows = ScheduleRules.buildRows(draft, original.userId, semester, semesterStart, classType) { day ->
                        if (original.isRecurring && day == original.dayOfWeek) original.recurrenceGroupId else UUID.randomUUID().toString()
                    }
                    val result = repository.schedules.replaceRows(seriesRows.map { it.id }.ifEmpty { listOf(original.id) }, rows)
                    cancelReminders(result.deleted)
                    scheduleReminders(result.updated + result.inserted)
                }
                closeDialog()
                snackbar.value = "Jadwal berhasil diperbarui."
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal memperbarui jadwal."
            }
        }
    }

    private fun offerUndo(rows: List<ScheduleEntity>, message: String) {
        if (rows.isEmpty()) return
        undoCounter += 1
        _pendingUndo.value = UndoDelete(undoCounter, message, rows)
    }

    /** Menghapus satu pertemuan, lalu menawarkan Urungkan. */
    fun deleteOne(scheduleId: Long) {
        scope.launch {
            try {
                val rows = repository.schedules.deleteAndReturn(scheduleId)
                cancelReminders(rows)
                offerUndo(rows, "Jadwal '${rows.firstOrNull()?.courseName.orEmpty()}' dihapus.")
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal menghapus jadwal."
            }
        }
    }

    /** Menghapus semua pengulangan satu seri. */
    fun deleteSeries(groupId: String) {
        val user = currentUser.value ?: return
        scope.launch {
            try {
                val rows = repository.schedules.deleteGroupAndReturn(user.id, groupId)
                cancelReminders(rows)
                offerUndo(rows, "${rows.size} pertemuan '${rows.firstOrNull()?.courseName.orEmpty()}' dihapus.")
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal menghapus jadwal berulang."
            }
        }
    }

    /** Menghapus semua jadwal pada semester terpilih (termasuk yang berasal dari KRS). */
    fun deleteAllForSemester(semester: Int) {
        val user = currentUser.value ?: return
        scope.launch {
            try {
                val rows = repository.schedules.deleteSemesterAndReturn(user.id, semester)
                cancelReminders(rows)
                if (rows.isEmpty()) snackbar.value = "Tidak ada jadwal kuliah yang perlu dihapus."
                else offerUndo(rows, "${rows.size} jadwal Semester $semester dihapus.")
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal menghapus seluruh jadwal kuliah."
            }
        }
    }

    /** Mengembalikan baris yang baru dihapus (aksi "Urungkan" pada Snackbar). */
    fun undoDelete(undo: UndoDelete) {
        scope.launch {
            try {
                repository.schedules.restore(undo.rows)
                scheduleReminders(undo.rows)
                snackbar.value = "Jadwal dikembalikan."
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                snackbar.value = error.message ?: "Gagal mengembalikan jadwal."
            }
        }
    }

    /** Menandai Snackbar [token] sudah selesai ditampilkan (diurungkan, hilang sendiri, atau digantikan). */
    fun consumeUndo(token: Long) {
        if (_pendingUndo.value?.token == token) _pendingUndo.value = null
    }
}
