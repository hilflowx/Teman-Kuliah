package com.temankuliah.app.ui.schedule

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.temankuliah.app.ui.components.TkScrollableDialogText
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.domain.ScheduleRules
import com.temankuliah.app.domain.ScheduleSlot
import com.temankuliah.app.domain.isOnlineSchedule
import com.temankuliah.app.domain.isRecurring
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.components.pressScale
import com.temankuliah.app.ui.icons.CalendarToday
import com.temankuliah.app.ui.icons.LocationOn
import com.temankuliah.app.ui.icons.OpenInNew
import com.temankuliah.app.ui.icons.Person
import com.temankuliah.app.ui.icons.Videocam
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.SurfaceVariantLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Hari dalam seminggu (1 = Senin ... 7 = Minggu); jatuh ke tanggal jadwal bila dayOfWeek tidak valid. */
fun dayOfWeekOf(schedule: ScheduleEntity): Int =
    if (schedule.dayOfWeek in 1..7) schedule.dayOfWeek
    else runCatching { LocalDate.parse(schedule.scheduledDate).dayOfWeek.value }.getOrDefault(1)

fun dayNameOf(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "Senin"
    2 -> "Selasa"
    3 -> "Rabu"
    4 -> "Kamis"
    5 -> "Jumat"
    6 -> "Sabtu"
    else -> "Minggu"
}

/** Durasi pertemuan dalam menit, 0 bila jam tidak valid. */
fun durationMinutesOf(schedule: ScheduleEntity): Int = ScheduleRules.durationMinutes(schedule.startTime, schedule.endTime)

/**
 * Kartu jadwal sesuai desain: blok jam di kiri (mulai tebal biru, selesai abu-abu); di kanan nama mata kuliah,
 * ikon hapus, dosen, chip SKS / durasi / mode, lalu ruangan (Offline) atau platform (Online) dan ikon link.
 * Mengetuk kartu membuka edit (tombol hapus ada di dialog edit); geser kiri untuk menghapus ditangani oleh pemanggil.
 */
@Composable
fun ScheduleCard(
    slot: ScheduleSlot,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    credits: Int? = null
) {
    val schedule = slot.representative
    val context = LocalContext.current
    val isOnline = schedule.isOnlineSchedule()
    val minutes = durationMinutesOf(schedule)
    val sks = schedule.sks.takeIf { it > 0 } ?: credits
    val dateLabel = remember(schedule.scheduledDate) {
        runCatching { LocalDate.parse(schedule.scheduledDate).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", IndonesianLocale)) }.getOrNull()
    }
    val room = schedule.room.trim()
    val platform = schedule.platform?.trim().orEmpty()
    val link = schedule.linkMeeting?.trim().orEmpty()

    Surface(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth().pressScale(),
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier
                    .widthIn(min = 84.dp)
                    .heightIn(min = 84.dp)
                    .clip(TkShape.lg)
                    .background(TemanKuliahBlueLight)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(schedule.startTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TemanKuliahBlue)
                Text(schedule.endTime, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        schedule.courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(top = 2.dp, end = 4.dp)
                    )
                }

                if (schedule.lecturer.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            schedule.lecturer,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!slot.isRecurring && dateLabel != null) {
                        Chip(dateLabel, SurfaceVariantLight, TextSecondary, icon = Icons.Default.CalendarToday)
                    }
                    if (sks != null && sks > 0) Chip("$sks SKS", TemanKuliahBlueLight, TemanKuliahBlueDark, bold = true)
                    if (minutes > 0) Chip("$minutes menit", SurfaceVariantLight, TextSecondary)
                    if (isOnline) Chip("Online", TemanKuliahGreenLight, TemanKuliahGreenDark, bold = true)
                }

                val place = if (isOnline) platform else room
                if (place.isNotEmpty() || (isOnline && link.isNotEmpty())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (place.isNotEmpty()) {
                            Icon(
                                if (isOnline) Icons.Default.Videocam else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                place,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        if (isOnline && link.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri())) }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .semantics { contentDescription = "Buka tautan kuliah daring ${schedule.courseName}" }
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Konfirmasi hapus. Jadwal Sekali: Hapus / Batal. Jadwal Berulang: "Hapus hanya ini" (pertemuan terdekat),
 * "Hapus semua pengulangan" (seluruh seri hari itu), atau Batal.
 */
@Composable
fun ScheduleDeleteDialog(
    schedule: ScheduleEntity,
    meetingCount: Int,
    onDeleteOne: () -> Unit,
    onDeleteSeries: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateLabel = remember(schedule.scheduledDate) {
        runCatching { LocalDate.parse(schedule.scheduledDate).format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", IndonesianLocale)) }
            .getOrElse { schedule.scheduledDate }
    }
    if (!schedule.isRecurring) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Hapus jadwal?", fontWeight = FontWeight.Bold) },
            text = {
                TkScrollableDialogText {
                    Text(
                        "Jadwal ${schedule.courseName} pada $dateLabel akan dihapus dari daftar dan kalender. " +
                            "Kamu masih bisa mengurungkannya sebentar setelah ini."
                    )
                }
            },
            confirmButton = { TextButton(onClick = onDeleteOne) { Text("Hapus", color = TemanKuliahRedDark, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Hapus jadwal berulang?", fontWeight = FontWeight.Bold) },
            text = {
                TkScrollableDialogText {
                    Text("${schedule.courseName} berulang setiap ${dayNameOf(dayOfWeekOf(schedule))} ($meetingCount pertemuan). Pertemuan terdekat: $dateLabel.")
                }
            },
            confirmButton = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    TextButton(onClick = onDeleteOne) { Text("Hapus hanya ini", color = TemanKuliahRedDark, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = onDeleteSeries) { Text("Hapus semua pengulangan", color = TemanKuliahRedDark, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = onDismiss) { Text("Batal") }
                }
            }
        )
    }
}

@Composable
private fun Chip(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp)
        )
    }
}
