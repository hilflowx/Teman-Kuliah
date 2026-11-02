package com.temankuliah.app.data.notifications

import com.temankuliah.app.domain.DEFAULT_REMINDER_MINUTES
import com.temankuliah.app.domain.ReminderWindow
import com.temankuliah.app.domain.TaskStatus

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.temankuliah.app.MainActivity
import com.temankuliah.app.R
import com.temankuliah.app.alarm.AlarmScheduler
import com.temankuliah.app.alarm.AlarmService
import com.temankuliah.app.alarm.ReminderNotificationView
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.data.local.TemanKuliahDatabase
import com.temankuliah.app.data.local.AcademicEventEntity
import com.temankuliah.app.data.local.AppStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReminderScheduler {
    const val KEY_TASKS = "notifications_tasks_enabled"
    const val KEY_CLASSES = "notifications_classes_enabled"
    const val KEY_SOUND = "notifications_sound_enabled"
    const val KEY_VIBRATION = "notifications_vibration_enabled"
    const val KEY_MINUTES = "notifications_reminder_minutes"
    private const val REMINDER_ACCENT_COLOR = 0xFF1F6FD9.toInt()
    private const val EXTRA_KIND = "kind"
    private const val EXTRA_ROW_ID = "row_id"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_MESSAGE = "message"
    /** Tab tujuan saat notifikasi diketuk: 2 = Tugas, 1 = Kalender. */
    const val EXTRA_OPEN_TAB = "open_tab"
    const val ACTION_TASK_DONE = "com.temankuliah.app.action.TASK_DONE"
    const val ACTION_TASK_SNOOZE = "com.temankuliah.app.action.TASK_SNOOZE"
    const val SNOOZE_MINUTES = 10L
    /** Pola getar pengingat (ms): diam, getar, jeda, getar. Dipakai channel, notifikasi, dan efek langsung. */
    private val VIBRATION_PATTERN = longArrayOf(0, 300, 200, 300)
    /** Lama nada dering efek langsung sebelum dihentikan. */
    private const val RINGTONE_STOP_DELAY_MS = 6_000L

    /** Aksi dan ID khusus tes pengingat; tidak bentrok dengan ID baris tugas/jadwal. */
    const val ACTION_TEST_ALARM = "com.temankuliah.app.action.TEST_ALARM"
    const val EXTRA_TEST_TOKEN = "test_token"
    const val TEST_NOTIFICATION_ID = Int.MAX_VALUE - 7
    private const val TEST_REQUEST_CODE = Int.MAX_VALUE - 7

    /** Cara alarm benar-benar dipasang ke AlarmManager. */
    enum class AlarmMode { EXACT, INEXACT, FAILED }

    /** Scope kerja receiver; dapat diganti di unit test agar pekerjaan async bisa ditunggu. */
    internal var receiverScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    /** Penyedia database untuk receiver; dapat diganti di unit test dengan database in-memory. */
    internal var databaseProvider: (Context) -> TemanKuliahDatabase = { TemanKuliahDatabase.getDatabase(it) }

    /**
     * [notifyIfWindowOpen]: bila waktu pengingat sudah lewat tetapi deadline belum, tampilkan notifikasi
     * sekali sekarang. Dipakai saat tugas dibuat atau deadline-nya diubah, bukan saat penjadwalan ulang
     * otomatis (login, boot), agar tidak memunculkan notifikasi yang sama berulang kali.
     */
    fun scheduleTask(context: Context, task: TaskEntity, reminderMinutes: Int, notifyIfWindowOpen: Boolean = false) {
        val prefs = AppStorage.getPreferences(context)
        if (task.status == TaskStatus.SELESAI) {
            // Ditandai selesai: alarm dan notifikasi yang masih tampil ikut dibersihkan.
            cancel(context, kind = "task", id = task.id)
            return
        }
        if (!prefs.getBoolean(KEY_TASKS, true)) {
            cancelAlarm(context, "task", task.id)
            return
        }
        val now = System.currentTimeMillis()
        val triggerAt = task.dueAtMillis - reminderMinutes.coerceIn(0, 180) * 60_000L
        if (triggerAt <= now) {
            // Waktu pengingat sudah lewat: batalkan alarm, jangan hapus notifikasi yang sedang tampil.
            cancelAlarm(context, "task", task.id)
            if (notifyIfWindowOpen && task.dueAtMillis > now) {
                showReminder(context, "task", task.id, taskTitle(task), taskMessage(task))
            }
            return
        }
        schedule(context, "task", task.id, taskTitle(task), taskMessage(task), triggerAt)
    }

    fun cancelTask(context: Context, taskId: Long) = cancel(context, "task", taskId)

    fun scheduleClass(context: Context, schedule: ScheduleEntity) {
        val prefs = AppStorage.getPreferences(context)
        if (schedule.isCompleted) {
            // Ditandai selesai: alarm dan notifikasi yang masih tampil ikut dibersihkan.
            cancel(context, "class", schedule.id)
            return
        }
        if (!prefs.getBoolean(KEY_CLASSES, true) || !schedule.alarmEnabled) {
            cancelAlarm(context, "class", schedule.id)
            return
        }
        val next = nextClassReminder(schedule)
        if (next == null) {
            // Pengingat sudah lewat, di luar jendela alarm (dipasang nanti), atau tanggal/jam tidak valid: batalkan alarm saja.
            cancelAlarm(context, "class", schedule.id)
            return
        }
        val minutes = schedule.reminderMinutesBefore.coerceIn(0, 180)
        val lead = if (minutes == 0) "sekarang" else "$minutes menit lagi"
        schedule(context, "class", schedule.id, "Kuliah ${schedule.courseName}", "Mulai pukul ${schedule.startTime} ($lead) · ${schedule.room.ifBlank { "ruangan belum diatur" }}", next)
    }

    fun cancelClass(context: Context, scheduleId: Long) = cancel(context, "class", scheduleId)

    /**
     * Alarm pengingat untuk ujian, kegiatan, dan acara kalender. Satu kejadian pada satu waktu; alarm hanya
     * dipasang bila pengingat aktif dan waktunya belum lewat. Jumlah agenda kecil, jadi tidak dibatasi jendela hari
     * seperti jadwal kuliah (acara yang dibuat jauh hari tetap berbunyi walau aplikasi tidak dibuka).
     */
    fun scheduleEvent(context: Context, event: AcademicEventEntity) {
        if (!event.alarmEnabled) {
            cancelAlarmCompletely(context, "event", event.id)
            return
        }
        val minutes = event.reminderMinutesBefore.coerceIn(0, 180)
        val triggerAt = event.scheduledAtMillis - minutes * 60_000L
        if (triggerAt <= System.currentTimeMillis()) {
            cancelAlarm(context, "event", event.id)
            return
        }
        val lead = if (minutes == 0) "sekarang" else "$minutes menit lagi"
        val place = event.description.trim()
        val message = "Mulai pukul ${formatClock(event.scheduledAtMillis)} ($lead)" + if (place.isEmpty()) "" else " · $place"
        schedule(context, "event", event.id, "${event.type}: ${event.title}", message, triggerAt)
    }

    fun cancelEvent(context: Context, eventId: Long) = cancel(context, "event", eventId)

    /**
     * Menjadwalkan ulang alarm hanya untuk data milik [userId] (akun aktif). Alarm semua akun lain
     * dibatalkan supaya notifikasi akun lain tidak muncul di perangkat bersama.
     */
    suspend fun rescheduleAll(context: Context, database: TemanKuliahDatabase, userId: Long) {
        val prefs = AppStorage.getPreferences(context)
        val minutes = prefs.getInt(KEY_MINUTES, DEFAULT_REMINDER_MINUTES).coerceIn(0, 180)
        val tasks = database.taskDao().getAllByUserOnce(userId)
        val schedules = database.scheduleDao().getAllSchedulesByUserOnce(userId)
        val allEvents = database.academicEventDao().getAllOnce()
        database.taskDao().getAllOnce().filter { it.userId != userId }.forEach { cancelAlarmCompletely(context, "task", it.id) }
        database.scheduleDao().getAllSchedulesOnce().filter { it.userId != userId }.forEach { cancelAlarmCompletely(context, "class", it.id) }
        allEvents.filter { it.userId != userId }.forEach { cancelAlarmCompletely(context, "event", it.id) }
        tasks.forEach { scheduleTask(context, it, minutes) }
        schedules.forEach { scheduleClass(context, it) }
        allEvents.filter { it.userId == userId }.forEach { scheduleEvent(context, it) }
    }

    /** Menjadwalkan ulang untuk akun yang sedang masuk; bila tidak ada akun aktif, semua alarm dibatalkan. */
    suspend fun rescheduleForActiveUser(context: Context, database: TemanKuliahDatabase) {
        val activeUserId = database.userDao().getActiveUser()?.id
        if (activeUserId != null) rescheduleAll(context, database, activeUserId) else cancelAllAlarms(context, database)
    }

    /** Membatalkan alarm semua tugas dan jadwal di database (mis. saat logout). Notifikasi yang sudah tampil tidak disentuh. */
    suspend fun cancelAllAlarms(context: Context, database: TemanKuliahDatabase) {
        database.taskDao().getAllOnce().forEach { cancelAlarmCompletely(context, "task", it.id) }
        database.scheduleDao().getAllSchedulesOnce().forEach { cancelAlarmCompletely(context, "class", it.id) }
        database.academicEventDao().getAllOnce().forEach { cancelAlarmCompletely(context, "event", it.id) }
    }

    /** Membatalkan alarm untuk ID yang sudah dikumpulkan sebelumnya (mis. sebelum restore menghapus datanya). */
    fun cancelAlarms(context: Context, taskIds: Collection<Long>, scheduleIds: Collection<Long>, eventIds: Collection<Long> = emptyList()) {
        taskIds.forEach { cancelAlarmCompletely(context, "task", it) }
        scheduleIds.forEach { cancelAlarmCompletely(context, "class", it) }
        eventIds.forEach { cancelAlarmCompletely(context, "event", it) }
    }

    suspend fun onClassReminderFired(context: Context, scheduleId: Long, database: TemanKuliahDatabase) {
        // Jadwal kuliah adalah satu kejadian pada satu tanggal, bukan pengingat berulang.
        // Hanya alarm yang dibersihkan; notifikasi yang baru ditampilkan harus tetap ada.
        val schedule = database.scheduleDao().getById(scheduleId)
        if (schedule != null) {
            cancelAlarm(context, "class", scheduleId)
            database.scheduleDao().updateSchedule(schedule.copy(alarmEnabled = false))
            // Satu alarm selesai: isi lagi jendela alarm dengan pertemuan berikutnya (lihat ReminderWindow).
            try {
                rescheduleForActiveUser(context, database)
            } catch (error: Exception) {
                com.temankuliah.app.domain.rethrowIfCancellation(error)
            }
        }
    }

    private fun schedule(context: Context, kind: String, id: Long, title: String, message: String, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).apply {
            putExtra(EXTRA_KIND, kind); putExtra(EXTRA_ROW_ID, id); putExtra(EXTRA_TITLE, title); putExtra(EXTRA_MESSAGE, message)
        }
        val pending = PendingIntent.getBroadcast(context, requestCode(kind, id), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        setAlarm(context, alarmManager, triggerAtMillis, pending)
    }

    /**
     * Memasang alarm tepat waktu. Urutan: (1) alarm presisi bila izin diberikan; (2) bila izin presisi
     * ditolak, alarm jam ([AlarmManager.setAlarmClock]) yang tetap tepat waktu, bebas throttle Doze, dan
     * tidak butuh izin khusus; (3) terakhir baru alarm tidak presisi. Mengembalikan cara yang benar-benar
     * dipakai supaya tes alarm bisa melaporkannya dengan jujur.
     */
    @Suppress("ReturnCount") // Tiga jalur cadangan alarm berurutan; return dini lebih jelas daripada flag.
    private fun setAlarm(context: Context, alarmManager: AlarmManager, triggerAtMillis: Long, pending: PendingIntent): AlarmMode {
        val exactAllowed = canScheduleExactAlarms(context)
        if (exactAllowed) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                return AlarmMode.EXACT
            } catch (_: SecurityException) {
                // Izin dicabut tepat saat dipasang: lanjut ke alarm jam.
            } catch (_: IllegalStateException) {
                return AlarmMode.FAILED
            }
        }
        try {
            val showIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), pending)
            return AlarmMode.EXACT
        } catch (_: SecurityException) {
            // lanjut ke alarm tidak presisi
        } catch (_: IllegalStateException) {
            // Batas alarm per aplikasi tercapai: lewati agar tidak menutup aplikasi; akan dicoba lagi saat penjadwalan ulang.
            return AlarmMode.FAILED
        }
        val inexactOk = runCatching { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending) }.isSuccess
        return if (inexactOk) AlarmMode.INEXACT else AlarmMode.FAILED
    }

    /** Memasang alarm sekali pakai untuk tes; jalurnya sama dengan alarm pengingat asli (AlarmManager lalu receiver). */
    internal fun armTestAlarm(context: Context, token: Long, triggerAtMillis: Long): AlarmMode {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return AlarmMode.FAILED
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).setAction(ACTION_TEST_ALARM).putExtra(EXTRA_TEST_TOKEN, token)
        val pending = PendingIntent.getBroadcast(context, TEST_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return setAlarm(context, alarmManager, triggerAtMillis, pending)
    }

    internal fun cancelTestAlarm(context: Context) {
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).setAction(ACTION_TEST_ALARM)
        val pending = PendingIntent.getBroadcast(context, TEST_REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pending != null) {
            (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(pending)
            pending.cancel()
        }
    }

    /** Tugas/jadwal dihapus atau ditandai selesai: batalkan alarm sekaligus notifikasi yang sedang tampil. */
    private fun cancel(context: Context, kind: String, id: Long) {
        cancelAlarmCompletely(context, kind, id)
        cancelNotification(context, kind, id)
    }

    /**
     * Membatalkan alarm utama, alarm tunda ([AlarmScheduler.scheduleSnooze]) dengan ID yang sama, dan
     * menghentikan bunyi bila alarm itu sedang berdering. Hanya dipakai pada pembatalan eksplisit (hapus,
     * selesai, logout, restore, ganti akun). Penjadwalan ulang biasa memakai [cancelAlarm] saja supaya
     * alarm tunda milik akun aktif tidak ikut terhapus tiap kali aplikasi dibuka.
     */
    private fun cancelAlarmCompletely(context: Context, kind: String, id: Long) {
        cancelAlarm(context, kind, id)
        val alarmId = alarmIdFor(kind, id)
        AlarmScheduler.cancel(context, alarmId)
        AlarmService.stopIfRinging(context, alarmId)
    }

    /** Apakah alarm presisi diizinkan. Di bawah Android 12 izin ini tidak diperlukan. */
    fun canScheduleExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.canScheduleExactAlarms() == true

    /** Apakah layar penuh alarm diizinkan. Di Android 14+ izin ini bisa dicabut pengguna. */
    fun canUseFullScreenIntent(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager)
                ?.canUseFullScreenIntent() == true

    /** Hanya membatalkan AlarmManager + PendingIntent; notifikasi yang sedang tampil tidak disentuh. */
    internal fun cancelAlarm(context: Context, kind: String, id: Long) {
        val intent = Intent(context, ScheduleNotificationReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, requestCode(kind, id), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pending != null) {
            (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(pending)
            pending.cancel()
        }
    }

    /** Hanya menghapus notifikasi yang sedang tampil; alarm tidak disentuh. */
    internal fun cancelNotification(context: Context, kind: String, id: Long) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(requestCode(kind, id))
    }

    private fun taskTitle(task: TaskEntity): String = "Tenggat: ${task.title}"

    private fun taskMessage(task: TaskEntity): String = "Tenggat pukul ${formatClock(task.dueAtMillis)}."

    /** PendingIntent tombol aksi. Action berbeda dari alarm sehingga tidak saling menimpa. */
    private fun actionPending(context: Context, action: String, id: Long): PendingIntent {
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).setAction(action).putExtra(EXTRA_ROW_ID, id)
        return PendingIntent.getBroadcast(context, requestCode("task", id), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * Dipanggil dari tombol notifikasi. "Selesai" menandai tugas selesai dan membersihkan alarm/notifikasi.
     * "Tunda" menjadwalkan pengingat ulang [SNOOZE_MINUTES] menit lagi, tidak melewati deadline.
     */
    internal suspend fun handleTaskAction(context: Context, taskId: Long, snooze: Boolean, database: TemanKuliahDatabase) {
        val task = database.taskDao().getById(taskId)
        if (task == null || task.status == TaskStatus.SELESAI) {
            cancel(context, "task", taskId)
            return
        }
        if (!snooze) {
            database.taskDao().update(task.copy(status = TaskStatus.SELESAI))
            cancel(context, "task", taskId)
            return
        }
        cancelNotification(context, "task", taskId)
        val now = System.currentTimeMillis()
        val triggerAt = minOf(now + SNOOZE_MINUTES * 60_000L, task.dueAtMillis)
        if (triggerAt <= now) {
            cancelAlarm(context, "task", taskId)
            return
        }
        schedule(context, "task", taskId, taskTitle(task), taskMessage(task), triggerAt)
    }

    /** Jam lokal dari waktu epoch, format HH:mm. */
    private fun formatClock(millis: Long): String =
        java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

    private fun requestCode(kind: String, id: Long): Int {
        // Selisih tiap jenis harus berbeda modulo 31 supaya tugas #5, kuliah #5, dan acara #5 tidak bentrok.
        val offset = when (kind) { "task" -> 13L; "event" -> 7L; else -> 29L }
        return ((id * 31L + offset) and 0x7fffffff).toInt()
    }

    /** ID alarm native ([AlarmScheduler]/[AlarmService]); sama dengan requestCode agar tugas #5 dan kuliah #5 tidak bentrok. */
    internal fun alarmIdFor(kind: String, id: Long): Int = requestCode(kind, id)

    private fun nextClassReminder(schedule: ScheduleEntity): Long? {
        val date = runCatching { LocalDate.parse(schedule.scheduledDate) }.getOrNull() ?: return null
        val time = runCatching { LocalTime.parse(schedule.startTime) }.getOrNull() ?: return null
        val triggerAt = date.atTime(time)
            .minusMinutes(schedule.reminderMinutesBefore.coerceIn(0, 180).toLong())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return triggerAt.takeIf { ReminderWindow.shouldArmClassAlarm(it, System.currentTimeMillis()) }
    }

    /**
     * Membuat (bila belum ada) saluran notifikasi untuk [kind] dan varian bunyi/getar, lalu mengembalikan ID-nya.
     * Menggunakan channel id v3 agar konfigurasi sound & vibration diperbarui pada instalasi perangkat yang sudah ada.
     */
    internal fun ensureChannel(manager: NotificationManager, kind: String, sound: Boolean, vibration: Boolean): String {
        val channelId = buildChannelId(kind, sound, vibration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(channelId) == null) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttributes = notificationAudioAttributes()
            val channel = NotificationChannel(channelId, channelTitle(kind, sound, vibration), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(if (sound) soundUri else null, if (sound) audioAttributes else null)
                enableVibration(vibration)
                vibrationPattern = if (vibration) VIBRATION_PATTERN else null
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableLights(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
        return channelId
    }

    private fun buildChannelId(kind: String, sound: Boolean, vibration: Boolean): String {
        val soundPart = if (sound) "sound" else "silent"
        val vibrationPart = if (vibration) "vibrate" else "still"
        return "temankuliah_v3_${kind}_${soundPart}_$vibrationPart"
    }

    private fun notificationAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .build()

    /** Nama saluran yang tampil di pengaturan sistem, mis. "Pengingat Tugas · Bunyi, Getar". */
    private fun channelTitle(kind: String, sound: Boolean, vibration: Boolean): String {
        val jenis = when (kind) { "task" -> "Tugas"; "event" -> "Acara"; else -> "Kuliah" }
        val varian = listOfNotNull(if (sound) "Bunyi" else null, if (vibration) "Getar" else null).joinToString(", ").ifEmpty { "Senyap" }
        return "Pengingat $jenis · $varian"
    }

    @Volatile private var activeRingtone: android.media.Ringtone? = null

    /** Memainkan getaran dan nada dering secara langsung untuk memastikan bunyi/getar terdengar pada skin Android (seperti Realme UI). */
    fun triggerAlertEffects(context: Context, sound: Boolean, vibration: Boolean) {
        if (vibration) vibrateNow(context)
        if (sound) playRingtoneNow(context)
    }

    private fun vibrateNow(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator?.vibrate(android.os.VibrationEffect.createWaveform(VIBRATION_PATTERN, -1))
            } else {
                @Suppress("DEPRECATION")
                val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib?.vibrate(android.os.VibrationEffect.createWaveform(VIBRATION_PATTERN, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vib?.vibrate(VIBRATION_PATTERN, -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun playRingtoneNow(context: Context) {
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            // Simpan referensi agar tidak di-GC (bunyi terpotong), dan hentikan setelah beberapa detik.
            activeRingtone?.stop()
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, soundUri)
            activeRingtone = ringtone
            ringtone?.audioAttributes = notificationAudioAttributes()
            ringtone?.play()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ runCatching { ringtone?.stop() } }, RINGTONE_STOP_DELAY_MS)
        } catch (_: Exception) {}
    }

    @Suppress("LongParameterList") // Semua argumen dipakai; mengelompokkannya ke objek akan mengubah pemanggil alarm.
    private fun baseBuilder(
        context: Context, channelId: String, title: String, message: String,
        openPending: PendingIntent, sound: Boolean, vibration: Boolean
    ): NotificationCompat.Builder {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val pattern = VIBRATION_PATTERN
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_temankuliah)
            .setColor(REMINDER_ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(ReminderNotificationView.build(context, title, message))
            .setCustomBigContentView(ReminderNotificationView.build(context, title, message))
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (sound) {
            builder.setSound(soundUri)
        }
        if (vibration) {
            builder.setVibrate(pattern)
        }
        var defaults = 0
        if (sound) defaults = defaults or NotificationCompat.DEFAULT_SOUND
        if (vibration) defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
        builder.setDefaults(defaults)

        return builder
    }

    /**
     * Mengirim notifikasi uji lewat saluran pengingat kuliah dengan bunyi/getar tersimpan. Tidak mengikuti
     * saklar pengingat tugas/kuliah, karena yang diuji adalah kemampuan perangkat menampilkan notifikasi.
     * Notifikasi uji lama dihapus dulu agar pemeriksaan "tampil" tidak terkecoh sisa tes sebelumnya.
     * Mengembalikan true bila Android menerima panggilan notify tanpa galat.
     */
    internal fun postTestNotification(context: Context, title: String, message: String): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        val prefs = AppStorage.getPreferences(context)
        val sound = prefs.getBoolean(KEY_SOUND, true)
        val vibration = prefs.getBoolean(KEY_VIBRATION, true)
        val channelId = ensureChannel(manager, "class", sound, vibration)
        val openIntent = Intent(context, MainActivity::class.java).setAction("com.temankuliah.app.action.OPEN_test")
        val openPending = PendingIntent.getActivity(
            context, TEST_NOTIFICATION_ID, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = baseBuilder(context, channelId, title, message, openPending, sound, vibration).build()
        manager.cancel(TEST_NOTIFICATION_ID)
        val posted = runCatching { manager.notify(TEST_NOTIFICATION_ID, notification) }.isSuccess
        if (posted) {
            triggerAlertEffects(context, sound, vibration)
        }
        return posted
    }

    internal fun showReminder(context: Context, kind: String, id: Long, title: String, message: String) {
        val prefs = AppStorage.getPreferences(context)
        // Acara kalender diatur per agenda (saklar di dialog), bukan lewat pengaturan global tugas/kuliah.
        val enabled = when (kind) { "task" -> prefs.getBoolean(KEY_TASKS, true); "event" -> true; else -> prefs.getBoolean(KEY_CLASSES, true) }
        if (!enabled) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val sound = prefs.getBoolean(KEY_SOUND, true)
        val vibration = prefs.getBoolean(KEY_VIBRATION, true)
        val channelId = ensureChannel(manager, kind, sound, vibration)
        // Action unik per jenis+ID agar PendingIntent tidak saling menimpa.
        val openIntent = Intent(context, MainActivity::class.java)
            .setAction("com.temankuliah.app.action.OPEN_${kind}_$id")
            .putExtra(EXTRA_OPEN_TAB, if (kind == "task") 2 else 1)
        val openPending = PendingIntent.getActivity(context, requestCode(kind, id), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = baseBuilder(context, channelId, title, message, openPending, sound, vibration)
        if (kind == "task") {
            builder.addAction(0, "Tandai selesai", actionPending(context, ACTION_TASK_DONE, id))
            builder.addAction(0, "Tunda $SNOOZE_MINUTES menit", actionPending(context, ACTION_TASK_SNOOZE, id))
        }
        val notification = builder.build()
        runCatching { manager.notify(requestCode(kind, id), notification) }
        triggerAlertEffects(context, sound, vibration)
    }
}

class ScheduleNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ReminderScheduler.ACTION_TEST_ALARM) {
            val token = intent.getLongExtra(ReminderScheduler.EXTRA_TEST_TOKEN, 0L)
            val firedAt = System.currentTimeMillis()
            val pending = goAsync()
            ReminderScheduler.receiverScope.launch {
                try {
                    // Pemicu AlarmService agar tes alarm benar-benar berbunyi keras dan memunculkan layar penuh
                    try {
                        val testServiceIntent = Intent(context, com.temankuliah.app.alarm.AlarmService::class.java).apply {
                            action = com.temankuliah.app.alarm.AlarmService.ACTION_START_ALARM
                            putExtra(com.temankuliah.app.alarm.AlarmScheduler.EXTRA_ALARM_ID, ReminderScheduler.TEST_NOTIFICATION_ID)
                            putExtra(com.temankuliah.app.alarm.AlarmScheduler.EXTRA_ALARM_TITLE, "Tes Alarm TemanKuliah")
                            putExtra(com.temankuliah.app.alarm.AlarmScheduler.EXTRA_ALARM_MESSAGE, "Alarm uji sedang berdering!")
                        }
                        androidx.core.content.ContextCompat.startForegroundService(context, testServiceIntent)
                    } catch (e: Exception) {
                        android.util.Log.w("ScheduleNotificationReceiver", "Gagal memulai AlarmService untuk tes alarm", e)
                    }
                    ReminderSelfTest.onAlarmFired(context, token, firedAt)
                } finally {
                    pending?.finish()
                }
            }
            return
        }
        val id = intent.getLongExtra("row_id", -1L)
        if (intent.action == ReminderScheduler.ACTION_TASK_DONE || intent.action == ReminderScheduler.ACTION_TASK_SNOOZE) {
            if (id <= 0) return
            val snooze = intent.action == ReminderScheduler.ACTION_TASK_SNOOZE
            val pending = goAsync()
            ReminderScheduler.receiverScope.launch {
                try { ReminderScheduler.handleTaskAction(context, id, snooze, ReminderScheduler.databaseProvider(context)) }
                finally { pending.finish() }
            }
            return
        }
        val kind = intent.getStringExtra("kind") ?: return
        if (id <= 0) return
        val title = intent.getStringExtra("title") ?: "Pengingat TemanKuliah"
        val message = intent.getStringExtra("message") ?: "Ada kegiatan akademik yang akan dimulai."
        ReminderScheduler.showReminder(context, kind, id, title, message)

        // Kuliah dan acara kalender memakai alarm layar penuh (AlarmService); tugas cukup notifikasi dengan aksi Selesai/Tunda.
        if (kind == "event") startAlarmService(context, kind, id, title, message)
        if (kind == "class") {
            startAlarmService(context, kind, id, title, message)
            val pending = goAsync()
            ReminderScheduler.receiverScope.launch {
                try { ReminderScheduler.onClassReminderFired(context, id, ReminderScheduler.databaseProvider(context)) }
                finally { pending?.finish() }
            }
        }
    }

    private fun startAlarmService(context: Context, kind: String, id: Long, title: String, message: String) {
        try {
            val serviceIntent = Intent(context, com.temankuliah.app.alarm.AlarmService::class.java).apply {
                action = com.temankuliah.app.alarm.AlarmService.ACTION_START_ALARM
                putExtra(com.temankuliah.app.alarm.AlarmScheduler.EXTRA_ALARM_ID, ReminderScheduler.alarmIdFor(kind, id))
                putExtra(com.temankuliah.app.alarm.AlarmScheduler.EXTRA_ALARM_TITLE, title)
                putExtra(com.temankuliah.app.alarm.AlarmScheduler.EXTRA_ALARM_MESSAGE, message)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            android.util.Log.w("ScheduleNotificationReceiver", "Gagal memulai AlarmService; hanya notifikasi yang tampil", e)
        }
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                "android.intent.action.TIME_SET",
                Intent.ACTION_TIMEZONE_CHANGED,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
            )) return
        val pending = goAsync()
        ReminderScheduler.receiverScope.launch {
            try { ReminderScheduler.rescheduleForActiveUser(context, ReminderScheduler.databaseProvider(context)) }
            finally { pending?.finish() }
        }
    }
}
