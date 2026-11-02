@file:Suppress("DEPRECATION")

package com.temankuliah.app.ui.calendar

import androidx.compose.animation.core.snap
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import com.temankuliah.app.ui.components.rememberReducedMotion
import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.tkShadow
import com.temankuliah.app.ui.theme.TkPalette
import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import com.temankuliah.app.ui.components.TkAddPillButton
import com.temankuliah.app.ui.components.TkPageHeader
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.data.local.AcademicEventEntity
import com.temankuliah.app.domain.TaskStatus
import com.temankuliah.app.domain.parseSemesterStartDate
import com.temankuliah.app.ui.DialogType
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.components.TkAlertDialog
import com.temankuliah.app.ui.components.TkDatePickerDialog
import com.temankuliah.app.ui.components.formatClock
import com.temankuliah.app.ui.icons.ChevronLeft
import com.temankuliah.app.ui.icons.ChevronRight
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkSpace
import com.temankuliah.app.ui.theme.rememberTkTwoPane
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CalendarTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale)
private val CalendarDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", IndonesianLocale)

private const val FILTER_ALL = "Semua"

private fun calendarCellDescription(date: LocalDate, eventCount: Int): String {
    val day = date.format(CalendarDateFormatter)
    return if (eventCount > 0) "$day, $eventCount agenda" else "$day, tidak ada agenda"
}

/**
 * Halaman Jadwal: kalender bulanan di atas, agenda tanggal terpilih di bawahnya.
 * Titik warna pada tanggal menandai jenis agenda: biru Kuliah, oranye Tugas, merah Ujian.
 * Kartu agenda memuat jadwal kuliah, deadline tugas, serta ujian dan kegiatan akademik.
 */
@Composable
fun CalendarScreen(viewModel: TemanKuliahViewModel, modifier: Modifier = Modifier) {
    val schedules by viewModel.scheduleController.schedules.collectAsStateWithLifecycle()
    val tasks by viewModel.taskController.tasks.collectAsStateWithLifecycle()
    val extraEvents by viewModel.calendarController.academicEvents.collectAsStateWithLifecycle()
    val allCourses by viewModel.courseController.courses.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val semesterStartDates by viewModel.preferencesController.semesterStartDates.collectAsStateWithLifecycle()

    var shownMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedFilter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    var isWeeklyView by rememberSaveable { mutableStateOf(false) }

    var showAddMenuSheet by remember { mutableStateOf(false) }
    var showAddAcademicDialog by remember { mutableStateOf(false) }
    var editorInitialType by remember { mutableStateOf("Ujian") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var editorEvent by remember { mutableStateOf<AcademicEventEntity?>(null) }
    var deletingEvent by remember { mutableStateOf<AcademicEventEntity?>(null) }

    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()

    val currentSchedules = schedules
    val currentTasks = remember(tasks, activeSemester) { tasks.filter { it.semester == activeSemester } }
    val currentExtra = remember(extraEvents, activeSemester) { extraEvents.filter { it.semester == activeSemester } }

    val startDateConfigured = parseSemesterStartDate(semesterStartDates[activeSemester]) != null

    // Jenis agenda per tanggal, dipakai untuk titik warna di grid bulanan.
    val kindsByDate: Map<LocalDate, Set<AgendaKind>> = remember(currentSchedules, currentTasks, currentExtra, zone) {
        val result = HashMap<LocalDate, MutableSet<AgendaKind>>()
        fun mark(date: LocalDate, kind: AgendaKind) {
            result.getOrPut(date) { mutableSetOf() }.add(kind)
        }
        currentSchedules.forEach { schedule ->
            runCatching { LocalDate.parse(schedule.scheduledDate) }.getOrNull()?.let { mark(it, AgendaKind.Kuliah) }
        }
        currentTasks.forEach { task ->
            mark(Instant.ofEpochMilli(task.dueAtMillis).atZone(zone).toLocalDate(), AgendaKind.Tugas)
        }
        currentExtra.forEach { event ->
            mark(Instant.ofEpochMilli(event.scheduledAtMillis).atZone(zone).toLocalDate(), agendaKindOf(event.type))
        }
        result
    }

    fun eventsFor(date: LocalDate): List<CalendarUiEvent> {
        val dateStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val nextDateStart = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val rows = mutableListOf<CalendarUiEvent>()

        // 1. Jadwal Kuliah
        currentSchedules.filter { it.scheduledDate == date.toString() }.forEach { schedule ->
            val time = runCatching { LocalTime.parse(schedule.startTime) }.getOrNull() ?: LocalTime.MIDNIGHT
            val millis = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
            val roomText = if (schedule.room.isNotBlank()) "Ruang ${schedule.room}" else "Ruang belum diatur"
            val lecturerText = if (schedule.lecturer.isNotBlank()) " · ${schedule.lecturer}" else ""
            rows += CalendarUiEvent(
                key = "schedule_${schedule.id}_$date",
                title = schedule.courseName,
                detail = "$roomText$lecturerText",
                type = "Kuliah",
                timestamp = millis,
                startLabel = schedule.startTime,
                captionLabel = schedule.endTime,
                onOpen = { viewModel.openDialog(DialogType.EditSchedule(schedule)) },
                completed = schedule.isCompleted,
                onToggleCompleted = { completed -> viewModel.scheduleController.setScheduleCompleted(schedule.id, completed) }
            )
        }

        // 2. Tugas dan deadline
        currentTasks.filter { it.dueAtMillis in dateStart until nextDateStart }.forEach { task ->
            val courseName = allCourses.firstOrNull { it.id == task.courseId }?.name ?: "Tugas mandiri"
            val isDone = task.status == TaskStatus.SELESAI
            val isOverdue = task.dueAtMillis < System.currentTimeMillis() && !isDone
            val priorityText = if (task.priority == "Tinggi" && !isDone) " · Prioritas tinggi" else ""
            rows += CalendarUiEvent(
                key = "task_${task.id}",
                title = task.title,
                detail = "$courseName · ${task.status}$priorityText",
                type = "Tugas",
                timestamp = task.dueAtMillis,
                startLabel = formatClock(task.dueAtMillis),
                captionLabel = "tenggat",
                isOverdue = isOverdue,
                onOpen = { viewModel.openDialog(DialogType.EditTask(task)) },
                completed = isDone,
                onToggleCompleted = { completed ->
                    viewModel.taskController.updateTaskStatus(
                        task,
                        if (completed) TaskStatus.SELESAI else TaskStatus.BELUM
                    )
                }
            )
        }

        // 3. Ujian dan kegiatan akademik
        currentExtra.filter { it.scheduledAtMillis in dateStart until nextDateStart }.forEach { event ->
            rows += CalendarUiEvent(
                key = "event_${event.id}",
                title = event.title,
                detail = event.description.ifBlank { event.type },
                type = event.type,
                timestamp = event.scheduledAtMillis,
                startLabel = formatClock(event.scheduledAtMillis),
                captionLabel = "mulai",
                onOpen = { editorEvent = event },
                onDelete = { deletingEvent = event }
            )
        }

        return rows.sortedBy { it.timestamp }
    }

    val rawDayEvents = remember(selectedDate, currentSchedules, currentTasks, currentExtra, allCourses) { eventsFor(selectedDate) }
    val filteredDayEvents = remember(rawDayEvents, selectedFilter) {
        when (selectedFilter) {
            "Kuliah" -> rawDayEvents.filter { it.type == "Kuliah" }
            "Tugas" -> rawDayEvents.filter { it.type == "Tugas" }
            "Ujian" -> rawDayEvents.filter { it.type == "Ujian" }
            "Kegiatan" -> rawDayEvents.filter { it.type.contains("Kegiatan", ignoreCase = true) || it.type == "Seminar" }
            else -> rawDayEvents
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
        shownMonth = YearMonth.from(date)
    }

    val calendarPane: LazyListScope.() -> Unit = {
        item(key = "header") {
            CalendarHeader(onAdd = { showAddMenuSheet = true })
        }

        item(key = "calendar_card") {
            val startOfWeek = remember(selectedDate) {
                selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            }
            val endOfWeek = remember(startOfWeek) { startOfWeek.plusDays(6) }
            val navTitle = if (isWeeklyView) {
                weekRangeLabel(startOfWeek, endOfWeek)
            } else {
                shownMonth.format(CalendarTitleFormatter).replaceFirstChar { it.titlecase(IndonesianLocale) }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = TkShape.card,
                color = SurfaceLight,
                border = BorderStroke(1.dp, DividerLight)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CalendarNavRow(
                        title = navTitle,
                        previousDescription = if (isWeeklyView) "Minggu sebelumnya" else "Bulan sebelumnya",
                        nextDescription = if (isWeeklyView) "Minggu berikutnya" else "Bulan berikutnya",
                        onPrevious = {
                            if (isWeeklyView) {
                                val prevWeek = selectedDate.minusWeeks(1)
                                selectedDate = prevWeek
                                shownMonth = YearMonth.from(prevWeek)
                            } else {
                                shownMonth = shownMonth.minusMonths(1)
                                selectedDate = shownMonth.atDay(minOf(selectedDate.dayOfMonth, shownMonth.lengthOfMonth()))
                            }
                        },
                        onNext = {
                            if (isWeeklyView) {
                                val nextWeek = selectedDate.plusWeeks(1)
                                selectedDate = nextWeek
                                shownMonth = YearMonth.from(nextWeek)
                            } else {
                                shownMonth = shownMonth.plusMonths(1)
                                selectedDate = shownMonth.atDay(minOf(selectedDate.dayOfMonth, shownMonth.lengthOfMonth()))
                            }
                        },
                        onToday = { selectDate(LocalDate.now()) }
                    )

                    // Tombol pengalih mode: Bulanan vs Mingguan (Segmented Pill Switcher)
                    CalendarViewModeSwitcher(
                        isWeeklyView = isWeeklyView,
                        onModeChange = { isWeeklyView = it }
                    )

                    val reducedMotion = rememberReducedMotion()
                    AnimatedContent(
                        targetState = isWeeklyView,
                        transitionSpec = {
                            if (reducedMotion) EnterTransition.None togetherWith ExitTransition.None else (fadeIn(animationSpec = tween(220, delayMillis = 30)) +
                             expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(150)) +
                                    shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing))
                                ).using(SizeTransform(clip = false))
                        },
                        label = "calendarGridTransition"
                    ) { weekly ->
                        if (weekly) {
                            WeekGrid(
                                startOfWeek = startOfWeek,
                                selectedDate = selectedDate,
                                today = today,
                                kindsByDate = kindsByDate,
                                onSelect = { selectedDate = it }
                            )
                        } else {
                            MonthGrid(
                                month = shownMonth,
                                selectedDate = selectedDate,
                                today = today,
                                kindsByDate = kindsByDate,
                                onSelect = { selectedDate = it }
                            )
                        }
                    }
                }
            }
        }
    }

    val agendaPane: LazyListScope.() -> Unit = {
        item(key = "agenda_summary_header") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = selectedDate.format(CalendarDateFormatter).replaceFirstChar { it.titlecase(IndonesianLocale) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = agendaCountLabel(rawDayEvents.size, selectedDate == today),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        item(key = "agenda_filters") {
            AgendaFilterChips(selected = selectedFilter, onSelect = { selectedFilter = it })
        }

        if (filteredDayEvents.isEmpty()) {
            item(key = "empty_agenda_${selectedDate}_$selectedFilter") {
                EmptyAgendaCard(filteredOut = rawDayEvents.isNotEmpty())
            }
        } else {
            items(filteredDayEvents, key = { it.key }) { event ->
                Box(
                    modifier = Modifier.animateItem()
                ) {
                    CalendarEventRow(event = event)
                }
            }
        }
    }


    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWide = rememberTkTwoPane()
        if (isWide) {
            // Tablet: kalender di kiri, agenda di kanan.
            Row(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(start = TkSpace.page, end = 10.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) { calendarPane() }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(start = 10.dp, end = TkSpace.page, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) { agendaPane() }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("calendar_screen"),
                contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                calendarPane()
                agendaPane()
            }
        }
    }

    if (showAddMenuSheet) {
        CalendarAddOptionBottomSheet(
            onDismiss = { showAddMenuSheet = false },
            onAddSchedule = {
                // Jadwal kuliah butuh tanggal mulai semester; tanyakan dulu bila belum ada.
                if (startDateConfigured) viewModel.openDialog(DialogType.AddSchedule) else showStartDatePicker = true
            },
            onAddTask = { viewModel.openDialog(DialogType.AddTask) },
            onAddAcademicEvent = {
                editorInitialType = "Ujian"
                showAddAcademicDialog = true
            },
            onAddActivity = {
                editorInitialType = "Kegiatan Kampus"
                showAddAcademicDialog = true
            }
        )
    }

    if (showStartDatePicker) {
        TkDatePickerDialog(
            title = "Tanggal Mulai Perkuliahan Semester $activeSemester",
            initialDate = parseSemesterStartDate(semesterStartDates[activeSemester]) ?: LocalDate.now(),
            onDismiss = { showStartDatePicker = false },
            onConfirm = { date ->
                viewModel.preferencesController.setSemesterStartDate(activeSemester, date.toString())
                showStartDatePicker = false
                viewModel.openDialog(DialogType.AddSchedule)
            }
        )
    }

    if (showAddAcademicDialog || editorEvent != null) {
        val initial = editorEvent
        AcademicEventEditorDialog(
            initial = initial,
            defaultTimestamp = selectedDate.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            initialType = editorInitialType,
            onDismiss = {
                showAddAcademicDialog = false
                editorEvent = null
            },
            onSave = { title, description, type, millis, remind, reminderMinutes ->
                if (initial == null) {
                    viewModel.calendarController.addAcademicEvent(title, description, type, millis, remind, reminderMinutes)
                } else {
                    viewModel.calendarController.updateAcademicEvent(initial, title, description, type, millis, remind, reminderMinutes)
                }
                showAddAcademicDialog = false
                editorEvent = null
            }
        )
    }

    deletingEvent?.let { event ->
        TkAlertDialog(
            onDismissRequest = { deletingEvent = null },
            title = "Hapus agenda?",
            text = "${event.title} akan dihapus dari kalender akademik.",
            confirmLabel = "Hapus",
            isDestructive = true,
            onConfirm = {
                viewModel.calendarController.deleteAcademicEvent(event)
                deletingEvent = null
            }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Bagian-bagian layar
// ---------------------------------------------------------------------------------------------

/** Judul "Jadwal" dengan tombol pil "Tambah" di kanan; memakai header halaman bersama. */
@Composable
private fun CalendarHeader(onAdd: () -> Unit) {
    TkPageHeader(
        title = "Jadwal",
        supporting = "Kuliah, tugas, ujian, dan agenda",
        trailing = {
            TkAddPillButton(
                onClick = onAdd,
                contentDescription = "Tambah agenda",
                modifier = Modifier.testTag("fab_add_schedule")
            )
        }
    )
}

/** Baris navigasi kalender: judul periode di kiri, dan di kanan tombol "Hari ini" beserta panah sebelumnya/berikutnya. */
@Composable
private fun CalendarNavRow(
    title: String,
    previousDescription: String,
    nextDescription: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val reducedMotion = rememberReducedMotion()
        AnimatedContent(
            targetState = title,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 8.dp),
            transitionSpec = {
                if (reducedMotion) EnterTransition.None togetherWith ExitTransition.None else (fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(200)) { it / 3 })
                    .togetherWith(
                        fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(150)) { -it / 3 }
                    ).using(SizeTransform(clip = false))
            },
            label = "calendarNavTitle"
        ) { targetTitle ->
            Text(
                text = targetTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                onClick = onToday,
                shape = TkShape.sm,
                color = Color.Transparent,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("btn_calendar_today")
            ) {
                Text(
                    text = "Hari ini",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TemanKuliahBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            NavSquareButton(Icons.Default.ChevronLeft, previousDescription, onPrevious)
            NavSquareButton(Icons.Default.ChevronRight, nextDescription, onNext)
        }
    }
}

@Composable
private fun NavSquareButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = TkShape.md,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        shadowElevation = tkShadow(0.5.dp),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                icon,
                contentDescription = description,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Pengalih tampilan kalender: Bulanan vs Mingguan.
 * Mengikuti tata letak segmented control pill pada desain referensi:
 * - Wadah pill melengkung penuh (full width) dengan latar abu-abu/biru lembut
 * - Tab aktif berupa kartu putih meluncur (sliding pill thumb) dengan bayangan lembut dan teks gelap tebal
 * - Transisi posisi menggunakan spring physics yang mulus dan natural
 * - Label teks bertransisi warna halus antara TextPrimary dan TextSecondary
 */
@Composable
private fun CalendarViewModeSwitcher(
    isWeeklyView: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(TkShape.md)
            .background(tkAdaptive(Color(0xFFF1F5F9), TkPalette.colors.surfaceVariant))
            .border(BorderStroke(1.dp, DividerLight), TkShape.md)
            .padding(3.dp)
    ) {
        val tabWidth = maxWidth / 2
        val reducedMotion = rememberReducedMotion()
        val animatedOffset by animateDpAsState(
            targetValue = if (isWeeklyView) tabWidth else 0.dp,
            animationSpec = if (reducedMotion) snap() else spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "pillOffset"
        )

        // Pill putih yang meluncur halus di bawah tab yang aktif
        Box(
            modifier = Modifier
                .offset { IntOffset(x = animatedOffset.roundToPx(), y = 0) }
                .width(tabWidth)
                .fillMaxHeight()
                .shadow(elevation = tkShadow(2.dp), shape = TkShape.sm, clip = false)
                .clip(TkShape.sm)
                .background(tkAdaptive(Color.White, Color(0xFF2B4470)))
        )

        // Label tab yang dapat diklik
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isMonthly = !isWeeklyView

            // Tab Bulanan
            val monthlyTextColor by animateColorAsState(
                targetValue = if (isMonthly) TextPrimary else TextSecondary,
                animationSpec = tween(durationMillis = 180),
                label = "monthlyTextColor"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(TkShape.sm)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onModeChange(false) }
                    .testTag("btn_calendar_monthly"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Bulanan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isMonthly) FontWeight.Bold else FontWeight.Medium,
                    color = monthlyTextColor,
                    maxLines = 1
                )
            }

            // Tab Mingguan
            val weeklyTextColor by animateColorAsState(
                targetValue = if (isWeeklyView) TextPrimary else TextSecondary,
                animationSpec = tween(durationMillis = 180),
                label = "weeklyTextColor"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(TkShape.sm)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onModeChange(true) }
                    .testTag("btn_calendar_weekly"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mingguan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isWeeklyView) FontWeight.Bold else FontWeight.Medium,
                    color = weeklyTextColor,
                    maxLines = 1
                )
            }
        }
    }
}

/** Titik penanda jenis agenda. Pada tanggal terpilih (latar biru) titiknya putih. */
@Composable
private fun AgendaDots(kinds: Set<AgendaKind>, onSelectedBackground: Boolean) {
    Row(
        modifier = Modifier.height(5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AgendaKind.entries.filter { it in kinds }.forEach { kind ->
            Box(
                Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (onSelectedBackground) Color.White else kind.accent)
            )
        }
    }
}

/** Semantik sel tanggal: satu tombol dengan deskripsi lengkap, tetap bisa diaktifkan TalkBack. */
private fun Modifier.dayCellSemantics(description: String, isSelected: Boolean, onSelect: () -> Unit): Modifier =
    clearAndSetSemantics {
        role = Role.Button
        selected = isSelected
        contentDescription = description
        onClick {
            onSelect()
            true
        }
    }

/** Grid mingguan: menampilkan 7 hari pada minggu terpilih (Senin - Minggu). */
@Composable
private fun WeekGrid(
    startOfWeek: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    kindsByDate: Map<LocalDate, Set<AgendaKind>>,
    onSelect: (LocalDate) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            ShortDayNames.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (index == 6) TemanKuliahRedDark else TextSecondary
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        val reducedMotion = rememberReducedMotion()
        AnimatedContent(
            targetState = startOfWeek,
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = tween(220)) { it / 3 } + fadeIn(animationSpec = tween(180)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { -it / 3 } + fadeOut(animationSpec = tween(140)))
                } else {
                    (slideInHorizontally(animationSpec = tween(220)) { -it / 3 } + fadeIn(animationSpec = tween(180)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { it / 3 } + fadeOut(animationSpec = tween(140)))
                }.using(SizeTransform(clip = false))
            },
            label = "weekDaysSlide"
        ) { currentStartOfWeek ->
            val days = (0..6).map { currentStartOfWeek.plusDays(it.toLong()) }
            Row(Modifier.fillMaxWidth()) {
                days.forEach { date ->
                    MonthDayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        kinds = kindsByDate[date].orEmpty(),
                        modifier = Modifier.weight(1f),
                        onSelect = { onSelect(date) }
                    )
                }
            }
        }
    }
}

/** Grid bulanan ringkas: hanya minggu yang berisi tanggal bulan ini, tanpa tanggal bulan lain. */
@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    kindsByDate: Map<LocalDate, Set<AgendaKind>>,
    onSelect: (LocalDate) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            ShortDayNames.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (index == 6) TemanKuliahRedDark else TextSecondary
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        val reducedMotion = rememberReducedMotion()
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = tween(220)) { it / 3 } + fadeIn(animationSpec = tween(180)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { -it / 3 } + fadeOut(animationSpec = tween(140)))
                } else {
                    (slideInHorizontally(animationSpec = tween(220)) { -it / 3 } + fadeIn(animationSpec = tween(180)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { it / 3 } + fadeOut(animationSpec = tween(140)))
                }.using(SizeTransform(clip = false))
            },
            label = "monthDaysSlide"
        ) { currentMonth ->
            val monthLeadingBlanks = currentMonth.atDay(1).dayOfWeek.value - 1
            val monthCells: List<LocalDate?> = List(monthLeadingBlanks) { null } + (1..currentMonth.lengthOfMonth()).map { currentMonth.atDay(it) }
            Column(Modifier.fillMaxWidth()) {
                monthCells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        for (column in 0 until 7) {
                            val date = week.getOrNull(column)
                            if (date == null) {
                                Spacer(Modifier.weight(1f).height(MonthCellHeight))
                            } else {
                                MonthDayCell(
                                    date = date,
                                    isSelected = date == selectedDate,
                                    isToday = date == today,
                                    kinds = kindsByDate[date].orEmpty(),
                                    modifier = Modifier.weight(1f),
                                    onSelect = { onSelect(date) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val MonthCellHeight = 40.dp

@Composable
private fun MonthDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    kinds: Set<AgendaKind>,
    modifier: Modifier,
    onSelect: () -> Unit
) {
    val isSunday = date.dayOfWeek.value == 7
    val cellBgColor by animateColorAsState(
        targetValue = if (isSelected) TemanKuliahBlue else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "cellBgColor"
    )
    val cellTextColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color.White
            isSunday -> TemanKuliahRedDark
            isToday -> TemanKuliahBlueDark
            else -> TextPrimary
        },
        animationSpec = tween(durationMillis = 180),
        label = "cellTextColor"
    )
    val cellScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = if (rememberReducedMotion()) snap() else spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "cellScale"
    )

    Column(
        modifier = modifier
            .height(MonthCellHeight)
            .clip(TkShape.md)
            .clickable(onClick = onSelect)
            .dayCellSemantics(calendarCellDescription(date, kinds.size), isSelected, onSelect),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .scale(cellScale)
                .clip(CircleShape)
                .background(cellBgColor)
                .then(if (isToday && !isSelected) Modifier.border(1.5.dp, TemanKuliahBlue, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = cellTextColor,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(1.dp))
        AgendaDots(kinds = kinds, onSelectedBackground = false)
    }
}

/** Chip filter: Semua, Kuliah, Tugas, Ujian, Kegiatan (dengan titik warna jenisnya). */
@Composable
private fun AgendaFilterChips(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipPill(
            label = FILTER_ALL,
            accent = null,
            soft = null,
            ink = null,
            isSelected = selected == FILTER_ALL,
            onClick = { onSelect(FILTER_ALL) }
        )
        AgendaKind.entries.forEach { kind ->
            FilterChipPill(
                label = kind.label,
                accent = kind.accent,
                soft = kind.soft,
                ink = kind.ink,
                isSelected = selected == kind.label,
                onClick = { onSelect(kind.label) }
            )
        }
        FilterChipPill(
            label = "Kegiatan",
            accent = ActivityKindColors.accent,
            soft = ActivityKindColors.soft,
            ink = ActivityKindColors.ink,
            isSelected = selected == "Kegiatan",
            onClick = { onSelect("Kegiatan") }
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    accent: Color?,
    soft: Color?,
    ink: Color?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val container = when {
        !isSelected -> SurfaceLight
        accent == null -> TemanKuliahBlue
        else -> soft ?: SurfaceLight
    }
    val content = when {
        !isSelected -> TextSecondary
        accent == null -> Color.White
        else -> ink ?: TextPrimary
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        border = when {
            accent == null && isSelected -> null
            isSelected -> BorderStroke(1.dp, accent ?: DividerLight)
            else -> BorderStroke(1.dp, DividerLight)
        },
        modifier = Modifier
            .heightIn(min = 40.dp)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (accent != null) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = content,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Kartu kosong "Tidak ada agenda". */
@Composable
private fun EmptyAgendaCard(filteredOut: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(TkShape.md).background(TemanKuliahBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TkOutlineIcons.Calendar,
                    contentDescription = null,
                    tint = TemanKuliahBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Tidak ada agenda",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (filteredOut) "Tidak ada agenda untuk filter ini. Pilih filter lain." else "Waktunya istirahat, atau tambahkan jadwal baru.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
