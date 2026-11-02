package com.temankuliah.app.ui.courses

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PdfPrintHelper
import android.provider.MediaStore
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.Executors

/** Hasil simpan PDF. [inDownloads] false berarti berkas hanya siap dibagikan (Android 8 ke bawah). */
internal sealed interface PdfExportResult {
    data class Saved(val uri: Uri, val fileName: String, val inDownloads: Boolean) : PdfExportResult
    data class Failed(val message: String) : PdfExportResult
}

@SuppressLint("StaticFieldLeak")
private var exportWebView: WebView? = null
private val exportExecutor = Executors.newSingleThreadExecutor()

/** Mengganti karakter yang tidak aman untuk nama berkas. */
internal fun safePdfFileName(title: String): String {
    val cleaned = title.replace(Regex("[\\\\/:*?\"<>|]"), " ").replace(Regex("\\s+"), " ").trim()
    return (cleaned.ifEmpty { "Transkrip Nilai" }) + ".pdf"
}

/**
 * Mengubah [html] menjadi PDF A4 lalu menyimpannya langsung ke folder Download tanpa pratinjau.
 * Harus dipanggil dari thread utama; [onResult] juga dipanggil di thread utama.
 */
internal fun exportHtmlToPdf(context: Context, html: String, title: String, onResult: (PdfExportResult) -> Unit) {
    val appContext = context.applicationContext
    val fileName = safePdfFileName(title)
    val main = Handler(Looper.getMainLooper())
    fun fail(message: String) {
        exportWebView?.destroy()
        exportWebView = null
        onResult(PdfExportResult.Failed(message))
    }
    try {
        val webView = WebView(appContext)
        webView.settings.apply {
            javaScriptEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            blockNetworkLoads = true
        }
        var started = false
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                if (started) return
                started = true
                val temp = File(File(appContext.cacheDir, "transcripts").apply { mkdirs() }, fileName)
                val descriptor = try {
                    ParcelFileDescriptor.open(
                        temp,
                        ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
                    )
                } catch (error: Exception) {
                    Log.e("TranscriptPdf", "Gagal membuat berkas sementara", error)
                    fail("Tidak dapat membuat berkas PDF.")
                    return
                }
                PdfPrintHelper.write(view.createPrintDocumentAdapter(fileName), descriptor) { success ->
                    runCatching { descriptor.close() }
                    if (!success) {
                        temp.delete()
                        fail("PDF gagal dibuat. Coba lagi.")
                        return@write
                    }
                    exportExecutor.execute {
                        val result = runCatching { saveToDownloads(appContext, temp, fileName) }
                            .onFailure { Log.e("TranscriptPdf", "Gagal menyimpan PDF", it) }
                            .getOrNull()
                        temp.takeIf { result?.inDownloads == true }?.delete()
                        main.post {
                            exportWebView?.destroy()
                            exportWebView = null
                            onResult(result ?: PdfExportResult.Failed("PDF tidak dapat disimpan ke penyimpanan."))
                        }
                    }
                }
            }
        }
        exportWebView = webView
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    } catch (error: Exception) {
        Log.e("TranscriptPdf", "Gagal menyiapkan PDF", error)
        fail("Perangkat ini tidak dapat membuat PDF.")
    }
}

private fun saveToDownloads(context: Context, source: File, fileName: String): PdfExportResult.Saved {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore menolak berkas")
        try {
            resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
                ?: error("Tidak dapat membuka tujuan")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
        return PdfExportResult.Saved(uri, fileName, inDownloads = true)
    }
    // Android 8 ke bawah: menulis ke Download butuh izin penyimpanan, jadi berkas dibagikan lewat FileProvider.
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", source)
    return PdfExportResult.Saved(uri, fileName, inDownloads = false)
}
