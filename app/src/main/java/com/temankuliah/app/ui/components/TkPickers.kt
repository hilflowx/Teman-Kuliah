package com.temankuliah.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.theme.BorderLight
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.SurfaceVariantLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TextMuted
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Kolom pemilih tanggal/jam yang seragam: ikon, label kecil, nilai tebal. Mengetuknya membuka dialog
 * pemilih; tidak ada ketikan manual sehingga format selalu benar.
 */
@Composable
fun TkPickerField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Surface(
        modifier = modifier.heightIn(min = 62.dp).semantics { contentDescription = "$label, $value" },
        shape = TkShape.md,
        color = SurfaceLight,
        border = BorderStroke(if (isError) 2.dp else 1.dp, if (isError) com.temankuliah.app.ui.theme.TemanKuliahRedDark else BorderLight)
    ) {
        Row(
            Modifier.clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
                Text(value, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Warna jam dan tanggal disamakan dengan tema aplikasi agar kedua dialog tampak satu keluarga. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun tkTimePickerColors() = TimePickerDefaults.colors(
    clockDialColor = SurfaceVariantLight,
    clockDialSelectedContentColor = Color.White,
    clockDialUnselectedContentColor = TextPrimary,
    selectorColor = TemanKuliahBlue,
    timeSelectorSelectedContainerColor = TemanKuliahBlueLight,
    timeSelectorSelectedContentColor = TemanKuliahBlueDark,
    timeSelectorUnselectedContainerColor = SurfaceVariantLight,
    timeSelectorUnselectedContentColor = TextPrimary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun tkDatePickerColors() = DatePickerDefaults.colors(
    containerColor = SurfaceLight,
    titleContentColor = TextPrimary,
    headlineContentColor = TextPrimary,
    weekdayContentColor = TextSecondary,
    subheadContentColor = TextSecondary,
    navigationContentColor = TemanKuliahBlue,
    yearContentColor = TextPrimary,
    disabledYearContentColor = TextMuted,
    currentYearContentColor = TemanKuliahBlue,
    selectedYearContainerColor = TemanKuliahBlue,
    selectedYearContentColor = Color.White,
    dayContentColor = TextPrimary,
    disabledDayContentColor = TextMuted,
    selectedDayContainerColor = TemanKuliahBlue,
    selectedDayContentColor = Color.White,
    todayContentColor = TemanKuliahBlue,
    todayDateBorderColor = TemanKuliahBlue,
    dayInSelectionRangeContainerColor = TemanKuliahBlueLight,
    dayInSelectionRangeContentColor = TemanKuliahBlueDark,
    dividerColor = DividerLight
)

/**
 * Dialog pilih tanggal bertema TemanKuliah:
 * sudut seragam, kartu putih bersih, aksen biru TemanKuliah, tombol Batal dan Pilih Tanggal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TkDatePickerDialog(
    title: String,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    // DatePicker memakai batas hari UTC, bukan waktu lokal.
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val headlineFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", IndonesianLocale) }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = TkShape.card,
        colors = tkDatePickerColors(),
        confirmButton = {
            Button(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue)
            ) {
                Text("Pilih Tanggal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    ) {
        DatePicker(
            state = state,
            showModeToggle = false,
            colors = tkDatePickerColors(),
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)
                )
            },
            headline = {
                Text(
                    text = state.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(headlineFormatter).replaceFirstChar { c -> c.titlecase(IndonesianLocale) }
                    } ?: "Pilih tanggal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TemanKuliahBlue,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                )
            }
        )
    }
}
