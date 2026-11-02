package com.temankuliah.app

import com.temankuliah.app.data.files.AcademicOcrReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrMemoryOptimizationTest {

    @Test
    fun `high resolution 300 DPI PDF is downsampled to safe edge`() {
        // Halaman A4 pada 300 DPI: 2480 x 3508 piksel
        val (width, height) = AcademicOcrReader.calculatePdfRenderDimensions(2480, 3508)
        assertTrue("Panjang sisi terpanjang tidak boleh melebihi 2048 piksel", maxOf(width, height) <= 2048)
        assertEquals(2048, height)
        assertTrue("Aspek rasio harus proporsional", width in 1440..1455)
    }

    @Test
    fun `extreme high resolution 600 DPI PDF scan is downsampled aggressively`() {
        // Halaman A4 pada 600 DPI: 4960 x 7016 piksel
        val (width, height) = AcademicOcrReader.calculatePdfRenderDimensions(4960, 7016)
        assertTrue("Panjang sisi terpanjang tidak boleh melebihi 2048 piksel", maxOf(width, height) <= 2048)
        assertEquals(2048, height)
        assertTrue("Aspek rasio harus proporsional", width in 1440..1455)
    }

    @Test
    fun `standard vector PDF page is scaled up within safe limit for OCR sharpness`() {
        // Halaman A4 vektor standar 72 DPI: 595 x 842 pt
        val (width, height) = AcademicOcrReader.calculatePdfRenderDimensions(595, 842)
        assertTrue("Panjang sisi terpanjang tidak boleh melebihi 2048 piksel", maxOf(width, height) <= 2048)
        assertTrue("Teks harus diperjelas minimal 1.5x", height >= (842 * 1.5).toInt())
    }

    @Test
    fun `small PDF page does not exceed safe cap`() {
        val (width, height) = AcademicOcrReader.calculatePdfRenderDimensions(300, 400)
        assertTrue("Panjang sisi terpanjang tidak boleh melebihi 2048 piksel", maxOf(width, height) <= 2048)
        assertTrue("Dimensi minimal 1 px", width >= 1 && height >= 1)
    }

    @Test
    fun `normal image within safe limit uses sample size 1`() {
        val sample = AcademicOcrReader.calculateImageSampleSize(1200, 1600)
        assertEquals(1, sample)
    }

    @Test
    fun `high megapixel camera photo computes power of two sample size`() {
        // Foto kamera 48 MP: 6000 x 8000 piksel
        val sample = AcademicOcrReader.calculateImageSampleSize(6000, 8000)
        assertEquals(4, sample) // 8000 / 4 = 2000 <= 2048
        val decodedHeight = 8000 / sample
        assertTrue(decodedHeight <= 2048)
    }

    @Test
    fun `ultra high megapixel photo computes appropriate sample size`() {
        // Foto kamera 108 MP: 9000 x 12000 piksel
        val sample = AcademicOcrReader.calculateImageSampleSize(9000, 12000)
        assertEquals(8, sample) // 12000 / 8 = 1500 <= 2048
        val decodedHeight = 12000 / sample
        assertTrue(decodedHeight <= 2048)
    }
}
