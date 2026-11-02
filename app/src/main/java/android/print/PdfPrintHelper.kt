package android.print

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor

/**
 * Menulis isi [PrintDocumentAdapter] (mis. dari WebView) langsung ke berkas PDF tanpa dialog cetak.
 * Berada di paket android.print karena konstruktor LayoutResultCallback dan WriteResultCallback
 * hanya dapat diakses dari paket tersebut. Harus dipanggil dari thread utama.
 */
object PdfPrintHelper {
    fun write(adapter: PrintDocumentAdapter, output: ParcelFileDescriptor, onDone: (Boolean) -> Unit) {
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        var finished = false
        fun finish(success: Boolean) {
            if (finished) return
            finished = true
            runCatching { adapter.onFinish() }
            onDone(success)
        }
        try {
            adapter.onStart()
            adapter.onLayout(
                null,
                attributes,
                CancellationSignal(),
                object : PrintDocumentAdapter.LayoutResultCallback() {
                    override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                        adapter.onWrite(
                            arrayOf(PageRange.ALL_PAGES),
                            output,
                            CancellationSignal(),
                            object : PrintDocumentAdapter.WriteResultCallback() {
                                override fun onWriteFinished(pages: Array<out PageRange>?) = finish(true)
                                override fun onWriteFailed(error: CharSequence?) = finish(false)
                                override fun onWriteCancelled() = finish(false)
                            }
                        )
                    }
                    override fun onLayoutFailed(error: CharSequence?) = finish(false)
                    override fun onLayoutCancelled() = finish(false)
                },
                null
            )
        } catch (error: Exception) {
            finish(false)
        }
    }
}
