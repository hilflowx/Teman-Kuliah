package com.temankuliah.app.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmModuleTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @Suppress("DEPRECATION")
    fun `schedule creates exact alarm clock in alarm manager`() {
        val triggerTime = System.currentTimeMillis() + 60_000L
        AlarmScheduler.schedule(
            context = context,
            alarmId = 101,
            triggerAtMillis = triggerTime,
            title = "Kuliah Pemrograman Mobile",
            message = "Ruang D.204"
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val shadowAlarmManager = shadowOf(alarmManager)
        val nextAlarm = shadowAlarmManager.nextScheduledAlarm

        assertNotNull("Alarm harus dijadwalkan di AlarmManager", nextAlarm)
        assertEquals(triggerTime, nextAlarm?.triggerAtTime)
    }

    @Test
    fun `cancel removes scheduled alarm`() {
        val triggerTime = System.currentTimeMillis() + 120_000L
        AlarmScheduler.schedule(context, 102, triggerTime, "Tugas Akhir", "Segera kumpulkan")
        AlarmScheduler.cancel(context, 102)

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms

        assertTrue("Alarm yang dibatalkan tidak boleh ada di antrean aktif", scheduledAlarms.isEmpty())
    }

    @Test
    fun `notification channel is created with high priority`() {
        NotificationHelper.createNotificationChannel(context)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID)

        assertNotNull("Kanal notifikasi alarm harus terdaftar", channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }

    @Test
    fun `notification helper builds valid alarm notification`() {
        val notification = NotificationHelper.buildAlarmNotification(
            context = context,
            alarmId = 201,
            title = "Ujian Tengah Semester",
            message = "Pukul 08.00 WIB"
        )

        assertNotNull("Notifikasi alarm tidak boleh null", notification)
        assertNotNull("FullScreenIntent harus disematkan", notification.fullScreenIntent)
        assertEquals(2, notification.actions.size)
    }

    @Test
    fun `alarm receiver triggers start alarm service intent`() {
        val receiver = AlarmReceiver()
        val intent = Intent().apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, 301)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, "Praktikum Jaringan")
            putExtra(AlarmScheduler.EXTRA_ALARM_MESSAGE, "Lab Komputer 3")
        }

        receiver.onReceive(context, intent)

        val started = shadowOf(context as android.app.Application).nextStartedService
        assertNotNull("Receiver harus menjalankan AlarmService", started)
        assertEquals(AlarmService::class.java.name, started.component?.className)
        assertEquals(AlarmService.ACTION_START_ALARM, started.action)
        assertEquals(301, started.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1))
        assertEquals("Praktikum Jaringan", started.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE))
    }
}
