package com.temankuliah.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/*
 * Skala sudut mengikuti desain Beranda:
 * sm = 8dp (lencana kecil dan penanda),
 * md = 12dp (tombol, ubin ikon, chip),
 * lg = 18dp (komponen sedang),
 * card = 22dp (kartu putih berbingkai biru muda),
 * xl = 28dp (kartu utama, dialog, lembar bawah).
 */
object TkShape {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(18.dp)
    val card = RoundedCornerShape(22.dp)
    val xl = RoundedCornerShape(28.dp)
}

/** Ukuran kontrol header yang dipakai bersama (tombol kembali/tutup, tombol aksi). */
object TkSize {
    val headerButton = 44.dp
}

/** Skala jarak lapang. Margin halaman 20dp, jarak antar seksi 24-28dp. */
object TkSpace {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val sectionGap = 24.dp
    val page = 20.dp
    val gap = 16.dp
}
