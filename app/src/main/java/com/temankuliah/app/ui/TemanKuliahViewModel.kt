package com.temankuliah.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.temankuliah.app.data.files.StudentFileStore
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.data.local.TemanKuliahDatabase
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.data.repository.TemanKuliahRepository
import com.temankuliah.app.ui.academic.documents.DocumentController
import com.temankuliah.app.ui.auth.AuthController
import com.temankuliah.app.ui.calendar.CalendarController
import com.temankuliah.app.ui.courses.CourseController
import com.temankuliah.app.ui.notifications.NotificationController
import com.temankuliah.app.ui.profile.BackupController
import com.temankuliah.app.ui.profile.PreferencesController
import com.temankuliah.app.ui.profile.ProfileController
import com.temankuliah.app.ui.projects.ProjectController
import com.temankuliah.app.ui.schedule.ScheduleController
import com.temankuliah.app.ui.tasks.TaskController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemanKuliahViewModel(
    application: Application,
    private val database: TemanKuliahDatabase,
    private val repository: TemanKuliahRepository
) : AndroidViewModel(application) {

    data class AuthSessionState(val initialized: Boolean = false, val user: UserEntity? = null)

    // Tahan tampilan login sampai Room selesai memeriksa sesi tersimpan.
    val authSession: StateFlow<AuthSessionState> = repository.activeUserFlow
        .map { user -> AuthSessionState(initialized = true, user = user) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthSessionState()
        )

    // Current authenticated user (Local Auth)
    val currentUser: StateFlow<UserEntity?> = authSession
        .map { it.user }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val activeSemester: StateFlow<Int> = currentUser.map { (it?.activeSemester ?: 1).coerceAtLeast(1) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    // Navigation and filtering states
    private val _selectedTab = MutableStateFlow(0) // 0=Dashboard, 1=Schedule, 2=Courses, 3=Profile
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // UI Dialog & Snackbars
    private val _activeDialog = MutableStateFlow<DialogType?>(null)
    val activeDialog: StateFlow<DialogType?> = _activeDialog.asStateFlow()

    private val _profileEditError = MutableStateFlow<String?>(null)
    val profileEditError: StateFlow<String?> = _profileEditError.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Logika tiap fitur ada di controller masing-masing (lihat folder fitur). Layar memakainya lewat
    // properti di bawah, misalnya viewModel.taskController.addTask(...).
    private val featureContext = FeatureContext(
        application = application,
        database = database,
        repository = repository,
        scope = viewModelScope,
        currentUser = currentUser,
        activeSemester = activeSemester,
        snackbar = _snackbarMessage,
        profileEditError = _profileEditError,
        closeDialog = { closeDialog() }
    )
    internal val preferencesController = PreferencesController(featureContext)
    internal val backupController = BackupController(
        ctx = featureContext,
        onRestored = {
            preferencesController.reload()
        }
    )
    internal val authController = AuthController(
        ctx = featureContext
    )
    internal val profileController = ProfileController(featureContext)
    internal val documentController = DocumentController(featureContext) { semester, date ->
        preferencesController.setSemesterStartDate(semester, date)
    }
    internal val courseController = CourseController(featureContext)
    internal val scheduleController = ScheduleController(featureContext, preferencesController.semesterStartDates)
    internal val taskController = TaskController(featureContext, preferencesController.reminderPreferences)
    internal val projectController = ProjectController(featureContext)
    internal val calendarController = CalendarController(featureContext)
    internal val notificationController = NotificationController(
        ctx = featureContext,
        tasks = taskController.tasks,
        schedules = scheduleController.schedules,
        events = calendarController.academicEvents,
        courses = courseController.courses,
        reminderPreferences = preferencesController.reminderPreferences
    )

    /**
     * Jumlah semester yang tampil: nilai tersimpan, atau lebih besar bila ada semester aktif atau data
     * (mata kuliah/jadwal) yang sudah berada di atasnya, sehingga tidak ada data yang "hilang" dari daftar.
     */
    val semesterCount: StateFlow<Int> = combine(
        currentUser, courseController.courses, scheduleController.schedules
    ) { user, courses, schedules ->
        maxOf(
            user?.semesterCount ?: 8,
            user?.activeSemester ?: 1,
            courses.maxOfOrNull { it.semester } ?: 0,
            schedules.maxOfOrNull { it.semester } ?: 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Best-effort migration for passwords written in plaintext by earlier builds.
            runCatching { repository.upgradeLegacyPasswords() }
            runCatching { StudentFileStore.cleanupOrphans(application, database) }
            // Rebuild reminder alarms from Room after process restart/app update.
            runCatching { ReminderScheduler.rescheduleForActiveUser(application, database) }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun setTab(index: Int) {
        _selectedTab.value = index.coerceIn(0, 4)
    }

    /** Tab yang diminta dari notifikasi; diterapkan UI setelah pengguna masuk. */
    private val _pendingOpenTab = MutableStateFlow<Int?>(null)
    val pendingOpenTab: StateFlow<Int?> = _pendingOpenTab.asStateFlow()

    fun requestOpenTab(index: Int) {
        _pendingOpenTab.value = index
    }

    fun consumePendingOpenTab() {
        _pendingOpenTab.value = null
    }

    fun setActiveSemester(semester: Int) {
        val user = currentUser.value ?: return
        val safe = semester.coerceAtLeast(1)
        viewModelScope.launch {
            // Pastikan daftar semester mencakup semester yang dipilih sebelum menyimpannya sebagai aktif.
            if (safe > semesterCount.value) repository.updateSemesterCount(user.id, safe)
            val result = repository.updateActiveSemester(user.id, safe)
            if (result.isSuccess) {
                _snackbarMessage.value = "Semester aktif diubah ke Semester $safe. Data semester lain tetap tersimpan."
            } else _snackbarMessage.value = result.exceptionOrNull()?.message ?: "Gagal mengubah semester."
        }
    }

    /** Menambah satu semester baru di akhir daftar. Tidak ada batas atas; data semester lain tidak berubah. */
    fun addSemester() {
        val user = currentUser.value ?: return
        val next = semesterCount.value + 1
        viewModelScope.launch {
            val result = repository.updateSemesterCount(user.id, next)
            _snackbarMessage.value = if (result.isSuccess) "Semester $next ditambahkan."
            else result.exceptionOrNull()?.message ?: "Gagal menambah semester."
        }
    }

    fun openDialog(dialog: DialogType) {
        if (dialog is DialogType.EditProfile) _profileEditError.value = null
        _activeDialog.value = dialog
    }

    fun closeDialog() {
        _activeDialog.value = null
        _profileEditError.value = null
    }

    companion object {
        /**
         * Membuat ViewModel dengan database dan repository yang sudah jadi, sehingga ViewModel tidak
         * lagi membangun dependensinya sendiri. Dipakai lewat `viewModels { TemanKuliahViewModel.Factory }`.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val database = TemanKuliahDatabase.getDatabase(app)
                TemanKuliahViewModel(app, database, TemanKuliahRepository.from(database))
            }
        }
    }
}

sealed class DialogType {
    object AddCourse : DialogType()
    data class AddCourseWithSemester(val semester: Int) : DialogType()
    object AddTask : DialogType()
    data class EditTask(val task: TaskEntity) : DialogType()
    object AddSchedule : DialogType()
    data class EditSchedule(val schedule: ScheduleEntity) : DialogType()
    data class EditCourseDetails(val course: CourseEntity) : DialogType()
    data class EditCourseGrade(val course: CourseEntity) : DialogType()
    data class EditProfile(val user: UserEntity) : DialogType()
    object AddProject : DialogType()
    object StudyTarget : DialogType()
}
