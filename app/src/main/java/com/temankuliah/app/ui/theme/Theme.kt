package com.temankuliah.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.ContextWrapper

private val AppShapes = Shapes(
    extraSmall = TkShape.sm,
    small = TkShape.md,
    medium = TkShape.lg,
    large = TkShape.lg,
    extraLarge = TkShape.xl
)

private fun lightSchemeFrom(c: TkColors): ColorScheme = lightColorScheme(
    primary = c.blue,
    onPrimary = Color.White,
    primaryContainer = c.blueLight,
    onPrimaryContainer = c.blueDark,
    // Chip terpilih (FilterChip) memakai secondaryContainer: biru penuh berteks putih seperti referensi.
    secondary = c.orangeDark,
    onSecondary = Color.White,
    secondaryContainer = c.blue,
    onSecondaryContainer = Color.White,
    tertiary = c.greenDark,
    onTertiary = Color.White,
    tertiaryContainer = c.greenLight,
    onTertiaryContainer = c.greenDark,
    error = c.redDark,
    onError = Color.White,
    errorContainer = c.redLight,
    onErrorContainer = c.redDark,
    background = c.background,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.surfaceVariant,
    onSurfaceVariant = c.textSecondary,
    surfaceTint = c.blue,
    outline = c.border,
    outlineVariant = c.divider,
    // Dialog, lembar bawah, menu, dan pemilih tanggal memakai permukaan bersih.
    surfaceContainerLowest = c.surface,
    surfaceContainerLow = c.surface,
    surfaceContainer = c.surface,
    surfaceContainerHigh = c.surface,
    surfaceContainerHighest = c.surfaceVariant
)

private fun darkSchemeFrom(c: TkColors): ColorScheme = darkColorScheme(
    primary = c.blue,
    onPrimary = Color.White,
    primaryContainer = c.blueLight,
    onPrimaryContainer = c.blueDark,
    // Chip terpilih (FilterChip) memakai secondaryContainer: biru penuh berteks putih seperti referensi.
    secondary = c.orangeDark,
    onSecondary = Color(0xFF2A1400),
    secondaryContainer = c.blue,
    onSecondaryContainer = Color.White,
    tertiary = c.greenDark,
    onTertiary = Color(0xFF00281A),
    tertiaryContainer = c.greenLight,
    onTertiaryContainer = c.greenDark,
    error = c.redDark,
    onError = Color(0xFF3A0000),
    errorContainer = c.redLight,
    onErrorContainer = c.redDark,
    background = c.background,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.surfaceVariant,
    onSurfaceVariant = c.textSecondary,
    surfaceTint = c.blue,
    outline = c.border,
    outlineVariant = c.divider,
    // Dialog, lembar bawah, menu, dan pemilih tanggal memakai permukaan bersih.
    surfaceContainerLowest = c.surface,
    surfaceContainerLow = c.surface,
    surfaceContainer = c.surface,
    surfaceContainerHigh = c.surface,
    surfaceContainerHighest = c.surfaceVariant
)

private val LightColorScheme = lightSchemeFrom(LightTkColors)
private val DarkColorScheme = darkSchemeFrom(DarkTkColors)

/** True bila tema gelap harus dipakai untuk [mode] pada kondisi sistem saat ini. */
@Composable
internal fun ThemeMode.resolveDark(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun TemanKuliahTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val dark = themeMode.resolveDark()
    // MainActivity sudah mengisi nilai awal sebelum setContent; baris ini menyamakan saat tema diganti atau
    // mode sistem berubah. Dilakukan sebagai efek samping, bukan saat komposisi, supaya tidak menulis state
    // di tengah penyusunan.
    SideEffect { if (TkPalette.isDark != dark) TkPalette.isDark = dark }
    // Ikon status bar dan bilah navigasi sistem mengikuti terang/gelap.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var ctx = view.context
            while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
            val window = (ctx as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    val density = LocalDensity.current
    // Mendukung font scale sistem (termasuk Large 1.3x) agar tata letak tidak rusak
    val safeDensity = remember(density) {
    Density(
        density = density.density,
        fontScale = density.fontScale.coerceAtMost(1.3f)
    )
    }
    CompositionLocalProvider(LocalDensity provides safeDensity) {
    MaterialTheme(
        colorScheme = if (dark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
    }
}
