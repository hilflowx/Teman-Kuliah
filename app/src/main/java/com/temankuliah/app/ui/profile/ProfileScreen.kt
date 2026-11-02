package com.temankuliah.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.BuildConfig
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.ui.components.TkPageHeader
import com.temankuliah.app.ui.ProfileSection
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.TkGroupedList
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.TkText
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TemanKuliahRedLight
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkSpace

/**
 * Halaman Profil (desain terbaru):
 * - Judul besar + keterangan
 * - Kartu hero gradien biru: avatar inisial/foto, identitas, serta pil Semester dan status
 * - Tiga kelompok menu: Akun, Preferensi, Data & Aplikasi
 * - Tombol Keluar dipisahkan di kartu sendiri di paling bawah
 */
@Composable
fun ProfileScreen(
    user: UserEntity,
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier,
    onOpenSection: (ProfileSection) -> Unit
) {
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showAvatarMenu by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel.profileController::setProfileImage)
    }
    val profileBitmap = rememberProfileBitmap(user)
    val pickPhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(BackgroundLight),
        contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "profile_header") {
            TkPageHeader(
                title = "Profil",
                supporting = "Kelola akun, preferensi, dan data aplikasi",
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        item(key = "profile_user_card") {
            ProfileHeroCard(
                user = user,
                activeSemester = activeSemester,
                profileBitmap = profileBitmap,
                showAvatarMenu = showAvatarMenu,
                onAvatarClick = { if (profileBitmap == null) pickPhoto() else showAvatarMenu = true },
                onCameraClick = { pickPhoto() },
                onDismissAvatarMenu = { showAvatarMenu = false },
                onChangePhoto = { showAvatarMenu = false; pickPhoto() },
                onRemovePhoto = { showAvatarMenu = false; viewModel.profileController.removeProfileImage() }
            )
        }

        item(key = "group_account_label") { ProfileSectionLabel("Akun") }
        item(key = "group_account_card") {
            TkGroupedList {
                ProfileMenuRow(
                    icon = TkOutlineIcons.IdCard,
                    title = "Biodata Mahasiswa",
                    subtitle = "NIM, program studi, dan fakultas",
                    onClick = { onOpenSection(ProfileSection.PERSONAL) }
                )
                ProfileMenuDivider()
                ProfileMenuRow(
                    icon = TkOutlineIcons.Person,
                    title = "Informasi Akun",
                    subtitle = if (user.isGuest) "Mode tamu di perangkat ini" else "Akun lokal di perangkat ini",
                    onClick = { onOpenSection(ProfileSection.ACCOUNT) }
                )
            }
        }

        item(key = "group_pref_label") { ProfileSectionLabel("Preferensi") }
        item(key = "group_pref_card") {
            TkGroupedList {
                ProfileMenuRow(
                    icon = TkOutlineIcons.Bell,
                    title = "Pengingat",
                    subtitle = "Jadwal kuliah dan tenggat tugas",
                    onClick = { onOpenSection(ProfileSection.NOTIFICATIONS) }
                )
                ProfileMenuDivider()
                ProfileMenuRow(
                    icon = TkOutlineIcons.Palette,
                    title = "Tema & Tampilan",
                    subtitle = "Mode terang/gelap dan animasi",
                    onClick = { onOpenSection(ProfileSection.APPEARANCE) }
                )
            }
        }

        item(key = "group_about_label") { ProfileSectionLabel("Data & Aplikasi") }
        item(key = "group_about_card") {
            TkGroupedList {
                ProfileMenuRow(
                    icon = TkOutlineIcons.ShieldCheck,
                    title = "Cadangkan & Pulihkan",
                    subtitle = "Simpan dan pulihkan data kuliahmu",
                    onClick = { onOpenSection(ProfileSection.PRIVACY) }
                )
                ProfileMenuDivider()
                ProfileMenuRow(
                    icon = TkOutlineIcons.InfoCircle,
                    title = "Tentang Aplikasi",
                    subtitle = "Versi ${BuildConfig.VERSION_NAME}",
                    onClick = { onOpenSection(ProfileSection.ABOUT) }
                )
            }
        }

        item(key = "group_logout_card") {
            TkGroupedList(Modifier.padding(top = 8.dp)) {
                ProfileMenuRow(
                    icon = TkOutlineIcons.Logout,
                    title = "Keluar",
                    subtitle = "Keluar dari akun di perangkat ini",
                    tileColor = TemanKuliahRedLight,
                    iconTint = TemanKuliahRedDark,
                    titleColor = TemanKuliahRedDark,
                    onClick = { showLogoutConfirm = true }
                )
            }
        }
    }

    if (showLogoutConfirm) com.temankuliah.app.ui.components.TkAlertDialog(
        onDismissRequest = { showLogoutConfirm = false },
        title = "Keluar dari TemanKuliah?",
        text = if (user.isGuest) "Sesi tamu akan diakhiri. Data tamu tetap tersimpan di perangkat dan terbuka lagi saat Anda masuk sebagai tamu." else "Sesi login akan diakhiri. Profil, nilai, jadwal, tugas, foto, dan dokumen lokal tetap tersimpan di perangkat.",
        confirmLabel = "Keluar",
        isDestructive = true,
        onConfirm = {
            showLogoutConfirm = false
            viewModel.authController.logout()
        }
    )
}

/** Kartu hero gradien biru berisi avatar, identitas mahasiswa, serta pil semester dan status. */
@Composable
private fun ProfileHeroCard(
    user: UserEntity,
    activeSemester: Int,
    profileBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    showAvatarMenu: Boolean,
    onAvatarClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDismissAvatarMenu: () -> Unit,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    val onBlue = Color.White
    val onBlueSoft = Color.White.copy(alpha = 0.92f)
    val pillColor = Color.White.copy(alpha = 0.22f)
    val initial = user.fullName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M"

    Column(
        modifier = Modifier
            .profileHeroSurface()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(Modifier.size(84.dp)) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.26f))
                        .clickable(role = Role.Button, onClick = onAvatarClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileBitmap != null) {
                        Image(
                            profileBitmap,
                            contentDescription = "Foto profil ${user.fullName}",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            initial,
                            color = onBlue,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { contentDescription = "Tambahkan foto profil" }
                        )
                    }
                }

                // Lencana kamera putih di sudut kanan bawah avatar
                // Area sentuh 48dp; lencana visual tetap 28dp di tengahnya.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onCameraClick),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            TkOutlineIcons.Camera,
                            contentDescription = "Ganti foto profil",
                            tint = TemanKuliahBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(expanded = showAvatarMenu, onDismissRequest = onDismissAvatarMenu) {
                    DropdownMenuItem(text = { Text("Ganti Foto") }, onClick = onChangePhoto)
                    DropdownMenuItem(text = { Text("Hapus Foto") }, onClick = onRemovePhoto)
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    user.fullName.ifBlank { "Nama belum diisi" },
                    style = TkText.screenTitle,
                    fontWeight = FontWeight.Bold,
                    color = onBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        user.nim.isNotBlank() -> "NIM ${user.nim}"
                        user.email.isNotBlank() -> user.email
                        user.isGuest -> "Mode Tamu"
                        else -> "NIM belum diisi"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBlueSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    user.studyProgram.ifBlank { "Program studi belum diisi" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBlueSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroPill("Semester $activeSemester", Modifier.weight(1f), pillColor)
            if (!user.isGuest) {
                HeroPill("Mahasiswa aktif", Modifier.weight(1f), pillColor, showDot = true)
            }
        }
    }
}

/** Membungkus sub-halaman Profil dengan tombol kembali rounded square dan judul. */
@Composable
fun ProfileSubPage(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Column(modifier.fillMaxSize().background(BackgroundLight)) {
        ProfileSubHeader(title = title, supporting = supporting, onBack = onBack)
        content(Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
internal fun ProfileSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = modifier.padding(start = 6.dp).semantics { heading() }
    )
}
