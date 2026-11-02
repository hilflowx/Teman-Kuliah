package com.temankuliah.app.domain

import java.time.LocalDate

/** Awalan kunci SharedPreferences untuk tanggal mulai perkuliahan; kuncinya "semester_start_date_<semester>". */
const val SEMESTER_START_DATE_KEY_PREFIX = "semester_start_date_"

fun semesterStartDateKey(semester: Int): String = "$SEMESTER_START_DATE_KEY_PREFIX$semester"

/**
 * Satu-satunya aturan "tanggal mulai sah": teks ISO yyyy-MM-dd yang bisa diurai. Kosong atau tidak valid
 * dianggap belum diatur (null). Semua pembaca tanggal mulai semester memakai fungsi ini.
 */
fun parseSemesterStartDate(value: String?): LocalDate? =
    value?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
