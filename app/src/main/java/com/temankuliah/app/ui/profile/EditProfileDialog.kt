package com.temankuliah.app.ui.profile

import com.temankuliah.app.ui.components.TkFormSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.OutlinedTextField
import com.temankuliah.app.ui.components.TkFormError
import com.temankuliah.app.ui.theme.TkText
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TextSecondary

@Composable
fun EditProfileDialog(
    user: UserEntity,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (fullName: String, nim: String, studyProgram: String, faculty: String, university: String) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var nim by remember { mutableStateOf(user.nim) }
    var studyProgram by remember { mutableStateOf(user.studyProgram) }
    var faculty by remember { mutableStateOf(user.faculty) }
    var university by remember { mutableStateOf(user.university) }

    TkFormSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Perbarui Profil Mahasiswa",
                    style = TkText.screenTitle
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    isError = fullName.isBlank(),
                    supportingText = if (fullName.isBlank()) { { Text("Nama wajib diisi.") } } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nim,
                    onValueChange = { nim = it },
                    label = { Text("NIM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = studyProgram,
                    onValueChange = { studyProgram = it },
                    label = { Text("Program Studi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Fakultas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = university,
                    onValueChange = { university = it },
                    label = { Text("Universitas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (fullName.isBlank()) {
                    Text("NIM, program studi, fakultas, dan universitas boleh diisi nanti.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                TkFormError(errorMessage)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (fullName.isNotBlank()) {
                                onSave(fullName, nim, studyProgram, faculty, university)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue)
                    ) {
                        Text("Simpan Perubahan")
                    }
                }
            }
        }
}
