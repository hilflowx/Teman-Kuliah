package com.temankuliah.app.ui.profile

import com.temankuliah.app.ui.theme.TemanKuliahRedFill
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.BuildConfig
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.TkCard
import com.temankuliah.app.ui.components.TkIconTile
import com.temankuliah.app.ui.components.TkPill
import com.temankuliah.app.ui.icons.CheckCircle
import com.temankuliah.app.ui.icons.Info
import com.temankuliah.app.ui.icons.School
import com.temankuliah.app.ui.theme.TkText
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkGradientEnd
import com.temankuliah.app.ui.theme.TkGradientStart
import com.temankuliah.app.ui.theme.TkShape
import com.temankuliah.app.ui.theme.TkSpace
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import com.temankuliah.app.data.backup.BackupCrypto
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import java.time.LocalDate
import com.temankuliah.app.ui.theme.tkShadow

/**
 * "Privasi dan Keamanan": Penyimpanan lokal di perangkat (tanpa biaya server)
 * dan cadangan file ZIP lokal.
 */
@Composable
fun PrivacySecurityScreen(viewModel: TemanKuliahViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val backupPreview by viewModel.backupController.backupPreview.collectAsStateWithLifecycle()
    val backupBusy by viewModel.backupController.backupBusy.collectAsStateWithLifecycle()

    // Peluncur dokumen lokal SAF (.tkbackup)
    // Sandi cadangan (BL-19): diminta sebelum memilih lokasi simpan, lalu dipakai satu kali untuk enkripsi.
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var exportPasswordConfirm by remember { mutableStateOf("") }
    var pendingExportPassword by remember { mutableStateOf<CharArray?>(null) }
    var importPassword by remember { mutableStateOf("") }
    val backupPasswordRequest by viewModel.backupController.backupPasswordRequest.collectAsStateWithLifecycle()
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val password = pendingExportPassword
        pendingExportPassword = null
        if (uri != null && password != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            viewModel.backupController.exportBackup(uri, password)
        } else {
            password?.fill('\u0000')
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selected ->
            runCatching { context.contentResolver.takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.backupController.previewBackup(selected)
        }
    }

    val lastBackupAt by viewModel.backupController.lastBackupAt.collectAsStateWithLifecycle()
    val launchExport = { showExportPasswordDialog = true }
    val launchImport = { importPicker.launch(arrayOf("application/octet-stream", "application/zip", "application/x-zip-compressed", "*/*")) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(BackgroundLight),
        contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = TkSpace.sm, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kartu status
        item(key = "backup_hero") {
            Column(
                modifier = Modifier.profileHeroSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(52.dp).clip(TkShape.lg).background(Color.White.copy(alpha = 0.26f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(TkOutlineIcons.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Data tersimpan di perangkat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "100% lokal · format .tkbackup",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Terakhir dicadangkan",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatLastBackup(lastBackupAt),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item(key = "actions_label") { ProfileSectionLabel("Aksi", Modifier.padding(top = 8.dp)) }

        // Dua kartu aksi sama tinggi
        item(key = "actions_cards") {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackupActionCard(
                    title = "Ekspor data",
                    subtitle = "Ekspor semua data ke file .tkbackup",
                    icon = TkOutlineIcons.CloudUpload,
                    primary = true,
                    busy = backupBusy,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { launchExport() }
                )
                BackupActionCard(
                    title = "Pulihkan data",
                    subtitle = "Pilih file .tkbackup dari perangkat",
                    icon = TkOutlineIcons.History,
                    primary = false,
                    busy = false,
                    enabled = !backupBusy,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { launchImport() }
                )
            }
        }

        if (backupBusy) {
            item(key = "backup_busy") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = TemanKuliahBlue)
                    Spacer(Modifier.width(10.dp))
                    Text("Memproses data lokal…", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        // Catatan
        item(key = "backup_note") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(TkShape.card)
                    .background(TemanKuliahBlueLight)
                    .padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(TkOutlineIcons.InfoCircle, contentDescription = null, tint = TemanKuliahBlueDark, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
                Text(
                    "Simpan file .tkbackup di tempat aman, misalnya Drive, agar mudah dipindahkan ke perangkat lain.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TemanKuliahBlueDark
                )
            }
        }
    }

    // Dialog sandi cadangan untuk ekspor (enkripsi wajib)
    if (showExportPasswordDialog) {
        val tooShort = exportPassword.length < BackupCrypto.MIN_PASSWORD_LENGTH
        val mismatch = exportPassword != exportPasswordConfirm
        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false; exportPassword = ""; exportPasswordConfirm = "" },
            title = { Text("Sandi cadangan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cadangan dienkripsi dengan sandi ini. Simpan sandinya baik-baik: tanpa sandi, cadangan tidak bisa dibuka.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    OutlinedTextField(
                        value = exportPassword, onValueChange = { exportPassword = it }, singleLine = true,
                        label = { Text("Sandi cadangan (min. ${BackupCrypto.MIN_PASSWORD_LENGTH} karakter)") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = exportPasswordConfirm, onValueChange = { exportPasswordConfirm = it }, singleLine = true,
                        label = { Text("Ulangi sandi cadangan") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = exportPasswordConfirm.isNotEmpty() && mismatch
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !tooShort && !mismatch,
                    onClick = {
                        pendingExportPassword = exportPassword.toCharArray()
                        showExportPasswordDialog = false
                        exportPassword = ""
                        exportPasswordConfirm = ""
                        exportPicker.launch("temankuliah_backup_${LocalDate.now()}.tkbackup")
                    }
                ) { Text("Lanjut") }
            },
            dismissButton = { TextButton(onClick = { showExportPasswordDialog = false; exportPassword = ""; exportPasswordConfirm = "" }) { Text("Batal") } }
        )
    }

    // Dialog sandi untuk cadangan terenkripsi saat impor
    backupPasswordRequest?.let { requestUri ->
        AlertDialog(
            onDismissRequest = { importPassword = ""; viewModel.backupController.cancelBackupPassword() },
            title = { Text("Cadangan terenkripsi") },
            text = {
                OutlinedTextField(
                    value = importPassword, onValueChange = { importPassword = it }, singleLine = true,
                    label = { Text("Sandi cadangan") }, visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                Button(
                    enabled = importPassword.isNotEmpty() && !backupBusy,
                    onClick = {
                        viewModel.backupController.previewBackup(requestUri, importPassword.toCharArray())
                        importPassword = ""
                    }
                ) { Text("Buka") }
            },
            dismissButton = { TextButton(onClick = { importPassword = ""; viewModel.backupController.cancelBackupPassword() }) { Text("Batal") } }
        )
    }

    // Dialog Konfirmasi Pemulihan Cadangan Lokal (.tkbackup)
    if (backupPreview != null) {
        val summary = backupPreview!!.summary
        AlertDialog(
            onDismissRequest = viewModel.backupController::cancelBackupRestore,
            title = { Text("Konfirmasi Pemulihan Data") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("File cadangan .tkbackup tervalidasi. Data lokal saat ini akan diganti oleh isi cadangan ini:", fontWeight = FontWeight.SemiBold)
                    Text("• Akun: ${summary.users}\n• Mata kuliah: ${summary.courses}\n• Jadwal: ${summary.schedules}\n• Tugas: ${summary.tasks}\n• Proyek kuliah: ${summary.projects}\n• Tahapan proyek: ${summary.milestones}\n• Dokumen KRS/KHS: ${summary.documents}\n• Lampiran: ${summary.attachments}")
                    Text("Proses pemulihan menggunakan transaksi database yang aman.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            },
            confirmButton = { Button(onClick = viewModel.backupController::restorePendingBackup, enabled = !backupBusy, colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahRedFill)) { Text("Ganti Data Lokal") } },
            dismissButton = { TextButton(onClick = viewModel.backupController::cancelBackupRestore) { Text("Batal") } }
        )
    }
}

/** "Tentang Aplikasi" sub-page. */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = TkSpace.md, bottom = TkSpace.xl),
        verticalArrangement = Arrangement.spacedBy(TkSpace.gap)
    ) {
        item {
            TkCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TkIconTile(Icons.Default.School, size = 54.dp, shape = TkShape.lg)
                    Text("TemanKuliah", style = TkText.screenTitle, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Asisten produktivitas akademik mahasiswa", style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
                    TkPill("Versi ${BuildConfig.VERSION_NAME}")
                }
            }
        }
        item { ProfileSection("Tentang Aplikasi") {
            Text(
                "TemanKuliah membantu mengelola jadwal perkuliahan, tugas, proyek, nilai, dan dokumen akademik dalam satu aplikasi.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            ProfileDivider()
            ProfileAction(Icons.Default.Info, "Informasi versi", "Versi aplikasi yang terpasang", trailing = BuildConfig.VERSION_NAME)
            ProfileDivider()
            ProfileAction(Icons.Default.School, "Teknologi", "Kotlin · Jetpack Compose Material 3 · Room SQLite")
            ProfileDivider()
            ProfileAction(Icons.Default.CheckCircle, "Penyimpanan", "100% Offline-First · Arsip Lokal .tkbackup")
            Text(
                "Komponen Kotlin/AndroidX/Room didistribusikan sesuai lisensi Apache-2.0; komponen transitif mengikuti lisensi masing-masing paket.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        } }
    }
}

/** Kartu aksi cadangan: [primary] berlatar gradien biru, lainnya putih berbingkai. */
@Composable
private fun BackupActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean,
    busy: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = !busy,
    onClick: () -> Unit
) {
    val shape = TkShape.card
    val surface = if (primary) {
        modifier
            .shadow(tkShadow(6.dp), shape, ambientColor = TkGradientEnd.copy(alpha = 0.20f), spotColor = TkGradientEnd.copy(alpha = 0.35f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(TkGradientStart, TkGradientEnd)))
    } else {
        modifier.clip(shape).background(SurfaceLight).border(1.dp, DividerLight, shape)
    }
    Column(
        surface.clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(TkShape.md)
                .background(if (primary) Color.White.copy(alpha = 0.26f) else TemanKuliahBlueLight),
            contentAlignment = Alignment.Center
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(icon, contentDescription = null, tint = if (primary) Color.White else TemanKuliahBlue, modifier = Modifier.size(20.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (primary) Color.White else TextPrimary
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (primary) Color.White.copy(alpha = 0.92f) else TextSecondary
            )
        }
    }
}
