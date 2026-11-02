package com.temankuliah.app.ui.profile

import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.notifications.ReminderSelfTest
import com.temankuliah.app.domain.CheckStatus
import com.temankuliah.app.domain.ReminderCheck
import com.temankuliah.app.domain.reminderTestVerdict
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.OutlinedButton
import com.temankuliah.app.ui.components.TkCard
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.TkText
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TemanKuliahOrangeLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TemanKuliahRedLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class TestPhase { IDLE, NOTIFICATION, ALARM }

private data class TestReport(val label: String, val finishedAtMillis: Long, val checks: List<ReminderCheck>)

/**
 * Kartu "Tes pengingat" di Pengaturan > Pengingat. Tes notifikasi menampilkan satu notifikasi uji sekarang;
 * tes alarm memasang alarm uji [ReminderSelfTest.ALARM_DELAY_MS] ms ke depan lewat jalur alarm asli.
 * [hasUnsavedChanges] true bila saklar di layar berbeda dari preferensi tersimpan, karena tes memakai yang tersimpan.
 */
@Composable
internal fun ReminderTestCard(hasUnsavedChanges: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(TestPhase.IDLE) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var report by remember { mutableStateOf<TestReport?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val busy = phase != TestPhase.IDLE

    TkCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(TkShape.md).background(TemanKuliahBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(TkOutlineIcons.AlarmClock, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Tes pengingat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        "Uji jalur pengingat agar siap sebelum kuliah",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Text(
                "Tes notifikasi memunculkan satu notifikasi sekarang. Tes alarm memasang alarm yang berbunyi " +
                    "${ReminderSelfTest.ALARM_DELAY_MS / 1000} detik lagi lewat jalur alarm asli sistem.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (hasUnsavedChanges) {
                Surface(
                    shape = TkShape.md,
                    color = TemanKuliahOrangeLight,
                    border = BorderStroke(1.dp, TemanKuliahOrangeDark.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Ada perubahan preferensi yang belum disimpan. Tes memakai preferensi yang sudah tersimpan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TemanKuliahOrangeDark,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        report = null
                        job = scope.launch {
                            phase = TestPhase.NOTIFICATION
                            try {
                                val checks = ReminderSelfTest.runNotificationTest(context)
                                report = TestReport("Tes notifikasi", System.currentTimeMillis(), checks)
                            } finally {
                                phase = TestPhase.IDLE
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                ) {
                    Icon(TkOutlineIcons.Bell, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tes notifikasi", maxLines = 1, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        report = null
                        job = scope.launch {
                            secondsLeft = (ReminderSelfTest.ALARM_DELAY_MS / 1000L).toInt()
                            phase = TestPhase.ALARM
                            try {
                                val checks = ReminderSelfTest.runAlarmTest(context) { secondsLeft = it }
                                report = TestReport("Tes alarm", System.currentTimeMillis(), checks)
                            } finally {
                                phase = TestPhase.IDLE
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                ) {
                    Icon(TkOutlineIcons.AlarmClock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tes alarm", maxLines = 1, fontWeight = FontWeight.SemiBold)
                }
            }

            when (phase) {
                TestPhase.NOTIFICATION -> {
                    Surface(
                        shape = TkShape.card,
                        color = TemanKuliahBlueLight,
                        border = BorderStroke(1.dp, TemanKuliahBlue.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TemanKuliahBlue)
                            Text(
                                "Mengirim notifikasi uji…",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TemanKuliahBlueDark,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                    }
                }
                TestPhase.ALARM -> {
                    Surface(
                        shape = TkShape.card,
                        color = TemanKuliahBlueLight,
                        border = BorderStroke(1.dp, TemanKuliahBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(Modifier.fillMaxWidth(), color = TemanKuliahBlue)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (secondsLeft > 0) "Alarm uji berbunyi dalam $secondsLeft detik. Tetap di halaman ini."
                                    else "Menunggu alarm terpicu…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TemanKuliahBlueDark,
                                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }
                                )
                                TextButton(onClick = { job?.cancel() }) {
                                    Text("Batalkan", fontWeight = FontWeight.Bold, color = TemanKuliahRedDark)
                                }
                            }
                        }
                    }
                }
                TestPhase.IDLE -> Unit
            }

            report?.let { TestReportView(it) }
        }
    }
}

@Composable
private fun TestReportView(report: TestReport) {
    val verdict = reminderTestVerdict(report.checks)
    val colors = statusColors(verdict)
    val finishedAt = Instant.ofEpochMilli(report.finishedAtMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = TkShape.card,
            color = colors.container,
            border = BorderStroke(1.dp, colors.content.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(verdictTitle(verdict), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.content)
                Text(verdictBody(verdict), style = MaterialTheme.typography.bodySmall, color = colors.content)
                Text("${report.label} selesai pukul $finishedAt", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = colors.content.copy(alpha = 0.85f))
            }
        }
        report.checks.forEachIndexed { index, check ->
            if (index > 0) ProfileDetailDivider()
            CheckRow(check)
        }
    }
}

@Composable
private fun CheckRow(check: ReminderCheck) {
    val colors = statusColors(check.status)
    val label = when (check.status) {
        CheckStatus.OK -> "Lolos"
        CheckStatus.INFO -> "Info"
        CheckStatus.WARNING -> "Perlu diperiksa"
        CheckStatus.FAILED -> "Gagal"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(40.dp).clip(TkShape.md).background(colors.container),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (check.status == CheckStatus.OK) TkOutlineIcons.Check else TkOutlineIcons.InfoCircle,
                contentDescription = label,
                tint = colors.content,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(check.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(check.detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

private class StatusColors(val container: Color, val content: Color)

private fun statusColors(status: CheckStatus): StatusColors = when (status) {
    CheckStatus.OK -> StatusColors(TemanKuliahGreenLight, TemanKuliahGreenDark)
    CheckStatus.INFO -> StatusColors(TemanKuliahBlueLight, TemanKuliahBlueDark)
    CheckStatus.WARNING -> StatusColors(TemanKuliahOrangeLight, TemanKuliahOrangeDark)
    CheckStatus.FAILED -> StatusColors(TemanKuliahRedLight, TemanKuliahRedDark)
}

private fun verdictTitle(status: CheckStatus): String = when (status) {
    CheckStatus.OK, CheckStatus.INFO -> "Lolos"
    CheckStatus.WARNING -> "Berfungsi, dengan catatan"
    CheckStatus.FAILED -> "Belum berfungsi"
}

private fun verdictBody(status: CheckStatus): String = when (status) {
    CheckStatus.OK, CheckStatus.INFO -> "Semua pemeriksaan otomatis lolos. Bunyi dan getar tetap perlu kamu pastikan sendiri."
    CheckStatus.WARNING -> "Pengingat bisa sampai, tetapi ada hal di bawah yang perlu diperiksa."
    CheckStatus.FAILED -> "Ada pemeriksaan yang gagal. Pengingat belum akan sampai sebelum itu diperbaiki."
}
