package com.temankuliah.app.ui.courses

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

import android.annotation.SuppressLint
import android.util.Log

/** Menahan WebView selama halaman dimuat agar tidak dibersihkan GC sebelum dialog cetak muncul. Memakai applicationContext agar aman dari leak. */
@SuppressLint("StaticFieldLeak")
private var pendingWebView: WebView? = null

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Membuka dialog cetak Android (bisa dicetak atau disimpan sebagai PDF) untuk [html].
 * Harus dipanggil dari thread utama. Mengembalikan false bila perangkat tidak dapat mencetak.
 */
internal fun printHtmlDocument(context: Context, html: String, jobName: String): Boolean {
    val activity = context.findActivity() ?: return false
    val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
    return try {
        val webView = WebView(activity.applicationContext)
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
                val attributes = PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
                printManager.print(jobName, view.createPrintDocumentAdapter(jobName), attributes)
                pendingWebView = null
            }
        }
        pendingWebView = webView
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        true
    } catch (error: Exception) {
        Log.e("TranscriptPrinter", "Failed to print HTML document", error)
        pendingWebView = null
        false
    }
}
