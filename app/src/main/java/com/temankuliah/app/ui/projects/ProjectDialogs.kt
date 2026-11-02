package com.temankuliah.app.ui.projects

import com.temankuliah.app.ui.components.TkFormSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.ProjectEntity
import com.temankuliah.app.data.local.ProjectMilestoneEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.DeadlinePickerField
import com.temankuliah.app.ui.components.OutlinedButton
import com.temankuliah.app.ui.components.OutlinedTextField
import com.temankuliah.app.ui.components.TkFormError
import com.temankuliah.app.ui.components.formatDateTime
import com.temankuliah.app.ui.icons.Add
import com.temankuliah.app.ui.icons.Close
import com.temankuliah.app.ui.icons.DeleteOutline
import com.temankuliah.app.ui.icons.ViewKanban
import com.temankuliah.app.ui.theme.TkText
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape

@Composable
fun ProjectEditorDialog(
    initial: ProjectEntity?,
    defaultSemester: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Int) -> Unit
) {
    val defaultDeadline = System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var deadlineAtMillis by remember(initial?.id) { mutableLongStateOf(initial?.deadlineAtMillis ?: defaultDeadline) }
    var progress by remember(initial?.id) { mutableStateOf((initial?.progress ?: 0).toString()) }
    var error by remember(initial?.id) { mutableStateOf<String?>(null) }

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
                        Icon(Icons.Default.ViewKanban, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(23.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(if (initial == null) "Tambah Proyek" else "Ubah Proyek", style = TkText.screenTitle, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Kelola detail dan tenggat proyek", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }
                Text("Semester $defaultSemester", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                val titleInvalid = error != null && title.isBlank()
                val progressInvalid = error != null && !titleInvalid && deadlineAtMillis > 0L && progress.toIntOrNull().let { it == null || it !in 0..100 }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("Nama proyek") },
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
                    Text("Tenggat proyek", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    DeadlinePickerField(deadlineAtMillis = deadlineAtMillis, onDeadlineChange = { deadlineAtMillis = it; error = null })
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Progres saat ini", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 25, 50, 75, 100).forEach { p ->
                            FilterChip(
                                selected = progress == p.toString(),
                                onClick = { progress = p.toString(); error = null },
                                label = { Text("$p%") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = progress,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            progress = value
                        }
                        error = null
                    },
                    label = { Text("Atur Progres Khusus (0–100%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = progressInvalid,
                    supportingText = if (progressInvalid) { { Text(error.orEmpty()) } } else null,
                    shape = TkShape.md,
                    modifier = Modifier.fillMaxWidth()
                )
                TkFormError(error.takeUnless { titleInvalid || progressInvalid })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = TkShape.md) { Text("Batal") }
                    Button(
                        onClick = {
                            val percent = progress.toIntOrNull()
                            when {
                                title.isBlank() -> error = "Nama proyek wajib diisi."
                                deadlineAtMillis <= 0L -> error = "Tanggal dan waktu tenggat tidak valid."
                                percent == null || percent !in 0..100 -> error = "Progres harus antara 0 dan 100."
                                else -> onSave(title.trim(), description.trim(), deadlineAtMillis, percent)
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = TkShape.md,
                        colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue)
                    ) { Text("Simpan Proyek") }
                }
            }
        }
}

@Composable
internal fun ProjectMilestonesDialog(
    project: ProjectEntity,
    milestones: List<ProjectMilestoneEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, Long) -> Unit,
    onToggle: (ProjectMilestoneEntity, Boolean) -> Unit,
    onDelete: (ProjectMilestoneEntity) -> Unit
) {
    var title by remember(project.id) { mutableStateOf("") }
    var deadlineAtMillis by remember(project.id) { mutableLongStateOf(project.deadlineAtMillis) }
    var error by remember(project.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text("Tahapan · ${project.title}", maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Tandai tahapan yang selesai. Setiap tahapan punya tenggat sendiri.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (milestones.isEmpty()) Text("Belum ada tahapan.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                milestones.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.isCompleted, onCheckedChange = { onToggle(item, it) })
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(formatDateTime(item.deadlineAtMillis), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus ${item.title}", tint = TemanKuliahRedDark) }
                    }
                }
                val titleInvalid = error != null && title.isBlank()
                OutlinedTextField(title, { title = it; error = null }, label = { Text("Nama tahapan baru") }, singleLine = true, isError = titleInvalid, supportingText = if (titleInvalid) { { Text(error.orEmpty()) } } else null, modifier = Modifier.fillMaxWidth())
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Tenggat tahapan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    DeadlinePickerField(deadlineAtMillis = deadlineAtMillis, onDeadlineChange = { deadlineAtMillis = it; error = null })
                }
                TkFormError(error.takeUnless { titleInvalid })
                Button(onClick = {
                    when {
                        title.isBlank() -> error = "Nama tahapan wajib diisi."
                        deadlineAtMillis <= 0L -> error = "Tanggal dan waktu tenggat tidak valid."
                        else -> { onAdd(title.trim(), deadlineAtMillis); title = ""; error = null }
                    }
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue)) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Tambah Tahapan") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Selesai") } }
    )
}

@Composable
internal fun LinkProjectTasksDialog(
    project: ProjectEntity,
    tasks: List<TaskEntity>,
    onDismiss: () -> Unit,
    onSave: (TaskEntity, Boolean) -> Unit
) {
    val eligibleTasks = tasks.filter { it.projectId == null || it.projectId == project.id }
    var checkedIds by remember(project.id, tasks.size) { mutableStateOf(tasks.filter { it.projectId == project.id }.map { it.id }.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tugas terkait · ${project.title}", maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Centang tugas untuk menghubungkannya. Satu tugas hanya dapat dikaitkan ke satu proyek.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (eligibleTasks.isEmpty()) Text("Belum ada tugas yang bisa dihubungkan pada semester aktif.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                eligibleTasks.forEach { task ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                            value = task.id in checkedIds,
                            role = Role.Checkbox,
                            onValueChange = { checked -> checkedIds = if (checked) checkedIds + task.id else checkedIds - task.id }
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = task.id in checkedIds, onCheckedChange = null)
                        Column(Modifier.weight(1f)) {
                            Text(task.title, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("Tenggat ${formatDateTime(task.dueAtMillis)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
                tasks.filter { it.projectId != null && it.projectId != project.id }.forEach { task ->
                    Text("${task.title} · sudah terhubung ke proyek lain", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        },
        confirmButton = { Button(onClick = {
            eligibleTasks.forEach { task -> onSave(task, task.id in checkedIds) }
            onDismiss()
        }, colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue)) { Text("Simpan Relasi") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
