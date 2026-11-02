package com.temankuliah.app.domain

import java.time.LocalDate

/**
 * Aturan jadwal berulang mingguan. Semua tanggal dihitung dari tanggal pertemuan pertama yang
 * dipilih pengguna; tidak ada tanggal atau data bawaan.
 */
object ScheduleRecurrence {
    /** Jumlah pertemuan terbanyak dalam satu semester. */
    const val MAX_MEETINGS = 16

    /** Tanggal pertemuan ke-1 sampai ke-[meetingCount], masing-masing berselang tepat 7 hari. */
    fun weeklyDates(firstDate: LocalDate, meetingCount: Int): List<LocalDate> {
        require(meetingCount in 1..MAX_MEETINGS) { "Jumlah pertemuan harus 1 sampai $MAX_MEETINGS." }
        return List(meetingCount) { index -> firstDate.plusWeeks(index.toLong()) }
    }
}
