package com.temankuliah.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import com.temankuliah.app.widget.TodayWidgetUpdater

/** Satu-satunya sumber versi skema Room; dipakai anotasi di bawah dan manifest cadangan. */
const val DATABASE_SCHEMA_VERSION = 4

@Database(
    entities = [
        UserEntity::class, CourseEntity::class, ScheduleEntity::class,
        AcademicDocumentEntity::class, TaskEntity::class, AcademicEventEntity::class,
        ProjectEntity::class, ProjectMilestoneEntity::class
    ],
    version = DATABASE_SCHEMA_VERSION,
    exportSchema = true
)
abstract class TemanKuliahDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courseDao(): CourseDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun academicDocumentDao(): AcademicDocumentDao
    abstract fun taskDao(): TaskDao
    abstract fun academicEventDao(): AcademicEventDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectMilestoneDao(): ProjectMilestoneDao

    companion object {
        @Volatile private var INSTANCE: TemanKuliahDatabase? = null

        fun getDatabase(context: Context): TemanKuliahDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: run {
                val appContext = context.applicationContext
                Room.databaseBuilder(
                    appContext, TemanKuliahDatabase::class.java, AppStorage.DATABASE_NAME
                ).addMigrations(*ALL_MIGRATIONS).build().also { db ->
                    INSTANCE = db
                    // Widget memakai tabel ini; perubahan apa pun (termasuk restore cadangan) memicu pembaruan.
                    db.invalidationTracker.addObserver(object : InvalidationTracker.Observer("users", "schedules", "tasks") {
                        override fun onInvalidated(tables: Set<String>) = TodayWidgetUpdater.refreshAsync(appContext)
                    })
                }
            }
        }
    }
}
