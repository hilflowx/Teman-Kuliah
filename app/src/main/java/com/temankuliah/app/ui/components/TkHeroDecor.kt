package com.temankuliah.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Aksen dua lingkaran lembut untuk semua kartu hero biru (sama dengan kartu Pencapaian Akademik di
 * Beranda): besar di sudut kanan atas, kecil di kiri bawah. Pasang SETELAH `clip` + `background`
 * dan SEBELUM `padding`/`clickable` agar menutupi seluruh kartu.
 */
fun Modifier.tkHeroCircles(): Modifier = drawBehind {
    drawCircle(
        color = Color.White.copy(alpha = 0.10f),
        radius = 137.dp.toPx(),
        center = Offset(size.width - 3.dp.toPx(), -12.dp.toPx())
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.07f),
        radius = 72.dp.toPx(),
        center = Offset(10.dp.toPx(), size.height - 2.dp.toPx())
    )
}
