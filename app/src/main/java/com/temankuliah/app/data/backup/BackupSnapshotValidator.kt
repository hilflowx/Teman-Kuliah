package com.temankuliah.app.data.backup

import com.temankuliah.app.data.backup.LocalDataArchive.Snapshot
import com.temankuliah.app.data.files.StudentFileStore
import com.temankuliah.app.data.local.AUTH_GUEST
import com.temankuliah.app.data.local.AUTH_LOCAL
import com.temankuliah.app.data.repository.isValidCourseCredits
import com.temankuliah.app.domain.Grades
import com.temankuliah.app.domain.SEMESTER_START_DATE_KEY_PREFIX
import com.temankuliah.app.domain.TaskStatus
import com.temankuliah.app.domain.normalizeNim
import java.io.IOException
import java.time.LocalDate
import com.temankuliah.app.domain.ACADEMIC_EVENT_TYPES

/** Pemeriksaan konsistensi isi cadangan sebelum dipulihkan; cadangan yang tidak konsisten ditolak seluruhnya. */
internal object BackupSnapshotValidator {
    private const val SEMESTER_START_DATE_PREFIX = SEMESTER_START_DATE_KEY_PREFIX
    private val TIME_REGEX = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")

    /**
     * Mengembalikan alasan penolakan, atau null bila pasangan kunci/nilai boleh ditulis ke
     * SharedPreferences: kunci harus "semester_start_date_" + bilangan bulat kanonik >= 1
     * dan nilainya tanggal ISO yang valid (yyyy-MM-dd).
     */
    internal fun semesterStartDateProblem(key: String, value: String): String? {
        if (!key.startsWith(SEMESTER_START_DATE_PREFIX)) {
            return "Kunci preferensi '${key.take(40)}' tidak diizinkan; hanya ${SEMESTER_START_DATE_PREFIX}<semester> yang boleh."
        }
        val suffix = key.removePrefix(SEMESTER_START_DATE_PREFIX)
        val semester = suffix.toIntOrNull()
        if (semester == null || semester < 1 || semester.toString() != suffix) {
            return "Kunci preferensi '${key.take(40)}' tidak valid; sisa kunci harus bilangan bulat minimal 1."
        }
        val date = runCatching { LocalDate.parse(value) }.getOrNull()
        if (date == null || date.toString() != value) {
            return "Tanggal mulai semester untuk '${key.take(40)}' bukan tanggal ISO yang valid (yyyy-MM-dd): '${value.take(20)}'."
        }
        return null
    }

    internal fun validateSnapshot(s: Snapshot) {
        if (s.users.isEmpty()) throw IOException("Cadangan tidak berisi akun pengguna.")
        val userIds = s.users.map { it.id }
        if (userIds.any { it <= 0 } || userIds.distinct().size != userIds.size) throw IOException("ID akun tidak valid atau ganda.")
        if (s.users.count { it.isLoggedIn } > 1) throw IOException("Cadangan memiliki lebih dari satu sesi aktif.")
        // Akun boleh tanpa NIM; hanya NIM yang terisi yang harus unik.
        val filledNims = s.users.map { normalizeNim(it.nim) }.filter { it.isNotEmpty() }
        if (filledNims.distinct().size != filledNims.size) throw IOException("NIM duplikat ditemukan pada cadangan.")
        if (s.courses.map { it.id }.any { it <= 0 } || s.courses.map { it.id }.distinct().size != s.courses.size) throw IOException("ID mata kuliah tidak valid atau ganda.")
        if (s.schedules.map { it.id }.any { it <= 0 } || s.schedules.map { it.id }.distinct().size != s.schedules.size) throw IOException("ID jadwal tidak valid atau ganda.")
        if (s.tasks.map { it.id }.any { it <= 0 } || s.tasks.map { it.id }.distinct().size != s.tasks.size) throw IOException("ID tugas tidak valid atau ganda.")
        if (s.documents.map { it.id }.any { it <= 0 } || s.documents.map { it.id }.distinct().size != s.documents.size) throw IOException("ID dokumen tidak valid atau ganda.")
        if (s.events.map { it.id }.any { it <= 0 } || s.events.map { it.id }.distinct().size != s.events.size) throw IOException("ID kegiatan akademik tidak valid atau ganda.")
        if (s.projects.map { it.id }.any { it <= 0 } || s.projects.map { it.id }.distinct().size != s.projects.size) throw IOException("ID proyek tidak valid atau ganda.")
        if (s.milestones.map { it.id }.any { it <= 0 } || s.milestones.map { it.id }.distinct().size != s.milestones.size) throw IOException("ID tahapan proyek tidak valid atau ganda.")
        if (s.settings.targetCredits !in 0..360 || s.settings.reminderMinutes !in 0..180) throw IOException("Preferensi pengingat atau target SKS tidak valid.")
        s.settings.semesterStartDates.forEach { (key, value) ->
            semesterStartDateProblem(key, value)?.let { throw IOException(it) }
        }
        val userSet = userIds.toSet()
        if (s.users.any { it.fullName.isBlank() || it.authType !in setOf(AUTH_GUEST, AUTH_LOCAL) || it.activeSemester < 1 || it.semesterCount < it.activeSemester || (it.profileImagePath != null && (!StudentFileStore.isSafeRelativePath(it.profileImagePath) || !it.profileImagePath.startsWith("profile_images/"))) }) throw IOException("Profil, jenis akun, atau semester aktif tidak valid.")

        // Mata kuliah: tidak ada yang diperbaiki diam-diam (SKS, nilai, kode, semester, akun pemilik).
        s.courses.forEach { c ->
            val label = "Mata kuliah '${c.name.ifBlank { c.code }}' (id ${c.id})"
            if (c.userId !in userSet) throw IOException("$label merujuk akun (userId ${c.userId}) yang tidak ada di cadangan. Cadangan ditolak; data tidak dipindahkan ke akun lain.")
            if (c.code.isBlank()) throw IOException("$label tidak memiliki kode.")
            if (c.name.isBlank()) throw IOException("Mata kuliah dengan kode '${c.code}' (id ${c.id}) tidak memiliki nama.")
            if (c.semester < 1) throw IOException("$label memiliki semester ${c.semester}; semester minimal 1.")
            if (!Grades.isValid(c.gradeLetter)) throw IOException("$label memiliki nilai huruf tidak valid: '${c.gradeLetter.take(10)}'.")
            if (!isValidCourseCredits(c.credits, c.gradeLetter)) throw IOException("$label memiliki SKS ${c.credits}; SKS harus 1–30, atau 0 hanya bila nilai '-'.")
            if (!c.gradePoint.isFinite() || c.gradePoint !in 0.0..4.0) throw IOException("$label memiliki bobot nilai di luar 0–4.")
        }
        if (s.courses.map { Triple(it.userId, it.code.trim().uppercase(), it.semester) }.distinct().size != s.courses.size) throw IOException("Kode mata kuliah ganda ditemukan pada semester yang sama.")

        // Jadwal: tanggal yang tidak cocok dengan hari ditolak, bukan diganti "hari ini".
        s.schedules.forEach { sc ->
            val label = "Jadwal '${sc.courseName.ifBlank { "(tanpa nama)" }}' (id ${sc.id})"
            if (sc.userId !in userSet) throw IOException("$label merujuk akun (userId ${sc.userId}) yang tidak ada di cadangan. Cadangan ditolak; data tidak dipindahkan ke akun lain.")
            if (sc.courseName.isBlank()) throw IOException("Jadwal (id ${sc.id}) tidak memiliki nama mata kuliah.")
            if (sc.dayOfWeek !in 1..7) throw IOException("$label memiliki hari ${sc.dayOfWeek}; harus 1–7.")
            val date = runCatching { LocalDate.parse(sc.scheduledDate) }.getOrNull()
                ?: throw IOException("$label tidak memiliki tanggal yang valid: '${sc.scheduledDate.take(20)}'.")
            if (date.dayOfWeek.value != sc.dayOfWeek) throw IOException("$label bertanggal ${sc.scheduledDate} yang jatuh pada hari ke-${date.dayOfWeek.value}, tetapi hari jadwalnya ${sc.dayOfWeek}.")
            if (sc.semester < 1) throw IOException("$label memiliki semester ${sc.semester}; semester minimal 1.")
            if (sc.reminderMinutesBefore !in 0..180) throw IOException("$label memiliki pengingat ${sc.reminderMinutesBefore} menit; harus 0–180.")
            if (!sc.startTime.matches(TIME_REGEX) || !sc.endTime.matches(TIME_REGEX)) throw IOException("$label memiliki jam yang tidak valid (${sc.startTime.take(10)}–${sc.endTime.take(10)}).")
            if (sc.endTime <= sc.startTime) throw IOException("$label berakhir (${sc.endTime}) tidak setelah mulai (${sc.startTime}).")
        }
        val courseIdsByUser = s.courses.groupBy { it.userId }.mapValues { (_, items) -> items.map { it.id }.toSet() }
        if (s.tasks.any { it.userId !in userSet || it.title.isBlank() || it.semester < 1 || it.dueAtMillis <= 0L || it.priority !in setOf("Rendah", "Sedang", "Tinggi") || it.status !in setOf(TaskStatus.BELUM, TaskStatus.SEDANG, TaskStatus.SELESAI) || (it.courseId != null && it.courseId !in (courseIdsByUser[it.userId] ?: emptySet())) }) throw IOException("Data tugas atau relasi mata kuliahnya tidak konsisten.")
        val projectIds = s.projects.map { it.id }.toSet()
        val projectsById = s.projects.associateBy { it.id }
        if (s.events.any { it.userId !in userSet || it.semester < 1 || it.title.isBlank() || it.type !in ACADEMIC_EVENT_TYPES || it.scheduledAtMillis <= 0L || it.reminderMinutesBefore !in 0..180 }) throw IOException("Kegiatan akademik tidak konsisten.")
        if (s.projects.any { it.userId !in userSet || it.semester < 1 || it.title.isBlank() || it.deadlineAtMillis <= 0L || it.progress !in 0..100 }) throw IOException("Data proyek tidak konsisten.")
        if (s.milestones.any { milestone -> milestone.userId !in userSet || milestone.projectId !in projectIds || milestone.title.isBlank() || milestone.deadlineAtMillis <= 0L || projectsById[milestone.projectId]?.userId != milestone.userId }) throw IOException("Tahapan proyek tidak konsisten.")
        if (s.tasks.any { task -> task.projectId != null && (task.projectId !in projectIds || projectsById[task.projectId]?.userId != task.userId) }) throw IOException("Relasi tugas dan proyek tidak konsisten.")
        if (s.documents.any { it.id <= 0 || it.userId !in userSet || it.semester < 1 || it.type !in setOf("KRS", "KHS") || it.displayName.isBlank() || !StudentFileStore.isSafeRelativePath(it.storedPath) || !it.storedPath.startsWith("academic_documents/") }) throw IOException("Metadata dokumen tidak konsisten.")
        val allAssetPaths = s.users.mapNotNull { it.profileImagePath } + s.documents.map { it.storedPath }
        if (allAssetPaths.distinct().size != allAssetPaths.size) throw IOException("Beberapa metadata mengarah ke satu file lampiran yang sama.")
        if (s.documents.map { Triple(it.userId, it.semester, it.type) }.distinct().size != s.documents.size) throw IOException("Duplikasi metadata KRS/KHS untuk semester yang sama.")
    }
}
