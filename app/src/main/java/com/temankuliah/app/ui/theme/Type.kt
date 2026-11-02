package com.temankuliah.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.temankuliah.app.R

/**
 * Poppins untuk seluruh aplikasi, sama dengan desain Beranda.
 * Berkas yang dibundel: Regular, Medium, Bold. SemiBold dan ExtraBold dipetakan ke Bold.
 */
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_bold, FontWeight.ExtraBold)
)

/*
 * Skala tipografi TemanKuliah. Poppins lebih lebar daripada font sistem, jadi ukuran dan jarak huruf
 * sedikit lebih rapat. Ukuran disesuaikan 10-15% lebih ringkas agar pas di layar perangkat nyata
 * dengan skala font standar maupun font scale sistem hingga 1.3x.
 */
val Typography = Typography(
    displayLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.3).sp),
    displaySmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.2).sp),
    headlineLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    titleLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, lineHeight = 20.sp),
    titleMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    titleSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp),
    bodyLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 10.5.sp, lineHeight = 15.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, lineHeight = 15.sp, letterSpacing = 0.1.sp),
    labelSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.1.sp)
)

/**
 * Gaya teks tingkat halaman. Dipakai bersama oleh semua header supaya judul antar halaman,
 * sub-halaman, dan dialog layar penuh berukuran sama.
 * - pageTitle: judul tab utama (Jadwal, Tugas, Proyek, Akademik, Profil) 24sp.
 * - screenTitle: judul sub-halaman dan dialog layar penuh 18sp.
 * - supporting: keterangan di bawah judul 12sp.
 */
object TkText {
    val pageTitle = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp)
    val screenTitle = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp)
    val supporting = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.sp)
}
