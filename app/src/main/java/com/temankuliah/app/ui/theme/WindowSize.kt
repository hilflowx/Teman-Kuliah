@file:Suppress("DEPRECATION")

package com.temankuliah.app.ui.theme

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

/** Kelas lebar jendela; satu-satunya sumber breakpoint tata letak (Compact < 600dp, Medium < 840dp, Expanded >= 840dp). */
enum class TkWidthClass { Compact, Medium, Expanded }

/** Lebar minimum (dp) agar kelas Medium sudah cukup lega untuk tata letak dua panel. */
private const val MEDIUM_TWO_PANE_MIN_DP = 700

@Composable
fun rememberTkWidthClass(): TkWidthClass {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    return when (sizeClass) {
        WindowWidthSizeClass.EXPANDED -> TkWidthClass.Expanded
        WindowWidthSizeClass.MEDIUM -> TkWidthClass.Medium
        else -> TkWidthClass.Compact
    }
}

/**
 * True bila layar cukup lebar untuk dua panel: Expanded, atau Medium dengan lebar >= 700dp.
 * Navigasi samping (rail) hanya dipakai saat Expanded, sehingga lebar layar ~ lebar konten pada Medium.
 */
@Suppress("ConfigurationScreenWidthHeight")
@Composable
fun rememberTkTwoPane(): Boolean {
    val widthClass = rememberTkWidthClass()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return widthClass == TkWidthClass.Expanded ||
        (widthClass == TkWidthClass.Medium && screenWidthDp >= MEDIUM_TWO_PANE_MIN_DP)
}

/** Lebar maksimum konten halaman per kelas lebar; konten dipusatkan di dalamnya. */
fun TkWidthClass.maxContentWidth(): Dp = when (this) {
    TkWidthClass.Expanded -> 1160.dp
    TkWidthClass.Medium -> 860.dp
    TkWidthClass.Compact -> 640.dp
}
