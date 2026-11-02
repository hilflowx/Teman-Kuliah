package com.temankuliah.app.data.notifications

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.temankuliah.app.data.local.AppStorage
import com.temankuliah.app.domain.ChannelState
import com.temankuliah.app.domain.CheckStatus
import com.temankuliah.app.domain.ReminderCheck
import com.temankuliah.app.domain.ReminderEnvironment
import com.temankuliah.app.domain.evaluateAlarmFire
import com.temankuliah.app.domain.evaluateNotificationDelivery
import com.temankuliah.app.domain.evaluateReminderEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Tes pengingat dari Pengaturan. Tes memakai jalur yang sama dengan pengingat asli (saluran notifikasi,
 * AlarmManager, lalu [ScheduleNotificationReceiver]) dan hanya melaporkan apa yang benar-benar terukur:
 * izin dan saluran dibaca dari sistem, notifikasi diperiksa di daftar notifikasi aktif, dan alarm dianggap
 * berhasil hanya bila receiver benar-benar terpanggil. Bunyi dan getar tidak dapat didengar aplikasi,
 * sehingga hanya pengaturan perangkatnya yang diperiksa.
 */
object ReminderSelfTest {
    /** Jeda sebelum alarm uji terpicu. */
    const val ALARM_DELAY_MS = 15_000L

    /** Tambahan waktu tunggu setelah waktu alarm; lebih panjang bila alarm presisi tidak diizinkan. */
    private const val EXACT_GRACE_MS = 30_000L
    private const val INEXACT_GRACE_MS = 75_000L
    private const val VISIBILITY_POLL_MS = 100L
    private const val VISIBILITY_TIMEOUT_MS = 1_500L
    private const val WAIT_POLL_MS = 250L
    private const val MILLIS_PER_SECOND = 1000L

    /** Catatan alarm uji yang terpicu, dikirim dari receiver ke layar yang sedang menunggu. */
    data class AlarmFire(val token: Long, val firedAtMillis: Long, val posted: Boolean, val visible: Boolean?)

    private val _lastFire = MutableStateFlow<AlarmFire?>(null)
    val lastFire: StateFlow<AlarmFire?> = _lastFire.asStateFlow()

    /** Membaca kondisi perangkat saat ini. */
    internal fun snapshot(context: Context): ReminderEnvironment {
        val prefs = AppStorage.getPreferences(context)
        val sound = prefs.getBoolean(ReminderScheduler.KEY_SOUND, true)
        val vibration = prefs.getBoolean(ReminderScheduler.KEY_VIBRATION, true)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val channels = mutableListOf<ChannelState>()
        if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            listOf("task" to "Tugas", "class" to "Kuliah").forEach { (kind, label) ->
                val channel = manager.getNotificationChannel(ReminderScheduler.ensureChannel(manager, kind, sound, vibration))
                if (channel != null) channels += ChannelState(label, channel.importance, channel.sound != null)
            }
        }
        val filter = manager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        return ReminderEnvironment(
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            exactAlarmAllowed = ReminderScheduler.canScheduleExactAlarms(context),
            soundPreferred = sound,
            vibrationPreferred = vibration,
            channels = channels,
            doNotDisturbActive = filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN,
            ringerNormal = audio?.ringerMode == AudioManager.RINGER_MODE_NORMAL,
            notificationVolume = audio?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 0
        )
    }

    /** Tes notifikasi langsung: periksa pengaturan perangkat, kirim satu notifikasi uji, lalu pastikan notifikasi itu tampil. */
    suspend fun runNotificationTest(context: Context): List<ReminderCheck> = withContext(Dispatchers.Default) {
        val app = context.applicationContext
        val env = snapshot(app)
        val checks = evaluateReminderEnvironment(env, includeAlarm = false).toMutableList()
        if (!env.notificationsEnabled) {
            checks += evaluateNotificationDelivery(permitted = false, posted = false, visible = null)
        } else {
            val posted = ReminderScheduler.postTestNotification(
                app, "Tes notifikasi TemanKuliah", "Jika kamu membaca ini, notifikasi TemanKuliah berfungsi."
            )
            val visible = if (posted) awaitTestNotificationVisible(app) else false
            checks += evaluateNotificationDelivery(permitted = true, posted = posted, visible = visible)
        }
        checks
    }

    /**
     * Tes alarm: memasang alarm [ALARM_DELAY_MS] ke depan lewat AlarmManager, menunggu sampai receiver terpanggil,
     * lalu memeriksa ketepatan waktunya dan notifikasi yang ditampilkan. [onWaiting] menerima sisa detik menuju
     * alarm. Bila coroutine dibatalkan atau waktu tunggu habis, alarm uji dibatalkan.
     */
    suspend fun runAlarmTest(context: Context, onWaiting: (secondsLeft: Int) -> Unit): List<ReminderCheck> {
        val app = context.applicationContext
        val env = withContext(Dispatchers.Default) { snapshot(app) }
        val checks = evaluateReminderEnvironment(env, includeAlarm = true).toMutableList()

        val token = System.currentTimeMillis()
        val triggerAt = token + ALARM_DELAY_MS
        val mode = ReminderScheduler.armTestAlarm(app, token, triggerAt)
        if (mode == ReminderScheduler.AlarmMode.FAILED) {
            checks += ReminderCheck(
                "Alarm terpicu", CheckStatus.FAILED,
                "Android menolak alarm uji (batas alarm aplikasi tercapai atau izin ditolak)."
            )
            return checks
        }
        val exact = mode == ReminderScheduler.AlarmMode.EXACT
        val deadline = triggerAt + if (exact) EXACT_GRACE_MS else INEXACT_GRACE_MS
        var fire: AlarmFire? = null
        try {
            while (true) {
                fire = _lastFire.value?.takeIf { it.token == token }
                val now = System.currentTimeMillis()
                if (fire != null || now >= deadline) break
                onWaiting(((triggerAt - now).coerceAtLeast(0L) / MILLIS_PER_SECOND).toInt())
                delay(WAIT_POLL_MS)
            }
        } finally {
            if (fire == null) ReminderScheduler.cancelTestAlarm(app)
        }

        val waitedSeconds = ((deadline - token) / MILLIS_PER_SECOND).toInt()
        checks += evaluateAlarmFire(triggerAt, fire?.firedAtMillis, exact, waitedSeconds)
        if (fire != null) {
            checks += evaluateNotificationDelivery(
                permitted = env.notificationsEnabled,
                posted = fire.posted,
                visible = fire.visible
            )
        }
        return checks
    }

    /** Dipanggil receiver saat alarm uji terpicu: kirim notifikasi uji, periksa tampilnya, lalu catat hasilnya. */
    internal suspend fun onAlarmFired(context: Context, token: Long, firedAtMillis: Long) {
        val app = context.applicationContext
        val time = Instant.ofEpochMilli(firedAtMillis).atZone(ZoneId.systemDefault()).toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val posted = NotificationManagerCompat.from(app).areNotificationsEnabled() &&
            ReminderScheduler.postTestNotification(app, "Tes alarm TemanKuliah", "Alarm uji terpicu pukul $time.")
        val visible = if (posted) awaitTestNotificationVisible(app) else false
        _lastFire.value = AlarmFire(token, firedAtMillis, posted, visible)
    }

    /** Menunggu sebentar sampai notifikasi uji terbaca di daftar aktif; null bila daftar tidak dapat dibaca. */
    @Suppress("ReturnCount") // Loop menunggu dengan beberapa jalur keluar (null/true/false); return dini paling jelas.
    private suspend fun awaitTestNotificationVisible(context: Context): Boolean? {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return null
        var waited = 0L
        while (true) {
            val active = runCatching { manager.activeNotifications }.getOrNull() ?: return null
            if (active.any { it.id == ReminderScheduler.TEST_NOTIFICATION_ID }) return true
            if (waited >= VISIBILITY_TIMEOUT_MS) return false
            delay(VISIBILITY_POLL_MS)
            waited += VISIBILITY_POLL_MS
        }
    }
}
