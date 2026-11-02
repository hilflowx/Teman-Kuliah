package com.temankuliah.app.data.local

import com.temankuliah.app.domain.DEFAULT_REMINDER_MINUTES
import androidx.room.Entity
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "users",
    indices = [Index(value = ["nim"]), Index(value = ["email"])]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    /** Diisi pengguna di Profil > Informasi Pribadi. Boleh kosong; keunikan NIM dijaga di repository. */
    val nim: String,
    val studyProgram: String,
    val faculty: String,
    val university: String,
    /** Hash PBKDF2 untuk akun lokal (authType = "local"). Kosong untuk akun tamu dan akun yang belum membuat sandi (mis. hasil restore). */
    val password: String,
    val isLoggedIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "1") val activeSemester: Int = 1,
    val profileImagePath: String? = null,
    /** Email akun; kosong untuk akun tamu dan akun lokal lama. */
    @ColumnInfo(defaultValue = "''") val email: String = "",
    /** [AUTH_GUEST] atau [AUTH_LOCAL] (akun NIM/email + kata sandi). */
    @ColumnInfo(defaultValue = "'local'") val authType: String = AUTH_LOCAL,
    /** Jumlah semester yang sudah dibuat pengguna. Awalnya 8, dan bertambah lewat tombol Tambah semester tanpa batas atas. */
    @ColumnInfo(defaultValue = "8") val semesterCount: Int = 8
) {
    val isGuest: Boolean get() = authType == AUTH_GUEST
}

const val AUTH_GUEST = "guest"
const val AUTH_LOCAL = "local"

@Entity(
    tableName = "courses",
    indices = [Index(value = ["userId", "semester"])]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val code: String,
    val name: String,
    val credits: Int, // SKS
    val gradeLetter: String, // A, B+, B, C+, C, D, E, "-"
    val gradePoint: Double, // 4.0, 3.5, 3.0, 2.5, 2.0, 1.0, 0.0
    val semester: Int, // e.g. 1, 2, 3...
    val academicYear: String = "",
    /** Dosen pengampu dari KRS; kosong bila dokumen tidak memuatnya. */
    @ColumnInfo(defaultValue = "''") val lecturer: String = "",
    /** Jam kuliah dari KRS (mis. "Senin 08:00-09:40"); kosong bila dokumen tidak memuatnya. */
    @ColumnInfo(defaultValue = "''") val classTime: String = ""
)

val CourseEntity.theoryCredits: Int
    get() {
        val regex = Regex("""\[(?:T|Teori):(\d+),\s*(?:P|Praktik|Praktek):(\d+)\]""", RegexOption.IGNORE_CASE)
        val match = regex.find(classTime)
        if (match != null) {
            return match.groupValues[1].toIntOrNull() ?: credits
        }
        return if (name.contains("Praktikum", ignoreCase = true) || name.contains("Praktik", ignoreCase = true) || name.contains("Praktek", ignoreCase = true)) 0 else credits
    }

val CourseEntity.practiceCredits: Int
    get() {
        val regex = Regex("""\[(?:T|Teori):(\d+),\s*(?:P|Praktik|Praktek):(\d+)\]""", RegexOption.IGNORE_CASE)
        val match = regex.find(classTime)
        if (match != null) {
            return match.groupValues[2].toIntOrNull() ?: 0
        }
        return if (name.contains("Praktikum", ignoreCase = true) || name.contains("Praktik", ignoreCase = true) || name.contains("Praktek", ignoreCase = true)) credits else 0
    }

val CourseEntity.displayClassTime: String
    get() {
        val regex = Regex("""\[(?:T|Teori):\d+,\s*(?:P|Praktik|Praktek):\d+\]\s*""", RegexOption.IGNORE_CASE)
        return classTime.replace(regex, "").trim()
    }


@Entity(
    tableName = "schedules",
    indices = [Index(value = ["userId", "dayOfWeek"]), Index(value = ["userId", "scheduledDate"])]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val courseName: String,
    val dayOfWeek: Int, // 1=Senin, 2=Selasa, 3=Rabu, 4=Kamis, 5=Jumat, 6=Sabtu, 7=Minggu
    val startTime: String, // e.g. "08:00"
    val endTime: String,   // e.g. "09:40"
    val room: String,
    val lecturer: String,
    val type: String = "Kuliah Teori", // "Kuliah Teori", "Praktikum", "Seminar"
    @ColumnInfo(defaultValue = "1") val semester: Int = 1,
    @ColumnInfo(defaultValue = "0") val alarmEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "15") val reminderMinutesBefore: Int = DEFAULT_REMINDER_MINUTES,
    /** Tanggal kejadian satu kali dalam format ISO yyyy-MM-dd; bukan jadwal berulang mingguan. */
    @ColumnInfo(defaultValue = "''") val scheduledDate: String = defaultScheduleDateForDay(dayOfWeek),
    /** Status penyelesaian tidak menghapus tanggal/jam atau entri kalender. */
    @ColumnInfo(defaultValue = "0") val isCompleted: Boolean = false,
    /** Kosong = Jadwal Sekali. Terisi (sama untuk semua pertemuan dalam satu seri) = bagian dari Jadwal Berulang. */
    @ColumnInfo(defaultValue = "''") val recurrenceGroupId: String = "",
    /** [SCHEDULE_SOURCE_KRS] bila dipilih dari KRS, selain itu [SCHEDULE_SOURCE_MANUAL]. */
    @ColumnInfo(defaultValue = "'MANUAL'") val sumber: String = SCHEDULE_SOURCE_MANUAL,
    /** Id mata kuliah KRS (tabel courses) bila [sumber] = KRS; null untuk jadwal manual. */
    val krsId: Long? = null,
    /** SKS yang tampil di kartu jadwal. 0 = tidak diketahui (kartu memakai SKS dari KRS bila ada). */
    @ColumnInfo(defaultValue = "0") val sks: Int = 0,
    /** [SCHEDULE_MODE_OFFLINE] atau [SCHEDULE_MODE_ONLINE]. Data lama otomatis OFFLINE. */
    @ColumnInfo(defaultValue = "'OFFLINE'") val mode: String = SCHEDULE_MODE_OFFLINE,
    /** Platform kuliah daring (mis. Zoom, Meet, Teams); hanya bermakna bila mode ONLINE. */
    val platform: String? = null,
    /** Tautan rapat daring (http/https); hanya bermakna bila mode ONLINE. */
    val linkMeeting: String? = null,
    val catatan: String? = null
)

const val SCHEDULE_SOURCE_KRS = "KRS"
const val SCHEDULE_SOURCE_MANUAL = "MANUAL"
const val SCHEDULE_MODE_OFFLINE = "OFFLINE"
const val SCHEDULE_MODE_ONLINE = "ONLINE"

/** Default kompatibilitas untuk konstruksi data lama; jadwal baru selalu mengirim tanggal pilihan pengguna. */
fun defaultScheduleDateForDay(dayOfWeek: Int): String {
    val today = LocalDate.now()
    val delta = (dayOfWeek.coerceIn(1, 7) - today.dayOfWeek.value + 7) % 7
    return today.plusDays(delta.toLong()).toString()
}


@Entity(
    tableName = "academic_documents",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "semester", "type"], unique = true)
    ]
)
data class AcademicDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val semester: Int,
    val type: String, // KRS or KHS
    val displayName: String,
    /** Relative path under app-private filesDir, not an external SAF URI. */
    val storedPath: String,
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val extractedText: String = "",
    @ColumnInfo(defaultValue = "''") val processingMessage: String = ""
)

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["userId", "semester"]), Index(value = ["userId", "dueAtMillis"]), Index(value = ["projectId"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val semester: Int = 1,
    val title: String,
    val description: String = "",
    val courseId: Long? = null,
    val dueAtMillis: Long,
    val priority: String = "Sedang", // Rendah, Sedang, Tinggi
    val status: String = "Belum Dikerjakan", // Belum Dikerjakan, Sedang Dikerjakan, Selesai
    val projectId: Long? = null
)

@Entity(
    tableName = "academic_events",
    indices = [Index(value = ["userId", "scheduledAtMillis"]), Index(value = ["userId", "semester"])]
)
data class AcademicEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val semester: Int = 1,
    val title: String,
    val description: String = "",
    val type: String = "Kegiatan Akademik", // Ujian, Kegiatan Akademik, Kegiatan Kampus, atau Seminar
    val scheduledAtMillis: Long,
    /** Alarm pengingat sebelum agenda dimulai, sama seperti jadwal kuliah. Data lama otomatis mati. */
    @ColumnInfo(defaultValue = "0") val alarmEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "15") val reminderMinutesBefore: Int = DEFAULT_REMINDER_MINUTES
)

@Entity(
    tableName = "projects",
    indices = [Index(value = ["userId", "semester"]), Index(value = ["userId", "deadlineAtMillis"])]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val semester: Int = 1,
    val title: String,
    val description: String = "",
    val deadlineAtMillis: Long,
    val progress: Int = 0
)

@Entity(
    tableName = "project_milestones",
    indices = [Index(value = ["userId", "projectId"]), Index(value = ["projectId", "deadlineAtMillis"])]
)
data class ProjectMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val projectId: Long,
    val title: String,
    val deadlineAtMillis: Long,
    val isCompleted: Boolean = false
)
