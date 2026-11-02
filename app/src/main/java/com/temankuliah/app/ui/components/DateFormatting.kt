package com.temankuliah.app.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val IndonesianLocale = Locale.forLanguageTag("id-ID")

private val CalendarDateTimeFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", IndonesianLocale)

internal fun formatDateTime(timestamp: Long): String = CalendarDateTimeFormatter.format(Date(timestamp))

/** Jam saja, mis. "08:30". */
internal fun formatClock(millis: Long): String = SimpleDateFormat("HH:mm", IndonesianLocale).format(Date(millis))

/** Hari singkat + tanggal, mis. "Sen, 5 Okt". */
internal fun formatWeekdayDate(millis: Long): String = SimpleDateFormat("EEE, d MMM", IndonesianLocale).format(Date(millis))

