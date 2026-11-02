package com.temankuliah.app.ui.calendar

import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import com.temankuliah.app.domain.DEFAULT_REMINDER_MINUTES
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Switch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.AcademicEventEntity
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.DeadlinePickerField
import com.temankuliah.app.ui.components.OutlinedTextField
import com.temankuliah.app.ui.components.TkFormError
import com.temankuliah.app.ui.components.TkFormSheet
import com.temankuliah.app.ui.components.TkIconTile
import com.temankuliah.app.ui.icons.Assignment
import com.temankuliah.app.ui.icons.DeleteOutline
import com.temankuliah.app.ui.icons.Event
import com.temankuliah.app.ui.icons.School
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TemanKuliahRedLight
import com.temankuliah.app.ui.theme.TextMuted
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary

/**
 * Kartu agenda: kolom jam di kiri (jam mulai di atas, jam selesai atau "tenggat" di bawah), lalu kartu putih
 * bergaris tepi warna jenis agenda. Kotak centang selesai ada di bawah jam; tombol hapus hanya untuk ujian/kegiatan.
 */
@Composable
internal fun CalendarEventRow(
    event: CalendarUiEvent,
    modifier: Modifier = Modifier
) {
    val kind = agendaKindOf(event.type)
    val isDone = event.completed == true
    val accentBar = if (isDone) DividerLight else kind.accent

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.widthIn(min = 52.dp, max = 64.dp).padding(top = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = event.startLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDone) TextMuted else TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = event.captionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (event.onToggleCompleted != null && event.completed != null) {
                Checkbox(
                    checked = event.completed,
                    onCheckedChange = event.onToggleCompleted,
                    colors = CheckboxDefaults.colors(
                        checkedColor = TemanKuliahBlue,
                        uncheckedColor = TextSecondary
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Tandai selesai: ${event.title}" }
                )
            }
        }

        Surface(
            onClick = event.onOpen,
            shape = TkShape.lg,
            color = SurfaceLight,
            border = BorderStroke(1.dp, DividerLight),
            modifier = Modifier.weight(1f)
        ) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.width(4.dp).fillMaxHeight().background(accentBar))
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = event.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) TextMuted else TextPrimary,
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AgendaBadge(
                                text = agendaBadgeLabel(event.type),
                                container = if (isDone) DividerLight else kind.soft,
                                content = if (isDone) TextMuted else kind.ink
                            )
                            if (event.isOverdue && !isDone) {
                                AgendaBadge(
                                    text = "Terlambat",
                                    container = TemanKuliahRedLight,
                                    content = TemanKuliahRedDark
                                )
                            }
                        }
                    }

                    if (event.detail.isNotBlank() || event.onDelete != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = event.detail,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (event.onDelete != null) {
                                IconButton(onClick = event.onDelete, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Hapus ${event.title}",
                                        tint = TemanKuliahRedDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaBadge(text: String, container: Color, content: Color) {
    Surface(color = container, shape = CircleShape) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

/** Bottom Sheet Tambah Agenda Terpadu: Kuliah, Tugas, Ujian, atau Kegiatan */
@Composable
internal fun CalendarAddOptionBottomSheet(
    onDismiss: () -> Unit,
    onAddSchedule: () -> Unit,
    onAddTask: () -> Unit,
    onAddAcademicEvent: () -> Unit,
    onAddActivity: () -> Unit = onAddAcademicEvent
) {
    TkFormSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Tambah ke Kalender",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Pilih jenis agenda yang ingin kamu jadwalkan:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            AddOptionTile(
                title = "Jadwal Perkuliahan",
                description = "Mata kuliah mingguan, jam kelas, ruang, dan dosen pengampu",
                icon = Icons.Default.School,
                accent = AgendaKind.Kuliah.ink,
                background = AgendaKind.Kuliah.soft,
                onClick = {
                    onDismiss()
                    onAddSchedule()
                }
            )

            AddOptionTile(
                title = "Tenggat Tugas Kuliah",
                description = "Tugas mandiri/kelompok, tenggat waktu, dan prioritas tugas",
                icon = Icons.Default.Assignment,
                accent = AgendaKind.Tugas.ink,
                background = AgendaKind.Tugas.soft,
                onClick = {
                    onDismiss()
                    onAddTask()
                }
            )

            AddOptionTile(
                title = "Ujian & Kuis",
                description = "UTS, UAS, kuis mingguan, ujian praktikum, atau evaluasi",
                icon = Icons.Default.Event,
                accent = AgendaKind.Ujian.ink,
                background = AgendaKind.Ujian.soft,
                onClick = {
                    onDismiss()
                    onAddAcademicEvent()
                }
            )

            AddOptionTile(
                title = "Kegiatan & Acara Kampus",
                description = "Seminar, webinar, rapat organisasi, workshop, kepanitiaan, atau lomba",
                icon = Icons.Default.Event,
                accent = ActivityKindColors.ink,
                background = ActivityKindColors.soft,
                onClick = {
                    onDismiss()
                    onAddActivity()
                }
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AddOptionTile(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    background: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button),
        shape = TkShape.lg,
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, DividerLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TkIconTile(
                icon = icon,
                tint = accent,
                background = background,
                shape = TkShape.md,
                size = 46.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/** Pilihan waktu pengingat (menit sebelum mulai); semuanya dalam batas 0..180 yang diterima penjadwal alarm. */
private val EventReminderOptions = listOf(
    0 to "Tepat waktu", 5 to "5 mnt", 10 to "10 mnt", 15 to "15 mnt", 30 to "30 mnt", 60 to "1 jam", 120 to "2 jam", 180 to "3 jam"
)

@Composable
internal fun AcademicEventEditorDialog(
    initial: AcademicEventEntity?,
    defaultTimestamp: Long,
    initialType: String = "Ujian",
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, Boolean, Int) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var type by remember(initial?.id, initialType) { mutableStateOf(initial?.type ?: initialType) }
    var scheduledAt by remember(initial?.id, defaultTimestamp) { mutableLongStateOf(initial?.scheduledAtMillis ?: defaultTimestamp) }
    var remind by remember(initial?.id) { mutableStateOf(initial?.alarmEnabled ?: true) }
    var reminderMinutes by remember(initial?.id) { mutableIntStateOf(initial?.reminderMinutesBefore ?: DEFAULT_REMINDER_MINUTES) }
    var error by remember(initial?.id) { mutableStateOf<String?>(null) }

    val isActivity = type.contains("Kegiatan", ignoreCase = true) || type.contains("Seminar", ignoreCase = true) || type.contains("Acara", ignoreCase = true)

    TkFormSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initial == null) {
                    if (isActivity) "Tambah Kegiatan / Acara" else "Tambah Ujian / Kuis"
                } else {
                    if (isActivity) "Ubah Kegiatan / Acara" else "Ubah Ujian / Kuis"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = type == "Ujian",
                    onClick = { type = "Ujian" },
                    label = { Text("Ujian / Kuis") }
                )
                FilterChip(
                    selected = type == "Kegiatan Kampus",
                    onClick = { type = "Kegiatan Kampus" },
                    label = { Text("Kegiatan Kampus") }
                )
                FilterChip(
                    selected = type == "Seminar",
                    onClick = { type = "Seminar" },
                    label = { Text("Seminar / Acara") }
                )
            }

            val titleInvalid = error != null && title.isBlank()
            OutlinedTextField(
                title,
                { title = it; error = null },
                label = { Text(if (isActivity) "Nama kegiatan / acara" else "Nama agenda / ujian") },
                placeholder = { Text(if (isActivity) "mis. Seminar Nasional AI atau Rapat BEM" else "mis. UTS Pemrograman Mobile") },
                singleLine = true,
                isError = titleInvalid,
                supportingText = if (titleInvalid) { { Text(error.orEmpty()) } } else null,
                modifier = Modifier.fillMaxWidth()
            )

            DeadlinePickerField(
                deadlineAtMillis = scheduledAt,
                onDeadlineChange = { scheduledAt = it },
                dateTitle = "Pilih tanggal kegiatan",
                timeTitle = "Pilih jam kegiatan"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Ingatkan saya", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            if (remind) "Alarm berbunyi sebelum acara dimulai" else "Tidak ada pengingat",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(checked = remind, onCheckedChange = { remind = it })
                }
                if (remind) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EventReminderOptions.forEach { (minutes, label) ->
                            FilterChip(
                                selected = reminderMinutes == minutes,
                                onClick = { reminderMinutes = minutes },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                description,
                { description = it },
                label = { Text("Lokasi / Catatan / Keterangan (opsional)") },
                placeholder = { Text("mis. Ruang Aula Utama, dress code batik") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            TkFormError(error.takeUnless { titleInvalid })

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Batal") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isBlank()) error = "Nama kegiatan/ujian wajib diisi."
                        else onSave(title.trim(), description.trim(), type, scheduledAt, remind, reminderMinutes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue)
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}
