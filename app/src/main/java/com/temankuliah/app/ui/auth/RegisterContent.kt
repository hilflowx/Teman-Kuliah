package com.temankuliah.app.ui.auth

import com.temankuliah.app.ui.components.rememberReducedMotion
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.components.LightSystemBarIcons
import com.temankuliah.app.ui.icons.TkOutlineIcons

internal const val REGISTER_TOTAL_STEPS = 3

private val EmailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")

/**
 * Layar Daftar 3 langkah (mockup baru): header biru dengan stepper, lembar putih berisi form, dan bar
 * aksi tetap di bawah.
 *  1. Data diri  — nama, NIM + semester, program studi, fakultas, universitas
 *  2. Akun       — tab Email/Nomor HP, kata sandi dengan pengukur kekuatan, ulangi kata sandi
 *  3. Tinjau     — ringkasan per bagian (tombol Ubah), persetujuan, lalu "Buat akun"
 *
 * [email] menampung email ATAU nomor HP (sama seperti sebelumnya) sehingga repository tidak berubah.
 */
@Composable
internal fun RegisterMultiStepContent(
    currentStep: Int,
    busy: Boolean,
    errorMessage: String?,
    fullName: String,
    nim: String,
    prodi: String,
    faculty: String,
    university: String,
    semester: Int,
    email: String,
    password: String,
    confirmPassword: String,
    onFullNameChange: (String) -> Unit,
    onNimChange: (String) -> Unit,
    onProdiChange: (String) -> Unit,
    onFacultyChange: (String) -> Unit,
    onUniversityChange: (String) -> Unit,
    onSemesterChange: (Int) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onGoToStep: (Int) -> Unit,
    onSubmitRegistration: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val step = currentStep.coerceIn(1, REGISTER_TOTAL_STEPS)
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var contactIsPhone by rememberSaveable { mutableStateOf(email.isNotBlank() && !email.contains('@')) }
    var passVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }
    var agreed by rememberSaveable { mutableStateOf(false) }

    LightSystemBarIcons()
    BackHandler(enabled = !busy) { onPrevStep() }
    // Setiap pindah langkah mulai dari atas lembar.
    LaunchedEffect(step) { scrollState.scrollTo(0) }

    val contactValid = if (contactIsPhone) email.count { it.isDigit() } in 8..15 else EmailPattern.matches(email)
    val canProceedStep1 = fullName.isNotBlank() && nim.isNotBlank() && prodi.isNotBlank() &&
        faculty.isNotBlank() && university.isNotBlank()
    val canProceedStep2 = contactValid && password.length >= 8 && password == confirmPassword
    val canProceed = when (step) {
        1 -> canProceedStep1
        2 -> canProceedStep2
        else -> !busy && agreed
    }

    val (title, subtitle) = when (step) {
        1 -> "Data diri kamu" to "Isi sesuai KRS dan kartu mahasiswa."
        2 -> "Buat akun & sandi" to "Dipakai untuk masuk ke TemanKuliah."
        else -> "Periksa data kamu" to "Pastikan semuanya benar sebelum akun dibuat."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuthSurface)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            RegisterHeader(
                step = step,
                totalSteps = REGISTER_TOTAL_STEPS,
                title = title,
                subtitle = subtitle,
                backEnabled = !busy,
                onBack = {
                    focusManager.clearFocus()
                    onPrevStep()
                }
            )

            Column(
                modifier = Modifier
                    .overlapTop(RegisterSheetCorner)
                    .fillMaxWidth()
                    .background(
                        AuthSurface,
                        RoundedCornerShape(topStart = RegisterSheetCorner, topEnd = RegisterSheetCorner)
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp)
            ) {
                if (errorMessage != null) {
                    AuthErrorBanner(errorMessage)
                    Spacer(Modifier.height(16.dp))
                }

                val reducedMotion = rememberReducedMotion()
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (fadeIn(tween(if (reducedMotion) 0 else 220)) togetherWith fadeOut(tween(if (reducedMotion) 0 else 90)))
                            .using(SizeTransform(clip = false) { _, _ -> snap() })
                    },
                    label = "RegisterStepBody"
                ) { s ->
                    when (s) {
                        1 -> PersonalStep(
                            fullName = fullName,
                            nim = nim,
                            prodi = prodi,
                            faculty = faculty,
                            university = university,
                            semester = semester,
                            onFullNameChange = onFullNameChange,
                            onNimChange = onNimChange,
                            onProdiChange = onProdiChange,
                            onFacultyChange = onFacultyChange,
                            onUniversityChange = onUniversityChange,
                            onSemesterChange = onSemesterChange,
                            onDone = { focusManager.clearFocus() },
                            onNavigateToLogin = onNavigateToLogin
                        )

                        2 -> AccountStep(
                            isPhone = contactIsPhone,
                            contact = email,
                            contactValid = contactValid,
                            password = password,
                            confirmPassword = confirmPassword,
                            passVisible = passVisible,
                            confirmVisible = confirmVisible,
                            onTogglePass = { passVisible = !passVisible },
                            onToggleConfirm = { confirmVisible = !confirmVisible },
                            onContactTypeChange = { phone ->
                                if (phone != contactIsPhone) {
                                    contactIsPhone = phone
                                    onEmailChange("")
                                }
                            },
                            onContactChange = onEmailChange,
                            onPasswordChange = onPasswordChange,
                            onConfirmPasswordChange = onConfirmPasswordChange,
                            onDone = {
                                focusManager.clearFocus()
                                if (canProceedStep2) onNextStep()
                            }
                        )

                        else -> ReviewStep(
                            fullName = fullName,
                            nim = nim,
                            prodi = prodi,
                            faculty = faculty,
                            university = university,
                            semester = semester,
                            contactIsPhone = contactIsPhone,
                            contact = email,
                            passwordLength = password.length,
                            agreed = agreed,
                            onAgreedChange = { agreed = it },
                            onEditPersonal = { onGoToStep(1) },
                            onEditAccount = { onGoToStep(2) }
                        )
                    }
                }
            }
        }

        RegisterBottomBar {
            RegisterPrimaryButton(
                text = when (step) {
                    1 -> "Lanjut"
                    2 -> "Tinjau data"
                    else -> "Buat akun"
                },
                onClick = {
                    focusManager.clearFocus()
                    if (step < REGISTER_TOTAL_STEPS) onNextStep() else onSubmitRegistration()
                },
                enabled = canProceed,
                loading = step == REGISTER_TOTAL_STEPS && busy,
                modifier = Modifier.testTag("btn_register_next")
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Langkah 1 — Data diri
// -------------------------------------------------------------------------------------------------

@Composable
private fun PersonalStep(
    fullName: String,
    nim: String,
    prodi: String,
    faculty: String,
    university: String,
    semester: Int,
    onFullNameChange: (String) -> Unit,
    onNimChange: (String) -> Unit,
    onProdiChange: (String) -> Unit,
    onFacultyChange: (String) -> Unit,
    onUniversityChange: (String) -> Unit,
    onSemesterChange: (Int) -> Unit,
    onDone: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RegisterFieldGap)) {
        LoginTextField(
            label = "Nama lengkap",
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = "Nama sesuai kartu mahasiswa",
            leadingIcon = TkOutlineIcons.Person,
            tag = "input_register_name",
            capitalization = KeyboardCapitalization.Words,
            valid = fullName.isNotBlank()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            LoginTextField(
                label = "NIM",
                value = nim,
                onValueChange = onNimChange,
                placeholder = "2023102016",
                leadingIcon = TkOutlineIcons.IdCard,
                modifier = Modifier.weight(1.25f),
                tag = "input_register_nim",
                valid = nim.isNotBlank()
            )
            RegisterSemesterField(
                selected = semester,
                onSelected = onSemesterChange,
                modifier = Modifier.weight(1f)
            )
        }

        LoginTextField(
            label = "Program studi",
            value = prodi,
            onValueChange = onProdiChange,
            placeholder = "Contoh: Teknik Informatika",
            leadingIcon = TkOutlineIcons.GraduationCap,
            tag = "input_register_prodi",
            capitalization = KeyboardCapitalization.Words,
            valid = prodi.isNotBlank()
        )
        LoginTextField(
            label = "Fakultas",
            value = faculty,
            onValueChange = onFacultyChange,
            placeholder = "Contoh: Ilmu Komputer",
            leadingIcon = TkOutlineIcons.Building,
            tag = "input_register_faculty",
            capitalization = KeyboardCapitalization.Words,
            valid = faculty.isNotBlank()
        )
        LoginTextField(
            label = "Universitas",
            value = university,
            onValueChange = onUniversityChange,
            placeholder = "Nama kampus kamu",
            leadingIcon = TkOutlineIcons.MapPin,
            tag = "input_register_university",
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            valid = university.isNotBlank()
        )

        AuthSwitchLine(
            prefix = "Sudah punya akun?",
            action = "Masuk",
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            actionTag = "btn_nav_to_login"
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Langkah 2 — Akun & sandi
// -------------------------------------------------------------------------------------------------

@Composable
private fun AccountStep(
    isPhone: Boolean,
    contact: String,
    contactValid: Boolean,
    password: String,
    confirmPassword: String,
    passVisible: Boolean,
    confirmVisible: Boolean,
    onTogglePass: () -> Unit,
    onToggleConfirm: () -> Unit,
    onContactTypeChange: (Boolean) -> Unit,
    onContactChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val mismatch = confirmPassword.isNotEmpty() && confirmPassword != password

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RegisterFieldGap)) {
        RegisterContactTabs(isPhone = isPhone, onChange = onContactTypeChange)

        if (isPhone) {
            LoginTextField(
                label = "Nomor HP",
                value = contact,
                onValueChange = { raw ->
                    onContactChange(raw.filter { it.isDigit() || it == '+' }.take(16))
                },
                placeholder = "08123456789",
                leadingIcon = TkOutlineIcons.Phone,
                tag = "input_register_contact",
                keyboardType = KeyboardType.Phone,
                valid = contactValid,
                trailing = { if (contactValid) ValidCheck() }
            )
        } else {
            LoginTextField(
                label = "Alamat email",
                value = contact,
                onValueChange = { raw -> onContactChange(raw.filterNot { it.isWhitespace() }) },
                placeholder = "nama@email.com",
                leadingIcon = TkOutlineIcons.Mail,
                tag = "input_register_contact",
                keyboardType = KeyboardType.Email,
                valid = contactValid,
                trailing = { if (contactValid) ValidCheck() }
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            LoginPasswordField(
                label = "Kata sandi",
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "Minimal 8 karakter",
                visible = passVisible,
                onToggleVisible = onTogglePass,
                tag = "input_register_password",
                imeAction = ImeAction.Next
            )
            PasswordStrengthMeter(password = password)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            LoginPasswordField(
                label = "Ulangi kata sandi",
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                placeholder = "Ketik ulang kata sandi",
                visible = confirmVisible,
                onToggleVisible = onToggleConfirm,
                tag = "input_register_password_confirm",
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                valid = confirmPassword.isNotEmpty() && !mismatch,
                invalid = mismatch && confirmPassword.length >= password.length
            )
            ConfirmPasswordHint(password = password, confirm = confirmPassword)
        }
    }
}

@Composable
private fun ValidCheck() {
    Icon(
        imageVector = TkOutlineIcons.Check,
        contentDescription = "Valid",
        tint = SuccessGreenText,
        modifier = Modifier.size(22.dp)
    )
}

// -------------------------------------------------------------------------------------------------
// Langkah 3 — Periksa data
// -------------------------------------------------------------------------------------------------

@Composable
private fun ReviewStep(
    fullName: String,
    nim: String,
    prodi: String,
    faculty: String,
    university: String,
    semester: Int,
    contactIsPhone: Boolean,
    contact: String,
    passwordLength: Int,
    agreed: Boolean,
    onAgreedChange: (Boolean) -> Unit,
    onEditPersonal: () -> Unit,
    onEditAccount: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RegisterFieldGap)) {
        ReviewProfileCard(fullName = fullName, nim = nim)

        ReviewSection(
            title = "Data kuliah",
            rows = listOf(
                "Program studi" to prodi,
                "Fakultas" to faculty,
                "Universitas" to university,
                "Semester" to "Semester $semester"
            ),
            onEdit = onEditPersonal,
            editTag = "btn_register_edit_personal"
        )

        ReviewSection(
            title = "Akun",
            rows = listOf(
                (if (contactIsPhone) "Nomor HP" else "Email") to contact,
                "Kata sandi" to "•".repeat(passwordLength.coerceIn(1, 12))
            ),
            onEdit = onEditAccount,
            editTag = "btn_register_edit_account"
        )

        ConsentRow(checked = agreed, onCheckedChange = onAgreedChange)
    }
}
