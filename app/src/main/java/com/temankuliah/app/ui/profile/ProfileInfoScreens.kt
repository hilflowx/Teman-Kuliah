package com.temankuliah.app.ui.profile

import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.TkPalette
import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.ui.components.TkCard
import com.temankuliah.app.ui.components.TkPill
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.ui.DialogType
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.icons.Person
import com.temankuliah.app.ui.theme.TkSpace

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight

/** Sub-halaman "Biodata Mahasiswa": kartu hero ringkas, daftar data identitas, dan tombol Edit biodata yang rapi. */
@Composable
fun PersonalInfoScreen(user: UserEntity, viewModel: TemanKuliahViewModel, modifier: Modifier = Modifier) {
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val profileBitmap = rememberProfileBitmap(user)
    val initial = user.fullName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M"
    val blank = "Belum diisi"

    LazyColumn(
        modifier = modifier.fillMaxSize().background(BackgroundLight),
        contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "personal_hero") {
            Row(
                modifier = Modifier
                    .profileHeroSurface()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f)),
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
                        Text(initial, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        user.fullName.ifBlank { "Nama belum diisi" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (user.nim.isNotBlank()) "NIM ${user.nim}" else if (user.isGuest) "Mode Tamu" else "NIM belum diisi",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.24f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Semester $activeSemester",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        item(key = "personal_details") {
            TkCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Data identitas",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                "Informasi pribadi & akademik",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Surface(
                            onClick = { viewModel.openDialog(DialogType.EditProfile(user)) },
                            shape = TkShape.md,
                            color = TemanKuliahBlueLight,
                            border = BorderStroke(1.dp, tkAdaptive(Color(0xFFD0E1FD), TkPalette.colors.divider)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    TkOutlineIcons.Pencil,
                                    contentDescription = null,
                                    tint = TemanKuliahBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    "Ubah",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TemanKuliahBlue
                                )
                            }
                        }
                    }
                    ProfileDetailRow(TkOutlineIcons.Person, "Nama lengkap", user.fullName.ifBlank { blank })
                    ProfileDetailDivider()
                    ProfileDetailRow(TkOutlineIcons.Hash, "NIM", user.nim.ifBlank { blank })
                    ProfileDetailDivider()
                    ProfileDetailRow(TkOutlineIcons.GraduationCap, "Program studi", user.studyProgram.ifBlank { blank })
                    ProfileDetailDivider()
                    ProfileDetailRow(TkOutlineIcons.BookOpen, "Fakultas", user.faculty.ifBlank { blank })
                    ProfileDetailDivider()
                    ProfileDetailRow(TkOutlineIcons.Building, "Universitas", user.university.ifBlank { blank })
                }
            }
        }
    }
}

/** Sub-halaman "Informasi Akun": kartu hero status akun dan daftar detail akun. */
@Composable
fun AccountScreen(
    user: UserEntity,
    viewModel: TemanKuliahViewModel,
    modifier: Modifier = Modifier
) {
    val typeLabel = if (user.isGuest) "Mode Tamu" else "Akun Lokal TemanKuliah"
    val profileBitmap = rememberProfileBitmap(user)
    val lastBackupAt by viewModel.backupController.lastBackupAt.collectAsStateWithLifecycle()
    val initial = user.fullName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M"

    LazyColumn(
        modifier = modifier.fillMaxSize().background(BackgroundLight),
        contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "account_hero") {
            Row(
                modifier = Modifier
                    .profileHeroSurface()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f)),
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
                        Text(initial, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        user.fullName.ifBlank { "Nama belum diisi" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            typeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.24f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        item(key = "account_details") {
            TkCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        "Detail akun",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 4.dp).semantics { heading() }
                    )
                    ProfileDetailRow(TkOutlineIcons.ShieldCheck, "Jenis akun", typeLabel)
                    ProfileDetailDivider()
                    ProfileDetailRow(TkOutlineIcons.Person, "Nama", user.fullName.ifBlank { "Belum diisi" })
                    if (user.email.isNotBlank()) {
                        ProfileDetailDivider()
                        ProfileDetailRow(TkOutlineIcons.Phone, "Email / No. HP", user.email)
                    }
                    ProfileDetailDivider()
                    ProfileDetailRow(
                        TkOutlineIcons.Lock, "Penyimpanan data", "Lokal & privat",
                        badge = { TkPill("Aman", container = TemanKuliahGreenLight, content = TemanKuliahGreenDark, shape = CircleShape) }
                    )
                    ProfileDetailDivider()
                    ProfileDetailRow(TkOutlineIcons.Calendar, "Akun dibuat", formatLongDate(user.createdAt))
                    ProfileDetailDivider()
                    ProfileDetailRow(TkOutlineIcons.CloudUpload, "Terakhir dicadangkan", formatLastBackup(lastBackupAt))
                }
            }
        }
    }
}
