package com.temankuliah.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Pilihan tema yang disimpan di preferensi. [key] adalah nilai yang ditulis ke SharedPreferences. */
enum class ThemeMode(val key: String, val label: String) {
    LIGHT("light", "Terang"),
    DARK("dark", "Gelap"),
    SYSTEM("system", "Ikuti sistem");

    companion object {
        /** Nilai tersimpan yang tidak dikenal (atau belum ada) jatuh ke Terang, perilaku lama aplikasi. */
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: LIGHT
    }
}

/**
 * Satu set warna lengkap untuk satu tema. Properti di Color.kt dan DashboardColors.kt membaca set aktif
 * dari [TkPalette], sehingga ratusan titik pemakaian lama ikut berganti tema tanpa diubah satu per satu.
 */
internal class TkColors(
    // Merek
    val blue: Color,
    val blueDark: Color,
    val blueLight: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    // Status
    val orangeDark: Color,
    val orangeLight: Color,
    val greenDark: Color,
    val greenLight: Color,
    val redDark: Color,
    val redLight: Color,
    val purple: Color,
    val purpleLight: Color,
    // Permukaan
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    // Teks dan garis
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val border: Color,
    val ink: Color,
    // Beranda
    val dashMuted: Color,
    val dashBlue: Color,
    val dashSky: Color,
    val dashSkySoft: Color,
    val dashTint: Color,
    val dashTrack: Color,
    val dashTile: Color,
    val dashLine: Color,
    val dashLineSoft: Color,
    val dashOrange: Color,
    val dashOrangeSoft: Color,
    val dashOkSoft: Color,
    val dashOkInk: Color,
    val dashErrSoft: Color,
    val dashErrInk: Color,
    val dashHeroStart: Color,
    val dashHeroEnd: Color,
    val dashAvatarStart: Color,
    val dashAvatarEnd: Color,
    // Bilah navigasi
    val navIconActive: Color,
    val navLabelActive: Color,
    val navInactive: Color,
    val navIconSecondaryActive: Color,
    val navIconSecondaryInactive: Color
)

internal val LightTkColors = TkColors(
    blue = Color(0xFF1F6FD9),
    blueDark = Color(0xFF1858B8),
    blueLight = Color(0xFFEAF3FF),
    gradientStart = Color(0xFF4FA6FD),
    gradientEnd = Color(0xFF2576E1),
    orangeDark = Color(0xFF9A3D00),
    orangeLight = Color(0xFFFFF0E3),
    greenDark = Color(0xFF065F46),
    greenLight = Color(0xFFD1FAE5),
    redDark = Color(0xFFB91C1C),
    redLight = Color(0xFFFEE2E2),
    purple = Color(0xFF334E75),
    purpleLight = Color(0xFFEAF3FF),
    background = Color(0xFFFBFDFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAF3FF),
    textPrimary = Color(0xFF16335E),
    textSecondary = Color(0xFF5B7194),
    divider = Color(0xFFDCE9FA),
    border = Color(0xFF7A94B8),
    ink = Color(0xFF16335E),
    dashMuted = Color(0xFF7F93B2),
    dashBlue = Color(0xFF2A7EE7),
    dashSky = Color(0xFF3D9BFF),
    dashSkySoft = Color(0xFF8CC4FF),
    dashTint = Color(0xFFEAF3FF),
    dashTrack = Color(0xFFE8F2FF),
    dashTile = Color(0xFFF1F7FF),
    dashLine = Color(0xFFD3E4F9),
    dashLineSoft = Color(0xFFDFEDFF),
    dashOrange = Color(0xFFD9702A),
    dashOrangeSoft = Color(0xFFFFF0E3),
    dashOkSoft = Color(0xFFE6F8EF),
    dashOkInk = Color(0xFF117A52),
    dashErrSoft = Color(0xFFFEEEEE),
    dashErrInk = Color(0xFFC62F2F),
    dashHeroStart = Color(0xFF4FA6FD),
    dashHeroEnd = Color(0xFF2576E1),
    dashAvatarStart = Color(0xFF76B5FA),
    dashAvatarEnd = Color(0xFF3D8BEC),
    navIconActive = Color(0xFF3D9BFF),
    navLabelActive = Color(0xFF2B7FE8),
    navInactive = Color(0xFF7F93B2),
    navIconSecondaryActive = Color(0xFF9ECDFF),
    navIconSecondaryInactive = Color(0xFFBCC8DC)
)

/** Nilai gelap diambil dari desain beranda-dark.png (latar #0A101C, kartu #111A2C, garis #1F2D47). */
internal val DarkTkColors = TkColors(
    blue = Color(0xFF3B8BF5),
    blueDark = Color(0xFF8CC4FF),
    blueLight = Color(0xFF1A2A48),
    gradientStart = Color(0xFF2A66D6),
    gradientEnd = Color(0xFF0D1A3E),
    orangeDark = Color(0xFFFFA566),
    orangeLight = Color(0xFF3A2412),
    greenDark = Color(0xFF5FE0A8),
    greenLight = Color(0xFF12352A),
    redDark = Color(0xFFFF8A8A),
    redLight = Color(0xFF3D1A20),
    purple = Color(0xFF9DB4D8),
    purpleLight = Color(0xFF1A2A48),
    background = Color(0xFF0A101C),
    surface = Color(0xFF111A2C),
    surfaceVariant = Color(0xFF1A2A48),
    textPrimary = Color(0xFFF2F6FF),
    textSecondary = Color(0xFF8C9CBA),
    divider = Color(0xFF1F2D47),
    border = Color(0xFF5A7098),
    ink = Color(0xFF223A66),
    dashMuted = Color(0xFF8C9CBA),
    dashBlue = Color(0xFF5AAAFF),
    dashSky = Color(0xFF3B8BF5),
    dashSkySoft = Color(0xFF35629F),
    dashTint = Color(0xFF1A2A48),
    dashTrack = Color(0xFF111A2C),
    dashTile = Color(0xFF1A2A48),
    dashLine = Color(0xFF1F2D47),
    dashLineSoft = Color(0xFF203251),
    dashOrange = Color(0xFFF08A3C),
    dashOrangeSoft = Color(0xFF3A2412),
    dashOkSoft = Color(0xFF12352A),
    dashOkInk = Color(0xFF5FE0A8),
    dashErrSoft = Color(0xFF3D1A20),
    dashErrInk = Color(0xFFFF8A8A),
    dashHeroStart = Color(0xFF2A66D6),
    dashHeroEnd = Color(0xFF0D1A3E),
    dashAvatarStart = Color(0xFF4190F3),
    dashAvatarEnd = Color(0xFF2E73E0),
    navIconActive = Color(0xFF5AAAFF),
    navLabelActive = Color(0xFF5AAAFF),
    navInactive = Color(0xFF8C9CBA),
    navIconSecondaryActive = Color(0xFF2F5FA8),
    navIconSecondaryInactive = Color(0xFF3A4A66)
)

/**
 * Palet aktif. [isDark] adalah state Compose: setiap komposisi yang membaca warna dari properti di
 * Color.kt atau DashboardColors.kt otomatis berlangganan, jadi mengubah nilainya menyusun ulang layar.
 * Diisi oleh MainActivity sebelum setContent (supaya bingkai pertama sudah benar) dan oleh
 * [TemanKuliahTheme] saat pengguna mengganti tema atau mode sistem berubah.
 */
internal object TkPalette {
    var isDark: Boolean by mutableStateOf(false)

    val colors: TkColors get() = if (isDark) DarkTkColors else LightTkColors
}

/** Memilih nilai terang atau gelap untuk warna yang tadinya tertulis tetap di satu layar. */
internal fun tkAdaptive(light: Color, dark: Color): Color = if (TkPalette.isDark) dark else light

/** Bayangan hanya di tema terang; kartu gelap rata dan berbingkai (PRD 7.3 butir 4). */
internal fun tkShadow(light: Dp): Dp = if (TkPalette.isDark) 0.dp else light
