package com.temankuliah.app.data.local

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Menandai pekerjaan yang hanya boleh berjalan sekali (mis. pembersihan data contoh lama).
 * Disimpan di SharedPreferences agar tetap tercatat setelah aplikasi dimulai ulang.
 */
class OneTimeTaskGate(private val prefs: SharedPreferences) {
    fun isDone(key: String): Boolean = prefs.getBoolean(key, false)

    fun markDone(key: String) {
        prefs.edit { putBoolean(key, true) }
    }

    companion object {
        const val PREFS_NAME = "temankuliah_one_time_tasks"
    }
}
