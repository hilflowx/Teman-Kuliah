package com.temankuliah.app.domain

/**
 * Jenis agenda kalender (ujian, kegiatan, acara) yang boleh disimpan. Satu daftar untuk dialog,
 * repositori, dan validasi cadangan supaya pilihan di dialog tidak ditolak saat disimpan.
 */
val ACADEMIC_EVENT_TYPES: Set<String> = setOf("Ujian", "Kegiatan Akademik", "Kegiatan Kampus", "Seminar")
