package com.temankuliah.app.domain

import com.temankuliah.app.data.local.CourseEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** The GPA policy used by TemanKuliah until a user-configurable grading table is added. */
data class GpaSummary(
    val cumulativeGpa: Double = 0.0,
    /** SKS in the GPA denominator, including graded failures (E). */
    val totalCredits: Int = 0,
    /** SKS passed with a positive grade point; used for graduation progress. */
    val passedCredits: Int = 0,
    /** Number of course records, including those without a grade. */
    val totalCourses: Int = 0,
    val honorsPredicate: String = "Belum Ada Nilai",
    val semesterGpaMap: Map<Int, Double> = emptyMap(),
    val semesterCreditsMap: Map<Int, Int> = emptyMap(),
    val semesterKrsCreditsMap: Map<Int, Int> = emptyMap()
)

/** Ringkasan satu semester untuk pratinjau/tabel KHS. IPS hanya memakai mata kuliah yang sudah bernilai. */
data class SemesterTotals(
    /** Seluruh SKS pada tabel, termasuk mata kuliah yang belum bernilai. */
    val totalCredits: Int,
    /** SKS yang sudah bernilai; pembagi IPS. */
    val gradedCredits: Int,
    val totalQualityPoints: Double,
    val ips: Double
)

/**
 * Satu kebijakan pembulatan untuk semua angka IP: mutu per mata kuliah dibulatkan 2 desimal (HALF_UP),
 * total mutu dijumlahkan sebagai BigDecimal, lalu IPS/IPK dibagi dan dibulatkan 2 desimal.
 * Predikat kelulusan membaca IPK yang sudah dibulatkan, sehingga angka di layar dan predikatnya selalu sama.
 */
object GpaCalculator {
    private val CUM_LAUDE_MIN = BigDecimal("3.51")
    private val VERY_SATISFACTORY_MIN = BigDecimal("3.00")
    private val SATISFACTORY_MIN = BigDecimal("2.50")

    fun gradeLetterToPoint(letter: String): Double =
        Grades.pointOf(letter)

    fun isGraded(letter: String): Boolean = Grades.isGraded(letter)

    /** Mutu satu mata kuliah = SKS x bobot, sebagai BigDecimal dengan 2 desimal. */
    private fun mutuOf(credits: Int, point: Double): BigDecimal =
        BigDecimal.valueOf(credits.toLong())
            .multiply(BigDecimal.valueOf(point))
            .setScale(2, RoundingMode.HALF_UP)

    /**
     * Mutu satu mata kuliah = SKS x bobot, dihitung dengan desimal (bukan floating point biner) dan
     * dibulatkan ke 2 angka di belakang koma. Nilai kosong ("-") atau tidak dikenal bernilai 0.
     */
    fun qualityPoints(credits: Int, letter: String): Double {
        if (credits <= 0 || !isGraded(letter)) return 0.0
        return mutuOf(credits, gradeLetterToPoint(letter)).toDouble()
    }

    /** Total SKS, total mutu, dan IPS (total mutu / SKS bernilai) dari pasangan (SKS, nilai huruf). */
    fun semesterTotals(entries: List<Pair<Int, String>>): SemesterTotals {
        var totalCredits = 0
        var gradedCredits = 0
        var totalMutu = BigDecimal.ZERO
        entries.forEach { (credits, letter) ->
            if (credits <= 0) return@forEach
            totalCredits += credits
            if (isGraded(letter)) {
                gradedCredits += credits
                totalMutu = totalMutu.add(mutuOf(credits, gradeLetterToPoint(letter)))
            }
        }
        return SemesterTotals(
            totalCredits = totalCredits,
            gradedCredits = gradedCredits,
            totalQualityPoints = totalMutu.setScale(2, RoundingMode.HALF_UP).toDouble(),
            ips = indexOf(totalMutu, gradedCredits).toDouble()
        )
    }

    /** Indeks = total mutu / SKS, dibulatkan 2 desimal. 0 bila tidak ada SKS bernilai. */
    private fun indexOf(totalMutu: BigDecimal, credits: Int): BigDecimal =
        if (credits == 0) BigDecimal.ZERO.setScale(2)
        else totalMutu.divide(BigDecimal(credits), 2, RoundingMode.HALF_UP)

    /**
     * A grade of "-", blank or unsupported is not a graded course and is excluded.
     * Each graded record counts once. Repeated courses across semesters are separate attempts,
     * since the existing data model has no explicit grade-replacement marker.
     */
    fun calculateSummary(courses: List<CourseEntity>): GpaSummary {
        var totalCredits = 0
        var passedCredits = 0
        var totalMutu = BigDecimal.ZERO
        val semesterCredits = mutableMapOf<Int, Int>()
        val semesterKrsCredits = mutableMapOf<Int, Int>()
        val semesterMutu = mutableMapOf<Int, BigDecimal>()

        courses.forEach { course ->
            if (course.credits <= 0) return@forEach
            semesterKrsCredits[course.semester] = semesterKrsCredits.getOrDefault(course.semester, 0) + course.credits
            val normalizedGrade = course.gradeLetter.trim().uppercase(Locale.ROOT)
            val point = Grades.POINTS[normalizedGrade] ?: return@forEach
            val mutu = mutuOf(course.credits, point)
            totalCredits += course.credits
            if (point > 0.0) passedCredits += course.credits
            totalMutu = totalMutu.add(mutu)
            semesterCredits[course.semester] = semesterCredits.getOrDefault(course.semester, 0) + course.credits
            semesterMutu[course.semester] = (semesterMutu[course.semester] ?: BigDecimal.ZERO).add(mutu)
        }

        val gpa = indexOf(totalMutu, totalCredits)
        val predicate = when {
            totalCredits == 0 -> "Belum Ada Nilai"
            gpa >= CUM_LAUDE_MIN -> "Dengan Pujian (Cum Laude)"
            gpa >= VERY_SATISFACTORY_MIN -> "Sangat Memuaskan"
            gpa >= SATISFACTORY_MIN -> "Memuaskan"
            else -> "Cukup"
        }

        return GpaSummary(
            cumulativeGpa = gpa.toDouble(),
            totalCredits = totalCredits,
            passedCredits = passedCredits,
            totalCourses = courses.size,
            honorsPredicate = predicate,
            semesterGpaMap = semesterCredits.mapValues { (semester, credits) ->
                indexOf(semesterMutu[semester] ?: BigDecimal.ZERO, credits).toDouble()
            },
            semesterCreditsMap = semesterCredits,
            semesterKrsCreditsMap = semesterKrsCredits
        )
    }
}
