package com.temankuliah.app.ui.tasks

import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.TkPalette
import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.local.CourseEntity
import com.temankuliah.app.data.local.ProjectEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.domain.TaskStatus
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.icons.CheckCircle
import com.temankuliah.app.ui.icons.DeleteOutline
import com.temankuliah.app.ui.icons.Edit
import com.temankuliah.app.ui.icons.Schedule
import com.temankuliah.app.ui.theme.BorderLight
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TemanKuliahOrangeLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TextMuted
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.components.pressScale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

private const val DAY_MILLIS = 86_400_000L

/**
 * Kartu satu tugas sesuai desain: lingkaran status di kiri (ketuk untuk menandai selesai),
 * judul dan mata kuliah/proyek di tengah, pil tenggat di kanan. Mengetuk kartu membuka menu Ubah/Hapus.
 * Tugas terlambat diberi bingkai dan lingkaran oranye.
 */
@Composable
internal fun TaskCard(
    task: TaskEntity,
    courses: List<CourseEntity>,
    now: Long,
    dayStart: Long,
    tomorrow: Long,
    onStatusChange: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    projects: List<ProjectEntity> = emptyList()
) {
    var menuOpen by remember { mutableStateOf(false) }

    val contextLabel = remember(task.courseId, task.projectId, courses, projects) {
        val courseName = task.courseId?.let { id -> courses.firstOrNull { it.id == id }?.name }
        val projectTitle = task.projectId?.let { id -> projects.firstOrNull { it.id == id }?.title }
        listOfNotNull(courseName, projectTitle?.let { "Proyek: $it" })
            .joinToString(" · ")
            .ifEmpty { "Tugas mandiri" }
    }

    val isDone = task.status == TaskStatus.SELESAI
    val isInProgress = task.status == "Sedang Dikerjakan"
    val isOverdue = !isDone && task.dueAtMillis < now
    val isToday = !isDone && task.dueAtMillis in dayStart until tomorrow

    val pill = taskDuePill(task.dueAtMillis, now, dayStart, isDone, isOverdue, isToday)
    val targetCircleColor = when {
        isDone -> TemanKuliahGreenDark
        isOverdue -> TemanKuliahOrangeDark
        isInProgress -> TemanKuliahBlue
        else -> BorderLight
    }
    val circleColor by animateColorAsState(
        targetValue = targetCircleColor,
        animationSpec = tween(durationMillis = 200),
        label = "taskCircleColor"
    )
    val checkPopScale by animateFloatAsState(
        targetValue = if (isDone) 1.0f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "checkPopScale"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (isDone) 0.45f else 1.0f,
        animationSpec = tween(durationMillis = 250),
        label = "taskTitleAlpha"
    )
    val toggleDescription = when {
        isDone -> "${task.title}: selesai. Tandai belum selesai"
        isInProgress -> "${task.title}: sedang dikerjakan. Tandai selesai"
        else -> "${task.title}: belum dikerjakan. Tandai selesai"
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = TkShape.card,
            color = if (isOverdue) tkAdaptive(Color(0xFFFFFAF4), TkPalette.colors.orangeLight) else SurfaceLight,
            border = if (isOverdue) BorderStroke(1.5.dp, tkAdaptive(Color(0xFFF3C08A), Color(0xFF7A4A1E))) else BorderStroke(1.dp, DividerLight),
            modifier = Modifier.fillMaxWidth().pressScale()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = "Buka menu ${task.title}") { menuOpen = true }
                    .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                    .testTag("task_card_${task.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Checkbox, onClickLabel = "Ubah status ${task.title}") {
                            onStatusChange(if (isDone) TaskStatus.BELUM else TaskStatus.SELESAI)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isDone -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = toggleDescription,
                            tint = circleColor,
                            modifier = Modifier
                                .size(30.dp)
                                .graphicsLayer {
                                    scaleX = checkPopScale
                                    scaleY = checkPopScale
                                }
                        )
                        else -> Box(
                            modifier = Modifier
                                .size(30.dp)
                                .border(2.dp, circleColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isInProgress) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = toggleDescription,
                                    tint = circleColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Spacer(Modifier.size(1.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) TextMuted else TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer { alpha = titleAlpha }
                    )
                    Text(
                        text = contextLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                Surface(shape = CircleShape, color = pill.container) {
                    Text(
                        text = pill.text,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = pill.content,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier.background(SurfaceLight)
        ) {
            DropdownMenuItem(
                text = { Text("Ubah", color = TextPrimary) },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                onClick = { menuOpen = false; onEdit() },
                modifier = Modifier.testTag("task_menu_edit")
            )
            DropdownMenuItem(
                text = { Text("Hapus", color = TemanKuliahRedDark) },
                leadingIcon = {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TemanKuliahRedDark, modifier = Modifier.size(18.dp))
                },
                onClick = { menuOpen = false; onDelete() },
                modifier = Modifier.testTag("task_menu_delete")
            )
        }
    }
}


private class DuePill(val text: String, val container: Color, val content: Color)

private fun taskDuePill(
    dueAtMillis: Long,
    now: Long,
    dayStart: Long,
    isDone: Boolean,
    isOverdue: Boolean,
    isToday: Boolean
): DuePill {
    if (isDone) return DuePill("Selesai", TemanKuliahGreenLight, TemanKuliahGreenDark)
    if (isOverdue) {
        val dueDay = Calendar.getInstance().apply {
            timeInMillis = dueAtMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val days = Math.round((dayStart - dueDay) / DAY_MILLIS.toDouble()).toInt()
        val text = when {
            days >= 1 -> "Lewat $days hari"
            (now - dueAtMillis) / 3_600_000L >= 1 -> "Lewat ${(now - dueAtMillis) / 3_600_000L} jam"
            else -> "Baru lewat"
        }
        return DuePill(text, TemanKuliahOrangeLight, TemanKuliahOrangeDark)
    }
    if (isToday) {
        return DuePill(SimpleDateFormat("HH:mm", IndonesianLocale).format(Date(dueAtMillis)), TemanKuliahBlueLight, TemanKuliahBlueDark)
    }
    val sameYear = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR) ==
        Calendar.getInstance().apply { timeInMillis = dueAtMillis }.get(Calendar.YEAR)
    val pattern = if (sameYear) "EEEE, d MMM" else "EEEE, d MMM yyyy"
    return DuePill(SimpleDateFormat(pattern, IndonesianLocale).format(Date(dueAtMillis)), tkAdaptive(Color(0xFFEEF2F8), TkPalette.colors.surfaceVariant), TextSecondary)
}
