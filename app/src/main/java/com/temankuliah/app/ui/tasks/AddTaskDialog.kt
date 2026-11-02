package com.temankuliah.app.ui.tasks

import com.temankuliah.app.ui.components.TkFormSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.DeadlinePickerField
import com.temankuliah.app.ui.components.OutlinedButton
import com.temankuliah.app.ui.components.OutlinedTextField
import com.temankuliah.app.ui.components.TkFormError
import com.temankuliah.app.ui.icons.Assignment
import com.temankuliah.app.ui.icons.Close
import com.temankuliah.app.ui.theme.TkText
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, dueAtMillis: Long, priority: String, courseId: Long?) -> Unit,
    initialTask: com.temankuliah.app.data.local.TaskEntity? = null,
    /** Semua mata kuliah pengguna; dialog hanya menawarkan yang semesternya [semester] (ditambah pilihan tugas lama). */
    courses: List<com.temankuliah.app.data.local.CourseEntity> = emptyList(),
    semester: Int = initialTask?.semester ?: 1
) {
    val courseOptions = remember(courses, semester, initialTask?.courseId) {
        courses.filter { it.semester == semester || it.id == initialTask?.courseId }.sortedBy { it.name }
    }
    val defaultDeadline = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
    var title by remember(initialTask?.id) { mutableStateOf(initialTask?.title.orEmpty()) }
    var description by remember(initialTask?.id) { mutableStateOf(initialTask?.description.orEmpty()) }
    var dueAtMillis by remember(initialTask?.id) { mutableLongStateOf(initialTask?.dueAtMillis ?: defaultDeadline) }
    var priority by remember(initialTask?.id) { mutableStateOf(initialTask?.priority ?: "Sedang") }
    // courseId yatim (mata kuliah sudah tidak ada) dianggap "tanpa mata kuliah" agar tersimpan bersih.
    var courseId by remember(initialTask?.id) { mutableStateOf(initialTask?.courseId?.takeIf { id -> courseOptions.any { it.id == id } }) }
    var error by remember(initialTask?.id) { mutableStateOf<String?>(null) }

    TkFormSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(TkShape.md).background(TemanKuliahBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(23.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(if (initialTask == null) "Tambah Tugas" else "Ubah Tugas", style = TkText.screenTitle, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Atur detail dan tenggat tugasmu", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                val titleInvalid = error != null && title.isBlank()
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("Judul tugas") },
                    singleLine = true,
                    isError = titleInvalid,
                    supportingText = if (titleInvalid) { { Text(error.orEmpty()) } } else null,
                    shape = TkShape.md,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi (opsional)") },
                    minLines = 2,
                    maxLines = 4,
                    shape = TkShape.md,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Tenggat tugas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    DeadlinePickerField(deadlineAtMillis = dueAtMillis, onDeadlineChange = { dueAtMillis = it; error = null })
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Prioritas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Rendah", "Sedang", "Tinggi").forEach { option ->
                            FilterChip(
                                selected = priority == option,
                                onClick = { priority = option },
                                label = { Text(option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mata kuliah (opsional)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = courseId == null, onClick = { courseId = null }, label = { Text("Tugas mandiri") })
                        courseOptions.forEach { course ->
                            FilterChip(selected = courseId == course.id, onClick = { courseId = course.id }, label = { Text(course.name, maxLines = 1) })
                        }
                    }
                }
                TkFormError(error.takeUnless { titleInvalid })

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = TkShape.md) { Text("Batal") }
                    Button(
                        onClick = {
                            when {
                                title.isBlank() -> error = "Judul tugas wajib diisi."
                                dueAtMillis <= 0L -> error = "Tanggal dan waktu tenggat tidak valid."
                                else -> onConfirm(title.trim(), description.trim(), dueAtMillis, priority, courseId)
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = TkShape.md,
                        colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue)
                    ) { Text("Simpan Tugas") }
                }
            }
        }
}
