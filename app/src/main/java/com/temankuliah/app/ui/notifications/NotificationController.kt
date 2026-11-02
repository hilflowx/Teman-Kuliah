package com.temankuliah.app.ui.notifications

import androidx.core.content.edit
import com.temankuliah.app.data.local.AcademicEventEntity
import com.temankuliah.app.data.local.AppStorage
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.domain.NotificationItem
import com.temankuliah.app.domain.buildNotificationFeed
import com.temankuliah.app.ui.FeatureContext
import com.temankuliah.app.ui.FeatureController
import com.temankuliah.app.ui.profile.ReminderPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

/** Satu notifikasi beserta status dibacanya. */
internal data class NotificationEntry(val item: NotificationItem, val isRead: Boolean)

/**
 * Daftar notifikasi di aplikasi. Isinya dihitung ulang dari data asli (jadwal, tugas, agenda) setiap
 * data berubah dan setiap menit, lalu mengikuti saklar di Profil > Pengingat. Status "sudah dibaca"
 * disimpan per akun di SharedPreferences.
 */
internal class NotificationController(
    ctx: FeatureContext,
    tasks: StateFlow<List<TaskEntity>>,
    schedules: StateFlow<List<ScheduleEntity>>,
    events: StateFlow<List<AcademicEventEntity>>,
    courses: StateFlow<List<CourseEntity>>,
    reminderPreferences: StateFlow<ReminderPreferences>
) : FeatureController(ctx) {
    private val preferences = AppStorage.getPreferences(application)
    private val readKeys = MutableStateFlow<Set<String>>(emptySet())

    private val minuteTicker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L)
        }
    }

    /** Data mentah yang menjadi bahan daftar notifikasi. */
    private class Sources(
        val tasks: List<TaskEntity>,
        val schedules: List<ScheduleEntity>,
        val events: List<AcademicEventEntity>,
        val courses: List<CourseEntity>,
        val prefs: ReminderPreferences
    )

    private val sources: Flow<Sources> = combine(
        tasks, schedules, events, courses, reminderPreferences
    ) { taskList, scheduleList, eventList, courseList, prefs ->
        Sources(taskList, scheduleList, eventList, courseList, prefs)
    }

    private val items: StateFlow<List<NotificationItem>> = combine(
        sources, activeSemester, minuteTicker
    ) { source, semester, now ->
        buildNotificationFeed(
            nowMillis = now,
            zone = ZoneId.systemDefault(),
            semester = semester,
            schedules = source.schedules,
            tasks = source.tasks,
            events = source.events,
            courseNames = source.courses.associate { it.id to it.name },
            includeSchedules = source.prefs.classRemindersEnabled,
            includeTasks = source.prefs.taskNotificationsEnabled
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feed: StateFlow<List<NotificationEntry>> = combine(items, readKeys) { list, read ->
        list.map { NotificationEntry(it, it.key in read) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = feed
        .map { list -> list.count { !it.isRead } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Status dibaca dimuat ulang setiap akun berganti agar tidak tercampur antar akun.
        scope.launch {
            currentUser.collect { user ->
                readKeys.value = if (user == null) emptySet() else load(user.id)
            }
        }
    }

    fun markRead(key: String) {
        val user = currentUser.value ?: return
        update(user.id) { it + key }
    }

    fun markAllRead() {
        val user = currentUser.value ?: return
        val all = items.value.map { it.key }
        update(user.id) { it + all }
    }

    /** Menyimpan status baru dan membuang kunci milik pesan yang sudah tidak ada, agar tidak menumpuk. */
    private fun update(userId: Long, change: (Set<String>) -> Set<String>) {
        val live = items.value.map { it.key }.toSet()
        val changed = change(readKeys.value)
        // Tanpa daftar aktif (belum ada yang berlangganan) jangan memangkas apa pun.
        val next = if (live.isEmpty()) changed else changed.filterTo(HashSet()) { it in live }
        readKeys.value = next
        preferences.edit { putStringSet(prefKey(userId), next) }
    }

    private fun load(userId: Long): Set<String> =
        preferences.getStringSet(prefKey(userId), emptySet())?.toSet().orEmpty()

    private fun prefKey(userId: Long) = "notifications_read_keys_$userId"
}
