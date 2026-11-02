package com.temankuliah.app.alarm

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.temankuliah.app.R

/**
 * Membuat isi notifikasi pengingat bergaya TemanKuliah: judul tebal, lalu chip waktu dan chip lokasi/keterangan.
 * Pesan dipecah di pemisah " · "; pesan tanpa pemisah ditampilkan sebagai teks biasa.
 */
internal object ReminderNotificationView {

    private const val SEPARATOR = " · "

    fun build(context: Context, title: String, message: String): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_reminder)
        views.setTextViewText(R.id.reminder_title, title)

        val parts = message.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size >= 2) {
            views.setViewVisibility(R.id.reminder_chips, View.VISIBLE)
            views.setViewVisibility(R.id.reminder_body, View.GONE)
            views.setTextViewText(R.id.reminder_chip_primary, parts.first())
            views.setTextViewText(R.id.reminder_chip_secondary, parts.drop(1).joinToString(SEPARATOR))
        } else {
            views.setViewVisibility(R.id.reminder_chips, View.GONE)
            views.setViewVisibility(R.id.reminder_body, View.VISIBLE)
            views.setTextViewText(R.id.reminder_body, message)
        }
        return views
    }
}
