package com.temankuliah.app.ui.projects

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.icons.Close
import com.temankuliah.app.ui.theme.TkText
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectFilterBottomSheet(
    courses: List<CourseEntity>,
    initialStatus: String,
    initialDeadline: String,
    initialCourseId: Long?,
    onDismiss: () -> Unit,
    onApply: (String, String, Long?) -> Unit,
    onReset: () -> Unit
) {
    var status by remember(initialStatus) { mutableStateOf(initialStatus) }
    var deadline by remember(initialDeadline) { mutableStateOf(initialDeadline) }
    var courseId by remember(initialCourseId) { mutableStateOf(initialCourseId) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = com.temankuliah.app.ui.theme.SurfaceLight
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Filter proyek", style = TkText.screenTitle, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Saring proyek berdasarkan progres, tenggat, dan mata kuliah terkait.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Tutup filter") }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status proyek", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Semua Status", "Belum Dimulai", "Berjalan", "Selesai").forEach { choice ->
                        FilterChip(selected = status == choice, onClick = { status = choice }, label = { Text(choice) })
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tenggat", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Semua Tenggat", "Tenggat Terlewat", "7 Hari ke Depan").forEach { choice ->
                        FilterChip(selected = deadline == choice, onClick = { deadline = choice }, label = { Text(choice) })
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mata kuliah terkait", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("Proyek dicocokkan dengan mata kuliah dari tugas yang terhubung.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = courseId == null, onClick = { courseId = null }, label = { Text("Semua") })
                    FilterChip(selected = courseId == -1L, onClick = { courseId = -1L }, label = { Text("Belum terkait") })
                    courses.forEach { course ->
                        FilterChip(selected = courseId == course.id, onClick = { courseId = course.id }, label = { Text(course.name, maxLines = 1) })
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Atur ulang") }
                Button(
                    onClick = { onApply(status, deadline, courseId) },
                    modifier = Modifier.weight(1.4f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue)
                ) { Text("Terapkan filter") }
            }
        }
    }
}
