package com.temankuliah.app.data.local

import android.content.Context
import android.content.SharedPreferences

/** Nama penyimpanan lokal aplikasi (database Room dan SharedPreferences). */
object AppStorage {
    const val PREFERENCES_NAME = "temankuliah_preferences"
    const val DATABASE_NAME = "temankuliah_database"
    const val DEFAULT_TARGET_CREDITS = 144

    fun getPreferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
