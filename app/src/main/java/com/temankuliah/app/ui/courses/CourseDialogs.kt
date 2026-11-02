@file:Suppress("DEPRECATION")

package com.temankuliah.app.ui.courses

import com.temankuliah.app.domain.Grades
import com.temankuliah.app.domain.MAX_SKS_PER_COURSE

import com.temankuliah.app.ui.components.TkFormSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.temankuliah.app.ui.icons.Schedule
import com.temankuliah.app.ui.components.MaterialTimePickerDialog
import com.temankuliah.app.ui.components.TkPickerField
import com.temankuliah.app.ui.theme.TkText
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.domain.GpaCalculator
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.OutlinedButton
import com.temankuliah.app.ui.components.OutlinedTextField
import com.temankuliah.app.ui.icons.Close
import com.temankuliah.app.ui.icons.MenuBook
import androidx.compose.foundation.BorderStroke
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.SurfaceVariantLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import java.util.Locale

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.temankuliah.app.data.local.practiceCredits
import com.temankuliah.app.data.local.theoryCredits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        code: String,
        name: String,
        credits: Int,
        grade: String,
        semester: Int,
        lecturer: String,
        classTime: String,
        autoCreateSchedule: Boolean,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String
    ) -> Unit,
    initialCourse: CourseEntity? = null,
    defaultSemester: Int = 1
) {
    val existingTimes = remember(initialCourse?.id) {
        initialCourse?.let { c ->
            val match = Regex("([01]?\\d|2[0-3])[.:]([0-5]\\d)\\s*(?:-|–|—|s/d|s\\.d\\.?|sd|sampai|to)\\s*([01]?\\d|2[0-3])[.:]([0-5]\\d)").find(c.classTime)
            if (match != null) {
                val s = String.format(Locale.US, "%02d:%02d", match.groupValues[1].toInt(), match.groupValues[2].toInt())
                val e = String.format(Locale.US, "%02d:%02d", match.groupValues[3].toInt(), match.groupValues[4].toInt())
                s to e
            } else {
                val single = Regex("(?<![\\d:.])([01]?\\d|2[0-3])[.:]([0-5]\\d)(?![\\d:.])").find(c.classTime)
                if (single != null) {
                    String.format(Locale.US, "%02d:%02d", single.groupValues[1].toInt(), single.groupValues[2].toInt()) to ""
                } else {
                    "" to ""
                }
            }
        } ?: ("" to "")
    }

    val existingRoom = remember(initialCourse?.id) {
        initialCourse?.let { c ->
            val parenthesized = Regex("""\(([^)]+)\)""").find(c.classTime)?.groupValues?.get(1)?.trim()
            if (!parenthesized.isNullOrBlank()) parenthesized
            else {
                val roomMatch = Regex("(?i)\\b(?:Ruang|R\\.|R\\s|Gedung|Lab|Komp)\\s*([A-Za-z0-9.-]+)").find(c.classTime)
                roomMatch?.value?.trim().orEmpty()
            }
        }.orEmpty()
    }

    var code by remember(initialCourse?.id) { mutableStateOf(initialCourse?.code.orEmpty()) }
    var name by remember(initialCourse?.id) { mutableStateOf(initialCourse?.name.orEmpty()) }
    var theoryCreditsText by remember(initialCourse?.id) {
        mutableStateOf(if (initialCourse != null) initialCourse.theoryCredits.toString() else "")
    }
    var practiceCreditsText by remember(initialCourse?.id) {
        mutableStateOf(if (initialCourse != null && initialCourse.practiceCredits > 0) initialCourse.practiceCredits.toString() else "")
    }
    var semesterText by remember(initialCourse?.id, defaultSemester) {
        mutableStateOf((initialCourse?.semester ?: defaultSemester).toString())
    }
    var lecturer by remember(initialCourse?.id) { mutableStateOf(initialCourse?.lecturer.orEmpty()) }

    val dayNames = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    val initialDayIndex = dayNames.indexOfFirst { initialCourse?.classTime?.contains(it, ignoreCase = true) == true }
    var selectedDayIndex by remember(initialCourse?.id) {
        mutableIntStateOf(if (initialDayIndex >= 0) initialDayIndex else 0)
    }
    var hasSelectedDay by remember(initialCourse?.id) {
        mutableStateOf(initialDayIndex >= 0)
    }

    var startTime by remember(initialCourse?.id) { mutableStateOf(existingTimes.first) }
    var endTime by remember(initialCourse?.id) { mutableStateOf(existingTimes.second) }
    var room by remember(initialCourse?.id) { mutableStateOf(existingRoom) }
    var autoCreateSchedule by remember(initialCourse?.id) { mutableStateOf(false) }

    var startPickerOpen by remember { mutableStateOf(false) }
    var endPickerOpen by remember { mutableStateOf(false) }

    var selectedGrade by remember(initialCourse?.id) { mutableStateOf(initialCourse?.gradeLetter ?: "-") }
    var gradeMenuExpanded by remember { mutableStateOf(false) }

    val gradeOptions = Grades.OPTIONS
    val theoryCreditsInt = theoryCreditsText.toIntOrNull() ?: 0
    val practiceCreditsInt = practiceCreditsText.toIntOrNull() ?: 0
    val totalCredits = theoryCreditsInt + practiceCreditsInt
    val point = GpaCalculator.gradeLetterToPoint(selectedGrade)
    val mutu = totalCredits * point
    val timeOrderInvalid = startTime.isNotBlank() && endTime.isNotBlank() && endTime <= startTime

    TkFormSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(TkShape.md)
                    .background(TemanKuliahBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (initialCourse == null) "Tambah Mata Kuliah" else "Ubah Mata Kuliah",
                    style = TkText.screenTitle,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "Semester $semesterText · Atur SKS, dosen, dan jadwal kelas",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = TemanKuliahBlueLight, modifier = Modifier.size(40.dp)) {
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

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Kode Matkul") },
                placeholder = { Text("mis. IF2101") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth().testTag("input_course_code")
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Mata Kuliah") },
                placeholder = { Text("mis. Pemrograman Berorientasi Objek") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth().testTag("input_course_name")
            )

            // SKS Teori, SKS Praktik, dan Semester
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = theoryCreditsText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) theoryCreditsText = it },
                    label = { Text("SKS Teori") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("input_course_theory_credits")
                )

                OutlinedTextField(
                    value = practiceCreditsText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) practiceCreditsText = it },
                    label = { Text("SKS Praktik") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("input_course_practice_credits")
                )

                OutlinedTextField(
                    value = semesterText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) semesterText = it },
                    label = { Text("Semester") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("input_course_semester")
                )
            }

            // Total SKS Info
            Card(
                shape = TkShape.sm,
                colors = CardDefaults.cardColors(containerColor = TemanKuliahBlueLight.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, DividerLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Beban SKS:", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Text("$totalCredits SKS ($theoryCreditsInt Teori + $practiceCreditsInt Praktik)", fontWeight = FontWeight.Bold, color = TemanKuliahBlue)
                }
            }

            OutlinedTextField(
                value = lecturer,
                onValueChange = { lecturer = it },
                label = { Text("Dosen Pengampu") },
                placeholder = { Text("mis. Dr. Ir. Budi Santoso, M.Kom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_course_lecturer")
            )

            // Jadwal Kuliah (Hari, Jam, Ruang)
            Text("Jadwal Perkuliahan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Hari
                var dayDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = dayDropdownExpanded,
                    onExpandedChange = { dayDropdownExpanded = !dayDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (hasSelectedDay) dayNames[selectedDayIndex] else "Pilih Hari",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hari") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dayDropdownExpanded,
                        onDismissRequest = { dayDropdownExpanded = false }
                    ) {
                        dayNames.forEachIndexed { idx, day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    selectedDayIndex = idx
                                    hasSelectedDay = true
                                    dayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

            }

            // Jam dipilih dengan Time Picker, sama seperti edit jadwal kuliah.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TkPickerField(
                    label = "Jam mulai",
                    value = startTime.ifBlank { "Pilih jam" },
                    icon = Icons.Default.Schedule,
                    onClick = { startPickerOpen = true },
                    modifier = Modifier.weight(1f)
                )

                TkPickerField(
                    label = "Jam selesai",
                    value = endTime.ifBlank { "Pilih jam" },
                    icon = Icons.Default.Schedule,
                    onClick = { endPickerOpen = true },
                    modifier = Modifier.weight(1f),
                    isError = timeOrderInvalid
                )
            }
            if (timeOrderInvalid) {
                Text(
                    "Jam selesai harus setelah jam mulai.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TemanKuliahRedDark
                )
            }

            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Ruang Kelas") },
                placeholder = { Text("Kosongkan bila belum ada") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Switch otomatis buat jadwal
            Card(
                shape = TkShape.md,
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, DividerLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Otomatis buat Jadwal Kuliah",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            if (hasSelectedDay && startTime.isNotBlank() && endTime.isNotBlank())
                                "Membuat 16 jadwal kuliah mingguan di kalender mengikuti KRS"
                            else
                                "Isi hari, jam mulai, dan jam selesai terlebih dahulu",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = autoCreateSchedule && hasSelectedDay && startTime.isNotBlank() && endTime.isNotBlank(),
                        enabled = hasSelectedDay && startTime.isNotBlank() && endTime.isNotBlank(),
                        onCheckedChange = { autoCreateSchedule = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TemanKuliahBlue)
                    )
                }
            }

            // Grade Selector
            ExposedDropdownMenuBox(
                expanded = gradeMenuExpanded,
                onExpandedChange = { gradeMenuExpanded = !gradeMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "Nilai: $selectedGrade",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Nilai Huruf (KHS)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = gradeMenuExpanded,
                    onDismissRequest = { gradeMenuExpanded = false }
                ) {
                    gradeOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                selectedGrade = opt
                                gradeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Bobot & Mutu Card (SKS x Bobot)
            Card(
                shape = TkShape.md,
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, DividerLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Bobot Nilai", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(String.format(Locale.US, "%.1f", point), fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Mutu (SKS × Bobot)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(String.format(Locale.US, "%.1f", mutu), fontWeight = FontWeight.Bold, color = TemanKuliahBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Batal")
            }
            Button(
                onClick = {
                    val semester = semesterText.toIntOrNull()
                    if (code.isNotBlank() && name.isNotBlank() && !timeOrderInvalid &&
                        totalCredits in 1..MAX_SKS_PER_COURSE &&
                        semester != null && semester >= 1
                    ) {
                        val tag = if (theoryCreditsInt + practiceCreditsInt > 0) "[T:$theoryCreditsInt, P:$practiceCreditsInt]" else ""
                        val timePart = when {
                            startTime.isNotBlank() && endTime.isNotBlank() -> "${startTime.trim()}-${endTime.trim()}"
                            startTime.isNotBlank() -> startTime.trim()
                            endTime.isNotBlank() -> endTime.trim()
                            else -> ""
                        }
                        val dayPart = if (hasSelectedDay) dayNames[selectedDayIndex] else ""
                        val roomPart = if (room.isNotBlank()) "(${room.trim()})" else ""

                        val scheduleText = listOf(dayPart, timePart, roomPart).filter { it.isNotBlank() }.joinToString(" ")
                        val formattedClassTime = listOf(tag, scheduleText).filter { it.isNotBlank() }.joinToString(" ").trim()

                        // Jadwal otomatis hanya dibuat bila hari dan kedua jam benar-benar diisi pengguna.
                        val canAutoSchedule = autoCreateSchedule && hasSelectedDay &&
                            startTime.isNotBlank() && endTime.isNotBlank()

                        onConfirm(
                            code.trim().uppercase(Locale.ROOT),
                            name.trim(),
                            totalCredits,
                            selectedGrade,
                            semester,
                            lecturer.trim(),
                            formattedClassTime,
                            canAutoSchedule,
                            if (hasSelectedDay) selectedDayIndex + 1 else 0,
                            startTime.trim(),
                            endTime.trim(),
                            room.trim()
                        )
                    }
                },
                enabled = code.isNotBlank() && name.isNotBlank() && !timeOrderInvalid &&
                    totalCredits in 1..MAX_SKS_PER_COURSE &&
                    (semesterText.toIntOrNull()?.let { it >= 1 } == true),
                colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue),
                modifier = Modifier.weight(1f).testTag("submit_course_button")
            ) {
                Text(if (initialCourse == null) "Simpan Matkul" else "Simpan Perubahan")
            }
        }
    }

    if (startPickerOpen) {
        MaterialTimePickerDialog(
            title = "Pilih Jam Mulai",
            initialTime = startTime.ifBlank { "08:00" },
            onDismiss = { startPickerOpen = false },
            onConfirm = { picked ->
                // Jam selesai ikut bergeser agar durasi kelas tetap (seperti edit jadwal kuliah).
                runCatching {
                    val duration = java.time.Duration.between(java.time.LocalTime.parse(startTime), java.time.LocalTime.parse(endTime))
                    if (!duration.isNegative && !duration.isZero) {
                        val newStart = java.time.LocalTime.parse(picked)
                        val shifted = newStart.plus(duration)
                        endTime = if (shifted.isAfter(newStart)) shifted.toString().take(5) else "23:59"
                    }
                }
                startTime = picked
                startPickerOpen = false
            }
        )
    }
    if (endPickerOpen) {
        MaterialTimePickerDialog(
            title = "Pilih Jam Selesai",
            initialTime = endTime.ifBlank { startTime.ifBlank { "08:00" } },
            onDismiss = { endPickerOpen = false },
            onConfirm = { picked ->
                endTime = picked
                endPickerOpen = false
            }
        )
    }
}


@Composable
fun EditCourseGradeDialog(
    course: CourseEntity,
    onDismiss: () -> Unit,
    onSaveGrade: (grade: String) -> Unit
) {
    val gradeOptions = Grades.OPTIONS
    var selectedGrade by remember { mutableStateOf(course.gradeLetter) }
    val point = GpaCalculator.gradeLetterToPoint(selectedGrade)
    val mutu = course.credits * point

    TkFormSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(TkShape.md)
                    .background(TemanKuliahBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = TemanKuliahBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Input Nilai KHS",
                    style = TkText.screenTitle,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${course.code} · ${course.name} (${course.credits} SKS)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = TemanKuliahBlueLight, modifier = Modifier.size(40.dp)) {
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
            Text(
                text = "Pilih nilai huruf yang diperoleh pada semester ${course.semester}:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                gradeOptions.take(5).forEach { gr ->
                    GradeOptionButton(
                        grade = gr,
                        isSelected = selectedGrade == gr,
                        onClick = { selectedGrade = gr },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                gradeOptions.drop(5).forEach { gr ->
                    GradeOptionButton(
                        grade = gr,
                        isSelected = selectedGrade == gr,
                        onClick = { selectedGrade = gr },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Card(
                shape = TkShape.md,
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, DividerLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Bobot Nilai", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(String.format(Locale.US, "%.1f", point), fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Mutu (SKS × Bobot)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(String.format(Locale.US, "%.1f", mutu), fontWeight = FontWeight.Bold, color = TemanKuliahBlue)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Batal")
            }
            Button(
                onClick = { onSaveGrade(selectedGrade) },
                colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue),
                modifier = Modifier.weight(1f)
            ) {
                Text("Simpan Nilai")
            }
        }
    }
}

@Composable
private fun GradeOptionButton(
    grade: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(TkShape.sm)
            .background(if (isSelected) TemanKuliahBlue else SurfaceVariantLight)
            .clickable(role = Role.RadioButton) { onClick() }
            .semantics { selected = isSelected },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = grade,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else TextPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
