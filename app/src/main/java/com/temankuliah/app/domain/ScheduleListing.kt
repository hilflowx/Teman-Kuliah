package com.temankuliah.app.domain

import com.temankuliah.app.data.local.ScheduleEntity

/** Urutan kronologis: tanggal (ISO yyyy-MM-dd), lalu jam mulai, lalu id. Tidak ada jadwal yang disaring. */
fun List<ScheduleEntity>.sortedChronologically(): List<ScheduleEntity> =
    sortedWith(compareBy<ScheduleEntity>({ it.scheduledDate }, { it.startTime }, { it.id }))

/** Jadwal berulang: salah satu pertemuan dari satu seri. Jadwal Sekali tidak punya id seri. */
val ScheduleEntity.isRecurring: Boolean get() = recurrenceGroupId.isNotBlank()

/**
 * Daftar di halaman Jadwal: Jadwal Sekali yang sudah selesai disembunyikan (datanya tetap tersimpan dan
 * tetap tampil di Kalender). Pertemuan Jadwal Berulang tetap tampil walau sudah selesai.
 */
fun List<ScheduleEntity>.visibleInScheduleList(): List<ScheduleEntity> =
    filterNot { it.isCompleted && !it.isRecurring }

/** Tipe jadwal menurut data: SEKALI (tanpa id seri) atau BERULANG (punya id seri). */
val ScheduleEntity.tipe: String get() = if (isRecurring) ScheduleRules.TIPE_BERULANG else ScheduleRules.TIPE_SEKALI
