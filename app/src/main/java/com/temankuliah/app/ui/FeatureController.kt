package com.temankuliah.app.ui

import android.app.Application
import com.temankuliah.app.data.local.TemanKuliahDatabase
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.data.repository.TemanKuliahRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Bagian ViewModel yang dipakai bersama oleh semua controller fitur: database, repository,
 * pengguna aktif, semester aktif, snackbar, dan penutup dialog. Dibuat sekali di
 * [TemanKuliahViewModel] lalu diberikan ke setiap controller.
 */
internal class FeatureContext(
    val application: Application,
    val database: TemanKuliahDatabase,
    val repository: TemanKuliahRepository,
    val scope: CoroutineScope,
    val currentUser: StateFlow<UserEntity?>,
    val activeSemester: StateFlow<Int>,
    val snackbar: MutableStateFlow<String?>,
    val profileEditError: MutableStateFlow<String?>,
    val closeDialog: () -> Unit
)

/**
 * Dasar semua controller fitur (AuthController, TaskController, dan seterusnya). Satu controller
 * memegang state dan aksi satu fitur; [TemanKuliahViewModel] hanya merangkai dan menyimpannya.
 */
internal abstract class FeatureController(protected val ctx: FeatureContext) {
    protected val application: Application get() = ctx.application
    protected val database: TemanKuliahDatabase get() = ctx.database
    protected val repository: TemanKuliahRepository get() = ctx.repository
    protected val scope: CoroutineScope get() = ctx.scope
    protected val currentUser: StateFlow<UserEntity?> get() = ctx.currentUser
    protected val activeSemester: StateFlow<Int> get() = ctx.activeSemester
    protected val snackbar: MutableStateFlow<String?> get() = ctx.snackbar
    protected val profileEditError: MutableStateFlow<String?> get() = ctx.profileEditError
    protected fun closeDialog() = ctx.closeDialog()
}
