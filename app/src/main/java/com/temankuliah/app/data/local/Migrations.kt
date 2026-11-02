package com.temankuliah.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Daftar migrasi skema Room.
 *
 * Saat mengubah skema (kolom/tabel/indeks):
 * 1. Naikkan `version` di [TemanKuliahDatabase] (mis. 2 -> 3). Jangan mengubah skema tanpa menaikkan versi.
 * 2. Tambahkan `Migration(2, 3)` di sini dengan SQL `ALTER TABLE`/`CREATE TABLE` yang setara.
 * 3. Build ulang agar Room menulis `app/schemas/.../3.json`, lalu commit file itu.
 * 4. Jalankan `MigrationGuardTest`; test ini gagal bila rantai migrasi atau file skema tidak lengkap.
 *
 * Sengaja TIDAK memakai `fallbackToDestructiveMigration()`: data kuliah pengguna tidak boleh terhapus diam-diam.
 */

/**
 * 1 -> 2: tabel `schedules` mendapat sumber (KRS/MANUAL), krsId, sks, mode (OFFLINE/ONLINE), platform,
 * linkMeeting, dan catatan. Semua baris lama dipertahankan apa adanya dan otomatis berstatus OFFLINE.
 * Baris lama yang namanya cocok dengan mata kuliah KRS di semester dan akun yang sama ditautkan ke KRS itu
 * (sumber KRS, krsId, dan SKS terisi); sisanya tetap MANUAL.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `schedules` ADD COLUMN `sumber` TEXT NOT NULL DEFAULT 'MANUAL'")
        db.execSQL("ALTER TABLE `schedules` ADD COLUMN `krsId` INTEGER")
        db.execSQL("ALTER TABLE `schedules` ADD COLUMN `sks` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `schedules` ADD COLUMN `mode` TEXT NOT NULL DEFAULT 'OFFLINE'")
        db.execSQL("ALTER TABLE `schedules` ADD COLUMN `platform` TEXT")
        db.execSQL("ALTER TABLE `schedules` ADD COLUMN `linkMeeting` TEXT")
        db.execSQL("ALTER TABLE `schedules` ADD COLUMN `catatan` TEXT")
        db.execSQL(
            "UPDATE `schedules` SET `krsId` = (SELECT c.`id` FROM `courses` c " +
                "WHERE c.`userId` = `schedules`.`userId` AND c.`semester` = `schedules`.`semester` " +
                "AND LOWER(TRIM(c.`name`)) = LOWER(TRIM(`schedules`.`courseName`)) ORDER BY c.`id` LIMIT 1)"
        )
        db.execSQL("UPDATE `schedules` SET `sumber` = 'KRS' WHERE `krsId` IS NOT NULL")
        db.execSQL(
            "UPDATE `schedules` SET `sks` = COALESCE((SELECT c.`credits` FROM `courses` c WHERE c.`id` = `schedules`.`krsId`), 0) " +
                "WHERE `krsId` IS NOT NULL"
        )
    }
}

/**
 * 2 -> 3 (BL-12): kolom `recoveryCodeHash` dihapus dari tabel `users` (tidak dipakai sejak kode pemulihan dihapus).
 * Tabel dibuat ulang karena `DROP COLUMN` tidak aman di semua versi Android yang didukung. Semua baris dan id
 * lama dipertahankan; urutan nomor `AUTOINCREMENT` tidak mundur.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `users_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`fullName` TEXT NOT NULL, `nim` TEXT NOT NULL, `studyProgram` TEXT NOT NULL, `faculty` TEXT NOT NULL, " +
                "`university` TEXT NOT NULL, `password` TEXT NOT NULL, `isLoggedIn` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `activeSemester` INTEGER NOT NULL DEFAULT 1, `profileImagePath` TEXT, " +
                "`email` TEXT NOT NULL DEFAULT '', `authType` TEXT NOT NULL DEFAULT 'local', " +
                "`semesterCount` INTEGER NOT NULL DEFAULT 8)"
        )
        db.execSQL(
            "INSERT INTO `users_new` (`id`, `fullName`, `nim`, `studyProgram`, `faculty`, `university`, `password`, " +
                "`isLoggedIn`, `createdAt`, `activeSemester`, `profileImagePath`, `email`, `authType`, `semesterCount`) " +
                "SELECT `id`, `fullName`, `nim`, `studyProgram`, `faculty`, `university`, `password`, " +
                "`isLoggedIn`, `createdAt`, `activeSemester`, `profileImagePath`, `email`, `authType`, `semesterCount` " +
                "FROM `users`"
        )
        db.execSQL(
            "UPDATE `sqlite_sequence` SET `seq` = MAX(`seq`, COALESCE((SELECT `seq` FROM `sqlite_sequence` WHERE `name` = 'users'), 0)) " +
                "WHERE `name` = 'users_new'"
        )
        db.execSQL("DROP TABLE `users`")
        db.execSQL("ALTER TABLE `users_new` RENAME TO `users`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_nim` ON `users` (`nim`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")
    }
}

/**
 * 3 -> 4: tabel `academic_events` mendapat `alarmEnabled` dan `reminderMinutesBefore` (pengingat alarm untuk
 * ujian, kegiatan, dan acara). Semua baris lama dipertahankan dan alarmnya mati (tidak ada alarm yang muncul tiba-tiba).
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `academic_events` ADD COLUMN `alarmEnabled` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `academic_events` ADD COLUMN `reminderMinutesBefore` INTEGER NOT NULL DEFAULT 15")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
