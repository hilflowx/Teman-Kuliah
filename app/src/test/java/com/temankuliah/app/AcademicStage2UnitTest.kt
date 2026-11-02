package com.temankuliah.app

import com.temankuliah.app.data.local.AppStorage
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.domain.GpaCalculator
import com.temankuliah.app.domain.Grades
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicStage2UnitTest {

    @Test
    fun testIndonesianGradePointsAndCalculations() {
        assertEquals(4.0, GpaCalculator.gradeLetterToPoint("A"), 0.001)
        assertEquals(3.5, GpaCalculator.gradeLetterToPoint("B+"), 0.001)
        assertEquals(3.0, GpaCalculator.gradeLetterToPoint("B"), 0.001)
        assertEquals(2.5, GpaCalculator.gradeLetterToPoint("C+"), 0.001)
        assertEquals(2.0, GpaCalculator.gradeLetterToPoint("C"), 0.001)
        assertEquals(1.0, GpaCalculator.gradeLetterToPoint("D"), 0.001)
        assertEquals(0.0, GpaCalculator.gradeLetterToPoint("E"), 0.001)
        assertEquals(0.0, GpaCalculator.gradeLetterToPoint("-"), 0.001)

        val courses = listOf(
            CourseEntity(id = 1, userId = 10, code = "IF101", name = "Dasar Pemrograman", credits = 3, gradeLetter = "A", gradePoint = 4.0, semester = 1),
            CourseEntity(id = 2, userId = 10, code = "IF102", name = "Matematika Diskrit", credits = 3, gradeLetter = "B+", gradePoint = 3.5, semester = 1),
            CourseEntity(id = 3, userId = 10, code = "IF103", name = "Kalkulus", credits = 3, gradeLetter = "B", gradePoint = 3.0, semester = 1)
        )

        val summary = GpaCalculator.calculateSummary(courses)
        assertEquals(9, summary.totalCredits)
        assertEquals(9, summary.passedCredits)
        // (3*4.0 + 3*3.5 + 3*3.0) / 9 = (12.0 + 10.5 + 9.0) / 9 = 31.5 / 9 = 3.5
        assertEquals(3.5, summary.cumulativeGpa, 0.001)
        assertTrue(summary.honorsPredicate.isNotBlank())
    }

    @Test
    fun testGraduationTargetDefaultConstant() {
        assertEquals(144, AppStorage.DEFAULT_TARGET_CREDITS)
    }

    @Test
    fun testAllGradesHaveIndonesianLabels() {
        assertTrue(Grades.OPTIONS.contains("A"))
        assertTrue(Grades.OPTIONS.contains("B+"))
        assertTrue(Grades.OPTIONS.contains("B"))
        assertTrue(Grades.OPTIONS.contains("C+"))
        assertTrue(Grades.OPTIONS.contains("C"))
        assertTrue(Grades.OPTIONS.contains("D"))
        assertTrue(Grades.OPTIONS.contains("E"))
        assertTrue(Grades.OPTIONS.contains("-"))
    }
}
