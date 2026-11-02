package com.temankuliah.app.ui.profile

import com.temankuliah.app.ui.theme.SurfaceLight
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.temankuliah.app.data.notifications.ReminderScheduler
import com.temankuliah.app.domain.OemVendor
import com.temankuliah.app.domain.getVendorGuide
import com.temankuliah.app.domain.isIgnoringBatteryOptimizations
import com.temankuliah.app.domain.openBatteryOptimizationSettings
import com.temankuliah.app.ui.components.TkCard
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TemanKuliahOrangeLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape

/**
 * Kartu interaktif Panduan Keandalan Alarm & Latar Belakang OEM (Tahap 5 PRD 10.1).
 * Menampilkan status optimasi baterai, status alarm presisi, panduan vendor otomatis,
 * dan tombol aksi sistem untuk mencegah alarm kuliah dibunuh oleh vendor sistem Android.
 */
@Composable
fun OemReliabilityCard(
    modifier: Modifier = Modifier,
    onOpenExactAlarmSettings: () -> Unit
) {
    val context = LocalContext.current
    val vendor = remember { OemVendor.fromManufacturer() }
    val guide = remember(vendor) { getVendorGuide(vendor) }

    var isBatteryOptimal by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var isExactAlarmAllowed by remember { mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isBatteryOptimal = isIgnoringBatteryOptimizations(context)
        isExactAlarmAllowed = ReminderScheduler.canScheduleExactAlarms(context)
    }

    TkCard(modifier = modifier.fillMaxWidth().testTag("oem_reliability_card")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header kartu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(TkShape.md)
                        .background(TemanKuliahBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = TkOutlineIcons.ShieldCheck,
                        contentDescription = null,
                        tint = TemanKuliahBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Keandalan Alarm & Latar Belakang",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Panduan optimasi agar alarm kuliah selalu berdering",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Status indikator real-time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusIndicatorChip(
                    title = "Hemat Baterai",
                    isOptimal = isBatteryOptimal,
                    optimalLabel = "Optimal (Bebas)",
                    restrictedLabel = "Membatasi",
                    modifier = Modifier.weight(1f)
                )
                StatusIndicatorChip(
                    title = "Alarm Presisi",
                    isOptimal = isExactAlarmAllowed,
                    optimalLabel = "Diizinkan",
                    restrictedLabel = "Dibatasi",
                    modifier = Modifier.weight(1f)
                )
            }

            // Kotak panduan spesifik vendor
            Surface(
                shape = TkShape.lg,
                color = TemanKuliahBlueLight.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, TemanKuliahBlue.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(TemanKuliahBlue)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Terdeteksi",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = guide.vendorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TemanKuliahBlueDark
                        )
                    }

                    Text(
                        text = guide.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(2.dp))

                    guide.steps.forEachIndexed { index, step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(TemanKuliahBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TemanKuliahBlueDark
                                )
                            }
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Tombol aksi sistem cepat
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isBatteryOptimal) {
                    ActionButton(
                        title = "Nonaktifkan Hemat Daya",
                        subtitle = "Kecualikan Teman Kuliah dari optimasi baterai",
                        icon = TkOutlineIcons.ShieldCheck,
                        testTag = "btn_disable_battery_optimization",
                        onClick = { openBatteryOptimizationSettings(context) }
                    )
                }

                if (!isExactAlarmAllowed) {
                    ActionButton(
                        title = "Izinkan Alarm Presisi",
                        subtitle = "Buka setelan alarm presisi Android",
                        icon = TkOutlineIcons.AlarmClock,
                        testTag = "btn_allow_exact_alarm",
                        onClick = onOpenExactAlarmSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorChip(
    title: String,
    isOptimal: Boolean,
    optimalLabel: String,
    restrictedLabel: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isOptimal) TemanKuliahGreenLight else TemanKuliahOrangeLight
    val textColor = if (isOptimal) TemanKuliahGreenDark else TemanKuliahOrangeDark
    val label = if (isOptimal) optimalLabel else restrictedLabel

    Surface(
        shape = TkShape.md,
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = TkShape.lg,
        color = SurfaceLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, TemanKuliahBlue.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(TkShape.lg)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(TkShape.md)
                    .background(TemanKuliahBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TemanKuliahBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TemanKuliahBlueDark
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = TkOutlineIcons.ArrowRight,
                contentDescription = null,
                tint = TemanKuliahBlue,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
