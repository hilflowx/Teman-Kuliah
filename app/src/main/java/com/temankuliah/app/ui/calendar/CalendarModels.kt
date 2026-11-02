package com.temankuliah.app.ui.calendar

import androidx.compose.ui.graphics.Color
import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.components.IndonesianLocale
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Tiga jenis agenda pada kalender Jadwal. Warna titik, chip filter, dan garis tepi kartu mengikuti
 * desain: Kuliah biru, Tugas oranye, Ujian merah. [ink] dipakai untuk teks di atas [soft] agar kontras terbaca.
 */
internal enum class AgendaKind(
    val label: String,
    private val lightAccent: Color,
    private val lightSoft: Color,
    private val lightInk: Color,
    private val darkAccent: Color,
    private val darkSoft: Color,
    private val darkInk: Color
) {
    Kuliah(
        "Kuliah",
        Color(0xFF2F80ED), Color(0xFFEAF3FF), Color(0xFF1858B8),
        Color(0xFF4C9AFF), Color(0xFF1A2A48), Color(0xFF8CC4FF)
    ),
    Tugas(
        "Tugas",
        Color(0xFFF2994A), Color(0xFFFFF0E3), Color(0xFF9A3D00),
        Color(0xFFF2994A), Color(0xFF3A2412), Color(0xFFFFA566)
    ),
    Ujian(
        "Ujian",
        Color(0xFFE5484D), Color(0xFFFEE2E2), Color(0xFFB91C1C),
        Color(0xFFFF6B70), Color(0xFF3D1A20), Color(0xFFFF8A8A)
    );

    // Dibaca saat komposisi, jadi ikut berganti saat tema berganti.
    val accent: Color get() = tkAdaptive(lightAccent, darkAccent)
    val soft: Color get() = tkAdaptive(lightSoft, darkSoft)
    val ink: Color get() = tkAdaptive(lightInk, darkInk)
}

/** Warna kategori "Kegiatan" (hijau), pelengkap [AgendaKind]. */
internal object ActivityKindColors {
    val accent: Color get() = tkAdaptive(Color(0xFF10B981), Color(0xFF34D399))
    val soft: Color get() = tkAdaptive(Color(0xFFD1FAE5), Color(0xFF12352A))
    val ink: Color get() = tkAdaptive(Color(0xFF047857), Color(0xFF5FE0A8))
}

/** Menentukan kategori agenda untuk pewarnaan dan pengelompokan. */
internal fun agendaKindOf(type: String): AgendaKind = when (type) {
    "Kuliah" -> AgendaKind.Kuliah
    "Tugas" -> AgendaKind.Tugas
    else -> AgendaKind.Ujian
}

/** Teks lencana pada kartu agenda. */
internal fun agendaBadgeLabel(type: String): String = when {
    type.contains("Kegiatan", ignoreCase = true) -> "Kegiatan"
    else -> agendaKindOf(type).label
}

internal data class CalendarUiEvent(
    val key: String,
    val title: String,
    val detail: String,
    val type: String, // "Kuliah", "Tugas", "Ujian", "Kegiatan Akademik"
    val timestamp: Long,
    /** Jam di kolom kiri kartu, misalnya jam mulai kuliah atau jam tenggat. */
    val startLabel: String,
    /** Baris kedua kolom kiri: jam selesai kuliah, atau "tenggat" / "mulai". */
    val captionLabel: String,
    val isOverdue: Boolean = false,
    val onOpen: () -> Unit,
    val onDelete: (() -> Unit)? = null,
    val completed: Boolean? = null,
    val onToggleCompleted: ((Boolean) -> Unit)? = null
)

private val DayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", IndonesianLocale)
private val DayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale)

internal val ShortDayNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

/** Label rentang minggu, misalnya "19 – 25 Okt 2026" atau "28 Sep – 4 Okt 2026" saat melewati batas bulan. */
internal fun weekRangeLabel(first: LocalDate, last: LocalDate): String = when {
    first.year != last.year -> "${first.format(DayMonthYearFormatter)} – ${last.format(DayMonthYearFormatter)}"
    first.month != last.month -> "${first.format(DayMonthFormatter)} – ${last.format(DayMonthYearFormatter)}"
    else -> "${first.dayOfMonth} – ${last.format(DayMonthYearFormatter)}"
}

/** Keterangan jumlah agenda di bawah tanggal terpilih. */
internal fun agendaCountLabel(count: Int, isToday: Boolean): String =
    if (isToday) "$count agenda hari ini" else "$count agenda"
