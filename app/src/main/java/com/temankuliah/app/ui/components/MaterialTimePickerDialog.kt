package com.temankuliah.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialTimePickerDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parsedTime = remember(initialTime) {
        runCatching { LocalTime.parse(initialTime) }.getOrDefault(LocalTime.of(8, 0))
    }
    val timeState = rememberTimePickerState(
        initialHour = parsedTime.hour,
        initialMinute = parsedTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = com.temankuliah.app.ui.theme.TkShape.xl,
        containerColor = com.temankuliah.app.ui.theme.SurfaceLight,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timeState, colors = tkTimePickerColors())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(String.format(Locale.ROOT, "%02d:%02d", timeState.hour, timeState.minute))
            }) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
