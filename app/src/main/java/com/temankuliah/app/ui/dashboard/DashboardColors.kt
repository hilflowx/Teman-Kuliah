package com.temankuliah.app.ui.dashboard

import androidx.compose.ui.graphics.Color
import com.temankuliah.app.ui.theme.TkPalette

/*
 * Warna Beranda diambil piksel demi piksel dari desain (161956.png).
 * Teks muted #7F93B2 sengaja dipakai apa adanya agar sama dengan desain; halaman lain memakai
 * versi yang lebih gelap (TextSecondary) demi kontras AA.
 */
internal val DashInk: Color get() = TkPalette.colors.textPrimary          // Judul, nama, jam
internal val DashMuted: Color get() = TkPalette.colors.dashMuted        // Teks sekunder, tab nonaktif, label nav nonaktif
internal val DashBlue: Color get() = TkPalette.colors.dashBlue         // Tautan, label pil biru, ikon
internal val DashSky: Color get() = TkPalette.colors.dashSky          // Tab aktif, batang jadwal berlangsung, ikon nav aktif
internal val DashSkySoft: Color get() = TkPalette.colors.dashSkySoft      // Batang jadwal berikutnya
internal val DashTint: Color get() = TkPalette.colors.dashTint         // Latar pil status biru
internal val DashTrack: Color get() = TkPalette.colors.dashTrack        // Latar kontainer tab
internal val DashTile: Color get() = TkPalette.colors.dashTile         // Ubin ikon dan tombol notifikasi
internal val DashLine: Color get() = TkPalette.colors.dashLine         // Garis tepi kartu dan bilah navigasi
internal val DashLineSoft: Color get() = TkPalette.colors.dashLineSoft     // Garis tepi kontainer tab
internal val DashOrange: Color get() = TkPalette.colors.dashOrange       // Pil mendesak (<= 1 hari)
internal val DashOrangeSoft: Color get() = TkPalette.colors.dashOrangeSoft
internal val DashMint = Color(0xFF7BEBB8)         // Titik status dan penanda batas cumlaude
internal val DashOk = Color(0xFF34C98B)           // Batang jadwal selesai
internal val DashOkSoft: Color get() = TkPalette.colors.dashOkSoft
internal val DashOkInk: Color get() = TkPalette.colors.dashOkInk
internal val DashErrSoft: Color get() = TkPalette.colors.dashErrSoft
internal val DashErrInk: Color get() = TkPalette.colors.dashErrInk
internal val DashAmber = Color(0xFFF5A524)

internal val DashHeroStart: Color get() = TkPalette.colors.dashHeroStart    // Gradien kartu utama, kiri-atas
internal val DashHeroEnd: Color get() = TkPalette.colors.dashHeroEnd      // Gradien kartu utama, kanan-bawah
internal val DashAvatarStart: Color get() = TkPalette.colors.dashAvatarStart
internal val DashAvatarEnd: Color get() = TkPalette.colors.dashAvatarEnd

// Permukaan kartu Beranda dan teks biru di atas tombol putih (kartu hero tetap biru di kedua tema).
internal val DashSurface: Color get() = TkPalette.colors.surface
internal val DashBlueOnWhite = Color(0xFF2A7EE7)

/** Cincin tipis di sekitar titik notifikasi: putih di tema terang, warna latar halaman di tema gelap. */
internal val DashBadgeRing: Color get() = if (TkPalette.isDark) TkPalette.colors.background else Color.White
