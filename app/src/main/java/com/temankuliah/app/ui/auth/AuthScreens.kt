package com.temankuliah.app.ui.auth

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import com.temankuliah.app.ui.components.rememberReducedMotion
import com.temankuliah.app.ui.theme.TkShape
import android.app.Activity
import android.app.KeyguardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.icons.ErrorOutline
import com.temankuliah.app.ui.theme.tkAdaptive

private enum class AuthMode {
    LOGIN,
    REGISTER,
    CREATE_PASSWORD,
    RESET_PASSWORD
}

/**
 * Wadah layar autentikasi TemanKuliah (gaya mockup): layar putih penuh dengan kolom konten di tengah,
 * banner kesalahan global, dan transisi halus antar mode masuk, daftar, dan pembuatan sandi.
 */
@Composable
fun AuthScreen(
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier
) {
    val authError by viewModel.authController.authError.collectAsStateWithLifecycle()
    val authLoading by viewModel.authController.authLoading.collectAsStateWithLifecycle()
    val busy = authLoading

    var authMode by rememberSaveable { mutableStateOf(AuthMode.LOGIN) }
    var registerStep by rememberSaveable { mutableIntStateOf(1) }
    var setupIdentifier by rememberSaveable { mutableStateOf("") }

    // Lupa kata sandi: akun harus ada, lalu kunci layar perangkat diverifikasi sebelum sandi boleh diganti.
    val context = LocalContext.current
    var resetIdentifier by rememberSaveable { mutableStateOf("") }
    // True bila verifikasi kunci layar dipakai untuk "Buat sandi" (akun tanpa sandi), false untuk lupa sandi.
    var verifyForSetup by rememberSaveable { mutableStateOf(false) }
    val deviceCredentialLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (verifyForSetup) {
                viewModel.authController.markDeviceVerifiedForSetup(setupIdentifier)
                authMode = AuthMode.CREATE_PASSWORD
            } else {
                viewModel.authController.markDeviceVerifiedForReset(resetIdentifier)
                authMode = AuthMode.RESET_PASSWORD
            }
        }
    }
    fun launchDeviceVerification(identifier: String, forSetup: Boolean = false) {
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        @Suppress("DEPRECATION")
        val intent = if (keyguard?.isDeviceSecure == true) {
            keyguard.createConfirmDeviceCredentialIntent(
                "Verifikasi kunci layar",
                "Konfirmasi bahwa ini perangkatmu untuk membuat kata sandi baru."
            )
        } else {
            null
        }
        if (intent == null) {
            val action = if (forSetup) "dibuat" else "direset"
            viewModel.authController.showAuthError(
                "HP ini belum memakai kunci layar (PIN, pola, atau sandi), jadi kata sandi belum bisa $action. " +
                    "Aktifkan kunci layar di Pengaturan HP lalu coba lagi."
            )
        } else {
            verifyForSetup = forSetup
            if (forSetup) setupIdentifier = identifier else resetIdentifier = identifier
            deviceCredentialLauncher.launch(intent)
        }
    }

    // Register Form States
    var regFullName by rememberSaveable { mutableStateOf("") }
    var regNim by rememberSaveable { mutableStateOf("") }
    var regProdi by rememberSaveable { mutableStateOf("") }
    var regFaculty by rememberSaveable { mutableStateOf("") }
    var regUniversity by rememberSaveable { mutableStateOf("") }
    var regSemester by rememberSaveable { mutableIntStateOf(1) }
    var regEmail by rememberSaveable { mutableStateOf("") }
    var regPassword by rememberSaveable { mutableStateOf("") }
    var regConfirmPassword by rememberSaveable { mutableStateOf("") }

    // Transisi antar mode (Masuk / Daftar / Buat Sandi). Masuk dan Daftar memakai layout penuh bergaya
    // mockup baru (header biru + lembar putih); Buat Sandi tetap memakai kolom putih seperti sebelumnya.
    val reducedMotion = rememberReducedMotion()
    AnimatedContent(
        targetState = authMode,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            if (reducedMotion) {
                (EnterTransition.None togetherWith ExitTransition.None).using(SizeTransform(clip = false))
            } else {
                (fadeIn(animationSpec = tween(durationMillis = 220)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 150)))
                    .using(SizeTransform(clip = false))
            }
        },
        label = "AuthModeTransition"
    ) { mode ->
        when (mode) {
            AuthMode.LOGIN -> {
                LoginContent(
                    busy = busy,
                    errorMessage = authError,
                    onLogin = { email, password ->
                        viewModel.authController.loginWithEmail(email, password) { identifier ->
                            // Akun tanpa sandi: verifikasi kunci layar dulu, baru layar "Buat sandi" dibuka.
                            launchDeviceVerification(identifier, forSetup = true)
                        }
                    },
                    onForgotPassword = { identifier ->
                        viewModel.authController.beginPasswordReset(identifier) { launchDeviceVerification(identifier) }
                    },
                    onGuestSignIn = {
                        if (!busy) {
                            viewModel.authController.clearAuthError()
                            viewModel.authController.signInAsGuest()
                        }
                    },
                    onNavigateToRegister = {
                        viewModel.authController.clearAuthError()
                        authMode = AuthMode.REGISTER
                        registerStep = 1
                    }
                )
            }

            AuthMode.CREATE_PASSWORD -> {
                AuthClassicLayout(authError = authError) { contentMinHeight ->
                    CreatePasswordContent(
                        identifier = setupIdentifier,
                        busy = busy,
                        modifier = Modifier.heightIn(min = contentMinHeight),
                        onSubmit = { password, confirmPassword ->
                            viewModel.authController.createPassword(setupIdentifier, password, confirmPassword)
                        },
                        onCancel = {
                            viewModel.authController.cancelPasswordSetup()
                            authMode = AuthMode.LOGIN
                        }
                    )
                }
            }

            AuthMode.RESET_PASSWORD -> {
                AuthClassicLayout(authError = authError) { contentMinHeight ->
                    CreatePasswordContent(
                        identifier = resetIdentifier,
                        busy = busy,
                        modifier = Modifier.heightIn(min = contentMinHeight),
                        isReset = true,
                        onSubmit = { password, confirmPassword ->
                            viewModel.authController.resetPassword(resetIdentifier, password, confirmPassword)
                        },
                        onCancel = {
                            viewModel.authController.cancelPasswordReset()
                            authMode = AuthMode.LOGIN
                        }
                    )
                }
            }

            AuthMode.REGISTER -> {
                RegisterMultiStepContent(
                    currentStep = registerStep,
                    busy = busy,
                    errorMessage = authError,
                    fullName = regFullName,
                    nim = regNim,
                    prodi = regProdi,
                    faculty = regFaculty,
                    university = regUniversity,
                    semester = regSemester,
                    email = regEmail,
                    password = regPassword,
                    confirmPassword = regConfirmPassword,
                    onFullNameChange = { regFullName = it },
                    onNimChange = { regNim = it },
                    onProdiChange = { regProdi = it },
                    onFacultyChange = { regFaculty = it },
                    onUniversityChange = { regUniversity = it },
                    onSemesterChange = { regSemester = it },
                    onEmailChange = { regEmail = it },
                    onPasswordChange = { regPassword = it },
                    onConfirmPasswordChange = { regConfirmPassword = it },
                    onNextStep = {
                        viewModel.authController.clearAuthError()
                        registerStep++
                    },
                    onPrevStep = {
                        viewModel.authController.clearAuthError()
                        if (registerStep > 1) registerStep--
                        else authMode = AuthMode.LOGIN
                    },
                    onGoToStep = { target ->
                        viewModel.authController.clearAuthError()
                        registerStep = target
                    },
                    onSubmitRegistration = {
                        viewModel.authController.register(
                            fullName = regFullName,
                            nim = regNim,
                            studyProgram = regProdi,
                            faculty = regFaculty,
                            university = regUniversity,
                            semester = regSemester,
                            email = regEmail,
                            password = regPassword,
                            onSuccess = {
                                authMode = AuthMode.LOGIN
                            }
                        )
                    },
                    onNavigateToLogin = {
                        viewModel.authController.clearAuthError()
                        authMode = AuthMode.LOGIN
                    }
                )
            }
        }
    }
}

/**
 * Kolom putih lama untuk Buat Sandi (tidak berubah): layar putih penuh dengan kolom konten di
 * tengah, lebar maks 480 dp di tablet, banner kesalahan global di atas konten. [content] menerima tinggi
 * minimal konten agar tautan di dasar menempel ke bawah layar.
 */
@Composable
private fun AuthClassicLayout(
    authError: String?,
    content: @Composable (contentMinHeight: Dp) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Tinggi minimal konten = tinggi layar dikurangi padding vertikal, agar tautan di bagian
        // bawah (Daftar sekarang / tombol lanjut) menempel ke dasar layar dan tetap bisa di-scroll.
        val contentMinHeight = (maxHeight - AuthVerticalPadding).coerceAtLeast(0.dp)

        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            authError?.let { err ->
                AuthErrorBanner(err)
                Spacer(Modifier.height(14.dp))
            }
            content(contentMinHeight)
        }
    }
}

/** Banner kesalahan global (merah muda) di layar autentikasi. */
@Composable
internal fun AuthErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TkShape.md)
            .background(ErrorRedLight)
            .border(1.dp, ErrorRedBorder, TkShape.md)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = message,
            color = tkAdaptive(Color(0xFF991B1B), Color(0xFFFF9A9A)),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

// Padding vertikal kolom konten (atas + bawah) yang dikurangkan dari tinggi layar.
private val AuthVerticalPadding = 56.dp
