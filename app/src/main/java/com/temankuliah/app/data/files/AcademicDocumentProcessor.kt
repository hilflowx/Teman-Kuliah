package com.temankuliah.app.data.files

import com.temankuliah.app.domain.GpaCalculator
import java.io.File

/**
 * On-device extraction; no document contents are sent to a server.
 *
 * Objek ini hanya pintu masuk: tipe data dan API publik tetap di sini, sedangkan logikanya dipisah per tanggung jawab:
 * [AcademicOcrReader] (baca berkas/OCR), [AcademicRowParser] (pengurai baris), [KrsRowParser], [KhsRowParser],
 * [AcademicScheduleText] (jadwal/jam), [AcademicNameText] (kode/nama/dosen), dan [AcademicTextPatterns] (pola regex bersama).
 */
object AcademicDocumentProcessor {
    data class Extraction(val text: String, val message: String = "")

    /**
     * Satu baris tabel KRS/KHS.
     * KRS memakai [code], [courseName], [lecturer], [schedule]; KHS memakai [code], [courseName],
     * [credits], [grade], [gradePoint], [qualityPoints]. Nilai yang tidak ditemukan di dokumen
     * dibiarkan kosong ("" / 0 / "-") dan tidak ditebak.
     */
    data class AcademicRow(
        val courseName: String,
        val credits: Int,
        val grade: String = "-",
        val no: Int = 1,
        val code: String = "",
        val gradePoint: Double = gradeLetterToPoint(grade),
        val qualityPoints: Double = credits * gradeLetterToPoint(grade),
        val lecturer: String = "",
        val schedule: String = "",
        /** KRS: SKS teori dan praktek (kolom TEORI / PRAKTEK). SKS total = teori + praktek. */
        val theory: Int = 0,
        val practice: Int = 0,
        /** KRS: kolom HARI, LAMA (menit), MULAI, dan SEL(esai) dengan format HH:mm. */
        val day: String = "",
        val lama: Int = 0,
        val start: String = "",
        val end: String = ""
    )

    /** Total yang tercetak pada baris "Jumlah SKS" di dasar tabel KRS; null bila kolom tidak terbaca. */
    data class KrsTotals(val theory: Int?, val practice: Int?, val sks: Int?)

    /** Hari, jam, dan lama kuliah (menit) hasil membaca teks jadwal seperti "Senin 08:00-10:30". */
    data class ScheduleParts(val day: String, val start: String, val end: String, val lamaMinutes: Int)

    /** Kata hasil OCR beserta kotak pembatasnya, dipakai untuk menyusun ulang baris tabel. */
    data class OcrWord(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

    /** Teks jadwal baku "Hari HH:mm-HH:mm"; bagian yang kosong dilewati. */
    fun buildSchedule(day: String, start: String, end: String): String = AcademicScheduleText.buildSchedule(day, start, end)

    /** Teks yang disimpan sebagai jam kuliah mata kuliah: penanda [T:x, P:y] dipertahankan agar kolom Teori/Praktek ikut tersimpan. */
    fun classTimeForStorage(row: AcademicRow): String {
        val schedule = row.schedule.trim()
        val tag = if (row.theory + row.practice > 0) "[T:${row.theory}, P:${row.practice}]" else ""
        return listOf(tag, schedule).filter { it.isNotBlank() }.joinToString(" ")
    }

    /** Pisahkan teks jadwal menjadi hari, mulai, selesai, dan lama (menit). */
    fun parseScheduleParts(text: String): ScheduleParts = AcademicScheduleText.parseScheduleParts(text)

    /** Selisih menit antara dua jam "HH:mm"; 0 bila salah satunya kosong atau selesai tidak setelah mulai. */
    fun minutesBetween(start: String, end: String): Int = AcademicScheduleText.minutesBetween(start, end)

    /** Terima "8.00", "08:00", atau "08.00 WIB" lalu kembalikan "08:00"; null bila bukan jam yang valid. */
    fun normalizeTime(text: String): String? = AcademicScheduleText.normalizeTime(text)

    /** Satu sumber bobot nilai: [GpaCalculator]. */
    fun gradeLetterToPoint(letter: String): Double = GpaCalculator.gradeLetterToPoint(letter)

    suspend fun extract(file: File): Extraction = AcademicOcrReader.extract(file)

    /**
     * Mengelompokkan kata OCR menjadi baris berdasarkan posisi vertikal, lalu mengurutkannya dari
     * kiri ke kanan. Jarak horizontal yang lebar dianggap batas kolom dan ditulis sebagai TAB,
     * sehingga sel tabel tidak tercampur dengan sel di kolom lain.
     */
    fun layoutRows(words: List<OcrWord>): String = AcademicOcrReader.layoutRows(words)

    /**
     * Membaca baris mata kuliah dari teks hasil OCR, PDF, atau teks yang disalin.
     * - KRS ([isKrs] = true): kode, nama mata kuliah, dosen, dan jam kuliah.
     * - KHS: kode, nama mata kuliah, SKS, nilai (bobot dan mutu dihitung dari nilai).
     * Bagian yang tidak cocok dengan kolom tabel dibiarkan kosong.
     */
    fun findAcademicRows(text: String, isKrs: Boolean = false): List<AcademicRow> = AcademicRowParser.findAcademicRows(text, isKrs)

    /**
     * Baca baris "Jumlah SKS" di dasar tabel KRS. Satu angka = total SKS; dua angka = total teori dan
     * praktek (SKS = jumlahnya); tiga angka atau lebih = teori, praktek, lalu total SKS.
     */
    fun findKrsTotals(text: String): KrsTotals? = KrsRowParser.findKrsTotals(text)

    /**
     * Pulihkan nilai huruf dari Bobot dan/atau Mutu yang tertera di dokumen. Mengembalikan "" bila
     * tidak pasti (tidak menebak): satu angka yang bisa berarti bobot sekaligus mutu dibiarkan kosong.
     */
    internal fun gradeFromNumbers(credits: Int, decimals: List<Double>): String = KhsRowParser.gradeFromNumbers(credits, decimals)

    /**
     * Mendeteksi nomor semester (1..14) dari teks dokumen KRS/KHS (angka, angka romawi, atau kata).
     * Mengembalikan null jika nomor semester tidak tertera dengan jelas.
     */
    fun extractSemesterNumber(text: String): Int? {
        if (text.isBlank()) return null
        // 1. Angka langsung: "Semester 3", "Semester: 3", "Semester ke-3", "Smtr 3", "Sem. 3"
        val digitRegex = Regex("""(?i)\b(?:semester|smtr|sem)\.?\s*(?:ke\s*[-:]?\s*|[:=-]\s*)?([1-9]|1[0-4])\b""")
        digitRegex.find(text)?.let {
            val num = it.groupValues[1].toIntOrNull()
            if (num != null && num in 1..14) return num
        }
        // 2. Angka Romawi: "Semester III", "Semester: IV", "Sem. VI"
        val romanRegex = Regex("""(?i)\b(?:semester|smtr|sem)\.?\s*[:=-]?\s*(I{1,3}|IV|V|VI{1,3}|IX|X|XI{1,3}|XIV)\b""")
        romanRegex.find(text)?.let {
            val roman = it.groupValues[1].uppercase(java.util.Locale.ROOT)
            val value = when (roman) {
                "I" -> 1; "II" -> 2; "III" -> 3; "IV" -> 4; "V" -> 5
                "VI" -> 6; "VII" -> 7; "VIII" -> 8; "IX" -> 9; "X" -> 10
                "XI" -> 11; "XII" -> 12; "XIII" -> 13; "XIV" -> 14
                else -> null
            }
            if (value != null) return value
        }
        // 3. Kata teks: "Semester Tiga", "Semester Satu"
        val wordRegex = Regex("""(?i)\bsemester\s*[:=-]?\s*(satu|dua|tiga|empat|lima|enam|tujuh|delapan)\b""")
        wordRegex.find(text)?.let {
            val word = it.groupValues[1].lowercase(java.util.Locale.ROOT)
            val value = when (word) {
                "satu" -> 1; "dua" -> 2; "tiga" -> 3; "empat" -> 4
                "lima" -> 5; "enam" -> 6; "tujuh" -> 7; "delapan" -> 8
                else -> null
            }
            if (value != null) return value
        }
        return null
    }

    /**
     * Mendeteksi tahun akademik / tahun ajaran dari teks dokumen, misalnya "2023/2024".
     */
    fun extractAcademicYear(text: String): String? {
        if (text.isBlank()) return null
        val yearRegex = Regex("""\b(20\d{2})\s*[/–-]\s*(20\d{2})\b""")
        return yearRegex.find(text)?.let { "${it.groupValues[1]}/${it.groupValues[2]}" }
    }
}

