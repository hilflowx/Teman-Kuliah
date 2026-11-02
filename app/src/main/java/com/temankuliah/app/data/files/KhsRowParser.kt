package com.temankuliah.app.data.files

import com.temankuliah.app.data.files.AcademicDocumentProcessor.AcademicRow
import com.temankuliah.app.data.files.AcademicNameText.extractCodeAndName
import com.temankuliah.app.data.files.AcademicNameText.hasLetters
import com.temankuliah.app.data.files.AcademicNameText.normalizeCourseName
import com.temankuliah.app.data.files.AcademicNameText.normalizeOcrDigits
import com.temankuliah.app.data.files.AcademicNameText.splitCode
import com.temankuliah.app.data.files.AcademicNameText.stripLeadingIndex
import com.temankuliah.app.domain.AcademicReconciler
import com.temankuliah.app.domain.Grades
import com.temankuliah.app.domain.MAX_SKS_PER_COURSE
import java.util.Locale

/** Pembacaan baris KHS (nilai) dari teks hasil OCR atau berkas teks. */
internal object KhsRowParser {
    /**
     * Baca baris KHS dari belakang: MUTU, BOBOT, NILAI, SKS, lalu nama. Dipakai lebih dulu karena angka SKS atau
     * huruf nilai yang salah baca dapat dipulihkan dari Bobot dan Mutu yang tercetak. Mengembalikan null bila
     * baris tidak berpola itu atau SKS x Bobot tidak sama dengan Mutu, sehingga pembaca lain tetap berlaku.
     */
    internal fun parseKhsByTail(line: String): AcademicRow? {
        val (code, rest) = splitCode(line)
        val tokens = rest.split(' ').filter { it.isNotBlank() }
        val tail = AcademicReconciler.parseKhsTail(tokens) ?: return null
        val name = tokens.take(tail.nameTokenCount).joinToString(" ").trim(' ', '-', ':', ',')
        if (!hasLetters(name, 3)) return null
        return AcademicRow(courseName = normalizeCourseName(name), credits = tail.sks, grade = tail.grade, code = code)
    }

    internal fun parseKhsFreeText(line: String): AcademicRow? {
        val text = stripLeadingIndex(line)

        fun build(rawTitle: String, credits: Int, grade: String): AcademicRow? {
            val (code, name) = extractCodeAndName(rawTitle)
            if (!hasLetters(name, 3)) return null
            return AcademicRow(courseName = normalizeCourseName(name), credits = credits, grade = grade, code = code)
        }

        CREDITS_FIRST.matchEntire(text)?.let { match ->
            val credits = normalizeOcrDigits(match.groupValues[2]).toIntOrNull() ?: 0
            val grade = normalizeGradeToken(match.groupValues[3].uppercase(Locale.ROOT))
            if (credits in 1..MAX_SKS_PER_COURSE && grade in Grades.VALID) build(match.groupValues[1].trim(), credits, grade)?.let { return it }
        }
        GRADE_FIRST.matchEntire(text)?.let { match ->
            val credits = normalizeOcrDigits(match.groupValues[3]).toIntOrNull() ?: 0
            val grade = normalizeGradeToken(match.groupValues[2].uppercase(Locale.ROOT))
            if (credits in 1..MAX_SKS_PER_COURSE && grade in Grades.VALID) build(match.groupValues[1].trim(), credits, grade)?.let { return it }
        }
        // Kolom Nilai tidak terbaca tetapi SKS + Bobot/Mutu ada: nilai dipulihkan dari angka tersebut.
        NAME_CREDITS_DECIMALS.matchEntire(text)?.let { match ->
            val credits = normalizeOcrDigits(match.groupValues[2]).toIntOrNull() ?: 0
            val decimals = listOf(match.groupValues[3], match.groupValues[4]).filter { it.isNotBlank() }
                .mapNotNull { it.replace(',', '.').toDoubleOrNull() }
            val grade = gradeFromNumbers(credits, decimals)
            if (credits in 1..MAX_SKS_PER_COURSE && grade.isNotBlank()) build(match.groupValues[1].trim(), credits, grade)?.let { return it }
        }
        // Baris yang hanya memuat sebagian kolom: diterima bila diawali kode mata kuliah, sisanya kosong.
        if (splitCode(text).first.isNotBlank()) {
            NAME_CREDITS_ONLY.matchEntire(text)?.let { match ->
                val credits = normalizeOcrDigits(match.groupValues[2]).toIntOrNull() ?: 0
                if (credits in 1..MAX_SKS_PER_COURSE) build(match.groupValues[1].trim(), credits, "-")?.let { return it }
            }
            NAME_GRADE_ONLY.matchEntire(text)?.let { match ->
                build(match.groupValues[1].trim(), 0, normalizeGradeToken(match.groupValues[2].uppercase(Locale.ROOT)))?.let { return it }
            }
        }
        return null
    }

    /**
     * Perbaiki salah baca OCR yang umum pada kolom Nilai: "BT" -> "B+", "B—" -> "B-". Hasil perbaikan hanya
     * dipakai bila nilai itu sah ([Grades.VALID]); "A+", "C-", "D+", dan sejenisnya tidak ada di skala ini,
     * jadi tokennya dibiarkan apa adanya dan akan ditolak pembaca.
     */
    internal fun normalizeGradeToken(upper: String): String {
        val token = upper.trim()
        val repaired = when (token) {
            // B/8 adalah salah baca OCR yang umum di kolom nilai huruf.
            "8" -> "B"
            else -> GRADE_OCR_PLUS.matchEntire(token)?.let { it.groupValues[1] + "+" }
                ?: GRADE_OCR_MINUS.matchEntire(token)?.let { it.groupValues[1] + "-" }
        }
        return repaired?.takeIf { Grades.isValid(it) } ?: token
    }

    /**
     * Pulihkan nilai huruf dari Bobot dan/atau Mutu yang tertera di dokumen. Mengembalikan "" bila
     * tidak pasti (tidak menebak): satu angka yang bisa berarti bobot sekaligus mutu dibiarkan kosong.
     */
    internal fun gradeFromNumbers(credits: Int, decimals: List<Double>): String {
        fun letterFor(point: Double): String? = Grades.letterForPoint(point)
        if (decimals.isEmpty()) return ""
        if (credits > 0 && decimals.size >= 2) {
            for (i in 0 until decimals.size - 1) {
                val bobot = decimals[i]
                val mutu = decimals[i + 1]
                if (kotlin.math.abs(credits * bobot - mutu) < 0.05) letterFor(bobot)?.let { return it }
            }
        }
        val first = decimals.first()
        val candidates = buildSet {
            letterFor(first)?.let { add(it) }
            if (credits > 0) letterFor(first / credits)?.let { add(it) }
        }
        return if (candidates.size == 1) candidates.first() else ""
    }

    private val GRADE_OCR_PLUS = Regex("^([A-E])\\s?[T\u2020]$")
    private val GRADE_OCR_MINUS = Regex("^([A-E])\\s?[\u2013\u2014\u2012]$")
    private val OCR_DIGIT_TOKEN = "[0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg]{1,2}"
    private val NAME_CREDITS_DECIMALS = Regex("(?i)^(.{3,100}?)\\s+($OCR_DIGIT_TOKEN)\\s*(?:SKS)?\\s+(\\d{1,2}[.,]\\d{1,2})(?:\\s+(\\d{1,3}[.,]\\d{1,2}))?$")
    private val GRADE_FIRST = Regex("(?i)^(.{3,100}?)\\s+(AB|BC|[A-E][+-]?|8|-)\\s+($OCR_DIGIT_TOKEN)\\s*(?:SKS)?$")
    private val CREDITS_FIRST = Regex("(?i)^(.{3,100}?)\\s+($OCR_DIGIT_TOKEN)\\s*(?:SKS)?\\s+(AB|BC|[A-E][+-]?|8|-)(?=\\s|$).*$")
    private val NAME_CREDITS_ONLY = Regex("(?i)^(.{3,100}?)\\s+($OCR_DIGIT_TOKEN)\\s*(?:SKS)?(?:\\s+[\\d.,]+)*$")
    private val NAME_GRADE_ONLY = Regex("(?i)^(.{3,100}?)\\s+(AB|BC|[A-E][+-]?|8)(?:\\s+[\\d.,]+)*$")
}
