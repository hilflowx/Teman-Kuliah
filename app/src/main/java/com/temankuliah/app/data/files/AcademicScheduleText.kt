package com.temankuliah.app.data.files

import com.temankuliah.app.data.files.AcademicDocumentProcessor.ScheduleParts
import com.temankuliah.app.data.files.AcademicTextPatterns.DAY_REGEX
import com.temankuliah.app.data.files.AcademicTextPatterns.SINGLE_TIME_ANYWHERE
import com.temankuliah.app.data.files.AcademicTextPatterns.TIME_RANGE
import com.temankuliah.app.data.files.AcademicTextPatterns.WHITESPACE
import java.util.Locale

/** Pembacaan dan penyusunan teks jadwal (hari, jam, lama kuliah) untuk KRS/KHS. */
internal object AcademicScheduleText {
    /** Teks jadwal baku "Hari HH:mm-HH:mm"; bagian yang kosong dilewati. */
    fun buildSchedule(day: String, start: String, end: String): String {
        val time = when {
            start.isNotBlank() && end.isNotBlank() -> "$start-$end"
            else -> start
        }
        return listOf(day, time).filter { it.isNotBlank() }.joinToString(" ")
    }

    /** Pisahkan teks jadwal menjadi hari, mulai, selesai, dan lama (menit). */
    fun parseScheduleParts(text: String): ScheduleParts {
        val day = DAY_REGEX.find(text)?.let { canonicalDay(it.value) }.orEmpty()
        val match = TIME_RANGE.find(text)
        val start = match?.let { String.format(Locale.US, "%02d:%s", it.groupValues[1].toInt(), it.groupValues[2]) }.orEmpty()
        val end = match?.let { String.format(Locale.US, "%02d:%s", it.groupValues[3].toInt(), it.groupValues[4]) }.orEmpty()
        return ScheduleParts(day, start, end, minutesBetween(start, end))
    }

    /** Selisih menit antara dua jam "HH:mm"; 0 bila salah satunya kosong atau selesai tidak setelah mulai. */
    fun minutesBetween(start: String, end: String): Int {
        val a = timeToMinutes(start) ?: return 0
        val b = timeToMinutes(end) ?: return 0
        return (b - a).coerceAtLeast(0)
    }

    /** Terima "8.00", "08:00", atau "08.00 WIB" lalu kembalikan "08:00"; null bila bukan jam yang valid. */
    fun normalizeTime(text: String): String? {
        val m = SINGLE_TIME.find(text.trim()) ?: return null
        return String.format(Locale.US, "%02d:%s", m.groupValues[1].toInt(), m.groupValues[2])
    }

    fun addMinutes(time: String, minutes: Int): String {
        val base = timeToMinutes(time) ?: return ""
        val total = base + minutes
        if (total !in 0 until 24 * 60) return ""
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
    }

    /** "2 Jam 30 Menit" -> 150, "1 Jam" -> 60, "90 Menit" -> 90; 0 bila teks bukan durasi. */
    internal fun parseLamaMinutes(text: String): Int {
        val match = LAMA_JAM_MENIT.find(text) ?: return 0
        val hours = match.groupValues[1].toIntOrNull()
        if (hours != null) return hours * 60 + (match.groupValues[2].toIntOrNull() ?: 0)
        return match.groupValues[3].toIntOrNull() ?: 0
    }

    internal data class KrsTime(val start: String, val end: String, val lama: Int)

    /**
     * Baca kolom LAMA, MULAI, dan SELESAI dari sisa baris setelah nama hari. LAMA boleh berupa menit
     * ("150") atau "2 Jam 30 Menit"; bagian yang tidak terbaca dilengkapi dari dua lainnya, tidak ditebak.
     */
    internal fun readKrsTime(rest: String): KrsTime {
        val times = SINGLE_TIME_ANYWHERE.findAll(rest).map {
            String.format(Locale.US, "%02d:%s", it.groupValues[1].toInt(), it.groupValues[2])
        }.toList()
        val withoutTimes = SINGLE_TIME_ANYWHERE.replace(rest, " ")
        var lama = parseLamaMinutes(withoutTimes)
        if (lama == 0) lama = LAMA_TOKEN.find(withoutTimes)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        var start = times.getOrNull(0).orEmpty()
        var end = times.getOrNull(1).orEmpty()
        if (start.isNotBlank() && end.isBlank() && lama in 30..600) end = addMinutes(start, lama)
        if (start.isBlank() && end.isNotBlank() && lama in 30..600) start = addMinutes(end, -lama)
        if (lama == 0 && start.isNotBlank() && end.isNotBlank()) lama = minutesBetween(start, end)
        return KrsTime(start, end, lama)
    }

    private fun timeToMinutes(time: String): Int? {
        val m = Regex("^(\\d{1,2}):(\\d{2})$").find(time.trim()) ?: return null
        return m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
    }

    /** Ambil "Hari HH:mm-HH:mm" dari teks; kembalikan jadwal terformat dan sisa teksnya. */
    internal fun extractSchedule(input: String): Pair<String, String> {
        val timeMatch = TIME_RANGE.find(input)
        val dayMatch = DAY_REGEX.find(input)
        if (timeMatch == null && dayMatch == null) return "" to input
        var remaining = input
        var time = ""
        if (timeMatch != null) {
            val g = timeMatch.groupValues
            time = String.format(Locale.US, "%02d:%s-%02d:%s", g[1].toInt(), g[2], g[3].toInt(), g[4])
            remaining = TIME_RANGE.replaceFirst(remaining, " ")
        }
        var day = ""
        if (dayMatch != null) {
            day = canonicalDay(dayMatch.value)
            remaining = DAY_REGEX.replaceFirst(remaining, " ")
        }
        if (time.isBlank()) return day to remaining.replace(WHITESPACE, " ").trim(' ', ',', ';', '/', '-')
        val schedule = listOf(day, time).filter { it.isNotBlank() }.joinToString(" ")
        return schedule to remaining.replace(WHITESPACE, " ").trim(' ', ',', ';', '/', '-')
    }

    internal fun canonicalDay(value: String): String = when (value.trim(' ', ',', ':').lowercase(Locale.ROOT).replace("’", "'")) {
        "senin" -> "Senin"
        "selasa" -> "Selasa"
        "rabu" -> "Rabu"
        "kamis" -> "Kamis"
        "jumat", "jum'at" -> "Jumat"
        "sabtu" -> "Sabtu"
        "minggu", "ahad" -> "Minggu"
        else -> value.trim()
    }

    private val SINGLE_TIME = Regex("^([01]?\\d|2[0-3])[.:]([0-5]\\d)(?:\\s*WI[BTA])?$", RegexOption.IGNORE_CASE)

    private val LAMA_TOKEN = Regex("(?<![\\d:.])(\\d{2,3})(?![\\d:.])")
    private val LAMA_JAM_MENIT = Regex("(?i)\\b(\\d{1,2})\\s*jam(?:\\s*(\\d{1,2})\\s*menit)?\\b|\\b(\\d{1,3})\\s*menit\\b")
}
