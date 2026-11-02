package com.temankuliah.app.alarm

import android.app.AlarmManager
import android.app.Application
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.data.notifications.ScheduleNotificationReceiver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import java.time.Duration

/** Pengujian perbaikan alarm: BL-02 sampai BL-07 (pembatalan, ID unik, penghentian, restart, layar alarm). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmReliabilityTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val controllers = mutableListOf<ServiceController<AlarmService>>()

    @After
    fun tearDown() {
        controllers.forEach { it.destroy() }
    }

    private fun scheduledAlarmCount(): Int =
        shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms.size

    private fun startIntent(alarmId: Int, title: String = "Alarm uji") =
        Intent(app, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_MESSAGE, "Pesan uji")
        }

    private fun startService(intent: Intent): ServiceController<AlarmService> =
        Robolectric.buildService(AlarmService::class.java, intent).create().startCommand(0, 1).also { controllers += it }

    // ---------------------------------------------------------------- BL-03

    @Test
    fun `alarm id tugas dan kuliah dengan id baris sama tidak bentrok`() {
        assertNotEquals(ReminderScheduler.alarmIdFor("task", 5L), ReminderScheduler.alarmIdFor("class", 5L))
    }

    @Test
    fun `receiver kuliah mengirim alarm id unik ke AlarmService`() {
        val intent = Intent(app, ScheduleNotificationReceiver::class.java)
            .putExtra("kind", "class").putExtra("row_id", 5L).putExtra("title", "Kuliah Jaringan").putExtra("message", "Pukul 10:00")
        ScheduleNotificationReceiver().onReceive(app, intent)

        val started = shadowOf(app).nextStartedService
        assertNotNull("AlarmService harus dijalankan", started)
        assertEquals(ReminderScheduler.alarmIdFor("class", 5L), started.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1))
    }

    @Test
    fun `receiver tugas hanya notifikasi dan tidak menjalankan AlarmService`() {
        val intent = Intent(app, ScheduleNotificationReceiver::class.java)
            .putExtra("kind", "task").putExtra("row_id", 5L).putExtra("title", "Deadline: Laporan").putExtra("message", "Pukul 10:00")
        ScheduleNotificationReceiver().onReceive(app, intent)

        assertNull("Tugas tidak boleh memicu alarm layar penuh", shadowOf(app).peekNextStartedService())
    }

    @Test
    fun `notifikasi foreground berbeda untuk tiap alarm dan tidak sama dengan id alarm`() {
        assertNotEquals(AlarmService.notificationIdFor(1), AlarmService.notificationIdFor(2))
        assertNotEquals(7, AlarmService.notificationIdFor(7))

        val controller = startService(startIntent(alarmId = 1))
        controller.get().onStartCommand(startIntent(alarmId = 2), 0, 2)
        assertEquals(AlarmService.notificationIdFor(2), shadowOf(controller.get()).lastForegroundNotificationId)
        assertEquals(2, AlarmService.ringingAlarmId)
    }

    // ---------------------------------------------------------------- BL-02

    @Test
    fun `membatalkan tugas ikut membatalkan alarm tunda`() {
        val taskAlarm = ReminderScheduler.alarmIdFor("task", 7L)
        AlarmScheduler.scheduleSnooze(app, 5, taskAlarm, "Tugas", "Segera")
        assertEquals(1, scheduledAlarmCount())

        ReminderScheduler.cancelTask(app, 7L)

        assertEquals("Antrean AlarmManager harus kosong setelah tugas dibatalkan", 0, scheduledAlarmCount())
    }

    @Test
    fun `membatalkan kuliah ikut membatalkan alarm tunda`() {
        AlarmScheduler.scheduleSnooze(app, 5, ReminderScheduler.alarmIdFor("class", 7L), "Kuliah", "Mulai")

        ReminderScheduler.cancelClass(app, 7L)

        assertEquals(0, scheduledAlarmCount())
    }

    @Test
    fun `membatalkan satu tugas tidak menghapus alarm tunda tugas lain`() {
        AlarmScheduler.scheduleSnooze(app, 5, ReminderScheduler.alarmIdFor("task", 7L), "Tugas 7", "x")
        AlarmScheduler.scheduleSnooze(app, 5, ReminderScheduler.alarmIdFor("task", 8L), "Tugas 8", "x")

        ReminderScheduler.cancelTask(app, 7L)

        assertEquals(1, scheduledAlarmCount())
    }

    @Test
    fun `cancelAlarms restore membatalkan alarm tunda semua id`() {
        AlarmScheduler.scheduleSnooze(app, 5, ReminderScheduler.alarmIdFor("task", 1L), "Tugas", "x")
        AlarmScheduler.scheduleSnooze(app, 5, ReminderScheduler.alarmIdFor("class", 2L), "Kuliah", "x")

        ReminderScheduler.cancelAlarms(app, listOf(1L), listOf(2L))

        assertEquals(0, scheduledAlarmCount())
    }

    // ---------------------------------------------------------------- BL-04

    @Test
    fun `stopIfRinging hanya menghentikan alarm dengan id yang sama`() {
        startService(startIntent(alarmId = 41))
        assertEquals(41, AlarmService.ringingAlarmId)

        AlarmService.stopIfRinging(app, 42)
        assertNull("Alarm lain tidak boleh dihentikan", shadowOf(app).peekNextStartedService())

        AlarmService.stopIfRinging(app, 41)
        assertEquals(AlarmService.ACTION_STOP_ALARM, shadowOf(app).nextStartedService.action)
    }

    @Test
    fun `stopIfRinging tidak menyalakan service bila tidak ada alarm berbunyi`() {
        AlarmService.stopIfRinging(app, 99)

        assertNull(shadowOf(app).peekNextStartedService())
    }

    @Test
    fun `membatalkan tugas yang sedang berbunyi menghentikan AlarmService`() {
        startService(startIntent(alarmId = ReminderScheduler.alarmIdFor("task", 9L)))

        ReminderScheduler.cancelTask(app, 9L)

        assertEquals(AlarmService.ACTION_STOP_ALARM, shadowOf(app).nextStartedService.action)
    }

    @Test
    fun `alarm berhenti otomatis setelah lima menit`() {
        val controller = startService(startIntent(alarmId = 51))
        assertEquals(51, AlarmService.ringingAlarmId)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(5))

        assertEquals(AlarmService.NO_ALARM, AlarmService.ringingAlarmId)
        assertTrue(shadowOf(controller.get()).isStoppedBySelf)
    }

    // ---------------------------------------------------------------- BL-05

    @Test
    fun `service tidak dimulai ulang otomatis dan mengabaikan intent null`() {
        val controller = Robolectric.buildService(AlarmService::class.java).create().also { controllers += it }

        val result = controller.get().onStartCommand(null, 0, 1)

        assertEquals(Service.START_NOT_STICKY, result)
        assertEquals(AlarmService.NO_ALARM, AlarmService.ringingAlarmId)
        assertTrue(shadowOf(controller.get()).isStoppedBySelf)
    }

    @Test
    fun `memulai alarm mengembalikan START_NOT_STICKY`() {
        val controller = Robolectric.buildService(AlarmService::class.java).create().also { controllers += it }

        assertEquals(Service.START_NOT_STICKY, controller.get().onStartCommand(startIntent(alarmId = 61), 0, 1))
    }

    @Test
    fun `volume alarm asli dipulihkan walau service pertama tidak sempat berhenti`() {
        val audio = app.getSystemService(AudioManager::class.java)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 1, 0)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        startService(startIntent(alarmId = 71))
        assertEquals("Volume dinaikkan ke maksimum saat berbunyi", max, audio.getStreamVolume(AudioManager.STREAM_ALARM))

        // Proses dimatikan sistem: tidak ada onDestroy. Instance baru yang menerima Stop harus memulihkan volume asli.
        val stop = Intent(app, AlarmService::class.java).setAction(AlarmService.ACTION_STOP_ALARM)
        startService(stop)

        assertEquals(1, audio.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    // ---------------------------------------------------------------- BL-07

    private fun alarmIntent(title: String, alarmId: Int) =
        Intent(app, AlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_MESSAGE, "Pesan")
        }

    private fun textsIn(view: View): List<String> = when (view) {
        is TextView -> listOf(view.text.toString())
        is ViewGroup -> (0 until view.childCount).flatMap { textsIn(view.getChildAt(it)) }
        else -> emptyList()
    }

    @Test
    fun `alarm kedua memperbarui isi layar alarm`() {
        val controller = Robolectric.buildActivity(AlarmActivity::class.java, alarmIntent("Kuliah Basis Data", 1)).setup()
        val root = controller.get().window.decorView
        assertTrue(textsIn(root).contains("Kuliah Basis Data"))

        controller.newIntent(alarmIntent("Deadline: Laporan", 2))

        val texts = textsIn(controller.get().window.decorView)
        assertTrue(texts.contains("Deadline: Laporan"))
        assertFalse("Judul alarm lama tidak boleh tersisa", texts.contains("Kuliah Basis Data"))
    }

    @Test
    fun `layar alarm tertutup saat bunyi dihentikan`() {
        val controller = Robolectric.buildActivity(AlarmActivity::class.java, alarmIntent("Kuliah", 3)).setup()

        app.sendBroadcast(Intent(AlarmService.ACTION_ALARM_STOPPED).setPackage(app.packageName))
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(controller.get().isFinishing)
    }

    @Test
    fun `menghentikan service menyiarkan penutupan layar alarm`() {
        val activity = Robolectric.buildActivity(AlarmActivity::class.java, alarmIntent("Kuliah", 4)).setup()
        val service = startService(startIntent(alarmId = 4)).get()

        service.onStartCommand(Intent(app, AlarmService::class.java).setAction(AlarmService.ACTION_STOP_ALARM), 0, 2)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(activity.get().isFinishing)
    }
}
