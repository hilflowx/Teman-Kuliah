package com.temankuliah.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.temankuliah.app.data.repository.UserRepository
import com.temankuliah.app.ui.icons.TkIcons

/**
 * Layar "Buat sandi" modern untuk akun yang belum punya sandi.
 */
@Composable
internal fun CreatePasswordContent(
    identifier: String,
    busy: Boolean,
    onSubmit: (password: String, confirmPassword: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    isReset: Boolean = false
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }

    val tooShort = password.isNotEmpty() && password.length < UserRepository.MIN_PASSWORD_LENGTH
    val mismatch = confirmPassword.isNotEmpty() && confirmPassword != password
    val canSubmit = !busy &&
        password.length >= UserRepository.MIN_PASSWORD_LENGTH &&
        confirmPassword == password

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isReset) "Atur ulang kata sandi" else "Buat kata sandi baru",
                color = TextMain,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.testTag("title_create_password")
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isReset) {
                    "Kunci layar terverifikasi. Tetapkan kata sandi baru untuk akun $identifier. Data akademikmu tidak berubah."
                } else {
                    "Akun $identifier belum memiliki kata sandi. Tetapkan kata sandi baru untuk mengamankan akunmu."
                },
                color = TextMuted,
                fontSize = 14.5.sp,
                lineHeight = 21.sp
            )

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AuthPasswordField(
                    label = "Kata sandi baru",
                    value = password,
                    onValueChange = { password = it },
                    visible = passwordVisible,
                    onToggleVisible = { passwordVisible = !passwordVisible },
                    tag = "input_create_password",
                    leadingIcon = TkIcons.Lock,
                    hintText = if (tooShort) "Minimal ${UserRepository.MIN_PASSWORD_LENGTH} karakter" else null,
                    hintColor = WarningOrange
                )

                AuthPasswordField(
                    label = "Konfirmasi kata sandi",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    visible = confirmVisible,
                    onToggleVisible = { confirmVisible = !confirmVisible },
                    tag = "input_create_password_confirm",
                    leadingIcon = TkIcons.Lock,
                    hintText = when {
                        confirmPassword.isEmpty() -> null
                        mismatch -> "Belum cocok"
                        else -> "✓ Cocok"
                    },
                    hintColor = if (mismatch) WarningOrange else SuccessGreen,
                    imeAction = ImeAction.Done
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuthPrimaryButton(
                text = "Simpan dan masuk",
                onClick = { onSubmit(password, confirmPassword) },
                enabled = canSubmit,
                loading = busy,
                modifier = Modifier.testTag("btn_submit_create_password")
            )

            TextButton(
                onClick = onCancel,
                enabled = !busy,
                modifier = Modifier.testTag("btn_cancel_create_password")
            ) {
                Text("← Batal dan kembali ke halaman masuk", color = BrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}
