@file:Suppress("DEPRECATION")

package com.temankuliah.app.ui

import androidx.core.content.edit
import com.temankuliah.app.ui.components.TkScrollableDialogText
import com.temankuliah.app.ui.components.TkAdaptiveNavigationSuiteScaffold
import com.temankuliah.app.ui.components.MotionTokens
import com.temankuliah.app.ui.components.rememberReducedMotion
import kotlinx.coroutines.delay
import com.temankuliah.app.ui.components.topLevelNavTransition
import com.temankuliah.app.ui.components.hierarchicalSubPageTransition
import com.temankuliah.app.ui.components.TkNavItem
import androidx.compose.animation.AnimatedContent
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
import com.temankuliah.app.data.local.AppStorage
import com.temankuliah.app.data.notifications.ReminderScheduler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.temankuliah.app.ui.academic.AcademicDashboardScreen
import com.temankuliah.app.ui.academic.KhsScreen
import com.temankuliah.app.ui.academic.KrsScreen
import com.temankuliah.app.ui.academic.SemesterActiveScreen
import com.temankuliah.app.ui.academic.StudyTargetDialog
import com.temankuliah.app.ui.auth.AuthScreen
import com.temankuliah.app.ui.calendar.CalendarScreen
import com.temankuliah.app.ui.courses.AddCourseDialog
import com.temankuliah.app.ui.courses.CoursesScreen
import com.temankuliah.app.ui.courses.EditCourseGradeDialog
import com.temankuliah.app.ui.dashboard.DashboardScreen
import com.temankuliah.app.ui.icons.Person
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.notifications.NotificationsScreen
import com.temankuliah.app.ui.profile.AboutScreen
import com.temankuliah.app.ui.profile.AccountScreen
import com.temankuliah.app.ui.profile.AppearanceScreen
import com.temankuliah.app.ui.profile.EditProfileDialog
import com.temankuliah.app.ui.profile.NotificationSettingsScreen
import com.temankuliah.app.ui.profile.PersonalInfoScreen
import com.temankuliah.app.ui.profile.PrivacySecurityScreen
import com.temankuliah.app.ui.profile.ProfileScreen
import com.temankuliah.app.ui.profile.ProfileSubPage
import com.temankuliah.app.ui.projects.ProjectEditorDialog
import com.temankuliah.app.ui.schedule.AddScheduleDialog
import com.temankuliah.app.ui.schedule.CourseScheduleScreen
import com.temankuliah.app.ui.splash.SplashHost
import com.temankuliah.app.ui.tasks.AddTaskDialog
import com.temankuliah.app.ui.tasks.TasksScreen
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TkPalette
import com.temankuliah.app.ui.theme.maxContentWidth
import com.temankuliah.app.ui.theme.rememberTkWidthClass

/**
 * Akar aplikasi: splash tampil dulu (animasi masuk, bar loading, transisi keluar), lalu layar masuk atau
 * Beranda muncul di bawahnya. Status "splash selesai" disimpan agar tidak diulang setelah rotasi.
 */
@Composable
fun TemanKuliahApp(viewModel: TemanKuliahViewModel) {
    val authSession by viewModel.authSession.collectAsStateWithLifecycle()
    var splashFinished by rememberSaveable { mutableStateOf(false) }
    SplashHost(
        finished = splashFinished,
        dataReady = authSession.initialized,
        onFinished = { splashFinished = true }
    ) {
        TemanKuliahAppContent(viewModel = viewModel, splashFinished = splashFinished)
    }
}

@Composable
private fun TemanKuliahAppContent(viewModel: TemanKuliahViewModel, splashFinished: Boolean) {
    val context = LocalContext.current
    val authSession by viewModel.authSession.collectAsStateWithLifecycle()
    val currentUser = authSession.user
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val activeDialog by viewModel.activeDialog.collectAsStateWithLifecycle()
    val profileEditError by viewModel.profileEditError.collectAsStateWithLifecycle()
    val targetCredits by viewModel.preferencesController.targetCredits.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var academicSection by rememberSaveable { mutableStateOf(AcademicSection.DASHBOARD) }
    var tasksShowingProjects by rememberSaveable { mutableStateOf(false) }
    var profileSection by rememberSaveable { mutableStateOf(ProfileSection.ROOT) }
    var dashboardSection by rememberSaveable { mutableStateOf(DashboardSection.ROOT) }
    val appPrefs = remember { AppStorage.getPreferences(context) }
    var showExactAlarmPrompt by remember { mutableStateOf(false) }
    // Izin alarm presisi (Android 12+) ditawarkan sekali; pengguna tetap bisa mengaturnya di Profil > Notifikasi.
    fun maybePromptExactAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !ReminderScheduler.canScheduleExactAlarms(context) &&
            !appPrefs.getBoolean("exact_alarm_prompted", false)
        ) {
            appPrefs.edit { putBoolean("exact_alarm_prompted", true) }
            showExactAlarmPrompt = true
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        maybePromptExactAlarm()
    }
    // Android 13+: tanpa izin ini pengingat tugas dan kuliah tidak tampil. Diminta sekali setelah pengguna masuk.
    // Dialog izin baru boleh muncul setelah splash selesai, supaya tidak menutupi animasi.
    LaunchedEffect(currentUser?.id, splashFinished) {
        if (currentUser == null || !splashFinished) return@LaunchedEffect
        val needsNotificationPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsNotificationPermission && !appPrefs.getBoolean("notification_permission_asked", false)) {
            appPrefs.edit { putBoolean("notification_permission_asked", true) }
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!needsNotificationPermission) {
            maybePromptExactAlarm()
        }
    }
    if (showExactAlarmPrompt) {
        AlertDialog(
            onDismissRequest = { showExactAlarmPrompt = false },
            title = { Text("Izinkan alarm presisi?") },
            text = {
                TkScrollableDialogText {
                    Text(
                        "Agar pengingat kuliah dan tugas berbunyi tepat waktu, izinkan alarm presisi untuk TemanKuliah. " +
                            "Tanpa izin ini, pengingat bisa terlambat."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmPrompt = false
                    runCatching {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                        }
                        context.startActivity(intent)
                    }
                }) { Text("Atur izin") }
            },
            dismissButton = { TextButton(onClick = { showExactAlarmPrompt = false }) { Text("Nanti") } }
        )
    }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            academicSection = AcademicSection.DASHBOARD
            profileSection = ProfileSection.ROOT
            dashboardSection = DashboardSection.ROOT
            tasksShowingProjects = false
            viewModel.setTab(0)
            viewModel.closeDialog()
        }
    }
    val pendingOpenTab by viewModel.pendingOpenTab.collectAsStateWithLifecycle()
    LaunchedEffect(currentUser, pendingOpenTab) {
        val tab = pendingOpenTab
        if (currentUser != null && tab != null) {
            academicSection = AcademicSection.DASHBOARD; profileSection = ProfileSection.ROOT; dashboardSection = DashboardSection.ROOT
            viewModel.setTab(tab)
            viewModel.consumePendingOpenTab()
        }
    }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSnackbar() }
    }

    if (!authSession.initialized) {
        Box(
            modifier = Modifier.fillMaxSize().background(BackgroundLight),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = TemanKuliahBlue)
        }
    } else if (currentUser == null) {
        AuthScreen(viewModel = viewModel)
    } else {
        val user = currentUser
        BackHandler(enabled = resolveBackAction(selectedTab, dashboardSection, academicSection, profileSection, tasksShowingProjects) != null) {
            when (resolveBackAction(selectedTab, dashboardSection, academicSection, profileSection, tasksShowingProjects)) {
                BackAction.CLOSE_DASHBOARD_SECTION -> dashboardSection = DashboardSection.ROOT
                BackAction.CLOSE_ACADEMIC_SECTION -> academicSection = AcademicSection.DASHBOARD
                BackAction.CLOSE_PROFILE_SECTION -> profileSection = ProfileSection.ROOT
                BackAction.CLOSE_PROJECTS -> tasksShowingProjects = false
                BackAction.GO_HOME -> {
                    tasksShowingProjects = false
                    profileSection = ProfileSection.ROOT
                    academicSection = AcademicSection.DASHBOARD
                    dashboardSection = DashboardSection.ROOT
                    viewModel.setTab(0)
                }
                null -> Unit
            }
        }
        val reducedMotion = rememberReducedMotion()
        // Reset sub-halaman ditunda sampai transisi tab selesai. Kalau direset bersamaan, AnimatedContent
        // bagian dalam di tab yang sedang keluar ikut menggeser konten saat tab itu sendiri sedang
        // memudar, sehingga konten terlihat bergoyang dua kali.
        LaunchedEffect(selectedTab) {
            delay(MotionTokens.DurationMedium.toLong() + 40L)
            if (selectedTab != 0) dashboardSection = DashboardSection.ROOT
            if (selectedTab != 2) tasksShowingProjects = false
            if (selectedTab != 3) academicSection = AcademicSection.DASHBOARD
            if (selectedTab != 4) profileSection = ProfileSection.ROOT
        }
        val onTabSelected: (Int) -> Unit = { index ->
            if (index == selectedTab) {
                academicSection = AcademicSection.DASHBOARD
                tasksShowingProjects = false
                profileSection = ProfileSection.ROOT
                dashboardSection = DashboardSection.ROOT
            }
            viewModel.setTab(index)
        }
        val contentMaxWidth = rememberTkWidthClass().maxContentWidth()

        val navTabs = remember(TkPalette.isDark) { buildNavTabs() }
        TkAdaptiveNavigationSuiteScaffold(
            items = navTabs,
            selectedIndex = selectedTab,
            onSelect = onTabSelected,
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = BackgroundLight,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = contentMaxWidth)
                            .fillMaxSize()
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = topLevelNavTransition(reducedMotion),
                            label = "tabContent"
                        ) { tab -> when (tab) {
                            0 -> AnimatedContent(
                                targetState = dashboardSection,
                                transitionSpec = hierarchicalSubPageTransition(reducedMotion, DashboardSection.ROOT),
                                label = "dashboardSection"
                            ) { section -> when (section) {
                                DashboardSection.NOTIFICATIONS -> NotificationsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { dashboardSection = DashboardSection.ROOT },
                                    onOpenTab = { target -> viewModel.setTab(target) }
                                )
                                DashboardSection.ROOT -> DashboardScreen(
                                    user = user,
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    onOpenNotifications = { dashboardSection = DashboardSection.NOTIFICATIONS }
                                )
                            } }
                            1 -> CalendarScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                            2 -> TasksScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding), onShowProjectsChange = { tasksShowingProjects = it })
                            3 -> AnimatedContent(targetState = academicSection, transitionSpec = hierarchicalSubPageTransition(reducedMotion, AcademicSection.DASHBOARD), label = "academicSection") { section -> when (section) {
                                AcademicSection.SEMESTER_ACTIVE -> SemesterActiveScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { academicSection = AcademicSection.DASHBOARD }
                                )
                                AcademicSection.KRS -> KrsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { academicSection = AcademicSection.DASHBOARD }
                                )
                                AcademicSection.KHS -> KhsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { academicSection = AcademicSection.DASHBOARD }
                                )
                                AcademicSection.SCHEDULE -> CourseScheduleScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { academicSection = AcademicSection.DASHBOARD }
                                )
                                AcademicSection.COURSES -> CoursesScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { academicSection = AcademicSection.DASHBOARD }
                                )
                                AcademicSection.DASHBOARD -> AcademicDashboardScreen(
                                    user = user,
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    onOpenSection = {
                                        if (it == AcademicSection.COURSES) {
                                            viewModel.courseController.setSearchQuery("")
                                        }
                                        academicSection = it
                                     },
                                    onOpenSchedule = { academicSection = AcademicSection.SCHEDULE }
                                )
                            } }
                            4 -> AnimatedContent(targetState = profileSection, transitionSpec = hierarchicalSubPageTransition(reducedMotion, ProfileSection.ROOT), label = "profileSection") { section -> when (section) {
                                ProfileSection.PERSONAL -> ProfileSubPage(
                                    title = "Biodata Mahasiswa",
                                    supporting = "Data identitas mahasiswa",
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { profileSection = ProfileSection.ROOT }
                                ) { childModifier ->
                                    PersonalInfoScreen(user = user, viewModel = viewModel, modifier = childModifier)
                                }
                                ProfileSection.NOTIFICATIONS -> ProfileSubPage(
                                    title = "Pengingat",
                                    supporting = "Jadwal kuliah dan tenggat tugas",
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { profileSection = ProfileSection.ROOT }
                                ) { childModifier ->
                                    NotificationSettingsScreen(viewModel = viewModel, modifier = childModifier)
                                }
                                ProfileSection.APPEARANCE -> ProfileSubPage(
                                    title = "Tampilan",
                                    supporting = "Preferensi tema aplikasi",
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { profileSection = ProfileSection.ROOT }
                                ) { childModifier ->
                                    AppearanceScreen(viewModel = viewModel, modifier = childModifier)
                                }
                                ProfileSection.ACCOUNT -> ProfileSubPage(
                                    title = "Informasi Akun",
                                    supporting = "Cara masuk dan keamanan data",
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { profileSection = ProfileSection.ROOT }
                                ) { childModifier ->
                                    AccountScreen(user = user, viewModel = viewModel, modifier = childModifier)
                                }
                                ProfileSection.PRIVACY -> ProfileSubPage(
                                    title = "Cadangkan & Pulihkan",
                                    supporting = "Simpan dan pulihkan data kuliahmu",
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { profileSection = ProfileSection.ROOT }
                                ) { childModifier ->
                                    PrivacySecurityScreen(viewModel = viewModel, modifier = childModifier)
                                }
                                ProfileSection.ABOUT -> ProfileSubPage(
                                    title = "Tentang Aplikasi",
                                    supporting = "Versi dan informasi aplikasi",
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { profileSection = ProfileSection.ROOT }
                                ) { childModifier ->
                                    AboutScreen(modifier = childModifier)
                                }
                                ProfileSection.ROOT -> ProfileScreen(
                                    user = user,
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding),
                                    onOpenSection = { profileSection = it }
                                )
                            } }
                        } }
                    }
                }
            }
        }

        val allCourses by viewModel.courseController.courses.collectAsStateWithLifecycle()
        val allScheduleRows by viewModel.scheduleController.allSchedules.collectAsStateWithLifecycle()
        val viewedScheduleSemester by viewModel.scheduleController.viewedSemester.collectAsStateWithLifecycle()
        val scheduleSemester = viewedScheduleSemester ?: user.activeSemester
        val activeSemesterStartDate = viewModel.preferencesController.semesterStartDateOf(scheduleSemester)
        val krsCourses = remember(allCourses, scheduleSemester) { allCourses.filter { it.semester == scheduleSemester }.sortedBy { it.name } }
        when (val dialog = activeDialog) {
            DialogType.AddCourse -> AddCourseDialog(
                onDismiss = viewModel::closeDialog,
                onConfirm = { code, name, credits, grade, semester, lecturer, classTime, autoSchedule, dayOfWeek, start, end, room ->
                    viewModel.courseController.addCourse(
                        code = code,
                        name = name,
                        credits = credits,
                        gradeLetter = grade,
                        semester = semester,
                        lecturer = lecturer,
                        classTime = classTime,
                        autoCreateSchedule = autoSchedule,
                        scheduleDayOfWeek = dayOfWeek,
                        scheduleStartTime = start,
                        scheduleEndTime = end,
                        scheduleRoom = room
                    )
                },
                defaultSemester = user.activeSemester
            )
            is DialogType.AddCourseWithSemester -> AddCourseDialog(
                onDismiss = viewModel::closeDialog,
                onConfirm = { code, name, credits, grade, semester, lecturer, classTime, autoSchedule, dayOfWeek, start, end, room ->
                    viewModel.courseController.addCourse(
                        code = code,
                        name = name,
                        credits = credits,
                        gradeLetter = grade,
                        semester = semester,
                        lecturer = lecturer,
                        classTime = classTime,
                        autoCreateSchedule = autoSchedule,
                        scheduleDayOfWeek = dayOfWeek,
                        scheduleStartTime = start,
                        scheduleEndTime = end,
                        scheduleRoom = room
                    )
                },
                defaultSemester = dialog.semester
            )
            is DialogType.EditCourseDetails -> AddCourseDialog(
                initialCourse = dialog.course,
                onDismiss = viewModel::closeDialog,
                onConfirm = { code, name, credits, grade, semester, lecturer, classTime, _, _, _, _, _ ->
                    viewModel.courseController.updateCourse(
                        course = dialog.course,
                        code = code,
                        name = name,
                        credits = credits,
                        gradeLetter = grade,
                        semester = semester,
                        lecturer = lecturer,
                        classTime = classTime
                    )
                },
                defaultSemester = user.activeSemester
            )
            DialogType.AddSchedule -> AddScheduleDialog(
                semester = scheduleSemester,
                semesterSubtitle = when {
                    scheduleSemester == user.activeSemester -> "Semester berjalan"
                    scheduleSemester < user.activeSemester -> "Semester selesai"
                    else -> "Belum dimulai"
                },
                semesterStartDate = activeSemesterStartDate,
                courseOptions = krsCourses,
                existingSchedules = allScheduleRows.filter { it.semester == scheduleSemester },
                onDismiss = viewModel::closeDialog,
                onSave = { _, draft -> viewModel.scheduleController.saveNew(draft) }
            )
            is DialogType.EditSchedule -> {
                val editSemester = dialog.schedule.semester
                AddScheduleDialog(
                    semester = editSemester,
                    semesterSubtitle = "",
                    semesterStartDate = viewModel.preferencesController.semesterStartDateOf(editSemester),
                    courseOptions = allCourses.filter { it.semester == editSemester }.sortedBy { it.name },
                    existingSchedules = allScheduleRows.filter { it.semester == editSemester },
                    initialSchedule = dialog.schedule,
                    onDismiss = viewModel::closeDialog,
                    onSave = { editScope, draft -> viewModel.scheduleController.saveEdit(dialog.schedule, editScope, draft) },
                    onDelete = { all ->
                        if (all) viewModel.scheduleController.deleteSeries(dialog.schedule.recurrenceGroupId)
                        else viewModel.scheduleController.deleteOne(dialog.schedule.id)
                        viewModel.closeDialog()
                    }
                )
            }
            is DialogType.EditCourseGrade -> EditCourseGradeDialog(course = dialog.course, onDismiss = viewModel::closeDialog, onSaveGrade = { viewModel.courseController.updateCourseGrade(dialog.course, it); viewModel.closeDialog() })
            is DialogType.EditProfile -> EditProfileDialog(user = dialog.user, errorMessage = profileEditError, onDismiss = viewModel::closeDialog, onSave = { fullName, nim, program, faculty, university -> viewModel.profileController.updateProfile(fullName, nim, program, faculty, university) })
            DialogType.StudyTarget -> StudyTargetDialog(initialTarget = targetCredits, onDismiss = viewModel::closeDialog, onSave = { viewModel.preferencesController.setTargetCredits(it); viewModel.closeDialog() })
            DialogType.AddTask -> AddTaskDialog(courses = allCourses, semester = user.activeSemester, onDismiss = viewModel::closeDialog, onConfirm = { title, description, deadline, priority, courseId -> viewModel.taskController.addTask(title, description, deadline, priority, courseId) })
            is DialogType.EditTask -> AddTaskDialog(initialTask = dialog.task, courses = allCourses, semester = dialog.task.semester, onDismiss = viewModel::closeDialog, onConfirm = { title, description, deadline, priority, courseId -> viewModel.taskController.updateTask(dialog.task, title, description, deadline, priority, courseId) })
            DialogType.AddProject -> ProjectEditorDialog(
                initial = null,
                defaultSemester = user.activeSemester,
                onDismiss = viewModel::closeDialog,
                onSave = { title, description, deadline, progress ->
                    viewModel.projectController.addProject(title, description, deadline, progress)
                    viewModel.closeDialog()
                }
            )
            null -> Unit
        }
    }

}

/** Ikon Beranda berwarna sendiri (keepColors), jadi daftar dibangun ulang saat tema berganti. */
private fun buildNavTabs(): List<TkNavItem> = listOf(
    TkNavItem(
        label = "Beranda",
        icon = TkOutlineIcons.homeGrid(primary = TkPalette.colors.navInactive, secondary = TkPalette.colors.navIconSecondaryInactive),
        testTag = "nav_dashboard",
        selectedIcon = TkOutlineIcons.homeGrid(primary = TkPalette.colors.navIconActive, secondary = TkPalette.colors.navIconSecondaryActive),
        keepColors = true
    ),
    TkNavItem("Kalender", TkOutlineIcons.Calendar, "nav_schedule"),
    TkNavItem("Tugas", TkOutlineIcons.Book, "nav_tasks"),
    TkNavItem("Akademik", TkOutlineIcons.School, "nav_courses"),
    TkNavItem("Profil", TkOutlineIcons.Person, "nav_profile")
)
