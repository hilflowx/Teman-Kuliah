package com.temankuliah.app.data.backup

import android.content.Context
import com.temankuliah.app.data.backup.BackupSnapshotValidator.semesterStartDateProblem
import com.temankuliah.app.data.backup.LocalDataArchive.BackupSettings
import com.temankuliah.app.data.backup.LocalDataArchive.FORMAT_VERSION
import com.temankuliah.app.data.backup.LocalDataArchive.Snapshot
import com.temankuliah.app.data.local.AUTH_LOCAL
import com.temankuliah.app.data.local.AcademicDocumentEntity
import com.temankuliah.app.data.local.AcademicEventEntity
import com.temankuliah.app.data.local.AppStorage
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.ProjectEntity
import com.temankuliah.app.data.local.ProjectMilestoneEntity
import com.temankuliah.app.data.local.SCHEDULE_MODE_OFFLINE
import com.temankuliah.app.data.local.SCHEDULE_MODE_ONLINE
import com.temankuliah.app.data.local.SCHEDULE_SOURCE_KRS
import com.temankuliah.app.data.local.SCHEDULE_SOURCE_MANUAL
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.domain.DEFAULT_REMINDER_MINUTES
import org.json.JSONArray
import org.json.JSONObject

/** Penyandian/penguraian JSON untuk isi cadangan: [LocalDataArchive.Snapshot] ke teks JSON dan sebaliknya, serta pengaturan aplikasi. */
internal object BackupSnapshotCodec {
    internal fun encodeSnapshot(s: Snapshot): String {
        val root = JSONObject().put("dataVersion", FORMAT_VERSION)
        root.put("users", JSONArray().apply {
            s.users.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("fullName", it.fullName)
                        .put("nim", it.nim)
                        .put("studyProgram", it.studyProgram)
                        .put("faculty", it.faculty)
                        .put("university", it.university)
                        .put("password", it.password)
                        .put("isLoggedIn", it.isLoggedIn)
                        .put("createdAt", it.createdAt)
                        .put("activeSemester", it.activeSemester)
                        .put("profileImagePath", it.profileImagePath ?: JSONObject.NULL)
                        .put("email", it.email)
                        .put("authType", it.authType)
                        .put("semesterCount", it.semesterCount)
                )
            }
        })
        root.put("courses", JSONArray().apply {
            s.courses.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("userId", it.userId)
                        .put("code", it.code)
                        .put("name", it.name)
                        .put("credits", it.credits)
                        .put("gradeLetter", it.gradeLetter)
                        .put("gradePoint", it.gradePoint)
                        .put("semester", it.semester)
                        .put("academicYear", it.academicYear)
                        .put("lecturer", it.lecturer)
                        .put("classTime", it.classTime)
                )
            }
        })
        root.put("schedules", JSONArray().apply {
            s.schedules.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("userId", it.userId)
                        .put("courseName", it.courseName)
                        .put("dayOfWeek", it.dayOfWeek)
                        .put("scheduledDate", it.scheduledDate)
                        .put("isCompleted", it.isCompleted)
                        .put("startTime", it.startTime)
                        .put("endTime", it.endTime)
                        .put("room", it.room)
                        .put("lecturer", it.lecturer)
                        .put("type", it.type)
                        .put("semester", it.semester)
                        .put("alarmEnabled", it.alarmEnabled)
                        .put("reminderMinutesBefore", it.reminderMinutesBefore)
                        .put("recurrenceGroupId", it.recurrenceGroupId)
                        .put("sumber", it.sumber)
                        .put("krsId", it.krsId ?: JSONObject.NULL)
                        .put("sks", it.sks)
                        .put("mode", it.mode)
                        .put("platform", it.platform ?: JSONObject.NULL)
                        .put("linkMeeting", it.linkMeeting ?: JSONObject.NULL)
                        .put("catatan", it.catatan ?: JSONObject.NULL)
                )
            }
        })
        root.put("tasks", JSONArray().apply {
            s.tasks.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("userId", it.userId)
                        .put("semester", it.semester)
                        .put("title", it.title)
                        .put("description", it.description)
                        .put("courseId", it.courseId ?: JSONObject.NULL)
                        .put("dueAtMillis", it.dueAtMillis)
                        .put("priority", it.priority)
                        .put("status", it.status)
                        .put("projectId", it.projectId ?: JSONObject.NULL)
                )
            }
        })
        root.put("documents", JSONArray().apply {
            s.documents.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("userId", it.userId)
                        .put("semester", it.semester)
                        .put("type", it.type)
                        .put("displayName", it.displayName)
                        .put("storedPath", it.storedPath)
                        .put("updatedAt", it.updatedAt)
                        .put("extractedText", it.extractedText)
                        .put("processingMessage", it.processingMessage)
                )
            }
        })
        root.put("events", JSONArray().apply {
            s.events.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("userId", it.userId)
                        .put("semester", it.semester)
                        .put("title", it.title)
                        .put("description", it.description)
                        .put("type", it.type)
                        .put("scheduledAtMillis", it.scheduledAtMillis)
                        .put("alarmEnabled", it.alarmEnabled)
                        .put("reminderMinutesBefore", it.reminderMinutesBefore)
                )
            }
        })
        root.put("projects", JSONArray().apply {
            s.projects.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("userId", it.userId)
                        .put("semester", it.semester)
                        .put("title", it.title)
                        .put("description", it.description)
                        .put("deadlineAtMillis", it.deadlineAtMillis)
                        .put("progress", it.progress)
                )
            }
        })
        root.put("milestones", JSONArray().apply {
            s.milestones.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("userId", it.userId)
                        .put("projectId", it.projectId)
                        .put("title", it.title)
                        .put("deadlineAtMillis", it.deadlineAtMillis)
                        .put("isCompleted", it.isCompleted)
                )
            }
        })
        root.put("preferences", JSONObject()
            .put("targetCredits", s.settings.targetCredits)
            .put("taskNotificationsEnabled", s.settings.taskNotificationsEnabled)
            .put("classRemindersEnabled", s.settings.classRemindersEnabled)
            .put("soundEnabled", s.settings.soundEnabled)
            .put("vibrationEnabled", s.settings.vibrationEnabled)
            .put("reminderMinutes", s.settings.reminderMinutes)
            .put("startDates", JSONObject().apply { s.settings.semesterStartDates.forEach { (k, v) -> put(k, v) } })
        )
        return root.toString()
    }

    internal fun JSONArray.toUsers() = (0 until length()).map { index ->
        val o = getJSONObject(index)
        UserEntity(
            o.getLong("id"),
            o.getString("fullName"),
            o.getString("nim"),
            o.getString("studyProgram"),
            o.getString("faculty"),
            o.getString("university"),
            o.getString("password"),
            o.getBoolean("isLoggedIn"),
            o.getLong("createdAt"),
            o.getInt("activeSemester"),
            o.optString("profileImagePath").takeUnless { it == "null" || it.isBlank() },
            o.optString("email",""),
            o.optString("authType",AUTH_LOCAL).ifBlank { AUTH_LOCAL },
            o.optInt("semesterCount",8).coerceAtLeast(1)
        )
    }
    internal fun JSONArray.toCourses() = (0 until length()).map { index ->
        val o = getJSONObject(index)
        CourseEntity(
            o.getLong("id"),
            o.getLong("userId"),
            o.getString("code"),
            o.getString("name"),
            o.getInt("credits"),
            o.getString("gradeLetter"),
            o.getDouble("gradePoint"),
            o.getInt("semester"),
            o.optString("academicYear"),
            o.optString("lecturer"),
            o.optString("classTime")
        )
    }
    internal fun JSONArray.toSchedules() = (0 until length()).map { index ->
        val o = getJSONObject(index)
        val day = o.getInt("dayOfWeek")
        ScheduleEntity(
            id = o.getLong("id"),
            userId = o.getLong("userId"),
            courseName = o.getString("courseName"),
            dayOfWeek = day,
            startTime = o.getString("startTime"),
            endTime = o.getString("endTime"),
            room = o.getString("room"),
            lecturer = o.getString("lecturer"),
            type = o.optString("type", "Kuliah Teori"),
            semester = o.optInt("semester", 1),
            alarmEnabled = o.optBoolean("alarmEnabled", false),
            reminderMinutesBefore = o.optInt("reminderMinutesBefore", DEFAULT_REMINDER_MINUTES),
            // Tanggal yang hilang tidak diisi diam-diam; validateSnapshot menolaknya dengan pesan jelas.
            scheduledDate = o.optString("scheduledDate").takeUnless { it == "null" }.orEmpty(),
            isCompleted = o.optBoolean("isCompleted", false),
            recurrenceGroupId = o.optString("recurrenceGroupId", "").takeIf { it != "null" } ?: "",
            // Cadangan lama tidak punya kolom di bawah ini: sumber MANUAL, mode OFFLINE, SKS 0.
            sumber = o.optString("sumber", SCHEDULE_SOURCE_MANUAL).takeIf { it == SCHEDULE_SOURCE_KRS } ?: SCHEDULE_SOURCE_MANUAL,
            krsId = if (o.isNull("krsId")) null else o.optLong("krsId").takeIf { it > 0 },
            sks = o.optInt("sks", 0).coerceIn(0, 30),
            mode = o.optString("mode", SCHEDULE_MODE_OFFLINE).takeIf { it == SCHEDULE_MODE_ONLINE } ?: SCHEDULE_MODE_OFFLINE,
            platform = if (o.isNull("platform")) null else o.optString("platform").ifBlank { null },
            linkMeeting = if (o.isNull("linkMeeting")) null else o.optString("linkMeeting").ifBlank { null },
            catatan = if (o.isNull("catatan")) null else o.optString("catatan").ifBlank { null }
        )
    }
    internal fun JSONArray.toTasks() = (0 until length()).map { index ->
        val o = getJSONObject(index)
        TaskEntity(
            o.getLong("id"),
            o.getLong("userId"),
            o.getInt("semester"),
            o.getString("title"),
            o.getString("description"),
            if (o.isNull("courseId")) null else o.getLong("courseId"),
            o.getLong("dueAtMillis"),
            o.getString("priority"),
            o.getString("status"),
            if (o.has("projectId") && !o.isNull("projectId")) o.getLong("projectId") else null
        )
    }
    internal fun JSONArray.toAcademicEvents() = (0 until length()).map { index ->
        val o = getJSONObject(index)
        AcademicEventEntity(
            o.getLong("id"),
            o.getLong("userId"),
            o.getInt("semester"),
            o.getString("title"),
            o.optString("description"),
            o.getString("type"),
            o.getLong("scheduledAtMillis"),
            o.optBoolean("alarmEnabled", false),
            o.optInt("reminderMinutesBefore", DEFAULT_REMINDER_MINUTES)
        )
    }
    internal fun JSONArray.toProjects() = (0 until length()).map { index ->
        val o = getJSONObject(index)
        ProjectEntity(
            o.getLong("id"),
            o.getLong("userId"),
            o.getInt("semester"),
            o.getString("title"),
            o.optString("description"),
            o.getLong("deadlineAtMillis"),
            o.getInt("progress")
        )
    }
    internal fun JSONArray.toProjectMilestones() = (0 until length()).map { index ->
        val o = getJSONObject(index)
        ProjectMilestoneEntity(
            o.getLong("id"),
            o.getLong("userId"),
            o.getLong("projectId"),
            o.getString("title"),
            o.getLong("deadlineAtMillis"),
            o.getBoolean("isCompleted")
        )
    }
    internal fun JSONArray.toDocuments() = (0 until length()).map { index ->
        val o = getJSONObject(index)
        AcademicDocumentEntity(
            o.getLong("id"),
            o.getLong("userId"),
            o.getInt("semester"),
            o.getString("type"),
            o.getString("displayName"),
            o.getString("storedPath"),
            o.getLong("updatedAt"),
            o.optString("extractedText"),
            o.optString("processingMessage")
        )
    }
    internal fun readSettings(context: Context): BackupSettings {
        val prefs = AppStorage.getPreferences(context)
        val startDates = mutableMapOf<String, String>()
        prefs.all.forEach { (k, v) ->
            // Hanya kunci/tanggal yang sah yang diekspor, selaras dengan aturan validasi saat restore.
            if (v is String && semesterStartDateProblem(k, v) == null) {
                startDates[k] = v
            }
        }
        return BackupSettings(
            targetCredits = if (prefs.contains("target_sks")) prefs.getInt("target_sks", 0).coerceIn(1, 360) else 0, // 0 = not set
            taskNotificationsEnabled = prefs.getBoolean("notifications_tasks_enabled", true),
            classRemindersEnabled = prefs.getBoolean("notifications_classes_enabled", true),
            soundEnabled = prefs.getBoolean("notifications_sound_enabled", true),
            vibrationEnabled = prefs.getBoolean("notifications_vibration_enabled", true),
            reminderMinutes = prefs.getInt("notifications_reminder_minutes", DEFAULT_REMINDER_MINUTES).coerceIn(0, 180),
            semesterStartDates = startDates
        )
    }
}
