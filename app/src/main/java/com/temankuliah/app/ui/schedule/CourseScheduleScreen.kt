@file:Suppress("DEPRECATION")

package com.temankuliah.app.ui.schedule

import com.temankuliah.app.ui.components.TkScrollableDialogText
import com.temankuliah.app.ui.theme.TkGradientEnd
import com.temankuliah.app.ui.theme.TkGradientStart
import androidx.compose.ui.draw.shadow
import com.temankuliah.app.ui.components.tkHeroCircles
import com.temankuliah.app.ui.components.TkSemesterPickerSheet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.data.local.SCHEDULE_MODE_ONLINE
import com.temankuliah.app.domain.ScheduleRules
import com.temankuliah.app.domain.ScheduleSlot
import com.temankuliah.app.domain.parseSemesterStartDate
import com.temankuliah.app.domain.visibleInScheduleList
import com.temankuliah.app.ui.DialogType
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.components.TkDatePickerDialog
import com.temankuliah.app.ui.components.TkSubHeader
import com.temankuliah.app.ui.icons.Add
import com.temankuliah.app.ui.icons.CalendarMonth
import com.temankuliah.app.ui.icons.ChevronDown
import com.temankuliah.app.ui.icons.Delete
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TemanKuliahOrangeLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TemanKuliahRedLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import com.temankuliah.app.ui.theme.TkSpace
import java.time.LocalDate
import java.util.Locale
import com.temankuliah.app.ui.theme.tkShadow

private val DayChips = listOf(0 to "Semua", 1 to "Sen", 2 to "Sel", 3 to "Rab", 4 to "Kam", 5 to "Jum", 6 to "Sab", 7 to "Min")
private val ModeChips = listOf(null to "Semua mode", "OFFLINE" to "Offline", SCHEDULE_MODE_ONLINE to "Online")

/**
 * Jadwal Kuliah sesuai desain: header (menu titik tiga), pemilih semester, pita statistik biru, lalu Daftar jadwal
 * (keadaan kosong, atau chip hari + chip mode + jadwal dikelompokkan per hari). Semua angka dihitung dari data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScheduleScreen(
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val controller = viewModel.scheduleController
    val courses by viewModel.courseController.courses.collectAsStateWithLifecycle()
    val allSchedules by controller.allSchedules.collectAsStateWithLifecycle()
    val pendingUndo by controller.pendingUndo.collectAsStateWithLifecycle()
    val semesterStartDates by viewModel.preferencesController.semesterStartDates.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val semesterCount by viewModel.semesterCount.collectAsStateWithLifecycle()

    var selectedSemester by remember(activeSemester) { mutableIntStateOf(activeSemester) }
    var semesterMenuExpanded by remember { mutableStateOf(false) }
    var headerMenuExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var confirmDeleteAllSchedules by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ScheduleSlot?>(null) }
    var selectedDayFilter by remember { mutableIntStateOf(0) }
    var selectedMode by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val today = remember { LocalDate.now() }

    val startDateConfigured = parseSemesterStartDate(semesterStartDates[selectedSemester]) != null
    val storedSemesterSchedules = remember(allSchedules, selectedSemester) {
        allSchedules.filter { it.semester == selectedSemester }
    }

    // Jadwal baru disimpan ke semester yang sedang dilihat di halaman ini, supaya langsung tampil di daftar.
    DisposableEffect(selectedSemester) {
        controller.viewedSemester.value = selectedSemester
        onDispose { controller.viewedSemester.value = null }
    }

    // Snackbar "Urungkan" setelah hapus.
    LaunchedEffect(pendingUndo?.token) {
        val undo = pendingUndo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(undo.message, actionLabel = "Urungkan", duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) controller.undoDelete(undo)
        controller.consumeUndo(undo.token)
    }

    val krsCourses = remember(courses, selectedSemester) { courses.filter { it.semester == selectedSemester } }
    val creditsByCourse = remember(krsCourses) { krsCourses.associate { it.name.trim().lowercase(Locale.ROOT) to it.credits } }
    // Jadwal Sekali yang sudah selesai tidak ditampilkan di sini, tetapi tetap ada di Kalender.
    // Memakai semua baris tersimpan (bukan yang disaring tanggal mulai) agar jadwal yang ada tidak "hilang"
    // dari halaman ini saat tanggal mulai belum diatur; Kalender dan Beranda tetap menunggu tanggal mulai.
    val listedSchedules = remember(storedSemesterSchedules) { storedSemesterSchedules.visibleInScheduleList() }
    val slots = remember(listedSchedules, today) { ScheduleRules.buildSlots(listedSchedules, today) }
    val weeklyHours = remember(slots) { ScheduleRules.formatHours(ScheduleRules.faceToFaceMinutesPerWeek(slots)) }
    val dayChips = remember(slots) {
        val extra = slots.map { it.representative.dayOfWeek }.toSet()
        DayChips.filter { (day, _) -> day <= 5 || day == 0 || day in extra }
    }
    // Filter mode hanya relevan (dan ditampilkan) bila ada jadwal Online; tanpa itu halaman sama dengan desain.
    val hasOnlineSlot = remember(slots) { slots.any { it.representative.mode == SCHEDULE_MODE_ONLINE } }
    val groupedSlots = remember(slots, selectedDayFilter, selectedMode, hasOnlineSlot) {
        slots
            .filter { selectedDayFilter == 0 || it.representative.dayOfWeek == selectedDayFilter }
            .filter { !hasOnlineSlot || selectedMode == null || it.representative.mode == selectedMode }
            .groupBy { it.representative.dayOfWeek }
            .toSortedMap()
            .toList()
    }

    fun addSchedule() {
        if (startDateConfigured) viewModel.openDialog(DialogType.AddSchedule) else showStartDatePicker = true
    }

    Box(modifier.fillMaxSize().background(BackgroundLight)) {
        Column(Modifier.fillMaxSize()) {
            TkSubHeader(
                title = "Jadwal kuliah",
                supporting = "Kelola jadwal perkuliahanmu",
                onBack = onBack,
                backDescription = "Kembali ke Akademik",
                trailing = {
                    Box {
                        Surface(
                            shape = TkShape.md,
                            color = SurfaceLight,
                            border = BorderStroke(1.dp, DividerLight),
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(onClick = { headerMenuExpanded = true }, modifier = Modifier.testTag("schedule_more_menu")) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Menu jadwal", tint = TemanKuliahBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                        DropdownMenu(expanded = headerMenuExpanded, onDismissRequest = { headerMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Atur tanggal mulai semester") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(18.dp)) },
                                onClick = { headerMenuExpanded = false; showStartDatePicker = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus semua jadwal", color = TemanKuliahRedDark) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = TemanKuliahRedDark, modifier = Modifier.size(18.dp)) },
                                enabled = storedSemesterSchedules.isNotEmpty(),
                                onClick = { headerMenuExpanded = false; confirmDeleteAllSchedules = true }
                            )
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "semester_picker") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pilih semester", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        SemesterPickerCard(
                            semester = selectedSemester,
                            subtitle = when {
                                selectedSemester == activeSemester -> "Semester berjalan"
                                selectedSemester < activeSemester -> "Semester selesai"
                                else -> "Belum dimulai"
                            },
                            isActive = selectedSemester == activeSemester,
                            onClick = { semesterMenuExpanded = true }
                        )
                    }
                }

                item(key = "stats_band") {
                    Surface(
                        shape = TkShape.xl,
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = tkShadow(16.dp),
                                shape = TkShape.xl,
                                ambientColor = TkGradientEnd.copy(alpha = 0.25f),
                                spotColor = TkGradientEnd.copy(alpha = 0.45f)
                            )
                    ) {
                        Row(
                            Modifier
                                .background(Brush.linearGradient(listOf(TkGradientStart, TkGradientEnd)))
                                .tkHeroCircles()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BandStat("Total jadwal", "${slots.size}", "pertemuan", Modifier.weight(1f))
                            BandStat("Mata kuliah", "${krsCourses.size}", "di KRS", Modifier.weight(1f))
                            BandStat("Tatap muka", weeklyHours, "per minggu", Modifier.weight(1f))
                        }
                    }
                }

                if (!startDateConfigured && storedSemesterSchedules.isEmpty()) {
                    item(key = "empty_card") {
                        Surface(
                            shape = TkShape.xl,
                            color = SurfaceLight,
                            border = BorderStroke(1.dp, DividerLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                ListHeader(onAdd = ::addSchedule)
                                StartDateEmptyState(semester = selectedSemester, onSetStart = { showStartDatePicker = true })
                            }
                        }
                    }
                } else {
                    item(key = "list_header") { ListHeader(onAdd = ::addSchedule) }

                    if (!startDateConfigured) {
                        item(key = "start_date_notice") {
                            StartDateNotice(semester = selectedSemester, onSetStart = { showStartDatePicker = true })
                        }
                    }

                    if (slots.isEmpty()) {
                        item(key = "no_schedule") { NoScheduleState(onAdd = ::addSchedule) }
                    } else {
                        item(key = "day_chips") {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                dayChips.forEach { (index, label) ->
                                    FilterPill(label, selectedDayFilter == index) { selectedDayFilter = index }
                                }
                            }
                        }
                        if (hasOnlineSlot) item(key = "mode_chips") {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ModeChips.forEach { (value, label) ->
                                    FilterPill(label, selectedMode == value) { selectedMode = value }
                                }
                            }
                        }

                        if (groupedSlots.isEmpty()) {
                            item(key = "no_match") {
                                Text(
                                    "Tidak ada jadwal yang cocok dengan filter ini.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp)
                                )
                            }
                        } else {
                            groupedSlots.forEach { (day, daySlots) ->
                                item(key = "day_header_$day") {
                                    Text(
                                        dayNameOf(day),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 4.dp).semantics { contentDescription = "Jadwal hari ${dayNameOf(day)}" }
                                    )
                                }
                                items(daySlots, key = { "slot_${it.key}" }) { slot ->
                                    key(slot.key) {
                                        val dismissState = rememberSwipeToDismissBoxState(
                                            confirmValueChange = { value ->
                                                if (value == SwipeToDismissBoxValue.EndToStart) pendingDelete = slot
                                                false
                                            }
                                        )
                                        SwipeToDismissBox(
                                            state = dismissState,
                                            modifier = Modifier.animateItem(),
                                            enableDismissFromStartToEnd = false,
                                            backgroundContent = {
                                                Box(
                                                    Modifier
                                                        .fillMaxSize()
                                                        .clip(TkShape.card)
                                                        .background(TemanKuliahRedLight)
                                                        .padding(end = 24.dp),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = TemanKuliahRedDark)
                                                }
                                            }
                                        ) {
                                            ScheduleCard(
                                                slot = slot,
                                                credits = creditsByCourse[slot.representative.courseName.trim().lowercase(Locale.ROOT)],
                                                onEdit = { viewModel.openDialog(DialogType.EditSchedule(slot.representative)) }
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
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 8.dp))
    }

    if (showStartDatePicker) {
        TkDatePickerDialog(
            title = "Tanggal Mulai Perkuliahan Semester $selectedSemester",
            initialDate = parseSemesterStartDate(semesterStartDates[selectedSemester]) ?: LocalDate.now(),
            onDismiss = { showStartDatePicker = false },
            onConfirm = { date ->
                viewModel.preferencesController.setSemesterStartDate(selectedSemester, date.toString())
                showStartDatePicker = false
            }
        )
    }

    pendingDelete?.let { slot ->
        ScheduleDeleteDialog(
            schedule = slot.representative,
            meetingCount = slot.rows.size,
            onDeleteOne = { controller.deleteOne(slot.representative.id); pendingDelete = null },
            onDeleteSeries = { controller.deleteSeries(slot.representative.recurrenceGroupId); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }

    if (confirmDeleteAllSchedules) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAllSchedules = false },
            title = { Text("Hapus semua jadwal Semester $selectedSemester?") },
            text = {
                TkScrollableDialogText {
                    Text("${storedSemesterSchedules.size} jadwal kuliah (semua pertemuan, termasuk yang berasal dari KRS) akan dihapus dan semua pengingatnya dibatalkan. Kamu masih bisa mengurungkannya sebentar setelah ini.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    controller.deleteAllForSemester(selectedSemester)
                    confirmDeleteAllSchedules = false
                }) { Text("Hapus semua", color = TemanKuliahRedDark) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAllSchedules = false }) { Text("Batal") } }
        )
    }

    if (semesterMenuExpanded) {
        TkSemesterPickerSheet(
            title = "Pilih semester",
            subtitle = "Jadwal tersimpan per semester",
            selectedSemester = selectedSemester,
            activeSemester = activeSemester,
            semesterCount = maxOf(8, semesterCount),
            summaryOf = { sem ->
                val count = allSchedules.count { it.semester == sem }
                if (count == 0) "Belum diatur" else "$count jadwal"
            },
            onSelect = { sem ->
                selectedSemester = sem
                selectedDayFilter = 0
                selectedMode = null
                semesterMenuExpanded = false
            },
            onDismissRequest = { semesterMenuExpanded = false }
        )
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) TemanKuliahBlue else SurfaceLight,
        border = if (selected) null else BorderStroke(1.dp, DividerLight),
        modifier = Modifier.heightIn(min = 40.dp)
    ) {
        Box(Modifier.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
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
private fun NoScheduleState(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Belum ada jadwal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(
            "Tambahkan jadwal kuliah untuk semester ini.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        androidx.compose.material3.Button(
            onClick = onAdd,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue, contentColor = Color.White),
            modifier = Modifier.heightIn(min = 52.dp).testTag("btn_add_first_schedule")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tambah jadwal", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SemesterPickerCard(semester: Int, subtitle: String, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_semester_picker")
            .semantics { contentDescription = "Semester $semester. Ketuk untuk memilih semester" }
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(TkShape.md).background(TemanKuliahBlue),
                contentAlignment = Alignment.Center
            ) {
                Text("$semester", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Semester $semester", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isActive) {
                Box(Modifier.clip(CircleShape).background(TemanKuliahGreenLight).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("Aktif", color = TemanKuliahGreenDark, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.ChevronDown, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun BandStat(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(TkShape.lg)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.92f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(caption, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.92f), maxLines = 1)
    }
}

@Composable
private fun ListHeader(onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Daftar jadwal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Surface(
            onClick = onAdd,
            shape = CircleShape,
            color = TemanKuliahBlueLight,
            modifier = Modifier.heightIn(min = 44.dp).testTag("btn_add_schedule")
        ) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tambah", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TemanKuliahBlue)
            }
        }
    }
}

/** Peringatan saat jadwal sudah ada tetapi tanggal mulai semester belum diatur. */
@Composable
private fun StartDateNotice(semester: Int, onSetStart: () -> Unit) {
    Surface(
        shape = TkShape.card,
        color = TemanKuliahOrangeLight,
        modifier = Modifier.fillMaxWidth().testTag("schedule_start_date_notice")
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Tanggal mulai Semester $semester belum diatur",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TemanKuliahOrangeDark
            )
            Text(
                "Jadwal di bawah belum tampil di Kalender dan Beranda sampai tanggal mulai diatur.",
                style = MaterialTheme.typography.bodySmall,
                color = TemanKuliahOrangeDark
            )
            TextButton(onClick = onSetStart, modifier = Modifier.testTag("btn_notice_set_start_date")) {
                Text("Atur tanggal mulai", fontWeight = FontWeight.Bold, color = TemanKuliahOrangeDark)
            }
        }
    }
}

@Composable
private fun StartDateEmptyState(semester: Int, onSetStart: () -> Unit) {
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 10f)) }
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = DividerLight,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = dash)
                )
            }
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(shape = TkShape.lg, color = SurfaceLight, shadowElevation = tkShadow(3.dp), modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(28.dp))
            }
        }
        Text(
            "Tanggal mulai kuliah belum diatur",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            "Atur tanggal mulai Semester $semester agar jadwal kuliah bisa dibuat dan ditampilkan.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(4.dp))
        androidx.compose.material3.Button(
            onClick = onSetStart,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue, contentColor = Color.White),
            modifier = Modifier.heightIn(min = 52.dp).testTag("btn_set_schedule_start_date")
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Atur tanggal mulai", fontWeight = FontWeight.Bold)
        }
    }
}
