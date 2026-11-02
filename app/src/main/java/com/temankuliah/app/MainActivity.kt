package com.temankuliah.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.ui.TemanKuliahApp
import com.temankuliah.app.ui.TemanKuliahViewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.ui.theme.TemanKuliahTheme
import com.temankuliah.app.ui.theme.ThemeMode
import com.temankuliah.app.ui.theme.TkPalette
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.LaunchedEffect
import com.temankuliah.app.ui.theme.resolveDark

class MainActivity : ComponentActivity() {
    internal val viewModel: TemanKuliahViewModel by viewModels { TemanKuliahViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hanya pada peluncuran baru. Setelah activity dibuat ulang (rotasi, proses dimatikan), sistem memberi
        // Intent peluncuran asli yang masih memuat extra; memprosesnya lagi akan membuka ulang tab tujuan.
        if (savedInstanceState == null) handleOpenIntent(intent)
        // Isi palet sebelum bingkai pertama supaya tidak berkedip terang lalu gelap saat aplikasi dibuka.
        TkPalette.isDark = when (viewModel.preferencesController.themeMode.value) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
        applyEdgeToEdge(TkPalette.isDark)
        setContent {
            val themeMode by viewModel.preferencesController.themeMode.collectAsStateWithLifecycle()
            // Scrim bilah navigasi tiga tombol mengikuti tema aplikasi, bukan tema sistem.
            val dark = themeMode.resolveDark()
            LaunchedEffect(dark) { applyEdgeToEdge(dark) }
            // Ukuran font mengikuti pengaturan sistem sepenuhnya (tanpa batas atas/bawah).
            TemanKuliahTheme(themeMode = themeMode) {
                TemanKuliahApp(viewModel = viewModel)
            }
        }
    }

    private fun applyEdgeToEdge(dark: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(
                Color.argb(0xE6, 0xFF, 0xFF, 0xFF),
                Color.argb(0x80, 0x0A, 0x10, 0x1C)
            ) { dark }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenIntent(intent)
    }

    /** Membuka tab tujuan dari notifikasi. Extra dihapus setelah dibaca agar tidak terulang saat activity dibuat ulang. */
    internal fun handleOpenIntent(intent: Intent?) {
        val tab = intent?.getIntExtra(ReminderScheduler.EXTRA_OPEN_TAB, -1) ?: -1
        if (tab >= 0) {
            intent?.removeExtra(ReminderScheduler.EXTRA_OPEN_TAB)
            viewModel.requestOpenTab(tab)
        }
    }
}
