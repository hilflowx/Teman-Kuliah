package com.temankuliah.app.ui.profile

import androidx.core.content.edit
import com.temankuliah.app.domain.DEFAULT_REMINDER_MINUTES
import com.temankuliah.app.domain.SEMESTER_START_DATE_KEY_PREFIX
import com.temankuliah.app.domain.parseSemesterStartDate
import com.temankuliah.app.domain.semesterStartDateKey
import com.temankuliah.app.data.local.AppStorage
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.ui.FeatureContext
import com.temankuliah.app.ui.FeatureController
import com.temankuliah.app.ui.components.AnimationMode
import com.temankuliah.app.ui.components.MotionPreferences
import com.temankuliah.app.ui.theme.ThemeMode
import com.temankuliah.app.ui.theme.TkPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class PreferencesController(
    ctx: FeatureContext
) : FeatureController(ctx) {
    private val preferences = AppStorage.getPreferences(application)

    /** Default target 144 SKS (kurikulum S1 standar), dapat disesuaikan pengguna (1–360). */
    private fun readTargetCredits(): Int =
        if (preferences.contains("target_sks")) preferences.getInt("target_sks", AppStorage.DEFAULT_TARGET_CREDITS).coerceIn(1, 360) else AppStorage.DEFAULT_TARGET_CREDITS

    private val _targetCredits = MutableStateFlow(readTargetCredits())
    val targetCredits: StateFlow<Int> = _targetCredits.asStateFlow()

    private val _reminderPreferences = MutableStateFlow(ReminderPreferences.from(preferences))
    val reminderPreferences: StateFlow<ReminderPreferences> = _reminderPreferences.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.fromKey(preferences.getString(KEY_THEME_MODE, null)))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    /** Menyimpan pilihan tema. Mode Terang/Gelap langsung mengganti palet; Ikuti sistem diputuskan oleh TemanKuliahTheme. */
    fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value == mode) return
        _themeMode.value = mode
        preferences.edit { putString(KEY_THEME_MODE, mode.key) }
        when (mode) {
            ThemeMode.LIGHT -> TkPalette.isDark = false
            ThemeMode.DARK -> TkPalette.isDark = true
            ThemeMode.SYSTEM -> Unit
        }
    }

    private val _animationMode = MutableStateFlow(AnimationMode.fromKey(preferences.getString(KEY_ANIMATION_MODE, null)))
    val animationMode: StateFlow<AnimationMode> = _animationMode.asStateFlow()

    init {
        MotionPreferences.mode = _animationMode.value
    }

    /** Menyimpan pilihan animasi: Otomatis (mati di HP spek rendah), Selalu hidup, atau Matikan. */
    fun setAnimationMode(mode: AnimationMode) {
        if (_animationMode.value == mode) return
        _animationMode.value = mode
        MotionPreferences.mode = mode
        preferences.edit { putString(KEY_ANIMATION_MODE, mode.key) }
    }

    companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ANIMATION_MODE = "animation_mode"
    }

    var onPreferencesChanged: (() -> Unit)? = null

    fun setTargetCredits(target: Int) {
        val safeTarget = target.coerceIn(1, 360)
        _targetCredits.value = safeTarget
        preferences.edit { putInt("target_sks", safeTarget) }
        onPreferencesChanged?.invoke()
    }

    fun updateReminderPreferences(value: ReminderPreferences, showSnackbar: Boolean = true) {
        val safe = value.copy(reminderMinutes = value.reminderMinutes.coerceIn(0, 180))
        preferences.edit {
            putBoolean(ReminderScheduler.KEY_TASKS, safe.taskNotificationsEnabled)
            putBoolean(ReminderScheduler.KEY_CLASSES, safe.classRemindersEnabled)
            putBoolean(ReminderScheduler.KEY_SOUND, safe.soundEnabled)
            putBoolean(ReminderScheduler.KEY_VIBRATION, safe.vibrationEnabled)
            putInt(ReminderScheduler.KEY_MINUTES, safe.reminderMinutes)
        }
        _reminderPreferences.value = safe
        scope.launch(Dispatchers.IO) {
            runCatching { ReminderScheduler.rescheduleForActiveUser(application, database) }
        }
        if (showSnackbar) {
            snackbar.value = "Preferensi pengingat disimpan. Perangkat dapat membatasi alarm presisi atau notifikasi."
        }
    }

    private val _semesterStartDates = MutableStateFlow<Map<Int, String>>(readSemesterStartDates())
    val semesterStartDates: StateFlow<Map<Int, String>> = _semesterStartDates.asStateFlow()

    private fun readSemesterStartDates(): Map<Int, String> {
        val all = preferences.all
        val result = mutableMapOf<Int, String>()
        for ((key, value) in all) {
            if (key.startsWith(SEMESTER_START_DATE_KEY_PREFIX) && value is String && parseSemesterStartDate(value) != null) {
                val sem = key.removePrefix(SEMESTER_START_DATE_KEY_PREFIX).toIntOrNull()
                if (sem != null) result[sem] = value
            }
        }
        return result
    }

    fun getSemesterStartDate(semester: Int): String? =
        preferences.getString(semesterStartDateKey(semester), null)

    /** Tanggal mulai yang sah, atau null bila belum diatur / tidak valid. */
    fun semesterStartDateOf(semester: Int): java.time.LocalDate? = parseSemesterStartDate(getSemesterStartDate(semester))

    fun setSemesterStartDate(semester: Int, dateIso: String) {
        preferences.edit { putString(semesterStartDateKey(semester), dateIso.trim()) }
        _semesterStartDates.value = readSemesterStartDates()
        onPreferencesChanged?.invoke()
    }

    /** Membaca ulang pengaturan dari penyimpanan, dipakai setelah data dipulihkan dari cadangan. */
    fun reload() {
        _targetCredits.value = readTargetCredits()
        _reminderPreferences.value = ReminderPreferences.from(preferences)
        _semesterStartDates.value = readSemesterStartDates()
        _themeMode.value = ThemeMode.fromKey(preferences.getString(KEY_THEME_MODE, null))
        _animationMode.value = AnimationMode.fromKey(preferences.getString(KEY_ANIMATION_MODE, null))
        MotionPreferences.mode = _animationMode.value
    }
}

data class ReminderPreferences(
    val taskNotificationsEnabled: Boolean = true,
    val classRemindersEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val reminderMinutes: Int = DEFAULT_REMINDER_MINUTES
) {
    companion object {
        fun from(preferences: android.content.SharedPreferences) = ReminderPreferences(
            taskNotificationsEnabled = preferences.getBoolean(ReminderScheduler.KEY_TASKS, true),
            classRemindersEnabled = preferences.getBoolean(ReminderScheduler.KEY_CLASSES, true),
            soundEnabled = preferences.getBoolean(ReminderScheduler.KEY_SOUND, true),
            vibrationEnabled = preferences.getBoolean(ReminderScheduler.KEY_VIBRATION, true),
            reminderMinutes = preferences.getInt(ReminderScheduler.KEY_MINUTES, DEFAULT_REMINDER_MINUTES).coerceIn(0, 180)
        )
    }
}
