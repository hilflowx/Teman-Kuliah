package com.temankuliah.app.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.SCHEDULE_MODE_OFFLINE
import com.temankuliah.app.data.local.SCHEDULE_MODE_ONLINE
import com.temankuliah.app.data.local.SCHEDULE_SOURCE_KRS
import com.temankuliah.app.data.local.SCHEDULE_SOURCE_MANUAL
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.domain.DEFAULT_REMINDER_MINUTES
import com.temankuliah.app.domain.ScheduleDraft
import com.temankuliah.app.domain.ScheduleRules
import com.temankuliah.app.domain.isRecurring
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.components.MaterialTimePickerDialog
import com.temankuliah.app.ui.components.OutlinedTextField
import com.temankuliah.app.ui.components.TkDatePickerDialog
import com.temankuliah.app.ui.components.TkFormSheet
import com.temankuliah.app.ui.components.TkPickerField
import com.temankuliah.app.ui.icons.CalendarToday
import com.temankuliah.app.ui.icons.ChevronDown
import com.temankuliah.app.ui.icons.Close
import com.temankuliah.app.ui.icons.Delete
import com.temankuliah.app.ui.icons.Notifications
import com.temankuliah.app.ui.icons.Schedule
import com.temankuliah.app.ui.icons.Timer
import com.temankuliah.app.ui.icons.Warning
import com.temankuliah.app.ui.theme.TkText
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.SurfaceVariantLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TemanKuliahRedLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.temankuliah.app.ui.theme.tkAdaptive

// Getter (bukan nilai tetap) agar ikut berganti saat tema berubah. Gelap: isian #B45309 berteks putih ±5:1.
private val AmberStrong: Color get() = tkAdaptive(Color(0xFFD97706), Color(0xFFB45309))
private val AmberIcon: Color get() = tkAdaptive(Color(0xFFD97706), Color(0xFFF5A524))
private val AmberSoft: Color get() = tkAdaptive(Color(0xFFFFF7E6), Color(0xFF3A2412))
private val AmberBorder: Color get() = tkAdaptive(Color(0xFFF6D08A), Color(0xFF7A4A1E))
private val AmberInk: Color get() = tkAdaptive(Color(0xFF7A4100), Color(0xFFFFA566))

private val DayOptions = listOf(1 to "Sen", 2 to "Sel", 3 to "Rab", 4 to "Kam", 5 to "Jum", 6 to "Sab", 7 to "Min")
private val ReminderOptions = listOf(0 to "Tepat waktu", 5 to "5 mnt", 10 to "10 mnt", 15 to "15 mnt", 30 to "30 mnt", 60 to "1 jam")
private val PlatformSuggestions = listOf("Google Meet", "Zoom", "Microsoft Teams")

private enum class PickerTarget { DATE, RANGE_START, RANGE_END, TIME_START, TIME_END }

private fun formatClock(totalMinutes: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", (totalMinutes / 60) % 24, totalMinutes % 60)

private fun longDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", IndonesianLocale)).replaceFirstChar { it.titlecase(IndonesianLocale) }

private fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale))

/**
 * Dialog Tambah/Edit jadwal (bottom sheet). Mengedit jadwal berulang lebih dulu menanyakan cakupan perubahan:
 * "Ubah hanya hari ini" atau "Ubah semua jadwal berulang".
 *
 * @param semesterSubtitle teks di bawah judul saat menambah, mis. "Semester berjalan".
 * @param existingSchedules seluruh baris jadwal semester ini, dipakai untuk mendeteksi bentrok dan membaca seri.
 * @param onSave dipanggil dengan cakupan perubahan (diabaikan saat menambah) dan isian yang sudah valid.
 * @param onDelete bila tidak null (mode edit), tombol Hapus tampil; parameternya true untuk "semua pengulangan".
 */
@Composable
internal fun AddScheduleDialog(
    semester: Int,
    semesterSubtitle: String,
    semesterStartDate: LocalDate?,
    courseOptions: List<CourseEntity>,
    existingSchedules: List<ScheduleEntity>,
    onDismiss: () -> Unit,
    onSave: (ScheduleEditScope, ScheduleDraft) -> Unit,
    initialSchedule: ScheduleEntity? = null,
    onDelete: ((deleteSeries: Boolean) -> Unit)? = null
) {
    val askScope = initialSchedule != null && initialSchedule.isRecurring
    var scopeChoice by remember(initialSchedule?.id) {
        mutableStateOf<ScheduleEditScope?>(if (askScope) null else ScheduleEditScope.ONLY_THIS)
    }
    val chosen = scopeChoice
    if (chosen == null) {
        val schedule = initialSchedule ?: return
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Ubah jadwal berulang", fontWeight = FontWeight.Bold) },
            text = { Text("${schedule.courseName} berulang setiap ${dayNameOf(dayOfWeekOf(schedule))}. Mau mengubah satu pertemuan saja atau seluruh seri?") },
            confirmButton = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { scopeChoice = ScheduleEditScope.ONLY_THIS }) { Text("Ubah hanya hari ini", fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { scopeChoice = ScheduleEditScope.ALL }) { Text("Ubah semua jadwal berulang", fontWeight = FontWeight.Bold) }
                    TextButton(onClick = onDismiss) { Text("Batal") }
                }
            }
        )
    } else {
        ScheduleFormSheet(
            editScope = chosen,
            semester = semester,
            semesterSubtitle = semesterSubtitle,
            semesterStartDate = semesterStartDate,
            courseOptions = courseOptions,
            existingSchedules = existingSchedules,
            onDismiss = onDismiss,
            onSave = onSave,
            initialSchedule = initialSchedule,
            onDelete = onDelete
        )
    }
}

@Composable
private fun ScheduleFormSheet(
    editScope: ScheduleEditScope,
    semester: Int,
    semesterSubtitle: String,
    semesterStartDate: LocalDate?,
    courseOptions: List<CourseEntity>,
    existingSchedules: List<ScheduleEntity>,
    onDismiss: () -> Unit,
    onSave: (ScheduleEditScope, ScheduleDraft) -> Unit,
    initialSchedule: ScheduleEntity?,
    onDelete: ((deleteSeries: Boolean) -> Unit)?
) {
    val key1 = initialSchedule?.id
    val key2 = editScope
    val editing = initialSchedule != null
    val repeatAllowed = editScope == ScheduleEditScope.ALL
    val today = remember { LocalDate.now() }

    val krsByName = remember(courseOptions) { courseOptions.associateBy { it.name.trim().lowercase(Locale.ROOT) } }
    val seriesRows = remember(initialSchedule, existingSchedules) {
        if (initialSchedule != null && initialSchedule.isRecurring) {
            existingSchedules.filter { it.userId == initialSchedule.userId && it.recurrenceGroupId == initialSchedule.recurrenceGroupId }
        } else emptyList()
    }
    val seriesInfo = remember(seriesRows, semesterStartDate) { ScheduleRules.seriesInfoOf(seriesRows, semesterStartDate) }
    val initialSks: Int? = initialSchedule?.let { s ->
        s.sks.takeIf { it > 0 } ?: krsByName[s.courseName.trim().lowercase(Locale.ROOT)]?.credits
    }
    val defaultSingleDate = remember(semesterStartDate) {
        if (semesterStartDate != null && today.isBefore(semesterStartDate)) semesterStartDate else today
    }
    val defaultRangeStart = semesterStartDate ?: today

    var source by remember(key1, key2) {
        mutableStateOf(
            when {
                courseOptions.isEmpty() -> SCHEDULE_SOURCE_MANUAL
                initialSchedule != null -> if (initialSchedule.sumber == SCHEDULE_SOURCE_KRS) SCHEDULE_SOURCE_KRS else SCHEDULE_SOURCE_MANUAL
                else -> SCHEDULE_SOURCE_KRS
            }
        )
    }
    var krsId by remember(key1, key2) { mutableStateOf(initialSchedule?.krsId) }
    var courseName by remember(key1, key2) { mutableStateOf(initialSchedule?.courseName.orEmpty()) }
    var lecturer by remember(key1, key2) { mutableStateOf(initialSchedule?.lecturer.orEmpty()) }
    var sksText by remember(key1, key2) { mutableStateOf(initialSks?.toString().orEmpty()) }

    var repeating by remember(key1, key2) { mutableStateOf(repeatAllowed && initialSchedule != null && initialSchedule.isRecurring) }
    var singleDate by remember(key1, key2) {
        mutableStateOf(initialSchedule?.let { runCatching { LocalDate.parse(it.scheduledDate) }.getOrNull() } ?: defaultSingleDate)
    }
    var repeatDays by remember(key1, key2) {
        mutableStateOf(
            when {
                initialSchedule != null && initialSchedule.isRecurring -> setOf(initialSchedule.dayOfWeek)
                else -> setOf(defaultRangeStart.dayOfWeek.value)
            }
        )
    }
    var rangeStart by remember(key1, key2) { mutableStateOf(seriesInfo?.start ?: defaultRangeStart) }
    var rangeEnd by remember(key1, key2) { mutableStateOf<LocalDate?>(seriesInfo?.end) }
    var untilEnd by remember(key1, key2) { mutableStateOf(seriesInfo?.untilSemesterEnd ?: true) }

    var startTime by remember(key1, key2) { mutableStateOf(initialSchedule?.startTime ?: "08:00") }
    var endTime by remember(key1, key2) { mutableStateOf(initialSchedule?.endTime ?: "10:00") }
    var mode by remember(key1, key2) { mutableStateOf(initialSchedule?.mode ?: SCHEDULE_MODE_OFFLINE) }
    var room by remember(key1, key2) { mutableStateOf(initialSchedule?.room.orEmpty()) }
    var platform by remember(key1, key2) { mutableStateOf(initialSchedule?.platform.orEmpty()) }
    var meetingLink by remember(key1, key2) { mutableStateOf(initialSchedule?.linkMeeting.orEmpty()) }
    var notes by remember(key1, key2) { mutableStateOf(initialSchedule?.catatan.orEmpty()) }
    var remind by remember(key1, key2) { mutableStateOf(initialSchedule?.alarmEnabled ?: true) }
    var reminderMinutes by remember(key1, key2) { mutableIntStateOf(initialSchedule?.reminderMinutesBefore ?: DEFAULT_REMINDER_MINUTES) }

    var nameTouched by remember(key1, key2) { mutableStateOf(false) }
    var attempted by remember(key1, key2) { mutableStateOf(false) }
    var picker by remember { mutableStateOf<PickerTarget?>(null) }
    var courseMenuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val isRepeating = repeating && repeatAllowed
    val draft = ScheduleDraft(
        sumber = source,
        krsId = if (source == SCHEDULE_SOURCE_KRS) krsId else null,
        courseName = courseName,
        lecturer = lecturer,
        sks = sksText.toIntOrNull() ?: 0,
        repeating = isRepeating,
        date = singleDate,
        repeatDays = repeatDays,
        rangeStart = rangeStart,
        rangeEnd = rangeEnd,
        untilSemesterEnd = untilEnd,
        startTime = startTime,
        endTime = endTime,
        mode = mode,
        room = room,
        platform = platform,
        meetingLink = meetingLink,
        notes = notes,
        alarmEnabled = remind,
        reminderMinutes = reminderMinutes
    )

    val startMinutes = ScheduleRules.minutesOf(startTime)
    val endMinutes = ScheduleRules.minutesOf(endTime)
    val timeOrderInvalid = startMinutes != null && endMinutes != null && endMinutes <= startMinutes
    val duration = ScheduleRules.durationMinutes(startTime, endTime)
    val linkInvalid = mode == SCHEDULE_MODE_ONLINE && meetingLink.isNotBlank() && !ScheduleRules.isValidMeetingUrl(meetingLink)
    val nameError = courseName.isBlank() && (nameTouched || attempted)
    val formError = ScheduleRules.validate(draft, semesterStartDate)

    val ignoreIds = remember(initialSchedule, editScope, seriesRows) {
        when {
            initialSchedule == null -> emptySet()
            initialSchedule.isRecurring && editScope == ScheduleEditScope.ALL -> seriesRows.map { it.id }.toSet() + initialSchedule.id
            else -> setOf(initialSchedule.id)
        }
    }
    val conflicts = remember(draft, existingSchedules, ignoreIds) {
        val probe = draft.copy(courseName = "x", lecturer = "", sks = 0, meetingLink = "", reminderMinutes = 0)
        val rows = ScheduleRules.buildRows(probe, 0L, semester, semesterStartDate)
        ScheduleRules.findConflicts(existingSchedules, rows, ignoreIds)
    }
    val hasConflict = conflicts.isNotEmpty()

    TkFormSheet(onDismissRequest = onDismiss) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (editing) "Ubah jadwal" else "Tambah jadwal",
                    style = TkText.screenTitle,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    if (initialSchedule != null) "${initialSchedule.courseName} · ${dayNameOf(dayOfWeekOf(initialSchedule))}"
                    else "Semester $semester · $semesterSubtitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(shape = CircleShape, color = TemanKuliahBlueLight, modifier = Modifier.size(40.dp)) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TemanKuliahBlue, modifier = Modifier.size(18.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sumber mata kuliah
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Sumber mata kuliah")
                Segmented(
                    options = listOf("Dari KRS", "Tulis manual"),
                    selectedIndex = if (source == SCHEDULE_SOURCE_KRS) 0 else 1,
                    enabledFlags = listOf(courseOptions.isNotEmpty(), true),
                    onSelect = { index ->
                        source = if (index == 0) SCHEDULE_SOURCE_KRS else SCHEDULE_SOURCE_MANUAL
                        if (index == 1) krsId = null
                    },
                    modifier = Modifier.testTag("schedule_source")
                )
                if (courseOptions.isEmpty()) {
                    Text("Belum ada mata kuliah KRS di Semester $semester, jadi tulis manual.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            if (source == SCHEDULE_SOURCE_KRS) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Mata kuliah (dari KRS)")
                    Box(Modifier.fillMaxWidth()) {
                        Surface(
                            onClick = { courseMenuOpen = true; nameTouched = true },
                            shape = TkShape.md,
                            color = SurfaceLight,
                            border = BorderStroke(1.dp, if (nameError) TemanKuliahRedDark else DividerLight),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("input_schedule_course")
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    courseName.ifBlank { "Pilih mata kuliah" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (courseName.isBlank()) TextSecondary else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ChevronDown, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                        DropdownMenu(expanded = courseMenuOpen, onDismissRequest = { courseMenuOpen = false }) {
                            courseOptions.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text(course.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        courseName = course.name
                                        krsId = course.id
                                        sksText = course.credits.takeIf { it > 0 }?.toString().orEmpty()
                                        if (course.lecturer.isNotBlank()) lecturer = course.lecturer
                                        courseMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                    if (nameError) ErrorText("Nama mata kuliah wajib diisi")
                }
                OutlinedTextField(
                    value = lecturer,
                    onValueChange = { lecturer = it },
                    placeholder = { Text("Nama dosen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SksField(sksText, { sksText = it })
                    Text("Terisi otomatis dari KRS, tetap bisa diubah", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Nama mata kuliah *")
                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it; nameTouched = true },
                        placeholder = { Text("Nama mata kuliah") },
                        singleLine = true,
                        isError = nameError,
                        modifier = Modifier.fillMaxWidth().testTag("input_schedule_course")
                    )
                    if (nameError) ErrorText("Nama mata kuliah wajib diisi")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel("Nama dosen", optional = true)
                        OutlinedTextField(
                            value = lecturer,
                            onValueChange = { lecturer = it },
                            placeholder = { Text("Nama dosen") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(Modifier.width(96.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel("SKS")
                        SksField(sksText, { sksText = it })
                    }
                }
            }

            // Tipe jadwal
            if (repeatAllowed) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Tipe jadwal")
                    Segmented(
                        options = listOf("Sekali", "Berulang"),
                        selectedIndex = if (isRepeating) 1 else 0,
                        onSelect = { repeating = it == 1 },
                        modifier = Modifier.testTag("schedule_kind")
                    )
                }
            }

            if (!isRepeating) {
                TkPickerField(
                    label = "Tanggal",
                    value = longDate(singleDate),
                    icon = Icons.Default.CalendarToday,
                    onClick = { picker = PickerTarget.DATE },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Hari")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOptions.forEach { (day, label) ->
                            val selected = day in repeatDays
                            Box(
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) TemanKuliahBlue else SurfaceLight)
                                    .border(1.dp, if (selected) TemanKuliahBlue else DividerLight, CircleShape)
                                    .toggleable(
                                        value = selected,
                                        role = Role.Checkbox,
                                        onValueChange = { on -> repeatDays = if (on) repeatDays + day else repeatDays - day }
                                    )
                                    .semantics { contentDescription = dayNameOf(day) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else TextPrimary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TkPickerField(
                        label = "Tanggal mulai",
                        value = shortDate(rangeStart),
                        icon = Icons.Default.CalendarToday,
                        onClick = { picker = PickerTarget.RANGE_START },
                        modifier = Modifier.weight(1f)
                    )
                    TkPickerField(
                        label = "Tanggal berakhir",
                        value = if (untilEnd) "–" else (rangeEnd?.let(::shortDate) ?: "Pilih tanggal"),
                        icon = Icons.Default.CalendarToday,
                        onClick = { if (!untilEnd) picker = PickerTarget.RANGE_END },
                        modifier = Modifier.weight(1f).alpha(if (untilEnd) 0.5f else 1f)
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = untilEnd, role = Role.Checkbox, onValueChange = { untilEnd = it }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = untilEnd, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sampai akhir semester", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                if (untilEnd && semesterStartDate == null) ErrorText("Tanggal mulai semester belum diatur.")
            }

            // Jam
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TkPickerField("Jam mulai", startTime, Icons.Default.Schedule, { picker = PickerTarget.TIME_START }, Modifier.weight(1f))
                TkPickerField("Jam selesai", endTime, Icons.Default.Schedule, { picker = PickerTarget.TIME_END }, Modifier.weight(1f), isError = timeOrderInvalid)
            }
            if (timeOrderInvalid) ErrorText("Jam selesai harus lebih besar dari jam mulai")
            DurationChip(minutes = duration, valid = !timeOrderInvalid && duration > 0)

            // Mode kuliah
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Mode kuliah")
                Segmented(
                    options = listOf("Offline", "Online"),
                    selectedIndex = if (mode == SCHEDULE_MODE_ONLINE) 1 else 0,
                    onSelect = { mode = if (it == 1) SCHEDULE_MODE_ONLINE else SCHEDULE_MODE_OFFLINE },
                    modifier = Modifier.testTag("schedule_mode")
                )
            }
            if (mode == SCHEDULE_MODE_ONLINE) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Platform", optional = true)
                    OutlinedTextField(
                        value = platform,
                        onValueChange = { platform = it },
                        placeholder = { Text("Contoh: Zoom") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlatformSuggestions.forEach { name ->
                            ChoiceChip(name, selected = platform.trim().equals(name, ignoreCase = true), onClick = { platform = name })
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Tautan kuliah daring", optional = true)
                    OutlinedTextField(
                        value = meetingLink,
                        onValueChange = { meetingLink = it },
                        placeholder = { Text("https://meet.google.com/...") },
                        singleLine = true,
                        isError = linkInvalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth().testTag("input_schedule_link")
                    )
                    if (linkInvalid) ErrorText("Format tautan tidak valid (contoh: https://meet.google.com/...)")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Ruangan", optional = true)
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        placeholder = { Text("Contoh: Lab Komputer 2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Pengingat
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Pengingat", optional = true)
                Surface(shape = TkShape.lg, color = SurfaceLight, border = BorderStroke(1.dp, DividerLight), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp).clip(TkShape.md).background(TemanKuliahBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ingatkan saya", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                if (remind) "Notifikasi sebelum kuliah dimulai" else "Tidak ada pengingat",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(checked = remind, onCheckedChange = { remind = it })
                    }
                }
                if (remind) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReminderOptions.forEach { (minutes, label) ->
                            ChoiceChip(label, selected = reminderMinutes == minutes, onClick = { reminderMinutes = minutes })
                        }
                    }
                    val caption = remember(isRepeating, repeatDays, singleDate, startMinutes, reminderMinutes) {
                        if (startMinutes == null) null else {
                            val at = formatClock(((startMinutes - reminderMinutes) % 1440 + 1440) % 1440)
                            if (isRepeating) {
                                if (repeatDays.isEmpty()) null
                                else "Diingatkan setiap ${repeatDays.sorted().joinToString(" & ") { dayNameOf(it) }} pukul $at"
                            } else "Diingatkan pada ${longDate(singleDate)} pukul $at"
                        }
                    }
                    if (caption != null) {
                        Text(caption, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TemanKuliahBlue)
                    }
                }
            }

            // Catatan
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Catatan", optional = true)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Tambahkan catatan…") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Peringatan bentrok
            if (hasConflict) {
                val first = conflicts.first()
                Surface(
                    shape = TkShape.lg,
                    color = AmberSoft,
                    border = BorderStroke(1.dp, AmberBorder),
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AmberIcon, modifier = Modifier.size(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Jadwal bentrok", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AmberInk)
                            val more = if (conflicts.size > 1) " (dan ${conflicts.size - 1} jadwal lain)" else ""
                            Text(
                                "Bertabrakan dengan ${first.courseName} pada ${dayNameOf(first.dayOfWeek)}, ${first.startTime}–${first.endTime}$more. " +
                                    "Kamu tetap bisa menyimpan setelah mengonfirmasi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberInk
                            )
                        }
                    }
                }
            }

            if (attempted && formError != null) ErrorText(formError)
            Spacer(Modifier.size(2.dp))
        }

        // Tombol
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editing && onDelete != null) {
                Surface(shape = CircleShape, color = TemanKuliahRedLight, modifier = Modifier.size(52.dp)) {
                    IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("btn_delete_schedule")) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus jadwal", tint = TemanKuliahRedDark, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Button(
                onClick = onDismiss,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlueLight, contentColor = TemanKuliahBlue),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp)
            ) { Text("Batal", fontWeight = FontWeight.Bold) }
            Button(
                onClick = {
                    attempted = true
                    if (formError == null) onSave(editScope, draft)
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasConflict) AmberStrong else TemanKuliahBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1.4f).heightIn(min = 52.dp).testTag("submit_schedule_button")
            ) { Text(if (hasConflict) "Tetap simpan" else "Simpan", fontWeight = FontWeight.Bold) }
        }
    }

    when (picker) {
        PickerTarget.DATE -> TkDatePickerDialog(
            title = "Pilih tanggal kuliah",
            initialDate = singleDate,
            onDismiss = { picker = null },
            onConfirm = { singleDate = it; picker = null }
        )
        PickerTarget.RANGE_START -> TkDatePickerDialog(
            title = "Pilih tanggal mulai",
            initialDate = rangeStart,
            onDismiss = { picker = null },
            onConfirm = { rangeStart = it; picker = null }
        )
        PickerTarget.RANGE_END -> TkDatePickerDialog(
            title = "Pilih tanggal berakhir",
            initialDate = rangeEnd ?: rangeStart,
            onDismiss = { picker = null },
            onConfirm = { rangeEnd = it; picker = null }
        )
        PickerTarget.TIME_START -> MaterialTimePickerDialog(
            title = "Pilih jam mulai",
            initialTime = startTime,
            onDismiss = { picker = null },
            onConfirm = { picked ->
                // Jam selesai ikut bergeser agar durasi kelas tetap.
                val oldStart = ScheduleRules.minutesOf(startTime)
                val oldEnd = ScheduleRules.minutesOf(endTime)
                val newStart = ScheduleRules.minutesOf(picked)
                if (oldStart != null && oldEnd != null && newStart != null) {
                    val keep = if (oldEnd > oldStart) oldEnd - oldStart else 100
                    val newEnd = newStart + keep
                    endTime = if (newEnd < 24 * 60) formatClock(newEnd) else "23:59"
                }
                startTime = picked
                picker = null
            }
        )
        PickerTarget.TIME_END -> MaterialTimePickerDialog(
            title = "Pilih jam selesai",
            initialTime = endTime,
            onDismiss = { picker = null },
            onConfirm = { endTime = it; picker = null }
        )
        null -> Unit
    }

    if (confirmDelete && initialSchedule != null && onDelete != null) {
        ScheduleDeleteDialog(
            schedule = initialSchedule,
            meetingCount = if (initialSchedule.isRecurring) seriesRows.count { it.dayOfWeek == initialSchedule.dayOfWeek }.coerceAtLeast(1) else 1,
            onDeleteOne = { confirmDelete = false; onDelete(false) },
            onDeleteSeries = { confirmDelete = false; onDelete(true) },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun FieldLabel(text: String, optional: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (optional) Text("Opsional", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = TemanKuliahRedDark,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    )
}

@Composable
private fun SksField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(2)) },
        placeholder = { Text("0") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().testTag("input_schedule_sks")
    )
}

/** Tombol segmen: latar tonal, pilihan aktif berwarna biru penuh. */
@Composable
private fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabledFlags: List<Boolean> = options.map { true }
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(TkShape.lg)
            .background(SurfaceVariantLight)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val enabled = enabledFlags.getOrElse(index) { true }
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(TkShape.md)
                    .background(if (selected) TemanKuliahBlue else Color.Transparent)
                    .alpha(if (enabled) 1f else 0.4f)
                    .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) TemanKuliahBlue else SurfaceLight,
        border = if (selected) null else BorderStroke(1.dp, DividerLight),
        modifier = Modifier.heightIn(min = 40.dp)
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else TextPrimary
            )
        }
    }
}

@Composable
private fun DurationChip(minutes: Int, valid: Boolean) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (valid) TemanKuliahBlueLight else TemanKuliahRedLight)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Timer,
            contentDescription = null,
            tint = if (valid) TemanKuliahBlueDark else TemanKuliahRedDark,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (valid) "Durasi $minutes menit" else "Durasi tidak valid",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (valid) TemanKuliahBlueDark else TemanKuliahRedDark
        )
    }
}
