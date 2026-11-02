package com.temankuliah.app.domain

import com.temankuliah.app.data.local.AcademicEventEntity
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.local.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Jenis notifikasi; menentukan ikon, warna, dan tab filter di layar Notifikasi. */
enum class NotificationKind { JADWAL, TUGAS, AGENDA }

/** Pengelompokan di layar Notifikasi. */
enum class NotificationGroup { HARI_INI, KEMARIN, SEBELUMNYA }

/**
 * Satu baris notifikasi.
 *
 * [key] stabil selama pesannya sama (mis. "tugas 4 hari lagi") dan berubah saat pesannya berubah
 * (menjadi "3 hari lagi"), sehingga status dibaca disimpan per pesan, bukan per tugas.
 * [targetTab] adalah tab tujuan saat diketuk: 1 = Kalender, 2 = Tugas (sama dengan notifikasi sistem).
 */
data class NotificationItem(
    val key: String,
    val kind: NotificationKind,
    val group: NotificationGroup,
    val title: String,
    val subtitle: String,
    val detail: String,
    val sortMillis: Long,
    val targetTab: Int
)

/** Batas hari agar daftar tetap relevan dan ringkas. */
internal const val FEED_TASK_AHEAD_DAYS = 7L
internal const val FEED_TASK_LATE_DAYS = 7L
internal const val FEED_EVENT_AHEAD_DAYS = 7L

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")

private fun clock(time: LocalTime): String = "%02d.%02d".format(IdLocale, time.hour, time.minute)

private fun clockOf(millis: Long, zone: ZoneId): String =
    clock(Instant.ofEpochMilli(millis).atZone(zone).toLocalTime())

private fun relativeDay(days: Long): String = when (days) {
    0L -> "hari ini"
    1L -> "besok"
    else -> "$days hari lagi"
}

/**
 * Menyusun daftar notifikasi dari data asli pengguna. Tidak ada data contoh.
 *
 * - Jadwal kuliah semester aktif yang belum selesai dan belum mulai, hari ini atau besok.
 * - Tugas yang belum selesai: tenggat 0..7 hari lagi, atau terlambat sampai 7 hari terakhir.
 * - Agenda/ujian 0..7 hari lagi.
 *
 * [includeSchedules]/[includeTasks] mengikuti saklar di Profil > Pengingat.
 */
@Suppress("LongParameterList") // API publik dengan default; pemanggil dan test memakai tanda tangan ini.
fun buildNotificationFeed(
    nowMillis: Long,
    zone: ZoneId,
    semester: Int,
    schedules: List<ScheduleEntity>,
    tasks: List<TaskEntity>,
    events: List<AcademicEventEntity>,
    courseNames: Map<Long, String>,
    includeSchedules: Boolean = true,
    includeTasks: Boolean = true
): List<NotificationItem> {
    val feed = FeedBuilder(nowMillis, zone, semester)
    val items = mutableListOf<NotificationItem>()
    if (includeSchedules) items += feed.schedules(schedules)
    if (includeTasks) items += feed.tasks(tasks, courseNames)
    items += feed.events(events)

    // Hari ini: yang paling dekat waktunya lebih dulu. Kemarin/Sebelumnya: yang terbaru lebih dulu.
    return items.sortedWith(
        compareBy<NotificationItem> { it.group.ordinal }
            .thenBy { if (it.group == NotificationGroup.HARI_INI) it.sortMillis else -it.sortMillis }
    )
}

/** Pembangun tiap bagian notifikasi; berbagi waktu sekarang, zona, hari ini, dan semester. */
private class FeedBuilder(private val nowMillis: Long, private val zone: ZoneId, private val semester: Int) {
    private val today: LocalDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

    fun schedules(schedules: List<ScheduleEntity>): List<NotificationItem> {
        val items = mutableListOf<NotificationItem>()
        schedules.asSequence()
            .filter { it.semester == semester && !it.isCompleted }
            .forEach { schedule ->
                val date = runCatching { LocalDate.parse(schedule.scheduledDate) }.getOrNull() ?: return@forEach
                val start = runCatching { LocalTime.parse(schedule.startTime) }.getOrNull() ?: return@forEach
                val startMillis = date.atTime(start).atZone(zone).toInstant().toEpochMilli()
                val days = ChronoUnit.DAYS.between(today, date)
                if (startMillis <= nowMillis || days !in 0L..1L) return@forEach
                val place = when {
                    schedule.isOnlineSchedule() -> schedule.platform?.takeIf { it.isNotBlank() }?.let { "Daring · $it" } ?: "Daring"
                    schedule.room.isNotBlank() -> schedule.room
                    else -> "Ruang belum diatur"
                }
                val end = runCatching { LocalTime.parse(schedule.endTime) }.getOrNull()
                items += NotificationItem(
                    key = "jadwal_${schedule.id}_d$days",
                    kind = NotificationKind.JADWAL,
                    group = NotificationGroup.HARI_INI,
                    title = "Kuliah ${relativeDay(days)} pukul ${clock(start)}",
                    subtitle = "${schedule.courseName} · $place",
                    detail = if (end != null) "${clock(start)} – ${clock(end)}" else clock(start),
                    sortMillis = startMillis,
                    targetTab = 1
                )
            }
        return items
    }

    fun tasks(tasks: List<TaskEntity>, courseNames: Map<Long, String>): List<NotificationItem> =
        tasks.asSequence()
            .filter { it.semester == semester && it.status != TaskStatus.SELESAI }
            .mapNotNull { task ->
                val dueDate = Instant.ofEpochMilli(task.dueAtMillis).atZone(zone).toLocalDate()
                val course = task.courseId?.let { courseNames[it] }?.takeIf { it.isNotBlank() }
                val clockText = clockOf(task.dueAtMillis, zone)
                if (task.dueAtMillis >= nowMillis) {
                    upcomingTaskItem(task, dueDate, course, clockText)
                } else {
                    overdueTaskItem(task, dueDate, course, clockText)
                }
            }
            .toList()

    /** Tugas yang tenggatnya belum lewat; null bila di luar jendela [FEED_TASK_AHEAD_DAYS]. */
    private fun upcomingTaskItem(task: TaskEntity, dueDate: LocalDate, course: String?, clockText: String): NotificationItem? {
        val detail = if (course != null) "$course · $clockText" else clockText
        val days = ChronoUnit.DAYS.between(today, dueDate)
        if (days !in 0L..FEED_TASK_AHEAD_DAYS) return null
        return NotificationItem(
            key = "tugas_${task.id}_d$days",
            kind = NotificationKind.TUGAS,
            group = NotificationGroup.HARI_INI,
            title = "Tenggat ${relativeDay(days)}",
            subtitle = task.title,
            detail = detail,
            sortMillis = task.dueAtMillis,
            targetTab = 2
        )
    }

    /** Tugas yang sudah lewat tenggat; null bila lebih lama dari [FEED_TASK_LATE_DAYS]. */
    private fun overdueTaskItem(task: TaskEntity, dueDate: LocalDate, course: String?, clockText: String): NotificationItem? {
        val daysAgo = ChronoUnit.DAYS.between(dueDate, today)
        if (daysAgo > FEED_TASK_LATE_DAYS) return null
        val whenText = when (daysAgo) {
            0L -> "Tenggat hari ini, $clockText"
            1L -> "Tenggat kemarin, $clockText"
            else -> "Tenggat $daysAgo hari lalu"
        }
        return NotificationItem(
            key = "tugas_${task.id}_late",
            kind = NotificationKind.TUGAS,
            group = when (daysAgo) {
                0L -> NotificationGroup.HARI_INI
                1L -> NotificationGroup.KEMARIN
                else -> NotificationGroup.SEBELUMNYA
            },
            title = "Tugas melewati tenggat",
            subtitle = if (course != null) "${task.title} · $course" else task.title,
            detail = whenText,
            sortMillis = task.dueAtMillis,
            targetTab = 2
        )
    }

    fun events(events: List<AcademicEventEntity>): List<NotificationItem> {
        val items = mutableListOf<NotificationItem>()
        events.asSequence()
            .filter { it.semester == semester && it.scheduledAtMillis >= nowMillis }
            .forEach { event ->
                val date = Instant.ofEpochMilli(event.scheduledAtMillis).atZone(zone).toLocalDate()
                val days = ChronoUnit.DAYS.between(today, date)
                if (days !in 0L..FEED_EVENT_AHEAD_DAYS) return@forEach
                val extra = event.description.trim().ifBlank { event.type }
                val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, IdLocale)
                items += NotificationItem(
                    key = "agenda_${event.id}_d$days",
                    kind = NotificationKind.AGENDA,
                    group = NotificationGroup.HARI_INI,
                    title = "${event.title} ${relativeDay(days)}",
                    subtitle = "${clockOf(event.scheduledAtMillis, zone)} · $extra",
                    detail = if (days <= 1L) clockOf(event.scheduledAtMillis, zone) else dayName,
                    sortMillis = event.scheduledAtMillis,
                    targetTab = 1
                )
            }
        return items
    }
}
