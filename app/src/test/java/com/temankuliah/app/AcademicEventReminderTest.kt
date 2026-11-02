@file:Suppress("DEPRECATION")

package com.temankuliah.app

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.temankuliah.app.alarm.AlarmScheduler
import com.temankuliah.app.data.local.AcademicEventEntity
import com.temankuliah.app.data.local.TemanKuliahDatabase
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.data.notifications.ScheduleNotificationReceiver
import com.temankuliah.app.data.repository.AcademicEventRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Pengingat alarm untuk ujian, kegiatan, dan acara kalender (seperti jadwal kuliah). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AcademicEventReminderTest {
    private lateinit var app: Application
    private lateinit var db: TemanKuliahDatabase

    private val dayMillis = 86_400_000L

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, TemanKuliahDatabase::class.java).allowMainThreadQueries().build()
        ReminderScheduler.databaseProvider = { db }
    }

    @After
    fun tearDown() {
        ReminderScheduler.databaseProvider = { TemanKuliahDatabase.getDatabase(it) }
        db.close()
    }

    private suspend fun insertUser(name: String, nim: String, loggedIn: Boolean): Long =
        db.userDao().insertUser(
            UserEntity(fullName = name, nim = nim, studyProgram = "Informatika", faculty = "Teknik", university = "USP", password = "test-hash", isLoggedIn = loggedIn)
        )

    private suspend fun insertEvent(userId: Long, alarm: Boolean = true, minutes: Int = 15, at: Long = System.currentTimeMillis() + 2 * dayMillis, type: String = "Ujian"): AcademicEventEntity {
        val event = AcademicEventEntity(userId = userId, title = "UTS Basis Data", description = "Ruang A1", type = type, scheduledAtMillis = at, alarmEnabled = alarm, reminderMinutesBefore = minutes)
        return event.copy(id = db.academicEventDao().insert(event))
    }

    private fun scheduledKeys(): Set<Pair<String, Long>> {
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return shadowOf(alarmManager).scheduledAlarms.mapNotNull { alarm ->
            val intent = alarm.operation?.let { shadowOf(it).savedIntent } ?: return@mapNotNull null
            (intent.getStringExtra("kind") ?: return@mapNotNull null) to intent.getLongExtra("row_id", -1L)
        }.toSet()
    }

    @Test
    fun `agenda dengan pengingat aktif dijadwalkan dan dibatalkan`() = runBlocking {
        val userId = insertUser("Mhs", "N1", loggedIn = true)
        val event = insertEvent(userId)

        ReminderScheduler.scheduleEvent(app, event)
        assertEquals(setOf("event" to event.id), scheduledKeys())

        ReminderScheduler.cancelEvent(app, event.id)
        assertTrue(scheduledKeys().isEmpty())
    }

    @Test
    fun `pengingat mati atau waktunya sudah lewat tidak dijadwalkan`() = runBlocking {
        val userId = insertUser("Mhs", "N1", loggedIn = true)
        ReminderScheduler.scheduleEvent(app, insertEvent(userId, alarm = false))
        // Acara 5 menit lagi dengan pengingat 15 menit: waktu pengingat sudah lewat.
        ReminderScheduler.scheduleEvent(app, insertEvent(userId, minutes = 15, at = System.currentTimeMillis() + 5 * 60_000L))
        assertTrue(scheduledKeys().isEmpty())
    }

    @Test
    fun `mematikan pengingat saat ubah agenda membatalkan alarmnya`() = runBlocking {
        val userId = insertUser("Mhs", "N1", loggedIn = true)
        val event = insertEvent(userId)
        ReminderScheduler.scheduleEvent(app, event)
        assertEquals(1, scheduledKeys().size)

        ReminderScheduler.scheduleEvent(app, event.copy(alarmEnabled = false))
        assertTrue(scheduledKeys().isEmpty())
    }

    @Test
    fun `rescheduleAll hanya memasang alarm agenda milik akun aktif`() = runBlocking {
        val me = insertUser("Mhs", "N1", loggedIn = true)
        val other = insertUser("Lain", "N2", loggedIn = false)
        val mine = insertEvent(me)
        val theirs = insertEvent(other)
        ReminderScheduler.scheduleEvent(app, theirs)

        ReminderScheduler.rescheduleAll(app, db, me)

        assertEquals(setOf("event" to mine.id), scheduledKeys())
    }

    @Test
    fun `receiver acara menjalankan AlarmService dengan alarm id acara`() = runBlocking {
        val userId = insertUser("Mhs", "N1", loggedIn = true)
        val event = insertEvent(userId)
        val intent = Intent(app, ScheduleNotificationReceiver::class.java)
            .putExtra("kind", "event").putExtra("row_id", event.id).putExtra("title", "Ujian: UTS Basis Data").putExtra("message", "Mulai pukul 08:00")

        ScheduleNotificationReceiver().onReceive(app, intent)

        val started = shadowOf(app).nextStartedService
        assertNotNull("AlarmService harus dijalankan untuk acara", started)
        assertEquals(ReminderScheduler.alarmIdFor("event", event.id), started.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1))
    }

    @Test
    fun `alarm id acara tidak bentrok dengan tugas dan kuliah`() {
        val ids = setOf(ReminderScheduler.alarmIdFor("task", 5L), ReminderScheduler.alarmIdFor("class", 5L), ReminderScheduler.alarmIdFor("event", 5L))
        assertEquals(3, ids.size)
    }

    @Test
    fun `repositori menerima semua jenis agenda dari dialog dan menolak yang lain`() = runBlocking {
        val userId = insertUser("Mhs", "N1", loggedIn = true)
        val repository = AcademicEventRepository(db.academicEventDao())
        for (type in listOf("Ujian", "Kegiatan Akademik", "Kegiatan Kampus", "Seminar")) {
            repository.addAcademicEvent(AcademicEventEntity(userId = userId, title = "Agenda $type", type = type, scheduledAtMillis = System.currentTimeMillis() + dayMillis))
        }
        assertEquals(4, db.academicEventDao().getAllOnce().size)

        try {
            repository.addAcademicEvent(AcademicEventEntity(userId = userId, title = "X", type = "Lain", scheduledAtMillis = System.currentTimeMillis() + dayMillis))
            fail("Jenis tidak dikenal harus ditolak")
        } catch (_: IllegalArgumentException) {
        }
        try {
            repository.addAcademicEvent(AcademicEventEntity(userId = userId, title = "X", type = "Ujian", scheduledAtMillis = System.currentTimeMillis() + dayMillis, reminderMinutesBefore = 500))
            fail("Waktu pengingat di luar 0..180 harus ditolak")
        } catch (_: IllegalArgumentException) {
        }
    }
}
