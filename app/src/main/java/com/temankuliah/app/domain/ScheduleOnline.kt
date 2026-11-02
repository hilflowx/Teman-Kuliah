package com.temankuliah.app.domain

import com.temankuliah.app.data.local.SCHEDULE_MODE_ONLINE
import com.temankuliah.app.data.local.ScheduleEntity
import java.util.Locale

/** Host rapat daring yang dikenali dari kolom Ruang. Cocok persis atau sebagai subdomain. */
private val ONLINE_MEETING_HOSTS = listOf("meet.google.com", "zoom.us", "teams.microsoft.com")

/** Jadwal daring hanya bila kolom `mode` bernilai ONLINE; baris lama otomatis OFFLINE. */
fun ScheduleEntity.isOnlineSchedule(): Boolean = mode == SCHEDULE_MODE_ONLINE

/**
 * Ruang seperti "Meeting Room" atau "Ruang 301" tidak dianggap daring. Hanya tautan http(s) yang hostnya
 * sama dengan, atau subdomain dari, salah satu host rapat di atas.
 */
fun isOnlineMeetingLink(room: String): Boolean {
    val text = room.trim().lowercase(Locale.ROOT)
    val afterScheme = when {
        text.startsWith("https://") -> text.removePrefix("https://")
        text.startsWith("http://") -> text.removePrefix("http://")
        else -> return false
    }
    val host = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
    return ONLINE_MEETING_HOSTS.any { host == it || host.endsWith(".$it") }
}
