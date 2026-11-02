package com.temankuliah.app.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.core.net.toUri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.stateDescription
import com.temankuliah.app.ui.theme.TkGradientStart
import com.temankuliah.app.ui.theme.TkGradientEnd
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.components.TkGroupedList
import com.temankuliah.app.ui.components.TkCard
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.app.NotificationManagerCompat
import com.temankuliah.app.data.notifications.ReminderScheduler
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.components.TkIconTile
import com.temankuliah.app.ui.icons.Palette
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.components.AnimationMode
import com.temankuliah.app.ui.components.rememberReducedMotion
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahOrangeLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.DarkTkColors
import com.temankuliah.app.ui.theme.LightTkColors
import com.temankuliah.app.ui.theme.ThemeMode
import com.temankuliah.app.ui.theme.TkPalette
import com.temankuliah.app.ui.theme.TkShape
import com.temankuliah.app.ui.theme.TkText
import com.temankuliah.app.ui.theme.TkSpace
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.role
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TemanKuliahOrangeLight
import kotlinx.coroutines.launch
import android.provider.Settings as AndroidSettings
import com.temankuliah.app.ui.theme.tkShadow

/** Sub-halaman "Pengingat": status, jadwal pengingat, perilaku notifikasi, izin Android, dan tombol simpan. */
@Composable
fun NotificationSettingsScreen(viewModel: TemanKuliahViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val initial by viewModel.preferencesController.reminderPreferences.collectAsStateWithLifecycle()
    var tasksEnabled by remember(initial) { mutableStateOf(initial.taskNotificationsEnabled) }
    var classesEnabled by remember(initial) { mutableStateOf(initial.classRemindersEnabled) }
    var soundEnabled by remember(initial) { mutableStateOf(initial.soundEnabled) }
    var vibrationEnabled by remember(initial) { mutableStateOf(initial.vibrationEnabled) }
    var minutes by remember(initial) { mutableIntStateOf(initial.reminderMinutes) }

    val savePreferencesInstantly = { tasks: Boolean, classes: Boolean, sound: Boolean, vibration: Boolean, mins: Int, feedback: String? ->
        tasksEnabled = tasks
        classesEnabled = classes
        soundEnabled = sound
        vibrationEnabled = vibration
        minutes = mins

        val updated = ReminderPreferences(
            taskNotificationsEnabled = tasks,
            classRemindersEnabled = classes,
            soundEnabled = sound,
            vibrationEnabled = vibration,
            reminderMinutes = mins
        )
        viewModel.preferencesController.updateReminderPreferences(updated, showSnackbar = false)
        if (feedback != null) {
            Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
        }
    }
    // Status izin dibaca ulang setiap layar kembali aktif, karena pengguna bisa mengubahnya di pengaturan sistem.
    var exactAlarmAllowed by remember { mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context)) }
    var fullScreenAllowed by remember { mutableStateOf(ReminderScheduler.canUseFullScreenIntent(context)) }
    var notificationsAllowedNow by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        exactAlarmAllowed = ReminderScheduler.canScheduleExactAlarms(context)
        fullScreenAllowed = ReminderScheduler.canUseFullScreenIntent(context)
        notificationsAllowedNow = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) runCatching {
            val intent = if (Build.VERSION.SDK_INT >= 26) {
                Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
            }
            context.startActivity(intent)
        }
    }
    val openNotificationSettings = {
        val notificationsAllowed = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else {
            val intent = if (Build.VERSION.SDK_INT >= 26) Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
            else Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
            runCatching { context.startActivity(intent) }
        }
    }
    val openExactAlarmSettings = {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
        } else {
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
        }
        runCatching { context.startActivity(intent) }
        Unit
    }

    val openFullScreenSettings = {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${context.packageName}".toUri())
        } else {
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
        }
        runCatching { context.startActivity(intent) }
        Unit
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .imePadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = TkSpace.sm, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Kartu status
        item(key = "reminder_hero") {
            Row(
                modifier = Modifier.profileHeroSurface().padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier.size(52.dp).clip(TkShape.lg).background(Color.White.copy(alpha = 0.26f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(TkOutlineIcons.Bell, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (tasksEnabled || classesEnabled) "Pengingat aktif" else "Semua pengingat nonaktif",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "${if (tasksEnabled) "Tugas aktif" else "Tugas mati"} · ${if (classesEnabled) "Kuliah aktif" else "Kuliah mati"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }
        }

        if (!notificationsAllowedNow || !exactAlarmAllowed) {
            item(key = "reminder_permission_warning") {
                Surface(
                    shape = TkShape.card,
                    color = TemanKuliahOrangeLight,
                    border = BorderStroke(1.dp, TemanKuliahOrangeDark.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(TkShape.md)
                                .background(TemanKuliahOrangeDark.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(TkOutlineIcons.InfoCircle, contentDescription = null, tint = TemanKuliahOrangeDark, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (!notificationsAllowedNow) "Notifikasi belum diizinkan" else "Alarm presisi belum diizinkan",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (!notificationsAllowedNow) "Pengingat tidak akan tampil sampai notifikasi diizinkan. Buka \"Pengaturan notifikasi\" di bagian Izin Android di bawah."
                                else "Pengingat tetap berjalan, tetapi bisa terlambat beberapa menit. Buka \"Alarm presisi\" di bagian Izin Android di bawah.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Petunjuk Otomatis Disimpan & Sakelar Kanan/Kiri
        item(key = "auto_save_info") {
            Surface(
                shape = TkShape.card,
                color = TemanKuliahBlueLight.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, TemanKuliahBlue.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(TemanKuliahBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TemanKuliahBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Preferensi Otomatis Disimpan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Kanan = ON (Aktif) • Kiri = OFF (Nonaktif). Perubahan langsung tersimpan otomatis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Jadwal pengingat
        item(key = "reminder_schedule") {
            TkCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        "Jadwal pengingat",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 6.dp).semantics { heading() }
                    )
                    ReminderToggleRow(
                        icon = TkOutlineIcons.Clipboard,
                        title = "Pengingat tugas",
                        subtitle = "Ingatkan sebelum tenggat tugas",
                        checked = tasksEnabled,
                        onCheckedChange = { on ->
                            savePreferencesInstantly(
                                on,
                                classesEnabled,
                                soundEnabled,
                                vibrationEnabled,
                                minutes,
                                "Pengingat tugas: ${if (on) "Aktif (ON)" else "Nonaktif (OFF)"} • Tersimpan otomatis"
                            )
                        }
                    )
                    ProfileDetailDivider()
                    ReminderToggleRow(
                        icon = TkOutlineIcons.Calendar,
                        title = "Pengingat kuliah",
                        subtitle = "Alarm untuk jadwal kuliah",
                        checked = classesEnabled,
                        onCheckedChange = { on ->
                            savePreferencesInstantly(
                                tasksEnabled,
                                on,
                                soundEnabled,
                                vibrationEnabled,
                                minutes,
                                "Pengingat kuliah: ${if (on) "Aktif (ON)" else "Nonaktif (OFF)"} • Tersimpan otomatis"
                            )
                        }
                    )
                    ProfileDetailDivider()
                    Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Ingatkan sebelum tenggat",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            "Pilih jeda waktu alarm sebelum jam kegiatan dimulai:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 30).forEach { option ->
                            ReminderMinuteChip(
                                option,
                                selected = minutes == option,
                                modifier = Modifier.weight(1f)
                            ) {
                                savePreferencesInstantly(
                                    tasksEnabled,
                                    classesEnabled,
                                    soundEnabled,
                                    vibrationEnabled,
                                    option,
                                    "Jeda pengingat $option menit • Tersimpan otomatis"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Perilaku notifikasi (Bunyi dan Getaran) - Tampilan penuh agar teks tidak terpotong
        item(key = "behavior_label") { ProfileSectionLabel("Perilaku notifikasi", Modifier.padding(top = 8.dp)) }
        item(key = "behavior_cards") {
            TkCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ReminderToggleRow(
                        icon = TkOutlineIcons.Volume,
                        title = "Bunyi dan nada dering",
                        subtitle = "Putar suara saat alarm dan pengingat aktif",
                        checked = soundEnabled,
                        onCheckedChange = { on ->
                            savePreferencesInstantly(
                                tasksEnabled,
                                classesEnabled,
                                on,
                                vibrationEnabled,
                                minutes,
                                "Bunyi: ${if (on) "Aktif (ON)" else "Nonaktif (OFF)"} • Tersimpan otomatis"
                            )
                        }
                    )
                    ProfileDetailDivider()
                    ReminderToggleRow(
                        icon = TkOutlineIcons.Vibrate,
                        title = "Getaran pengingat",
                        subtitle = "Getarkan perangkat saat notifikasi berbunyi",
                        checked = vibrationEnabled,
                        onCheckedChange = { on ->
                            savePreferencesInstantly(
                                tasksEnabled,
                                classesEnabled,
                                soundEnabled,
                                on,
                                minutes,
                                "Getaran: ${if (on) "Aktif (ON)" else "Nonaktif (OFF)"} • Tersimpan otomatis"
                            )
                        }
                    )
                }
            }
        }


        // Izin Android
        item(key = "permission_label") { ProfileSectionLabel("Izin Android", Modifier.padding(top = 8.dp)) }
        item(key = "permission_card") {
            TkGroupedList {
                ProfileMenuRow(
                    icon = TkOutlineIcons.Bell,
                    title = "Pengaturan notifikasi",
                    subtitle = "Pastikan notifikasi diizinkan",
                    onClick = { openNotificationSettings() }
                )
                ProfileMenuDivider()
                ProfileMenuRow(
                    icon = TkOutlineIcons.AlarmClock,
                    title = "Alarm presisi",
                    subtitle = "Agar pengingat tepat waktu",
                    onClick = { openExactAlarmSettings() }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ProfileMenuDivider()
                    ProfileMenuRow(
                        icon = TkOutlineIcons.AlarmClock,
                        title = "Layar penuh alarm",
                        subtitle = if (fullScreenAllowed) "Diizinkan" else "Belum diizinkan, alarm hanya tampil sebagai notifikasi",
                        onClick = { openFullScreenSettings() }
                    )
                }
            }
        }

        // Keandalan Sistem & Baterai OEM (Tahap 5 PRD)
        item(key = "oem_reliability_label") { ProfileSectionLabel("Keandalan Sistem & Baterai", Modifier.padding(top = 8.dp)) }
        item(key = "oem_reliability_card") {
            OemReliabilityCard(
                onOpenExactAlarmSettings = { openExactAlarmSettings() }
            )
        }

        // Tes pengingat
        item(key = "test_label") { ProfileSectionLabel("Tes", Modifier.padding(top = 8.dp)) }
        item(key = "reminder_test") {
            val hasUnsavedChanges = tasksEnabled != initial.taskNotificationsEnabled ||
                classesEnabled != initial.classRemindersEnabled ||
                soundEnabled != initial.soundEnabled ||
                vibrationEnabled != initial.vibrationEnabled ||
                minutes != initial.reminderMinutes
            ReminderTestCard(hasUnsavedChanges = hasUnsavedChanges)
        }
    }
}

/** Baris pengingat: ubin ikon, judul, keterangan, badge status ON/OFF, dan sakelar di kanan (Kanan = ON, Kiri = OFF). */
@Composable
private fun ReminderToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(TkShape.md)
                .background(if (checked) TemanKuliahBlueLight else DividerLight.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (checked) TemanKuliahBlue else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Label status eksplisit: Kanan = ON, Kiri = OFF
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (checked) TemanKuliahBlue.copy(alpha = 0.12f) else DividerLight.copy(alpha = 0.6f)
            ) {
                Text(
                    text = if (checked) "ON" else "OFF",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (checked) TemanKuliahBlue else TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            ProfileSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.semantics {
                    stateDescription = if (checked) "Aktif, sakelar di kanan (ON)" else "Nonaktif, sakelar di kiri (OFF)"
                }
            )
        }
    }
}

/** Pil pilihan menit: terpilih berlatar gradien biru dengan bayangan, lainnya putih berbingkai. */
@Composable
private fun ReminderMinuteChip(minutes: Int, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = TkShape.md
    val base = modifier.heightIn(min = 40.dp)
    val styled = if (selected) {
        base
            .shadow(tkShadow(3.dp), shape, ambientColor = TkGradientEnd.copy(alpha = 0.20f), spotColor = TkGradientEnd.copy(alpha = 0.35f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(TkGradientStart, TkGradientEnd)))
    } else {
        base.clip(shape).background(SurfaceLight).border(1.dp, DividerLight, shape)
    }
    Box(
        styled.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$minutes mnt",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

/** Sub-halaman "Tampilan": kartu tema aktif, pilihan Terang/Gelap, opsi ikuti sistem, preferensi visual, dan catatan. */
@Composable
fun AppearanceScreen(viewModel: TemanKuliahViewModel, modifier: Modifier = Modifier) {
    val themeMode by viewModel.preferencesController.themeMode.collectAsStateWithLifecycle()
    // Mode yang benar-benar tampil saat ini (membaca palet aktif, jadi ikut berubah saat mode sistem berganti).
    val showingDark = TkPalette.isDark
    val followSystem = themeMode == ThemeMode.SYSTEM
    val activeName = when (themeMode) {
        ThemeMode.SYSTEM -> if (showingDark) "Ikuti sistem (gelap)" else "Ikuti sistem (terang)"
        else -> if (showingDark) "Mode Gelap" else "Mode Terang"
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = TkSpace.md, bottom = TkSpace.xl),
        verticalArrangement = Arrangement.spacedBy(TkSpace.sectionGap)
    ) {
        item { AppearanceHero(themeName = activeName) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppearanceSectionTitle("Pilih Tema")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ThemeOptionCard(
                        title = "Terang",
                        subtitle = "Teks jelas & kontras optimal",
                        selected = themeMode == ThemeMode.LIGHT,
                        previewDark = false,
                        onClick = { viewModel.preferencesController.setThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionCard(
                        title = "Gelap",
                        subtitle = "Nyaman di malam hari & hemat baterai OLED",
                        selected = themeMode == ThemeMode.DARK,
                        previewDark = true,
                        onClick = { viewModel.preferencesController.setThemeMode(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Surface(
                    shape = TkShape.xl,
                    color = SurfaceLight,
                    border = BorderStroke(1.5.dp, DividerLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Ikuti pengaturan sistem", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Otomatis terang atau gelap sesuai perangkat",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        ProfileSwitch(
                            checked = followSystem,
                            onCheckedChange = { on ->
                                viewModel.preferencesController.setThemeMode(
                                    when {
                                        on -> ThemeMode.SYSTEM
                                        showingDark -> ThemeMode.DARK
                                        else -> ThemeMode.LIGHT
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            val animationMode by viewModel.preferencesController.animationMode.collectAsStateWithLifecycle()
            val reducedNow = rememberReducedMotion()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppearanceSectionTitle("Animasi")
                Surface(
                    shape = TkShape.xl,
                    color = SurfaceLight,
                    border = BorderStroke(1.5.dp, DividerLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        AnimationMode.values().forEachIndexed { index, mode ->
                            if (index > 0) Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(DividerLight))
                            AnimationModeRow(
                                title = when (mode) {
                                    AnimationMode.AUTO -> "Otomatis"
                                    AnimationMode.ON -> "Selalu hidup"
                                    AnimationMode.OFF -> "Tanpa animasi"
                                },
                                description = when (mode) {
                                    AnimationMode.AUTO -> "Animasi dimatikan otomatis di HP berspek rendah"
                                    AnimationMode.ON -> "Animasi selalu aktif di semua perangkat"
                                    AnimationMode.OFF -> "Matikan semua animasi agar aplikasi lebih ringan"
                                },
                                selected = animationMode == mode,
                                onClick = { viewModel.preferencesController.setAnimationMode(mode) }
                            )
                        }
                    }
                }
                Text(
                    if (reducedNow) "Saat ini animasi nonaktif." else "Saat ini animasi aktif.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppearanceSectionTitle("Preferensi Visual & Bahasa")
                Surface(
                    shape = TkShape.xl,
                    color = SurfaceLight,
                    border = BorderStroke(1.5.dp, DividerLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        PreferenceRow(
                            icon = TkOutlineIcons.Palette,
                            title = "Mode Tampilan",
                            description = if (followSystem) "Mengikuti pengaturan perangkat" else "Pilihan tema untuk seluruh aplikasi",
                            value = themeMode.label
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(DividerLight))
                        PreferenceRow(
                            icon = TkOutlineIcons.Check,
                            title = "Bahasa Aplikasi",
                            description = "Bahasa baku antarmuka aplikasi",
                            value = "Indonesia"
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(DividerLight))
                        PreferenceRow(
                            icon = TkOutlineIcons.ListLines,
                            title = "Gaya Huruf",
                            description = "Poppins (tipografi M3)",
                            value = "Bawaan"
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TkShape.card)
                    .background(TemanKuliahBlueLight)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = TkOutlineIcons.InfoCircle,
                    contentDescription = null,
                    tint = TemanKuliahBlue,
                    modifier = Modifier.padding(top = 2.dp).size(20.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    "Pilihan tema tersimpan di perangkat ini dan langsung berlaku di seluruh aplikasi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

/** Satu pilihan mode animasi: judul, keterangan, dan tombol radio di kanan. */
@Composable
private fun AnimationModeRow(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        androidx.compose.material3.RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun AppearanceSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.semantics { heading() }
    )
}

/** Kartu biru bergradasi: nama tema yang sedang dipakai dan lencana "Aktif". */
@Composable
private fun AppearanceHero(themeName: String) {
    val shape = TkShape.card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = tkShadow(6.dp),
                shape = shape,
                clip = false,
                ambientColor = TemanKuliahBlue.copy(alpha = 0.15f),
                spotColor = TemanKuliahBlue.copy(alpha = 0.25f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    if (TkPalette.isDark) listOf(DarkTkColors.gradientStart, DarkTkColors.gradientEnd)
                    else listOf(Color(0xFF2574E1), Color(0xFF4A96F2))
                )
            )
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = 55.dp.toPx(),
                    center = Offset(size.width - 30.dp.toPx(), 30.dp.toPx())
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Text("Tema saat ini", color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodySmall)
            Text(
                themeName,
                color = Color.White,
                style = TkText.screenTitle,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).background(Color.White, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("Aktif", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Kartu pilihan tema dengan pratinjau mini. [selected] diberi bingkai biru dan centang. */
@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    previewDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = TkShape.card
    Surface(
        shape = shape,
        color = SurfaceLight,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TemanKuliahBlue else DividerLight),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.shadow(
                    elevation = tkShadow(8.dp),
                    shape = shape,
                    clip = false,
                    ambientColor = TemanKuliahBlue.copy(alpha = 0.2f),
                    spotColor = TemanKuliahBlue.copy(alpha = 0.3f)
                ) else Modifier
            )
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp)) {
            ThemePreview(dark = previewDark)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (selected) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.size(22.dp).background(TemanKuliahBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = TkOutlineIcons.Check,
                            contentDescription = "Dipilih",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Pratinjau mini: bilah judul dan tiga kolom warna, memakai warna asli tema yang diwakilinya. */
@Composable
private fun ThemePreview(dark: Boolean) {
    val set = if (dark) DarkTkColors else LightTkColors
    val surface = if (dark) set.background else Color(0xFFF0F5FD)
    val bar = if (dark) set.divider else Color(0xFFC9D6EE)
    val c1 = set.blue
    val c2 = if (dark) set.orangeLight else Color(0xFFFDEFE3)
    val c3 = if (dark) set.greenLight else Color(0xFFD3F7E3)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(TkShape.lg)
            .background(surface)
            .border(1.dp, if (dark) set.divider else Color.Transparent, TkShape.lg)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.fillMaxWidth(0.52f).height(8.dp).clip(CircleShape).background(bar))
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(c1, c2, c3).forEach { color ->
                Box(Modifier.weight(1f).fillMaxHeight().clip(TkShape.md).background(color))
            }
        }
    }
}

/** Baris preferensi: ubin ikon, judul + keterangan, dan pil nilai saat ini di kanan. */
@Composable
private fun PreferenceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TkIconTile(icon, size = 38.dp, shape = TkShape.md)
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            value,
            color = TemanKuliahBlueDark,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(CircleShape)
                .background(TemanKuliahBlueLight)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
