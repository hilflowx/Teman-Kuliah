package com.temankuliah.app.domain

import com.temankuliah.app.data.local.SCHEDULE_MODE_OFFLINE
import com.temankuliah.app.data.local.SCHEDULE_MODE_ONLINE
import com.temankuliah.app.data.local.SCHEDULE_SOURCE_KRS
import com.temankuliah.app.data.local.SCHEDULE_SOURCE_MANUAL
import com.temankuliah.app.data.local.ScheduleEntity
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

/** Isian dialog Tambah/Edit jadwal, sebelum diubah menjadi baris database. */
data class ScheduleDraft(
    val sumber: String,
    val krsId: Long?,
    val courseName: String,
    val lecturer: String,
    val sks: Int,
    val repeating: Boolean,
    /** Tanggal untuk Jadwal Sekali. */
    val date: LocalDate?,
    /** Hari ulang (1 = Senin ... 7 = Minggu) untuk Jadwal Berulang. */
    val repeatDays: Set<Int>,
    val rangeStart: LocalDate?,
    val rangeEnd: LocalDate?,
    val untilSemesterEnd: Boolean,
    val startTime: String,
    val endTime: String,
    val mode: String,
    val room: String,
    val platform: String,
    val meetingLink: String,
    val notes: String,
    val alarmEnabled: Boolean,
    val reminderMinutes: Int
)

/** Ringkasan satu seri Jadwal Berulang yang dibaca dari baris-barisnya. */
data class SeriesInfo(val days: Set<Int>, val start: LocalDate, val end: LocalDate, val untilSemesterEnd: Boolean)

/** Jadwal yang bertabrakan dengan jadwal yang sedang dibuat atau diubah. */
data class ScheduleConflict(val courseName: String, val dayOfWeek: Int, val startTime: String, val endTime: String)

/**
 * Satu kartu di daftar: seluruh pertemuan satu seri pada hari dan jam yang sama digabung, sedangkan
 * Jadwal Sekali tampil satu kartu per baris. [representative] adalah pertemuan terdekat (yang akan datang,
 * atau yang terakhir bila semuanya sudah lewat).
 */
data class ScheduleSlot(val key: String, val representative: ScheduleEntity, val rows: List<ScheduleEntity>) {
    val isRecurring: Boolean get() = representative.recurrenceGroupId.isNotBlank()
}

/** Aturan murni jadwal kuliah (tanpa Android) supaya mudah diuji. */
object ScheduleRules {
    const val TIPE_SEKALI = "SEKALI"
    const val TIPE_BERULANG = "BERULANG"
    const val DEFAULT_CLASS_TYPE = "Kuliah Teori"

    /** Rentang Jadwal Berulang terpanjang (hari) dan batas jumlah pertemuan yang dibuat sekaligus. */
    const val MAX_RANGE_DAYS = 366
    const val MAX_ROWS = 400
    const val MAX_SKS = 30

    private val TIME_PATTERN = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
    private val ID_LOCALE = Locale.forLanguageTag("id-ID")

    /** Hari terakhir semester: 16 minggu penuh sejak [semesterStart] (sesuai [ScheduleRecurrence.MAX_MEETINGS]). */
    fun semesterEnd(semesterStart: LocalDate): LocalDate =
        semesterStart.plusWeeks(ScheduleRecurrence.MAX_MEETINGS.toLong()).minusDays(1)

    /** Semua tanggal berhari [dayOfWeek] dari [start] sampai [end] (keduanya inklusif). */
    fun datesFor(dayOfWeek: Int, start: LocalDate, end: LocalDate): List<LocalDate> {
        if (dayOfWeek !in 1..7 || end.isBefore(start)) return emptyList()
        var current = start.plusDays(((dayOfWeek - start.dayOfWeek.value + 7) % 7).toLong())
        val dates = ArrayList<LocalDate>()
        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusWeeks(1)
        }
        return dates
    }

    fun isClock(value: String): Boolean = TIME_PATTERN.matches(value.trim())

    fun minutesOf(time: String): Int? {
        if (!isClock(time)) return null
        val parts = time.trim().split(':')
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    /** Durasi dalam menit; 0 bila jam tidak valid atau selesai tidak setelah mulai. */
    fun durationMinutes(start: String, end: String): Int {
        val s = minutesOf(start) ?: return 0
        val e = minutesOf(end) ?: return 0
        return (e - s).coerceAtLeast(0)
    }

    /** URL rapat yang sah: http/https dengan host berbentuk domain. Tanpa skema atau berspasi ditolak. */
    fun isValidMeetingUrl(value: String): Boolean {
        val text = value.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return false
        val uri = runCatching { java.net.URI(text) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host ?: return false
        return host.contains('.') && !host.startsWith('.') && !host.endsWith('.')
    }

    /** "7,5 jam", "2 jam", "0 jam". */
    fun formatHours(minutes: Int): String =
        if (minutes % 60 == 0) "${minutes / 60} jam" else String.format(ID_LOCALE, "%.1f jam", minutes / 60.0)

    /** Tanggal pertemuan per hari ulang; Jadwal Sekali menghasilkan satu tanggal. Kosong bila isian belum lengkap. */
    fun occurrenceDates(draft: ScheduleDraft, semesterStart: LocalDate?): Map<Int, List<LocalDate>> {
        if (!draft.repeating) {
            val date = draft.date ?: return emptyMap()
            return mapOf(date.dayOfWeek.value to listOf(date))
        }
        val start = draft.rangeStart ?: return emptyMap()
        val end = effectiveEnd(draft, semesterStart) ?: return emptyMap()
        return draft.repeatDays.sorted().associateWith { datesFor(it, start, end) }
    }

    private fun effectiveEnd(draft: ScheduleDraft, semesterStart: LocalDate?): LocalDate? =
        if (draft.untilSemesterEnd) semesterStart?.let { semesterEnd(it) } else draft.rangeEnd

    /** Pesan kesalahan pertama untuk isian, atau null bila boleh disimpan. */
    fun validate(draft: ScheduleDraft, semesterStart: LocalDate?): String? {
        if (draft.courseName.isBlank()) return "Nama mata kuliah wajib diisi."
        if (draft.sks !in 0..MAX_SKS) return "SKS harus antara 0 dan $MAX_SKS."
        val start = minutesOf(draft.startTime) ?: return "Jam mulai harus menggunakan format HH:mm."
        val end = minutesOf(draft.endTime) ?: return "Jam selesai harus menggunakan format HH:mm."
        if (end <= start) return "Jam selesai harus lebih besar dari jam mulai."
        if (draft.mode != SCHEDULE_MODE_OFFLINE && draft.mode != SCHEDULE_MODE_ONLINE) return "Mode kuliah tidak valid."
        if (draft.mode == SCHEDULE_MODE_ONLINE && draft.meetingLink.isNotBlank() && !isValidMeetingUrl(draft.meetingLink)) {
            return "Format link tidak valid (contoh: https://meet.google.com/...)"
        }
        if (draft.reminderMinutes !in 0..180) return "Waktu pengingat tidak valid."
        if (!draft.repeating) {
            if (draft.date == null) return "Tanggal kuliah wajib dipilih."
            return null
        }
        if (draft.repeatDays.isEmpty() || draft.repeatDays.any { it !in 1..7 }) return "Pilih minimal satu hari."
        val rangeStart = draft.rangeStart ?: return "Tanggal mulai wajib dipilih."
        val rangeEnd = if (draft.untilSemesterEnd) {
            semesterStart?.let { semesterEnd(it) } ?: return "Tanggal mulai semester belum diatur."
        } else {
            draft.rangeEnd ?: return "Tanggal berakhir wajib dipilih."
        }
        if (rangeEnd.isBefore(rangeStart)) return "Tanggal berakhir tidak boleh sebelum tanggal mulai."
        if (java.time.temporal.ChronoUnit.DAYS.between(rangeStart, rangeEnd) > MAX_RANGE_DAYS) {
            return "Rentang jadwal berulang paling lama $MAX_RANGE_DAYS hari."
        }
        val perDay = occurrenceDates(draft, semesterStart)
        if (perDay.any { it.value.isEmpty() }) return "Salah satu hari yang dipilih tidak punya pertemuan dalam rentang tanggal ini."
        if (perDay.values.sumOf { it.size } > MAX_ROWS) return "Terlalu banyak pertemuan (maksimal $MAX_ROWS)."
        return null
    }

    /**
     * Mengubah isian menjadi baris pertemuan. Setiap hari ulang mendapat id seri sendiri (dari [groupIdFor]),
     * sehingga kartu "Rabu" dan "Kamis" bisa diubah atau dihapus terpisah. Jadwal Sekali tanpa id seri.
     * Memanggil [validate] lebih dulu; kembali kosong bila isian belum sah.
     */
    fun buildRows(
        draft: ScheduleDraft,
        userId: Long,
        semester: Int,
        semesterStart: LocalDate?,
        classType: String = DEFAULT_CLASS_TYPE,
        groupIdFor: (dayOfWeek: Int) -> String = { UUID.randomUUID().toString() }
    ): List<ScheduleEntity> {
        if (validate(draft, semesterStart) != null) return emptyList()
        val online = draft.mode == SCHEDULE_MODE_ONLINE
        val krs = draft.sumber == SCHEDULE_SOURCE_KRS
        return occurrenceDates(draft, semesterStart).flatMap { (day, dates) ->
            val group = if (draft.repeating) groupIdFor(day) else ""
            dates.map { date ->
                ScheduleEntity(
                    userId = userId,
                    courseName = draft.courseName.trim(),
                    dayOfWeek = date.dayOfWeek.value,
                    startTime = draft.startTime.trim(),
                    endTime = draft.endTime.trim(),
                    room = if (online) "" else draft.room.trim(),
                    lecturer = draft.lecturer.trim(),
                    type = classType.ifBlank { DEFAULT_CLASS_TYPE },
                    semester = semester,
                    alarmEnabled = draft.alarmEnabled,
                    reminderMinutesBefore = draft.reminderMinutes,
                    scheduledDate = date.toString(),
                    recurrenceGroupId = group,
                    sumber = if (krs && draft.krsId != null) SCHEDULE_SOURCE_KRS else SCHEDULE_SOURCE_MANUAL,
                    krsId = if (krs) draft.krsId else null,
                    sks = draft.sks,
                    mode = draft.mode,
                    platform = if (online) draft.platform.trim().ifEmpty { null } else null,
                    linkMeeting = if (online) draft.meetingLink.trim().ifEmpty { null } else null,
                    catatan = draft.notes.trim().ifEmpty { null }
                )
            }
        }
    }

    /** Membaca hari, rentang tanggal, dan "sampai akhir semester" dari pertemuan-pertemuan satu seri. */
    fun seriesInfoOf(rows: List<ScheduleEntity>, semesterStart: LocalDate?): SeriesInfo? {
        val dates = rows.mapNotNull { runCatching { LocalDate.parse(it.scheduledDate) }.getOrNull() }
        if (dates.isEmpty()) return null
        val start = dates.min()
        val end = dates.max()
        val untilEnd = semesterStart != null && end.plusWeeks(1).isAfter(semesterEnd(semesterStart))
        return SeriesInfo(dates.map { it.dayOfWeek.value }.toSet(), start, end, untilEnd)
    }

    /**
     * Jadwal lain yang bertabrakan: tanggal sama dan jam saling tumpang tindih. Jadwal Sekali yang sudah
     * ditandai selesai (tidak tampil di daftar) dan baris di [ignoreIds] (jadwal yang sedang diubah) diabaikan.
     */
    fun findConflicts(existing: List<ScheduleEntity>, candidates: List<ScheduleEntity>, ignoreIds: Set<Long> = emptySet()): List<ScheduleConflict> {
        val byDate = existing
            .asSequence()
            .filter { it.id !in ignoreIds }
            .filterNot { it.isCompleted && it.recurrenceGroupId.isBlank() }
            .groupBy { it.scheduledDate }
        val found = LinkedHashSet<ScheduleConflict>()
        for (candidate in candidates) {
            val cs = minutesOf(candidate.startTime) ?: continue
            val ce = minutesOf(candidate.endTime) ?: continue
            for (other in byDate[candidate.scheduledDate].orEmpty()) {
                val os = minutesOf(other.startTime) ?: continue
                val oe = minutesOf(other.endTime) ?: continue
                if (cs < oe && os < ce) found.add(ScheduleConflict(other.courseName, other.dayOfWeek, other.startTime, other.endTime))
            }
        }
        return found.toList()
    }

    /** Menggabungkan pertemuan menjadi kartu, diurutkan menurut hari lalu jam mulai lalu nama. */
    fun buildSlots(rows: List<ScheduleEntity>, today: LocalDate): List<ScheduleSlot> {
        val todayText = today.toString()
        val grouped = LinkedHashMap<String, MutableList<ScheduleEntity>>()
        for (row in rows) {
            val key = if (row.recurrenceGroupId.isBlank()) "s|${row.id}"
            else "g|${row.recurrenceGroupId}|${row.dayOfWeek}|${row.startTime}|${row.endTime}"
            grouped.getOrPut(key) { mutableListOf() }.add(row)
        }
        return grouped.map { (key, items) ->
            val sorted = items.sortedWith(compareBy<ScheduleEntity>({ it.scheduledDate }, { it.id }))
            val representative = sorted.firstOrNull { it.scheduledDate >= todayText } ?: sorted.last()
            ScheduleSlot(key, representative, sorted)
        }.sortedWith(compareBy<ScheduleSlot>({ it.representative.dayOfWeek }, { it.representative.startTime }, { it.representative.courseName.lowercase(Locale.ROOT) }))
    }

    /** Jam tatap muka per minggu (menit): hanya kartu Offline, tiap kartu dihitung satu kali. */
    fun faceToFaceMinutesPerWeek(slots: List<ScheduleSlot>): Int =
        slots.filter { it.representative.mode != SCHEDULE_MODE_ONLINE }
            .sumOf { durationMinutes(it.representative.startTime, it.representative.endTime) }
}
