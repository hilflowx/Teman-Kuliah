package com.temankuliah.app.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.temankuliah.app.R
import com.temankuliah.app.data.local.AppStorage
import java.io.IOException

/**
 * Foreground Service pemutar suara alarm keras berulang dan penggetar perangkat.
 * Menggunakan audio alarm_clock.mp3 dari res/raw dengan fallback berlapis ke nada alarm bawaan.
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable {
        Log.w(TAG, "Alarm dihentikan otomatis setelah ${AUTO_STOP_MS / MILLIS_PER_MINUTE} menit tanpa respons")
        stopAlarm()
        stopSelf()
    }

    companion object {
        private const val TAG = "AlarmService"
        const val ACTION_START_ALARM = "ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "ACTION_SNOOZE_ALARM"

        const val ACTION_START = ACTION_START_ALARM
        const val ACTION_STOP = ACTION_STOP_ALARM
        const val ACTION_SNOOZE = ACTION_SNOOZE_ALARM

        /** Siaran internal (dibatasi ke paket sendiri) saat bunyi berhenti, supaya [AlarmActivity] menutup diri. */
        const val ACTION_ALARM_STOPPED = "com.temankuliah.app.alarm.ACTION_ALARM_STOPPED"

        /** Penanda tidak ada alarm yang sedang berbunyi. */
        const val NO_ALARM = -1

        private const val SNOOZE_MINUTES = 5
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val WAKELOCK_TIMEOUT_MS = 10 * MILLIS_PER_MINUTE
        private const val AUTO_STOP_MS = 5 * MILLIS_PER_MINUTE
        private const val VIBRATION_REPEAT_INDEX = 0

        /** Pembeda ruang ID notifikasi alarm dari notifikasi pengingat biasa (yang memakai requestCode apa adanya). */
        private const val ALARM_NOTIFICATION_MASK = 0x40000000

        /** Kunci penyimpanan volume alarm asli pengguna; tahan terhadap proses yang dimatikan sistem. */
        private const val KEY_SAVED_ALARM_VOLUME = "alarm_saved_stream_volume"

        private val VIBRATION_PATTERN = longArrayOf(0, 800, 400, 800, 400)

        @Volatile private var ringingId: Int = NO_ALARM

        /** ID alarm yang sedang berbunyi, atau [NO_ALARM]. Hanya valid di proses aplikasi ini. */
        internal val ringingAlarmId: Int get() = ringingId

        /** ID notifikasi foreground per alarm, agar alarm kedua tidak menimpa notifikasi alarm pertama. */
        internal fun notificationIdFor(alarmId: Int): Int = alarmId xor ALARM_NOTIFICATION_MASK

        /**
         * Menghentikan bunyi bila alarm dengan [alarmId] itu yang sedang berbunyi (tugas dihapus atau selesai,
         * logout, ganti akun). Tidak melakukan apa pun bila tidak ada alarm berbunyi, sehingga tidak menyalakan service.
         */
        fun stopIfRinging(context: Context, alarmId: Int) {
            if (ringingId != alarmId) return
            try {
                context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_STOP_ALARM))
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Gagal menghentikan alarm yang sedang berbunyi", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initVibrator()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // Dimulai ulang sistem tanpa intent (proses sempat mati): jangan berbunyi lagi sebagai "Alarm" generik.
            stopSelf()
            return START_NOT_STICKY
        }
        val action = intent.action ?: ACTION_START_ALARM
        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, 0)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "Alarm"
        val message = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_MESSAGE) ?: "Waktunya kegiatan akademik!"

        when (action) {
            ACTION_STOP, "ACTION_STOP" -> {
                stopAlarm()
                stopSelf()
            }
            ACTION_SNOOZE, "ACTION_SNOOZE" -> {
                stopAlarm()
                AlarmScheduler.scheduleSnooze(this, SNOOZE_MINUTES, alarmId, title, message)
                stopSelf()
            }
            else -> startAlarm(alarmId, title, message)
        }

        // Tidak dimulai ulang otomatis: alarm yang prosesnya dimatikan sistem tidak boleh berbunyi lagi tanpa konteks.
        return START_NOT_STICKY
    }

    private fun startAlarm(alarmId: Int, title: String, message: String) {
        val previousId = ringingId
        ringingId = alarmId
        // startForeground() dipanggil sebagai baris pertama sebelum audio diputar
        val notification = NotificationHelper.buildAlarmNotification(this, alarmId, title, message)
        val notificationId = notificationIdFor(alarmId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
            ServiceCompat.startForeground(this, notificationId, notification, foregroundType)
        } else {
            startForeground(notificationId, notification)
        }
        if (previousId != NO_ALARM && previousId != alarmId) {
            // Alarm baru mengambil alih pemutar; bersihkan notifikasi alarm sebelumnya.
            NotificationManagerCompat.from(this).cancel(notificationIdFor(previousId))
        }

        // Buka AlarmActivity secara langsung agar layar penuh tampil baik saat layar terkunci maupun saat tidak terkunci
        launchAlarmActivity(alarmId, title, message)

        acquireWakeLock()
        startPlayback()
        startVibration()

        handler.removeCallbacks(autoStopRunnable)
        handler.postDelayed(autoStopRunnable, AUTO_STOP_MS)
    }

    private fun launchAlarmActivity(alarmId: Int, title: String, message: String) {
        if (!com.temankuliah.app.data.notifications.ReminderScheduler.canUseFullScreenIntent(this)) {
            Log.w(TAG, "Layar penuh tidak diizinkan; alarm hanya tampil sebagai notifikasi")
        }
        try {
            val activityIntent = Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
                putExtra(AlarmScheduler.EXTRA_ALARM_MESSAGE, message)
            }
            startActivity(activityIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal meluncurkan AlarmActivity secara langsung; mengandalkan fullScreenIntent", e)
        }
    }

    private fun rawAlarmResId(): Int = R.raw.alarm_clock

    private fun defaultAlarmUri(): Uri? =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun pickAlarmSound(): Uri? {
        val resId = rawAlarmResId()
        if (resId != 0) {
            return "android.resource://$packageName/$resId".toUri()
        }
        Log.w(TAG, "res/raw/alarm_clock.mp3 tidak ditemukan, fallback ke nada alarm bawaan")
        return defaultAlarmUri()
    }

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val am = audioManager ?: return false

        try {
            val prefs = AppStorage.getPreferences(this)
            // Simpan volume asli hanya sekali: bila proses sempat mati saat berbunyi, nilai asli tetap terjaga.
            if (!prefs.contains(KEY_SAVED_ALARM_VOLUME)) {
                prefs.edit { putInt(KEY_SAVED_ALARM_VOLUME, am.getStreamVolume(AudioManager.STREAM_ALARM)) }
            }
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mengatur volume alarm ke maksimum", e)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .build()
            audioFocusRequest = request
            am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            val prefs = AppStorage.getPreferences(this)
            val savedVolume = prefs.getInt(KEY_SAVED_ALARM_VOLUME, -1)
            if (savedVolume >= 0) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, savedVolume, 0)
                prefs.edit { remove(KEY_SAVED_ALARM_VOLUME) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mengembalikan audio focus / volume", e)
        }
    }

    @Suppress("DEPRECATION") // setLegacyStreamType diperlukan agar volume mengikuti STREAM_ALARM di Android lama.
    private fun alarmAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setLegacyStreamType(AudioManager.STREAM_ALARM)
        .build()

    /** Urutan sumber suara: alarm_clock.mp3 lewat URI, lewat fd resource, lalu nada alarm bawaan sistem. */
    private fun startPlayback() {
        if (mediaPlayer != null) return

        requestAudioFocus()
        val soundUri = pickAlarmSound()
        val rawResId = rawAlarmResId()
        mediaPlayer = tryPlay("URI") { setDataSource(this@AlarmService, requireNotNull(soundUri)) }
            ?: (if (rawResId != 0) tryPlay("raw resource fd") { setRawDataSource(rawResId) } else null)
            ?: tryPlay("nada alarm bawaan") { setDataSource(this@AlarmService, requireNotNull(defaultAlarmUri())) }
        if (mediaPlayer == null) {
            Log.e(TAG, "Semua sumber suara alarm gagal diputar; hanya getaran dan layar penuh yang aktif")
        }
    }

    private fun MediaPlayer.setRawDataSource(resId: Int) {
        // use{} menutup fd walau setDataSource melempar error.
        resources.openRawResourceFd(resId).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
    }

    /** Menyiapkan dan memutar satu sumber; bila gagal, pemutar dilepas (tidak bocor) dan hasilnya null. */
    private fun tryPlay(source: String, setup: MediaPlayer.() -> Unit): MediaPlayer? {
        val player = MediaPlayer()
        return try {
            player.setAudioAttributes(alarmAudioAttributes())
            player.setup()
            player.isLooping = true
            player.prepare()
            player.start()
            player
        } catch (e: IOException) {
            discardPlayer(player, source, e)
        } catch (e: IllegalStateException) {
            discardPlayer(player, source, e)
        } catch (e: IllegalArgumentException) {
            discardPlayer(player, source, e)
        } catch (e: SecurityException) {
            discardPlayer(player, source, e)
        }
    }

    private fun discardPlayer(player: MediaPlayer, source: String, error: Exception): MediaPlayer? {
        Log.e(TAG, "Gagal memutar audio alarm lewat $source", error)
        player.release()
        return null
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun startVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, VIBRATION_REPEAT_INDEX))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(VIBRATION_PATTERN, VIBRATION_REPEAT_INDEX)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal memulai getaran", e)
        }
    }

    private fun acquireWakeLock() {
        // Dipanggil per alarm (bukan di onCreate) karena stopAlarm() melepas wakelock; instance service bisa dipakai ulang.
        if (wakeLock?.isHeld == true) return
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TemanKuliah:AlarmServiceWakeLock")?.apply {
                setReferenceCounted(false)
                acquire(WAKELOCK_TIMEOUT_MS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal memperoleh WakeLock", e)
        }
    }

    private fun stopAlarm() {
        handler.removeCallbacks(autoStopRunnable)
        val stoppedId = ringingId
        ringingId = NO_ALARM

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error saat menghentikan MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error saat membatalkan Vibrator", e)
        }

        abandonAudioFocus()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error saat melepas WakeLock", e)
        } finally {
            wakeLock = null
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (stoppedId != NO_ALARM) {
            NotificationManagerCompat.from(this).cancel(notificationIdFor(stoppedId))
            // Tutup layar alarm juga bila bunyi dihentikan dari notifikasi, pembatalan, atau penghentian otomatis.
            sendBroadcast(Intent(ACTION_ALARM_STOPPED).setPackage(packageName))
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
