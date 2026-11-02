package com.temankuliah.app.data.local

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Uji migrasi skema Room 3 -> 4: `academic_events` mendapat `alarmEnabled` dan `reminderMinutesBefore`.
 * Database v3 dibangun dari skema resmi 3.json, lalu dibuka dengan Room v4 memakai rantai [ALL_MIGRATIONS].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration3To4Test {

    private val dbName = "migration_test_v3_to_v4.db"
    private lateinit var app: Application
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        dbFile = app.getDatabasePath(dbName)
        if (dbFile.exists()) dbFile.delete()
    }

    @After
    fun tearDown() {
        if (dbFile.exists()) dbFile.delete()
    }

    private fun createV3Database(): SupportSQLiteDatabase {
        val config = SupportSQLiteOpenHelper.Configuration.builder(app)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val schemaFile = File("schemas/com.temankuliah.app.data.local.TemanKuliahDatabase/3.json")
                    val databaseObj = JSONObject(schemaFile.readText()).getJSONObject("database")
                    val entities = databaseObj.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val entity = entities.getJSONObject(i)
                        val tableName = entity.getString("tableName")
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", tableName))
                        val indices = entity.optJSONArray("indices")
                        if (indices != null) {
                            for (j in 0 until indices.length()) {
                                db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", tableName))
                            }
                        }
                    }
                    val setupQueries = databaseObj.optJSONArray("setupQueries")
                    if (setupQueries != null) {
                        for (i in 0 until setupQueries.length()) db.execSQL(setupQueries.getString(i))
                    }
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun `migrasi 3 ke 4 menambah kolom alarm acara dan mempertahankan agenda lama dengan alarm mati`() {
        val v3Db = createV3Database()
        v3Db.execSQL(
            "INSERT INTO `academic_events` (`id`, `userId`, `semester`, `title`, `description`, `type`, `scheduledAtMillis`) " +
                "VALUES (11, 7, 3, 'UTS Pemrograman Mobile', 'Ruang A1', 'Ujian', 1800000000000)"
        )
        v3Db.execSQL(
            "INSERT INTO `academic_events` (`id`, `userId`, `semester`, `title`, `description`, `type`, `scheduledAtMillis`) " +
                "VALUES (12, 7, 3, 'Seminar Nasional AI', '', 'Kegiatan Akademik', 1800003600000)"
        )
        v3Db.version = 3
        v3Db.close()

        val roomDb = Room.databaseBuilder(app, TemanKuliahDatabase::class.java, dbName)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val db = roomDb.openHelper.writableDatabase
            assertEquals(DATABASE_SCHEMA_VERSION, db.version)

            val columns = mutableListOf<String>()
            db.query("PRAGMA table_info(`academic_events`)").use { c ->
                while (c.moveToNext()) columns += c.getString(c.getColumnIndexOrThrow("name"))
            }
            assertTrue("alarmEnabled" in columns)
            assertTrue("reminderMinutesBefore" in columns)

            val events = kotlinx.coroutines.runBlocking { roomDb.academicEventDao().getAllOnce() }.associateBy { it.id }
            assertEquals(2, events.size)
            val exam = events.getValue(11L)
            assertEquals("UTS Pemrograman Mobile", exam.title)
            assertEquals("Ruang A1", exam.description)
            assertEquals("Ujian", exam.type)
            assertEquals(1800000000000L, exam.scheduledAtMillis)
            assertFalse("Agenda lama tidak boleh tiba-tiba beralarm", exam.alarmEnabled)
            assertEquals(15, exam.reminderMinutesBefore)
            assertFalse(events.getValue(12L).alarmEnabled)

            val newId = kotlinx.coroutines.runBlocking {
                roomDb.academicEventDao().insert(
                    AcademicEventEntity(userId = 7, semester = 3, title = "Rapat BEM", type = "Kegiatan Kampus", scheduledAtMillis = 1800007200000, alarmEnabled = true, reminderMinutesBefore = 30)
                )
            }
            val saved = kotlinx.coroutines.runBlocking { roomDb.academicEventDao().getAllOnce() }.first { it.id == newId }
            assertTrue(saved.alarmEnabled)
            assertEquals(30, saved.reminderMinutesBefore)
        } finally {
            roomDb.close()
        }
    }
}
