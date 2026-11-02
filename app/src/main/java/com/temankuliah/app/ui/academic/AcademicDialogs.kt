package com.temankuliah.app.ui.academic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.OutlinedTextField
import com.temankuliah.app.ui.theme.TemanKuliahBlue

@Composable
fun StudyTargetDialog(
    initialTarget: Int?,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var targetText by remember(initialTarget) { mutableStateOf(initialTarget?.toString().orEmpty()) }
    val target = targetText.toIntOrNull()?.takeIf { it in 1..360 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Target SKS Kelulusan", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Isi jumlah SKS lulus yang menjadi target progres studi sesuai kurikulum program studi Anda.")
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { if (it.length <= 3 && it.all { ch -> ch.isDigit() }) targetText = it },
                    label = { Text("Target SKS") },
                    supportingText = { Text("Rentang: 1–360 SKS") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { target?.let(onSave) },
                enabled = target != null,
                colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue)
            ) { Text("Simpan Target") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
