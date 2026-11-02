package com.temankuliah.app.ui.projects

import com.temankuliah.app.ui.components.rememberEntranceProgress
import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.TkPalette
import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.temankuliah.app.ui.components.pressScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.ProjectEntity
import com.temankuliah.app.data.local.ProjectMilestoneEntity
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.icons.DeleteOutline
import com.temankuliah.app.ui.icons.Edit
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.icons.Timeline
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Kartu satu proyek sesuai desain: ubin ikon, judul + mata kuliah, pil status di kanan atas,
 * "x dari y tugas selesai" dengan persentase, bilah progres, dan tenggat.
 * Mengetuk kartu membuka menu: Tahapan, Tautkan tugas, Edit, dan Hapus.
 */
@Composable
internal fun ProjectCard(
    project: ProjectEntity,
    courseTag: String,
    milestones: List<ProjectMilestoneEntity>,
    relatedTaskCount: Int,
    doneTaskCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMilestones: () -> Unit,
    onLinkTasks: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    val isCompleted = project.progress >= 100
    val isNotStarted = project.progress == 0
    val statusText = when {
        isCompleted -> "Selesai"
        isNotStarted -> "Belum dimulai"
        else -> "Berjalan"
    }
    val statusBg = when {
        isCompleted -> TemanKuliahGreenLight
        isNotStarted -> tkAdaptive(Color(0xFFEEF2F8), TkPalette.colors.surfaceVariant)
        else -> TemanKuliahBlueLight
    }
    val statusColor = when {
        isCompleted -> TemanKuliahGreenDark
        isNotStarted -> TextSecondary
        else -> TemanKuliahBlueDark
    }
    val taskSummary = if (relatedTaskCount == 0) "Belum ada tugas tertaut" else "$doneTaskCount dari $relatedTaskCount tugas selesai"
    val deadline = SimpleDateFormat("d MMM yyyy", IndonesianLocale).format(Date(project.deadlineAtMillis))
    val fraction = if (isNotStarted) 0.02f else (project.progress / 100f).coerceIn(0.02f, 1f)
    val animatedFraction by rememberEntranceProgress(fraction)

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = TkShape.card,
            color = SurfaceLight,
            border = BorderStroke(1.dp, DividerLight),
            modifier = Modifier.fillMaxWidth().pressScale()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = "Buka menu ${project.title}") { menuOpen = true }
                    .padding(16.dp)
                    .testTag("project_card_${project.id}"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(TkShape.md)
                            .background(TemanKuliahBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(TkOutlineIcons.Kanban, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(22.dp))
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = courseTag,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(shape = CircleShape, color = statusBg) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = taskSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${project.progress}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(tkAdaptive(Color(0xFFE6EEF9), TkPalette.colors.surfaceVariant))
                            .semantics { contentDescription = "Progres ${project.progress} persen" }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction)
                                .clip(CircleShape)
                                .background(if (isCompleted) TemanKuliahGreenDark else TemanKuliahBlue)
                        )
                    }
                }

                Text(
                    text = "Tenggat · $deadline",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier.background(SurfaceLight)
        ) {
            DropdownMenuItem(
                text = { Text(if (milestones.isEmpty()) "Tahapan proyek" else "Tahapan proyek (${milestones.count { it.isCompleted }}/${milestones.size})", color = TextPrimary) },
                leadingIcon = { Icon(Icons.Default.Timeline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                onClick = { menuOpen = false; onMilestones() },
                modifier = Modifier.testTag("project_menu_milestones")
            )
            DropdownMenuItem(
                text = { Text("Tautkan tugas", color = TextPrimary) },
                leadingIcon = { Icon(TkOutlineIcons.Clipboard, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                onClick = { menuOpen = false; onLinkTasks() },
                modifier = Modifier.testTag("project_menu_link")
            )
            DropdownMenuItem(
                text = { Text("Ubah", color = TextPrimary) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                onClick = { menuOpen = false; onEdit() },
                modifier = Modifier.testTag("project_menu_edit")
            )
            DropdownMenuItem(
                text = { Text("Hapus", color = TemanKuliahRedDark) },
                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TemanKuliahRedDark, modifier = Modifier.size(18.dp)) },
                onClick = { menuOpen = false; onDelete() },
                modifier = Modifier.testTag("project_menu_delete")
            )
        }
    }
}
