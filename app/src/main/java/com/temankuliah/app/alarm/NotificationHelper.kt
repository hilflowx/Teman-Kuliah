package com.temankuliah.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.temankuliah.app.R

/**
 * Pengelola kanal dan penyusunan notifikasi alarm dengan full-screen intent.
 */
object NotificationHelper {

    const val CHANNEL_ID = "high_priority_alarm_channel"
    private const val ACCENT_COLOR = 0xFF1F6FD9.toInt()
    private const val CHANNEL_NAME = "Alarm Layar Penuh"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Saluran untuk alarm bersuara keras dan layar penuh"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                    enableVibration(false)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun buildAlarmNotification(
        context: Context,
        alarmId: Int,
        title: String,
        message: String
    ): Notification {
        createNotificationChannel(context)

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_MESSAGE, message)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            alarmId,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_MESSAGE, message)
        }
        val snoozePendingIntent = PendingIntent.getService(
            context,
            alarmId,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentView = ReminderNotificationView.build(context, title, message)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_temankuliah)
            .setColor(ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(contentView)
            .setCustomBigContentView(contentView)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hentikan", stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Tunda 5 mnt", snoozePendingIntent)
            .build()
    }
}
