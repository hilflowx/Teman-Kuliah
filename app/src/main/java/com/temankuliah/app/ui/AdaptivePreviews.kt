package com.temankuliah.app.ui

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.ui.academic.AcademicHeroCard
import com.temankuliah.app.ui.academic.AcademicHeroStat
import com.temankuliah.app.ui.academic.KhsLine
import com.temankuliah.app.ui.academic.KhsTableFooter
import com.temankuliah.app.ui.academic.KhsTableHeader
import com.temankuliah.app.ui.academic.KhsTableRow
import com.temankuliah.app.ui.academic.KrsLine
import com.temankuliah.app.ui.academic.KrsTableFooter
import com.temankuliah.app.ui.academic.KrsTableHeader
import com.temankuliah.app.ui.academic.KrsTableRow
import com.temankuliah.app.ui.auth.LoginContent
import com.temankuliah.app.ui.components.TkBottomNav
import com.temankuliah.app.ui.components.TkNavItem
import com.temankuliah.app.ui.components.TkNavRail
import com.temankuliah.app.ui.components.TkSegmentedControl
import com.temankuliah.app.ui.dashboard.DashTabs
import com.temankuliah.app.ui.dashboard.DashboardHeader
import com.temankuliah.app.ui.dashboard.GpaHeroCard
import com.temankuliah.app.ui.dashboard.NearestDeadlineSection
import com.temankuliah.app.ui.icons.Description
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.TemanKuliahTheme
import com.temankuliah.app.ui.theme.ThemeMode
import com.temankuliah.app.ui.components.TkPickerField
import com.temankuliah.app.ui.icons.CalendarToday

/**
 * Matriks pratinjau responsif: 4 lebar (360, 411, 448, 800 dp) x fontScale (1.0, 2.0) x mode siang/malam = 16 pratinjau
 * untuk setiap composable yang diberi anotasi ini.
 *
 * Catatan:
 * - Bingkai pratinjau memakai ThemeMode.SYSTEM: varian "siang" tampil terang dan varian "malam" tampil gelap,
 *   sehingga warna yang belum mengikuti tema gelap terlihat jelas di pratinjau.
 * - TemanKuliahTheme membatasi fontScale ke 1.3x. Bingkai pratinjau ([AdaptivePreviewFrame]) menerapkan ulang skala yang
 *   diminta di dalam tema, supaya varian 2.0x benar-benar menguji tata letak pada 2.0x.
 * - Layar utuh bergantung pada TemanKuliahViewModel (Room), yang tidak bisa dibuat di pratinjau. Layar utuh diuji di
 *   AdaptiveScreensRenderTest; pratinjau ini mencakup konten stateless tiap layar.
 */
@Preview(name = "360dp · 1.0x · siang", widthDp = 360, heightDp = 800, fontScale = 1.0f, uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "360dp · 1.0x · malam", widthDp = 360, heightDp = 800, fontScale = 1.0f, uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "360dp · 2.0x · siang", widthDp = 360, heightDp = 800, fontScale = 2.0f, uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "360dp · 2.0x · malam", widthDp = 360, heightDp = 800, fontScale = 2.0f, uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "411dp · 1.0x · siang", widthDp = 411, heightDp = 891, fontScale = 1.0f, uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "411dp · 1.0x · malam", widthDp = 411, heightDp = 891, fontScale = 1.0f, uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "411dp · 2.0x · siang", widthDp = 411, heightDp = 891, fontScale = 2.0f, uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "411dp · 2.0x · malam", widthDp = 411, heightDp = 891, fontScale = 2.0f, uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "448dp · 1.0x · siang", widthDp = 448, heightDp = 960, fontScale = 1.0f, uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "448dp · 1.0x · malam", widthDp = 448, heightDp = 960, fontScale = 1.0f, uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "448dp · 2.0x · siang", widthDp = 448, heightDp = 960, fontScale = 2.0f, uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "448dp · 2.0x · malam", widthDp = 448, heightDp = 960, fontScale = 2.0f, uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "800dp · 1.0x · siang", widthDp = 800, heightDp = 1280, fontScale = 1.0f, uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "800dp · 1.0x · malam", widthDp = 800, heightDp = 1280, fontScale = 1.0f, uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "800dp · 2.0x · siang", widthDp = 800, heightDp = 1280, fontScale = 2.0f, uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "800dp · 2.0x · malam", widthDp = 800, heightDp = 1280, fontScale = 2.0f, uiMode = UI_MODE_NIGHT_YES, showBackground = true)
annotation class AdaptiveMatrixPreview

@Composable
private fun AdaptivePreviewFrame(
    contentAlignment: Alignment = Alignment.TopStart,
    padded: Boolean = true,
    content: @Composable () -> Unit
) {
    // fontScale yang diminta pratinjau dibaca SEBELUM tema, karena tema membatasinya ke 1.3x.
    val requestedFontScale = LocalDensity.current.fontScale
    TemanKuliahTheme(themeMode = ThemeMode.SYSTEM) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, requestedFontScale)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .then(if (padded) Modifier.padding(16.dp) else Modifier),
                contentAlignment = contentAlignment
            ) {
                content()
            }
        }
    }
}

// ----------------------------------------------------------------------------------------------
// Beranda
// ----------------------------------------------------------------------------------------------

@AdaptiveMatrixPreview
@Composable
fun DashboardHeaderAdaptivePreview() {
    AdaptivePreviewFrame {
        DashboardHeader(
            timeGreeting = "Selamat pagi",
            firstName = "Budi",
            onOpenProfile = {},
            onOpenNotifications = {}
        )
    }
}

@AdaptiveMatrixPreview
@Composable
fun GpaHeroCardAdaptivePreview() {
    AdaptivePreviewFrame(contentAlignment = Alignment.TopCenter) {
        GpaHeroCard(
            cumulativeGpa = 3.82,
            totalCredits = 84,
            targetCredits = 144,
            semesterGpa = 3.90,
            semesterGradedCredits = 20,
            semester = 5,
            courseCount = 7,
            onOpenAcademic = {},
            onAddCourse = {}
        )
    }
}

@AdaptiveMatrixPreview
@Composable
fun DashTabsAdaptivePreview() {
    AdaptivePreviewFrame {
        DashTabs(items = listOf("Jadwal", "Tugas", "Agenda"), selectedIndex = 1, onSelect = {})
    }
}

@AdaptiveMatrixPreview
@Composable
fun NearestDeadlineAdaptivePreview() {
    val now = System.currentTimeMillis()
    val course = CourseEntity(
        id = 1, userId = 1, code = "IF2101", name = "Pemrograman Berorientasi Objek dan Pola Rancangan",
        credits = 3, gradeLetter = "-", gradePoint = 0.0, semester = 3
    )
    AdaptivePreviewFrame {
        NearestDeadlineSection(
            tasks = listOf(
                TaskEntity(id = 1, userId = 1, title = "Laporan praktikum basis data terdistribusi", courseId = 1, dueAtMillis = now + DAY_MILLIS * 2),
                TaskEntity(id = 2, userId = 1, title = "Kuis 3", dueAtMillis = now + DAY_MILLIS * 4, priority = "Tinggi")
            ),
            allCourses = listOf(course),
            onOpenTasksTab = {},
            onEditTask = {}
        )
    }
}

// ----------------------------------------------------------------------------------------------
// Masuk
// ----------------------------------------------------------------------------------------------

@AdaptiveMatrixPreview
@Composable
fun LoginAdaptivePreview() {
    AdaptivePreviewFrame(padded = false) {
        LoginContent(
            busy = false,
            errorMessage = "Email atau kata sandi tidak cocok. Periksa lagi lalu coba kembali.",
            onLogin = { _, _ -> },
            onGuestSignIn = {},
            onNavigateToRegister = {}
        )
    }
}

// ----------------------------------------------------------------------------------------------
// Akademik: KRS dan KHS
// ----------------------------------------------------------------------------------------------

private val sampleKrsLines = listOf(
    KrsLine("IF2101", "Pemrograman Berorientasi Objek dan Pola Rancangan", 2, 1, 3, "Dr. Ir. Budi Santoso, M.Kom.", "Kamis", "08:00", "10:30", 150, "Lab 3"),
    KrsLine("IF2203", "Basis Data", 2, 1, 3, "", "", "", "", 0)
)

private val sampleKhsLines = listOf(
    KhsLine("IF2101", "Pemrograman Berorientasi Objek dan Pola Rancangan", 3, "A"),
    KhsLine("IF2203", "Basis Data", 3, "B+"),
    KhsLine("IF2305", "Kecerdasan Buatan", 4, "-")
)

@AdaptiveMatrixPreview
@Composable
fun KrsTableAdaptivePreview() {
    AdaptivePreviewFrame {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            AcademicHeroCard(
                icon = Icons.Default.Description,
                label = "KRS",
                title = "Rencana semester 3",
                status = "Tersimpan",
                stats = listOf(
                    AcademicHeroStat("Mata kuliah", "2", "semester ini"),
                    AcademicHeroStat("Total SKS", "6", "direncanakan")
                )
            )
            KrsTableHeader()
            sampleKrsLines.forEachIndexed { index, line ->
                KrsTableRow(line = line, number = index + 1, onEdit = {}, onDelete = {})
            }
            KrsTableFooter(totalTheory = 4, totalPractice = 2, totalSks = 6)
        }
    }
}

@AdaptiveMatrixPreview
@Composable
fun KhsTableAdaptivePreview() {
    AdaptivePreviewFrame {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            KhsTableHeader()
            sampleKhsLines.forEachIndexed { index, line ->
                KhsTableRow(line = line, number = index + 1, onEdit = {}, onDelete = {})
            }
            KhsTableFooter(totalSks = 10, totalMutu = 33.0)
        }
    }
}

// ----------------------------------------------------------------------------------------------
// Navigasi dan kontrol umum
// ----------------------------------------------------------------------------------------------

private val sampleNavItems = listOf(
    TkNavItem("Beranda", TkOutlineIcons.Calendar, "nav_dashboard"),
    TkNavItem("Kalender", TkOutlineIcons.Calendar, "nav_schedule"),
    TkNavItem("Tugas", TkOutlineIcons.Clipboard, "nav_tasks"),
    TkNavItem("Akademik", TkOutlineIcons.BookOpen, "nav_courses"),
    TkNavItem("Profil", TkOutlineIcons.Person, "nav_profile")
)

@AdaptiveMatrixPreview
@Composable
fun BottomNavAdaptivePreview() {
    AdaptivePreviewFrame(contentAlignment = Alignment.BottomCenter, padded = false) {
        TkBottomNav(items = sampleNavItems, selectedIndex = 3, onSelect = {})
    }
}

@AdaptiveMatrixPreview
@Composable
fun NavRailAdaptivePreview() {
    AdaptivePreviewFrame(contentAlignment = Alignment.CenterStart, padded = false) {
        TkNavRail(items = sampleNavItems, selectedIndex = 1, onSelect = {})
    }
}

@AdaptiveMatrixPreview
@Composable
fun SegmentedControlAdaptivePreview() {
    AdaptivePreviewFrame {
        TkSegmentedControl(items = listOf("Semua", "Belum selesai", "Selesai"), selectedIndex = 1, onSelect = {})
    }
}

@AdaptiveMatrixPreview
@Composable
fun PickerFieldAdaptivePreview() {
    AdaptivePreviewFrame {
        TkPickerField(label = "Tanggal kuliah", value = "Kamis, 29 Okt 2026", icon = Icons.Default.CalendarToday, onClick = {})
    }
}

/** Satu hari dalam milidetik; dipakai data contoh pratinjau. */
private const val DAY_MILLIS = 86_400_000L
