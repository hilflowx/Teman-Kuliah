package com.temankuliah.app.ui.academic

import com.temankuliah.app.ui.components.IndonesianLocale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Jumlah minggu perkuliahan dalam satu semester yang dipakai untuk menghitung progres. */
private const val LECTURE_WEEKS = 16

/** Progres perkuliahan semester aktif, dipakai bersama oleh halaman Akademik dan Semester Aktif. */
internal data class SemesterProgress(
    val fraction: Float,
    val caption: String,
    val formattedStartDate: String?
) {
    val percent: Int get() = (fraction * 100).toInt()
}

internal fun semesterProgressOf(startDateIso: String?, today: LocalDate = LocalDate.now()): SemesterProgress {
    val startDate = startDateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val formatted = startDate?.format(DateTimeFormatter.ofPattern("d MMMM yyyy", IndonesianLocale))
    if (startDate == null) return SemesterProgress(0f, "Tanggal mulai kuliah belum diatur", null)

    val daysSince = ChronoUnit.DAYS.between(startDate, today)
    val week = if (daysSince < 0) 0 else ((daysSince / 7) + 1).toInt()
    val fraction = when {
        week <= 0 -> 0f
        week >= LECTURE_WEEKS -> 1f
        else -> week / LECTURE_WEEKS.toFloat()
    }
    val caption = when {
        week == 0 -> "Perkuliahan belum dimulai (mulai $formatted)"
        week <= LECTURE_WEEKS -> "Minggu ke-$week dari $LECTURE_WEEKS minggu perkuliahan"
        else -> "$LECTURE_WEEKS minggu perkuliahan telah selesai"
    }
    return SemesterProgress(fraction, caption, formatted)
}
