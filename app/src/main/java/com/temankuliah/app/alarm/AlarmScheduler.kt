package com.temankuliah.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Pengatur penjadwalan alarm menggunakan AlarmManager.setAlarmClock()
 * agar alarm tetap berdering tepat waktu menembus Doze mode sistem.
 */
object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_TITLE = "extra_alarm_title"
    const val EXTRA_ALARM_MESSAGE = "extra_alarm_message"

    /**
     * Memeriksa apakah aplikasi memiliki izin menyetel alarm presisi (Android 12+).
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Menjadwalkan alarm dengan setAlarmClock.
     */
    fun schedule(
        context: Context,
        alarmId: Int,
        triggerAtMillis: Long,
        title: String = "Alarm",
        message: String = "Waktunya kegiatan akademik!"
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return

        val showIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, title)
            putExtra(EXTRA_ALARM_MESSAGE, message)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, title)
            putExtra(EXTRA_ALARM_MESSAGE, message)
        }
        val operationPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setAlarmClock(alarmClockInfo, operationPendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operationPendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operationPendingIntent)
            } catch (e: SecurityException) {
                Log.w(TAG, "Gagal menjadwalkan alarm $alarmId", e)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Batas alarm aplikasi tercapai; alarm $alarmId tidak dijadwalkan", e)
            }
        } catch (e: IllegalStateException) {
            // Batas alarm per aplikasi tercapai: lewati agar aplikasi tidak crash.
            Log.w(TAG, "Batas alarm aplikasi tercapai; alarm $alarmId tidak dijadwalkan", e)
        }
    }

    private const val MILLIS_PER_MINUTE = 60_000L

    /**
     * Menjadwalkan snooze (tunda) beberapa menit dari sekarang.
     */
    fun scheduleSnooze(
        context: Context,
        minutes: Int = 5,
        alarmId: Int = 0,
        title: String = "Alarm",
        message: String = "Waktunya kegiatan akademik!"
    ) {
        val triggerAt = System.currentTimeMillis() + (minutes * MILLIS_PER_MINUTE)
        schedule(context, alarmId, triggerAt, title, message)
    }

    /**
     * Membatalkan alarm yang telah dijadwalkan.
     */
    fun cancel(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val receiverIntent = Intent(context, AlarmReceiver::class.java)
        // NO_CREATE: bila tidak ada alarm terjadwal, tidak perlu membuat PendingIntent baru hanya untuk dibatalkan.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            receiverIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
