package com.temankuliah.app.ui

/** Indeks tab navigasi bawah. */
const val TAB_HOME = 0
const val TAB_ACADEMIC = 3
const val TAB_PROFILE = 4

/** Halaman di dalam tab Beranda: [ROOT] = Beranda, [NOTIFICATIONS] = layar Notifikasi yang dibuka dari lonceng. */
enum class DashboardSection { ROOT, NOTIFICATIONS }

/** Halaman di dalam tab Akademik: [DASHBOARD] = ringkasan, sisanya halaman turunan. */
enum class AcademicSection { DASHBOARD, SEMESTER_ACTIVE, KRS, KHS, SCHEDULE, COURSES }

/** Halaman di dalam tab Profil: [ROOT] = Profil, sisanya halaman turunan. */
enum class ProfileSection { ROOT, PERSONAL, NOTIFICATIONS, APPEARANCE, ACCOUNT, PRIVACY, ABOUT }

/** Langkah yang dilakukan tombol back sistem. */
enum class BackAction { CLOSE_DASHBOARD_SECTION, CLOSE_ACADEMIC_SECTION, CLOSE_PROFILE_SECTION, CLOSE_PROJECTS, GO_HOME }

/**
 * Menentukan langkah tombol back, atau `null` bila back tidak ditangani aplikasi (Beranda di halaman utama).
 * Urutannya: tutup halaman turunan tab yang sedang aktif, lalu tutup daftar proyek, lalu kembali ke Beranda.
 */
fun resolveBackAction(
    tab: Int,
    dashboard: DashboardSection,
    academic: AcademicSection,
    profile: ProfileSection,
    showingProjects: Boolean
): BackAction? = when {
    tab == TAB_HOME && dashboard != DashboardSection.ROOT -> BackAction.CLOSE_DASHBOARD_SECTION
    tab == TAB_ACADEMIC && academic != AcademicSection.DASHBOARD -> BackAction.CLOSE_ACADEMIC_SECTION
    tab == TAB_PROFILE && profile != ProfileSection.ROOT -> BackAction.CLOSE_PROFILE_SECTION
    showingProjects -> BackAction.CLOSE_PROJECTS
    tab != TAB_HOME -> BackAction.GO_HOME
    else -> null
}
