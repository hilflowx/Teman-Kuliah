package com.temankuliah.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Palet TemanKuliah "Sky Blue": diambil langsung dari desain Beranda terbaru.
 * Latar putih kebiruan, kartu putih bergaris biru muda, teks navy #16335E, aksen langit #3D9BFF.
 *
 * Beranda memakai warna desain apa adanya (lihat DashboardColors.kt). Token di berkas ini dipakai
 * halaman lain dan memakai rona yang sama, tetapi satu tingkat lebih gelap pada teks dan tombol
 * supaya tetap memenuhi kontras WCAG AA >= 4.5:1 (lihat ThemeColorContrastTest).
 */

// Setiap warna di bawah membaca palet aktif (TkPalette), jadi berganti otomatis antara tema terang dan gelap.
// Nilai terang dan gelap didefinisikan di TkPalette.kt.

// Biru merek
val TemanKuliahBlue: Color get() = TkPalette.colors.blue                // Tombol dan teks biru (terang: kontras >= 4.5:1 di atas putih)
val TemanKuliahBlueDark: Color get() = TkPalette.colors.blueDark        // Teks biru di atas BlueLight
val TemanKuliahBlueLight: Color get() = TkPalette.colors.blueLight      // Ubin ikon, chip tonal, latar seleksi

val TkGradientStart: Color get() = TkPalette.colors.gradientStart
val TkGradientEnd: Color get() = TkPalette.colors.gradientEnd
val TkMint = Color(0xFF7BEBB8)

// Oranye khusus untuk status mendesak (prioritas tinggi, ujian)
val TemanKuliahOrangeDark: Color get() = TkPalette.colors.orangeDark
val TemanKuliahOrangeLight: Color get() = TkPalette.colors.orangeLight

// Hijau status (berhasil, selesai)
val TemanKuliahGreenLight: Color get() = TkPalette.colors.greenLight
val TemanKuliahGreenDark: Color get() = TkPalette.colors.greenDark

// Isian pekat berteks putih (tombol hapus, lencana IPS). Di tema gelap token "Dark" di atas berupa warna terang
// untuk teks, jadi isian berteks putih memakai token terpisah ini agar tetap terbaca.
val TemanKuliahRedFill: Color get() = tkAdaptive(Color(0xFFB91C1C), Color(0xFFC93A3A))
val TemanKuliahGreenFill: Color get() = tkAdaptive(Color(0xFF065F46), Color(0xFF12805C))

// Biru malam untuk kategori tugas
val TemanKuliahPurple: Color get() = TkPalette.colors.purple
val TemanKuliahPurpleLight: Color get() = TkPalette.colors.purpleLight

// Merah status (terlambat, galat, hapus)
val TemanKuliahRedDark: Color get() = TkPalette.colors.redDark
val TemanKuliahRedLight: Color get() = TkPalette.colors.redLight

// Permukaan dan kanvas (nama berakhiran "Light" dipertahankan agar pemakai lama tidak perlu diubah)
val BackgroundLight: Color get() = TkPalette.colors.background
val SurfaceLight: Color get() = TkPalette.colors.surface
val SurfaceVariantLight: Color get() = TkPalette.colors.surfaceVariant

// Tipografi
val TextPrimary: Color get() = TkPalette.colors.textPrimary
val TextSecondary: Color get() = TkPalette.colors.textSecondary
val TextMuted: Color get() = TkPalette.colors.textSecondary

// Garis tipis kartu dan pemisah
val DividerLight: Color get() = TkPalette.colors.divider
val BorderLight: Color get() = TkPalette.colors.border      // Garis kolom isian dan chip (kontras >= 3:1)

// Lencana tinta gelap
val TkInk: Color get() = TkPalette.colors.ink
