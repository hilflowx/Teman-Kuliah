package com.temankuliah.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.temankuliah.app.ui.theme.TkPalette
import java.util.WeakHashMap

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Jumlah layar yang sedang meminta ikon status bar putih. */
private class LightIconClaim(var count: Int)

private val lightIconClaims = WeakHashMap<Window, LightIconClaim>()

/**
 * Membuat ikon status bar (jam, sinyal, baterai) berwarna putih selama composable ini aktif, untuk layar
 * berlatar biru (splash dan masuk). Dihitung per jendela: bila splash dan layar masuk aktif bergantian
 * (saat transisi keduanya sempat tampil bersamaan), ikon baru dipulihkan setelah keduanya hilang.
 */
@Composable
fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            val claim = lightIconClaims.getOrPut(window) { LightIconClaim(0) }
            claim.count += 1
            controller.isAppearanceLightStatusBars = false
        }
        onDispose {
            if (window != null) {
                val claim = lightIconClaims[window]
                if (claim != null) {
                    claim.count -= 1
                    if (claim.count <= 0) {
                        lightIconClaims.remove(window)
                        // Pulihkan sesuai tema aktif: ikon gelap di tema terang, ikon putih di tema gelap.
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                            !TkPalette.isDark
                    }
                }
            }
        }
    }
}
