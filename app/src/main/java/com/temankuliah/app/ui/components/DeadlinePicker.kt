package com.temankuliah.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.icons.CalendarToday
import com.temankuliah.app.ui.icons.Schedule
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Combines a picked local calendar date with the existing local hour/minute without month overflow. */
internal fun combineDeadlineDatePreservingTime(deadlineAtMillis: Long, pickedDate: LocalDate): Long {
    val currentTime = Calendar.getInstance().apply { timeInMillis = deadlineAtMillis }
    return pickedDate.atTime(
        LocalTime.of(currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE), 0)
    ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

@Composable
fun DeadlinePickerField(
    deadlineAtMillis: Long,
    onDeadlineChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    dateTitle: String = "Pilih tanggal tenggat",
    timeTitle: String = "Pilih jam tenggat"
) {
    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM yyyy", IndonesianLocale) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", IndonesianLocale) }
    var datePickerOpen by remember { mutableStateOf(false) }
    var timePickerOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TkPickerField("Tanggal", dateFormatter.format(Date(deadlineAtMillis)), Icons.Default.CalendarToday, { datePickerOpen = true }, Modifier.weight(1.25f))
        TkPickerField("Jam", timeFormatter.format(Date(deadlineAtMillis)), Icons.Default.Schedule, { timePickerOpen = true }, Modifier.weight(0.75f))
    }

    if (datePickerOpen) {
        val selected = Calendar.getInstance().apply { timeInMillis = deadlineAtMillis }
        TkDatePickerDialog(
            title = dateTitle,
            initialDate = LocalDate.of(selected.get(Calendar.YEAR), selected.get(Calendar.MONTH) + 1, selected.get(Calendar.DAY_OF_MONTH)),
            onDismiss = { datePickerOpen = false },
            onConfirm = { picked ->
                onDeadlineChange(combineDeadlineDatePreservingTime(deadlineAtMillis, picked))
                datePickerOpen = false
            }
        )
    }

    if (timePickerOpen) {
        val current = Calendar.getInstance().apply { timeInMillis = deadlineAtMillis }
        val currentTime = String.format(Locale.ROOT, "%02d:%02d", current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE))
        MaterialTimePickerDialog(
            title = timeTitle,
            initialTime = currentTime,
            onDismiss = { timePickerOpen = false },
            onConfirm = { value ->
                val pickedTime = LocalTime.parse(value)
                val updated = Calendar.getInstance().apply {
                    timeInMillis = deadlineAtMillis
                    set(Calendar.HOUR_OF_DAY, pickedTime.hour)
                    set(Calendar.MINUTE, pickedTime.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onDeadlineChange(updated.timeInMillis)
                timePickerOpen = false
            }
        )
    }
}
