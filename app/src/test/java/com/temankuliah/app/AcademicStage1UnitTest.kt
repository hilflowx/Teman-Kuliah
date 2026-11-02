package com.temankuliah.app

import com.temankuliah.app.data.files.AcademicDocumentProcessor
import com.temankuliah.app.domain.GpaCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AcademicStage1UnitTest {

    @Test
    fun testExtractSemesterNumberFromDigits() {
        assertEquals(3, AcademicDocumentProcessor.extractSemesterNumber("KARTU HASIL STUDI\nSemester : 3\nTahun Akademik 2023/2024"))
        assertEquals(5, AcademicDocumentProcessor.extractSemesterNumber("Semester: 5"))
        assertEquals(2, AcademicDocumentProcessor.extractSemesterNumber("KRS Smtr 2 Reguler"))
        assertEquals(7, AcademicDocumentProcessor.extractSemesterNumber("Semester ke-7"))
        assertEquals(1, AcademicDocumentProcessor.extractSemesterNumber("Sem. 1 Gasal"))
    }

    @Test
    fun testExtractSemesterNumberFromRomanNumerals() {
        assertEquals(3, AcademicDocumentProcessor.extractSemesterNumber("KARTU RENCANA STUDI SEMESTER III"))
        assertEquals(4, AcademicDocumentProcessor.extractSemesterNumber("Semester: IV"))
        assertEquals(6, AcademicDocumentProcessor.extractSemesterNumber("KHS Sem. VI TA 2022/2023"))
        assertEquals(8, AcademicDocumentProcessor.extractSemesterNumber("Semester VIII (Delapan)"))
        assertEquals(1, AcademicDocumentProcessor.extractSemesterNumber("Semester I"))
    }

    @Test
    fun testExtractSemesterNumberFromWords() {
        assertEquals(3, AcademicDocumentProcessor.extractSemesterNumber("Kartu Hasil Studi Semester Tiga"))
        assertEquals(1, AcademicDocumentProcessor.extractSemesterNumber("Semester Satu"))
        assertEquals(4, AcademicDocumentProcessor.extractSemesterNumber("Semester Empat"))
    }

    @Test
    fun testExtractSemesterNumberReturnsNullWhenAbsent() {
        assertNull(AcademicDocumentProcessor.extractSemesterNumber("UNIVERSITAS TEKNOLOGI\nKARTU HASIL STUDI"))
        assertNull(AcademicDocumentProcessor.extractSemesterNumber(""))
    }

    @Test
    fun testExtractAcademicYear() {
        assertEquals("2023/2024", AcademicDocumentProcessor.extractAcademicYear("Tahun Ajaran 2023/2024"))
        assertEquals("2024/2025", AcademicDocumentProcessor.extractAcademicYear("Tahun Akademik 2024-2025"))
        assertNull(AcademicDocumentProcessor.extractAcademicYear("Tanpa tahun"))
    }

    @Test
    fun testGpaCalculations() {
        // A = 4.0 (3 SKS), B = 3.0 (3 SKS) -> Total 6 SKS, mutu 12 + 9 = 21 -> IPS = 3.50
        val items = listOf(3 to "A", 3 to "B")
        val totals = GpaCalculator.semesterTotals(items)
        assertEquals(6, totals.totalCredits)
        assertEquals(3.50, totals.ips, 0.01)

        // Empty courses return 0.0
        val emptyTotals = GpaCalculator.semesterTotals(emptyList())
        assertEquals(0, emptyTotals.totalCredits)
        assertEquals(0.0, emptyTotals.ips, 0.01)
    }

    @Test
    fun testDynamicTargetSksProgress() {
        val defaultTarget = 144
        val passedCredits = 72
        val remainingCredits = (defaultTarget - passedCredits).coerceAtLeast(0)
        val progressFraction = passedCredits.toFloat() / defaultTarget.toFloat()

        assertEquals(72, remainingCredits)
        assertEquals(0.5f, progressFraction, 0.001f)

        // Custom target for Diploma (e.g. 110 SKS)
        val customTarget = 110
        val passedD3 = 88
        val remainingD3 = (customTarget - passedD3).coerceAtLeast(0)
        assertEquals(22, remainingD3)
        assertEquals(0.8f, passedD3.toFloat() / customTarget.toFloat(), 0.001f)
    }
}
