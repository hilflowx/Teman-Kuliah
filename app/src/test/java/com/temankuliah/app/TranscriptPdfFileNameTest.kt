package com.temankuliah.app

import com.temankuliah.app.ui.courses.safePdfFileName
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptPdfFileNameTest {
    @Test
    fun `nama berkas menambah ekstensi pdf`() {
        assertEquals("Transkrip Nilai - Budi.pdf", safePdfFileName("Transkrip Nilai - Budi"))
    }

    @Test
    fun `karakter terlarang diganti spasi dan spasi berlebih dirapikan`() {
        assertEquals("Transkrip Nilai Budi Ani.pdf", safePdfFileName("Transkrip/Nilai: Budi*?  \"Ani\""))
    }

    @Test
    fun `judul kosong memakai nama bawaan`() {
        assertEquals("Transkrip Nilai.pdf", safePdfFileName("   "))
    }
}
