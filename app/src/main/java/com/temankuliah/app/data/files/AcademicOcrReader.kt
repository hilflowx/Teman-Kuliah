package com.temankuliah.app.data.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.temankuliah.app.data.files.AcademicDocumentProcessor.Extraction
import com.temankuliah.app.data.files.AcademicDocumentProcessor.OcrWord
import com.temankuliah.app.domain.AcademicReconciler
import com.temankuliah.app.domain.rethrowIfCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Pembacaan teks dari berkas dokumen (TXT/CSV, foto, PDF) di perangkat dengan ML Kit; tidak ada isi dokumen yang dikirim ke server. */
internal object AcademicOcrReader {
    suspend fun extract(file: File): Extraction = withContext(Dispatchers.IO) {
        val extension = file.extension.lowercase(Locale.ROOT)
        val result = when (extension) {
            "txt", "csv", "tsv", "md" -> extractTextFile(file)
            "pdf" -> extractPdf(file)
            "jpg", "jpeg", "png", "webp", "bmp", "gif" -> extractImage(file)
            else -> throw IOException("Format file belum didukung. Gunakan PDF, foto, TXT, CSV, TSV, atau Markdown.")
        }
        val cleanText = result.text.replace('\u0000', ' ').trim().take(MAX_TEXT_CHARS)
        if (cleanText.isBlank()) Extraction("", result.message.ifBlank { "Tidak ada teks yang berhasil dikenali. Pastikan dokumen jelas, lalu coba proses ulang." })
        else Extraction(cleanText, result.message)
    }

    /**
     * Mengelompokkan kata OCR menjadi baris berdasarkan posisi vertikal, lalu mengurutkannya dari
     * kiri ke kanan. Jarak horizontal yang lebar dianggap batas kolom dan ditulis sebagai TAB,
     * sehingga sel tabel tidak tercampur dengan sel di kolom lain.
     */
    fun layoutRows(words: List<OcrWord>): String {
        val usable = words.filter { it.text.isNotBlank() && it.bottom > it.top }
        if (usable.isEmpty()) return ""
        val heights = usable.map { it.bottom - it.top }.sorted()
        val medianHeight = heights[heights.size / 2].coerceAtLeast(1)
        val rowTolerance = (medianHeight * 0.85).coerceAtLeast(14.0)
        val columnGap = (medianHeight * 0.95).coerceAtLeast(16.0)

        val rows = mutableListOf<MutableList<OcrWord>>()
        val rowCenters = mutableListOf<Double>()
        usable.sortedBy { it.top + it.bottom }.forEach { word ->
            val center = (word.top + word.bottom) / 2.0
            val matchingRowIndex = rows.indices.reversed().take(3).firstOrNull { idx ->
                kotlin.math.abs(center - rowCenters[idx]) <= rowTolerance
            }
            if (matchingRowIndex != null) {
                rows[matchingRowIndex].add(word)
                rowCenters[matchingRowIndex] = rows[matchingRowIndex].sumOf { (it.top + it.bottom) / 2.0 } / rows[matchingRowIndex].size
            } else {
                rows.add(mutableListOf(word))
                rowCenters.add(center)
            }
        }
        return rows.joinToString("\n") { row ->
            val ordered = row.sortedBy { it.left }
            val builder = StringBuilder()
            ordered.forEachIndexed { index, word ->
                if (index > 0) {
                    val gap = word.left - ordered[index - 1].right
                    builder.append(if (gap > columnGap) '\t' else ' ')
                }
                builder.append(word.text.trim())
            }
            builder.toString()
        }
    }

    private fun extractTextFile(file: File): Extraction {
        if (file.length() > MAX_TEXT_FILE_BYTES) throw IOException("File teks terlalu besar untuk diproses (maksimal 10 MB).")
        val bytes = file.readBytes()
        val text = String(bytes, StandardCharsets.UTF_8).removePrefix("\uFEFF")
        val controls = text.count { it.code < 32 && it !in '\n'..'\r' && it != '\t' }
        if (text.isNotBlank() && controls.toDouble() / text.length.coerceAtLeast(1) > 0.03) {
            throw IOException("Isi file bukan teks UTF-8 yang dapat dibaca. Coba ekspor ulang sebagai TXT atau CSV.")
        }
        return Extraction(text)
    }

    internal fun calculatePdfRenderDimensions(pageWidth: Int, pageHeight: Int): Pair<Int, Int> {
        val maxEdge = maxOf(pageWidth, pageHeight).coerceAtLeast(1)
        val scale = if (maxEdge > MAX_SAFE_PDF_EDGE) {
            MAX_SAFE_PDF_EDGE.toFloat() / maxEdge
        } else {
            (PDF_TARGET_EDGE.toFloat() / maxEdge).coerceIn(1f, 2.5f)
        }
        val width = (pageWidth * scale).toInt().coerceAtLeast(1)
        val height = (pageHeight * scale).toInt().coerceAtLeast(1)
        return Pair(width, height)
    }

    internal fun calculateImageSampleSize(width: Int, height: Int): Int {
        val maxDimension = maxOf(width, height)
        var sample = 1
        while ((maxDimension / sample) > MAX_BITMAP_EDGE) {
            sample *= 2
        }
        return sample
    }

    private fun extractImage(file: File): Extraction {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Gambar tidak valid atau formatnya tidak didukung.")
        val sample = calculateImageSampleSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = try {
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (oom: OutOfMemoryError) {
            System.gc()
            throw IOException("Resolusi foto terlalu besar untuk diproses memori perangkat. Silakan gunakan foto dengan resolusi standar.", oom)
        } ?: throw IOException("Gambar tidak dapat dibaca.")

        // Foto kamera menyimpan orientasi di EXIF; tanpa rotasi, OCR membaca teks miring/terbalik.
        val rotation = readExifRotation(file)
        var prepared: Bitmap = bitmap
        return try {
            prepared = scaleIfNeeded(bitmap)
            Extraction(runOcr(prepared, rotation), "Teks dikenali dari gambar menggunakan OCR di perangkat.")
        } finally {
            if (prepared !== bitmap && !prepared.isRecycled) prepared.recycle()
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun readExifRotation(file: File): Int = try {
        when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (ignored: Exception) {
        rethrowIfCancellation(ignored)
        0
    }

    private fun extractPdf(file: File): Extraction {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val pages = mutableListOf<String>()
        var partial = false
        try {
            val renderer = PdfRenderer(descriptor)
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                try {
                    if (renderer.pageCount <= 0) throw IOException("PDF tidak memiliki halaman yang dapat dibaca.")
                    val pageLimit = minOf(renderer.pageCount, MAX_PDF_PAGES)
                    partial = renderer.pageCount > pageLimit
                    for (index in 0 until pageLimit) {
                        val page = renderer.openPage(index)
                        try {
                            val (width, height) = calculatePdfRenderDimensions(page.width, page.height)
                            try {
                                val bitmap = createBitmap(width, height)
                                try {
                                    // PdfRenderer menggambar di atas latar transparan; tanpa latar putih teks hitam
                                    // menjadi tidak terbaca oleh OCR.
                                    bitmap.eraseColor(Color.WHITE)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    val text = recognizeLayout(recognizer, bitmap, 0)
                                    if (text.isNotBlank()) pages += text
                                } finally {
                                    if (!bitmap.isRecycled) bitmap.recycle()
                                }
                            } catch (oom: OutOfMemoryError) {
                                System.gc()
                                throw IOException("Resolusi dokumen PDF terlalu besar untuk diproses memori perangkat. Silakan gunakan dokumen dengan resolusi standar.", oom)
                            }
                        } finally { page.close() }
                    }
                } finally { recognizer.close() }
            } finally { renderer.close() }
        } finally { descriptor.close() }
        return Extraction(
            pages.joinToString("\n"),
            if (partial) "Hanya $MAX_PDF_PAGES halaman pertama yang diproses. Untuk halaman lain, unggah bagian dokumen secara terpisah." else "Teks PDF dikenali menggunakan OCR di perangkat."
        )
    }

    private class OcrPass(val words: List<OcrWord>, val fullText: String) {
        val score = fullText.count(Char::isLetterOrDigit)
    }

    private fun recognizePass(recognizer: com.google.mlkit.vision.text.TextRecognizer, bitmap: Bitmap, rotationDegrees: Int): OcrPass {
        val result = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, rotationDegrees)), OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val words = result.textBlocks.flatMap { block ->
            block.lines.flatMap { line ->
                line.elements.mapNotNull { element ->
                    element.boundingBox?.let { box -> OcrWord(element.text, box.left, box.top, box.right, box.bottom) }
                }
            }
        }
        return OcrPass(words, result.text)
    }

    /**
     * ML Kit butuh karakter minimal 16 x 16 piksel. Bila teks hasil bacaan pertama lebih kecil (foto jauh,
     * tangkapan layar kecil, tabel padat), gambar diperbesar dan dibaca sekali lagi; hasil yang mengenali
     * lebih banyak karakter dipakai.
     */
    private fun recognizeLayout(recognizer: com.google.mlkit.vision.text.TextRecognizer, bitmap: Bitmap, rotationDegrees: Int): String {
        var best = recognizePass(recognizer, bitmap, rotationDegrees)
        val factor = AcademicReconciler.ocrUpscaleFactor(
            medianTextHeightPx = AcademicReconciler.median(best.words.filter { it.text.isNotBlank() }.map { it.bottom - it.top }),
            longestEdgePx = maxOf(bitmap.width, bitmap.height)
        )
        val maxEdge = maxOf(bitmap.width, bitmap.height)
        if (factor > 1f && maxEdge < MAX_BITMAP_EDGE) {
            val cappedFactor = minOf(factor, MAX_BITMAP_EDGE.toFloat() / maxEdge.coerceAtLeast(1))
            if (cappedFactor > 1.05f) {
                var enlarged: Bitmap? = null
                try {
                    enlarged = bitmap.scale(
                        (bitmap.width * cappedFactor).toInt().coerceAtLeast(1),
                        (bitmap.height * cappedFactor).toInt().coerceAtLeast(1)
                    )
                    val second = recognizePass(recognizer, enlarged, rotationDegrees)
                    if (second.score > best.score) best = second
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    // Abaikan kegagalan upscale, pertahankan hasil terbaik yang sudah terbaca
                } finally {
                    if (enlarged != null && enlarged !== bitmap && !enlarged.isRecycled) enlarged.recycle()
                }
            }
        }
        val laidOut = layoutRows(best.words)
        return if (laidOut.isNotBlank()) laidOut else best.fullText
    }

    private fun runOcr(bitmap: Bitmap, rotationDegrees: Int): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            recognizeLayout(recognizer, bitmap, rotationDegrees)
        } catch (error: Exception) {
            rethrowIfCancellation(error)
            throw IOException("OCR gagal memproses gambar. Coba gunakan foto yang lebih jelas.", error)
        } finally { recognizer.close() }
    }

    private fun scaleIfNeeded(bitmap: Bitmap): Bitmap {
        val edge = maxOf(bitmap.width, bitmap.height)
        if (edge <= MAX_BITMAP_EDGE) return bitmap
        val scale = MAX_BITMAP_EDGE.toFloat() / edge
        return try {
            bitmap.scale((bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1))
        } catch (oom: OutOfMemoryError) {
            System.gc()
            bitmap
        }
    }

    private const val MAX_TEXT_CHARS = 100_000
    private const val MAX_TEXT_FILE_BYTES = 10L * 1024L * 1024L
    private const val MAX_BITMAP_EDGE = 2048
    private const val MAX_SAFE_PDF_EDGE = 2048
    private const val PDF_TARGET_EDGE = 2048
    private const val MAX_PDF_PAGES = 30
    private const val OCR_TIMEOUT_SECONDS = 45L
}
