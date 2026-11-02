package com.temankuliah.app.domain

/**
 * Android membatasi jumlah alarm yang aktif bersamaan per aplikasi (sekitar 500). Jadwal berulang membuat satu
 * baris per pertemuan, jadi alarm kuliah hanya dipasang untuk pertemuan dalam jendela waktu ini. Alarm berikutnya
 * dipasang saat aplikasi dibuka, perangkat menyala ulang, atau setelah satu pengingat kuliah berbunyi.
 */
object ReminderWindow {
    const val CLASS_ALARM_WINDOW_DAYS = 21L

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    /** Benar bila waktu pengingat belum lewat dan masih dalam jendela [CLASS_ALARM_WINDOW_DAYS] hari dari [nowMillis]. */
    fun shouldArmClassAlarm(triggerAtMillis: Long, nowMillis: Long): Boolean =
        triggerAtMillis > nowMillis && triggerAtMillis <= nowMillis + CLASS_ALARM_WINDOW_DAYS * DAY_MILLIS
}
