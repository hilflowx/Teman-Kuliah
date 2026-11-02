package com.temankuliah.app.ui.academic.documents

import com.temankuliah.app.domain.Grades
import com.temankuliah.app.domain.MAX_SKS_PER_COURSE

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import com.temankuliah.app.ui.icons.Schedule
import com.temankuliah.app.ui.components.MaterialTimePickerDialog
import com.temankuliah.app.ui.components.TkPickerField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.files.AcademicDocumentProcessor
import com.temankuliah.app.domain.GpaCalculator
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.OutlinedTextField
import com.temankuliah.app.ui.components.TkFormSheet
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import java.util.Locale

private val DayChoices = listOf(
    "Sen" to "Senin", "Sel" to "Selasa", "Rab" to "Rabu", "Kam" to "Kamis",
    "Jum" to "Jumat", "Sab" to "Sabtu", "Min" to "Minggu"
)

/** Deretan pilihan (chip) yang membungkus ke baris baru, empat per baris, tanpa geser horizontal. */
@Composable
private fun ChipGrid(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(4).forEach { chunk ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chunk.forEach { (label, value) ->
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelect(if (selected == value) "" else value) },
                        label = { Text(label, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun digitsOnly(text: String, maxLength: Int): String? =
    if (text.length <= maxLength && text.all { it.isDigit() }) text else null

@Composable
internal fun RowEditorDialog(
    isKrs: Boolean,
    initialRow: AcademicDocumentProcessor.AcademicRow,
    onDismiss: () -> Unit,
    onSave: (AcademicDocumentProcessor.AcademicRow) -> Unit
) {
    var code by remember { mutableStateOf(initialRow.code) }
    var name by remember { mutableStateOf(initialRow.courseName) }
    var lecturer by remember { mutableStateOf(initialRow.lecturer) }
    var creditsText by remember { mutableStateOf(if (initialRow.credits > 0) initialRow.credits.toString() else "") }
    var grade by remember { mutableStateOf(initialRow.grade.ifBlank { "-" }) }

    // KRS: SKS tanpa pembagian dianggap teori, sama seperti data mata kuliah tersimpan.
    val parts = remember(initialRow) {
        if (initialRow.day.isBlank() && initialRow.start.isBlank()) AcademicDocumentProcessor.parseScheduleParts(initialRow.schedule) else null
    }
    val hasSplit = initialRow.theory + initialRow.practice > 0
    var theoryText by remember {
        mutableStateOf(
            when {
                hasSplit -> initialRow.theory.toString()
                initialRow.credits > 0 -> initialRow.credits.toString()
                else -> ""
            }
        )
    }
    var practiceText by remember { mutableStateOf(if (hasSplit) initialRow.practice.toString() else "") }
    var day by remember { mutableStateOf(parts?.day ?: initialRow.day) }
    var startText by remember { mutableStateOf(parts?.start ?: initialRow.start) }
    var endText by remember { mutableStateOf(parts?.end ?: initialRow.end) }
    var startPickerOpen by remember { mutableStateOf(false) }
    var endPickerOpen by remember { mutableStateOf(false) }

    val gradeList = Grades.OPTIONS
    val creditsInt = creditsText.toIntOrNull() ?: 0
    val point = AcademicDocumentProcessor.gradeLetterToPoint(grade)
    val mutu = GpaCalculator.qualityPoints(creditsInt, grade)

    val theoryInt = theoryText.toIntOrNull() ?: 0
    val practiceInt = practiceText.toIntOrNull() ?: 0
    // null = jam tidak valid; "" = dikosongkan.
    val startNorm: String? = if (startText.isBlank()) "" else AcademicDocumentProcessor.normalizeTime(startText)
    val endNorm: String? = if (endText.isBlank()) "" else AcademicDocumentProcessor.normalizeTime(endText)
    val startInvalid = startNorm == null
    val endInvalid = endNorm == null
    val lamaMinutes = if (startNorm != null && endNorm != null) AcademicDocumentProcessor.minutesBetween(startNorm, endNorm) else 0
    val orderInvalid = startNorm != null && endNorm != null &&
        startNorm.isNotBlank() && endNorm.isNotBlank() && lamaMinutes == 0

    // KRS hanya butuh nama; pada KHS SKS boleh dikosongkan di tabel pratinjau, tetapi wajib 1–30 bila diisi.
    val canSave = name.isNotBlank() && if (isKrs) {
        !startInvalid && !endInvalid && !orderInvalid
    } else {
        creditsText.isBlank() || creditsInt in 1..MAX_SKS_PER_COURSE
    }

    TkFormSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (isKrs) "Ubah Baris KRS" else "Ubah Baris KHS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Kode MK") },
                placeholder = { Text("Kode mata kuliah") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Mata Kuliah") },
                placeholder = { Text("Nama mata kuliah") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (isKrs) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = theoryText,
                        onValueChange = { digitsOnly(it, 1)?.let { v -> theoryText = v } },
                        label = { Text("Teori") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = practiceText,
                        onValueChange = { digitsOnly(it, 1)?.let { v -> practiceText = v } },
                        label = { Text("Praktik / Praktek") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "SKS = Teori + Praktik = ${theoryInt + practiceInt}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TemanKuliahBlueDark
                )

                OutlinedTextField(
                    value = lecturer,
                    onValueChange = { lecturer = it },
                    label = { Text("Dosen") },
                    placeholder = { Text("Kosongkan bila tidak ada") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Hari", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    ChipGrid(DayChoices, selected = day, onSelect = { day = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TkPickerField(
                        label = "Mulai",
                        value = startText.ifBlank { "Pilih jam" },
                        icon = Icons.Default.Schedule,
                        onClick = { startPickerOpen = true },
                        modifier = Modifier.weight(1f),
                        isError = startInvalid
                    )
                    TkPickerField(
                        label = "Selesai",
                        value = endText.ifBlank { "Pilih jam" },
                        icon = Icons.Default.Schedule,
                        onClick = { endPickerOpen = true },
                        modifier = Modifier.weight(1f),
                        isError = endInvalid || orderInvalid
                    )
                }
                Text(
                    when {
                        startInvalid || endInvalid -> "Gunakan format jam seperti 08:00 atau 08.00."
                        orderInvalid -> "Jam selesai harus setelah jam mulai."
                        lamaMinutes > 0 -> "Lama: $lamaMinutes menit"
                        else -> "Lama dihitung otomatis dari jam mulai dan selesai."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (startInvalid || endInvalid || orderInvalid) TemanKuliahRedDark else TextSecondary
                )
            } else {
                OutlinedTextField(
                    value = creditsText,
                    onValueChange = { digitsOnly(it, 2)?.let { v -> creditsText = v } },
                    label = { Text("SKS") },
                    placeholder = { Text("Kosongkan bila tidak terbaca") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nilai Huruf (pilih '-' bila kosong)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    ChipGrid(gradeList.map { it to it }, selected = grade, onSelect = { grade = it.ifBlank { "-" } })
                }

                // Bobot dan mutu dihitung otomatis dari nilai dan SKS
                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                    shape = TkShape.sm,
                    border = BorderStroke(1.dp, DividerLight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bobot: ${if (grade == "-") "" else String.format(Locale.US, "%.2f", point)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text("Mutu (SKS × Bobot): ${if (grade == "-" || creditsInt == 0) "" else String.format(Locale.US, "%.2f", mutu)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TemanKuliahBlue, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Batal") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (canSave) {
                            onSave(
                                if (isKrs) {
                                    val start = startNorm.orEmpty()
                                    val end = endNorm.orEmpty()
                                    initialRow.copy(
                                        code = code.trim().uppercase(Locale.ROOT),
                                        courseName = name.trim(),
                                        lecturer = lecturer.trim(),
                                        theory = theoryInt,
                                        practice = practiceInt,
                                        credits = theoryInt + practiceInt,
                                        day = day,
                                        start = start,
                                        end = end,
                                        lama = AcademicDocumentProcessor.minutesBetween(start, end),
                                        schedule = AcademicDocumentProcessor.buildSchedule(day, start, end)
                                    )
                                } else {
                                    initialRow.copy(
                                        code = code.trim().uppercase(Locale.ROOT),
                                        courseName = name.trim(),
                                        credits = creditsInt,
                                        grade = grade.trim().uppercase(Locale.ROOT),
                                        gradePoint = point,
                                        qualityPoints = mutu
                                    )
                                }
                            )
                        }
                    },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue)
                ) {
                    Text("Terapkan")
                }
            }
        }
    }

    if (startPickerOpen) {
        MaterialTimePickerDialog(
            title = "Pilih jam mulai",
            initialTime = startText.ifBlank { "08:00" },
            onDismiss = { startPickerOpen = false },
            onConfirm = { picked ->
                startText = picked
                startPickerOpen = false
            }
        )
    }
    if (endPickerOpen) {
        MaterialTimePickerDialog(
            title = "Pilih jam selesai",
            initialTime = endText.ifBlank { if (startText.isNotBlank()) startText else "10:30" },
            onDismiss = { endPickerOpen = false },
            onConfirm = { picked ->
                endText = picked
                endPickerOpen = false
            }
        )
    }
}
