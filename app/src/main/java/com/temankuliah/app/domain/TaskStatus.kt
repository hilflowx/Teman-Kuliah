package com.temankuliah.app.domain

/** Nilai status tugas yang disimpan di kolom `TaskEntity.status`. Gunakan konstanta ini, bukan string mentah. */
object TaskStatus {
    const val BELUM = "Belum Dikerjakan"
    const val SEDANG = "Sedang Dikerjakan"
    const val SELESAI = "Selesai"
}
