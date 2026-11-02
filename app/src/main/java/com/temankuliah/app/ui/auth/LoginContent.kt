package com.temankuliah.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.temankuliah.app.ui.components.AppLogoBadge
import com.temankuliah.app.ui.components.LightSystemBarIcons
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.Poppins

// Ukuran mengikuti mockup (1080 px = 360 dp): lembar putih mulai di y = 262 dp dengan sudut atas 30 dp.
private val SheetCorner = 30.dp

/** Tinggi isi header di bawah status bar: 36 (atas) + 82 (logo) + 10 + 28 (judul) + 4 + 20 (subjudul) + 58. */
private val HeaderBodyHeight = 238.dp
private val HeaderCircleColor = Color.White.copy(alpha = 0.07f)

/**
 * Layar Masuk bergaya mockup baru: header gradasi biru (logo, judul, subjudul) dan lembar putih
 * bersudut atas membulat berisi form, tombol Masuk, Masuk sebagai Tamu, tautan Daftar, dan catatan
 * persetujuan di dasar layar. Layar penuh (menggambar di bawah status bar), bisa di-scroll saat
 * keyboard terbuka atau layar pendek.
 */
@Composable
internal fun LoginContent(
    busy: Boolean,
    errorMessage: String?,
    onLogin: (email: String, pass: String) -> Unit,
    onGuestSignIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    onForgotPassword: (identifier: String) -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var showForgotDialog by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LightSystemBarIcons()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AuthSurface)
            .imePadding()
    ) {
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val headerHeight = statusTop + HeaderBodyHeight
        // Lembar menutupi sisa layar; SheetCorner ditambahkan karena lembar menimpa bagian bawah header.
        val sheetMinHeight = (maxHeight - headerHeight + SheetCorner).coerceAtLeast(0.dp)

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            LoginHeader(height = headerHeight)

            Column(
                modifier = Modifier
                    .overlapTop(SheetCorner)
                    .fillMaxWidth()
                    .heightIn(min = sheetMinHeight)
                    .background(AuthSurface, RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner))
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (errorMessage != null) {
                        AuthErrorBanner(errorMessage)
                        Spacer(Modifier.height(14.dp))
                    }

                    LoginTextField(
                        label = "Email, nomor HP, atau NIM",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "nama@email.com",
                        leadingIcon = TkOutlineIcons.Mail,
                        tag = "input_login_email",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                    Spacer(Modifier.height(12.dp))
                    LoginPasswordField(
                        label = "Kata sandi",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Masukkan kata sandi",
                        visible = passwordVisible,
                        onToggleVisible = { passwordVisible = !passwordVisible },
                        tag = "input_login_password",
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (!busy) onLogin(email, password)
                            }
                        )
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = "Lupa kata sandi?",
                            color = BrandBlue,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clickable(role = Role.Button) { showForgotDialog = true }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                .testTag("btn_forgot_password")
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    LoginPrimaryButton(
                        text = "Masuk",
                        onClick = {
                            focusManager.clearFocus()
                            onLogin(email, password)
                        },
                        enabled = !busy,
                        loading = busy,
                        modifier = Modifier.testTag("btn_submit_login")
                    )

                    Spacer(Modifier.height(12.dp))
                    LoginOrDivider("atau")
                    Spacer(Modifier.height(12.dp))

                    LoginOutlinedButton(
                        text = "Masuk sebagai Tamu",
                        leadingIcon = TkOutlineIcons.Person,
                        onClick = onGuestSignIn,
                        enabled = !busy,
                        modifier = Modifier.testTag("btn_guest_login")
                    )

                    Spacer(Modifier.height(16.dp))
                    AuthSwitchLine(
                        prefix = "Belum punya akun?",
                        action = "Daftar sekarang",
                        onClick = onNavigateToRegister,
                        actionTag = "btn_nav_to_register"
                    )
                }

                Text(
                    text = "Dengan masuk, kamu menyetujui\nSyarat & Ketentuan dan Kebijakan Privasi",
                    color = TextSubtle,
                    fontFamily = Poppins,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 28.dp, bottom = 20.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Lupa kata sandi?") },
            text = {
                Text(
                    "Isi email, nomor HP, atau NIM akunmu di kolom masuk, lalu verifikasi dengan kunci layar HP " +
                        "(PIN, pola, atau sandi) untuk membuat kata sandi baru. Data akademikmu tetap aman."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgotDialog = false
                        focusManager.clearFocus()
                        onForgotPassword(email)
                    },
                    modifier = Modifier.testTag("btn_forgot_verify")
                ) { Text("Verifikasi") }
            },
            dismissButton = { TextButton(onClick = { showForgotDialog = false }) { Text("Batal") } }
        )
    }
}

/** Header biru: gradasi, dua lingkaran dekoratif statis, logo, judul, dan subjudul. */
@Composable
private fun LoginHeader(height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .background(Brush.verticalGradient(listOf(Color(0xFF3A8DF2), Color(0xFF1F66D0))))
    ) {
        // Lingkaran kanan atas: pusat (lebar - 40, 50) dp, jari-jari 130 dp.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-80).dp)
                .size(260.dp)
                .background(HeaderCircleColor, CircleShape)
        )
        // Lingkaran kiri: pusat (30, 220) dp, jari-jari 100 dp.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-70).dp, y = 120.dp)
                .size(200.dp)
                .background(HeaderCircleColor, CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppLogoBadge(logoSize = 77.dp, ringWidth = 2.5.dp, contentDescription = "Logo TemanKuliah")
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Selamat Datang Kembali",
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Masuk untuk kelola jadwal, tugas, & IPK kamu",
                color = Color.White.copy(alpha = 0.92f),
                fontFamily = Poppins,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

/** Menumpuk elemen ini sejauh [overlap] ke atas elemen sebelumnya (tinggi total berkurang sebesar itu). */
internal fun Modifier.overlapTop(overlap: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = overlap.roundToPx().coerceAtMost(placeable.height)
    layout(placeable.width, placeable.height - shift) { placeable.place(0, -shift) }
}
