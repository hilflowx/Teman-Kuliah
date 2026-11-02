package com.temankuliah.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * BroadcastReceiver yang menerima pemicu alarm dari AlarmManager
 * dan langsung menjalankan Foreground Service untuk memutar audio & getar.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, 0)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "Alarm"
        val message = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_MESSAGE) ?: "Waktunya kegiatan akademik!"

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_MESSAGE, message)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Exception) {
            val notification = NotificationHelper.buildAlarmNotification(context, alarmId, title, message)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.notify(AlarmService.notificationIdFor(alarmId), notification)
        }
    }
}
