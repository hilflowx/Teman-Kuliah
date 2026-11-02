package com.temankuliah.app.ui.notifications

import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.temankuliah.app.ui.theme.SurfaceLight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.temankuliah.app.domain.NotificationGroup
import com.temankuliah.app.domain.NotificationKind
import com.temankuliah.app.ui.TemanKuliahViewModel
import com.temankuliah.app.ui.components.TkEmptyState
import com.temankuliah.app.ui.components.tkHeroCircles
import com.temankuliah.app.ui.dashboard.DashBlue
import com.temankuliah.app.ui.dashboard.DashHeroEnd
import com.temankuliah.app.ui.dashboard.DashHeroStart
import com.temankuliah.app.ui.dashboard.DashInk
import com.temankuliah.app.ui.dashboard.DashLine
import com.temankuliah.app.ui.dashboard.DashMint
import com.temankuliah.app.ui.dashboard.DashOkInk
import com.temankuliah.app.ui.dashboard.DashOkSoft
import com.temankuliah.app.ui.dashboard.DashOrange
import com.temankuliah.app.ui.dashboard.DashOrangeSoft
import com.temankuliah.app.ui.dashboard.DashTabs
import com.temankuliah.app.ui.dashboard.DashText
import com.temankuliah.app.ui.dashboard.DashTile
import com.temankuliah.app.ui.dashboard.DashTint
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.profile.ProfileSubHeader
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkSpace
import com.temankuliah.app.ui.theme.tkShadow

private val FilterLabels = listOf("Semua", "Jadwal", "Tugas", "Agenda")

/**
 * Layar Notifikasi (dibuka dari lonceng Beranda): kartu ringkasan belum dibaca, tab filter, lalu daftar
 * dikelompokkan Hari ini / Kemarin / Sebelumnya. Isinya data asli dari jadwal, tugas, dan agenda.
 *
 * [onOpenTab] dipanggil saat sebuah notifikasi diketuk: 1 = Kalender, 2 = Tugas.
 */
@Composable
internal fun NotificationsScreen(
    viewModel: TemanKuliahViewModel,
    onBack: () -> Unit,
    onOpenTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = viewModel.notificationController
    val feed by controller.feed.collectAsStateWithLifecycle()
    val unread by controller.unreadCount.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableIntStateOf(0) }

    val filtered = remember(feed, filter) {
        when (filter) {
            1 -> feed.filter { it.item.kind == NotificationKind.JADWAL }
            2 -> feed.filter { it.item.kind == NotificationKind.TUGAS }
            3 -> feed.filter { it.item.kind == NotificationKind.AGENDA }
            else -> feed
        }
    }

    Column(modifier.fillMaxSize()) {
        ProfileSubHeader(
            title = "Notifikasi",
            supporting = "Pengingat kuliah & tenggat tugas",
            onBack = onBack,
            backDescription = "Kembali ke Beranda"
        )
        LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("notifications_list"),
            contentPadding = PaddingValues(start = TkSpace.page, end = TkSpace.page, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "summary", span = { GridItemSpan(maxLineSpan) }) {
                SummaryCard(
                    unread = unread,
                    hasAny = feed.isNotEmpty(),
                    onMarkAllRead = controller::markAllRead
                )
            }
            item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.padding(top = 4.dp)) {
                    DashTabs(
                        items = FilterLabels,
                        selectedIndex = filter,
                        onSelect = { filter = it },
                        tagPrefix = "notifications_filter_"
                    )
                }
            }
            if (filtered.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    TkEmptyState(
                        icon = TkOutlineIcons.Bell,
                        title = if (feed.isEmpty()) "Belum ada notifikasi" else "Tidak ada notifikasi ${FilterLabels[filter].lowercase()}",
                        message = if (feed.isEmpty()) {
                            "Pengingat kuliah, tenggat tugas, dan agenda terdekat akan muncul di sini."
                        } else {
                            "Coba pilih tab lain untuk melihat notifikasi lainnya."
                        },
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                NotificationGroup.entries.forEach { group ->
                    val rows = filtered.filter { it.item.group == group }
                    if (rows.isEmpty()) return@forEach
                    item(key = "header_${group.name}", span = { GridItemSpan(maxLineSpan) }) {
                        GroupHeader(
                            title = when (group) {
                                NotificationGroup.HARI_INI -> "Hari ini"
                                NotificationGroup.KEMARIN -> "Kemarin"
                                NotificationGroup.SEBELUMNYA -> "Sebelumnya"
                            },
                            unread = rows.count { !it.isRead }
                        )
                    }
                    items(rows, key = { it.item.key }) { entry ->
                        Box(Modifier.animateItem()) {
                            NotificationCard(
                                entry = entry,
                                onClick = {
                                    controller.markRead(entry.item.key)
                                    onOpenTab(entry.item.targetTab)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Kartu ringkasan
// -------------------------------------------------------------------------------------------------

@Composable
private fun SummaryCard(unread: Int, hasAny: Boolean, onMarkAllRead: () -> Unit) {
    val shape = TkShape.xl
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notifications_summary")
            .shadow(
                elevation = tkShadow(20.dp),
                shape = shape,
                ambientColor = DashBlue.copy(alpha = 0.25f),
                spotColor = DashBlue.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(DashHeroStart, DashHeroEnd)))
            .tkHeroCircles()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                DashText(
                    text = if (unread > 0) "Belum dibaca" else "Ringkasan",
                    color = Color.White.copy(alpha = 0.92f),
                    size = 14.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                DashText(
                    text = when {
                        unread > 0 -> "$unread notifikasi baru"
                        hasAny -> "Semua sudah dibaca"
                        else -> "Belum ada notifikasi"
                    },
                    color = Color.White,
                    size = 26.sp,
                    weight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                if (unread > 0) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                            .clickable(role = Role.Button, onClick = onMarkAllRead)
                            .padding(horizontal = 18.dp)
                            .testTag("notifications_mark_all_read"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = TkOutlineIcons.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        DashText(
                            text = "Tandai semua dibaca",
                            color = Color.White,
                            size = 14.sp,
                            weight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TkOutlineIcons.Bell,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
                if (unread > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                            .padding(2.5.dp)
                            .clip(CircleShape)
                            .background(DashMint)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Daftar
// -------------------------------------------------------------------------------------------------

@Composable
private fun GroupHeader(title: String, unread: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DashText(
            text = title,
            color = DashInk,
            size = 20.sp,
            weight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (unread > 0) {
            DashText(
                text = "$unread baru",
                color = DashBlue,
                size = 14.sp,
                weight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private class KindStyle(
    val label: String,
    val icon: ImageVector,
    val tile: Color,
    val iconTint: Color,
    val pillText: Color
)

private fun styleOf(kind: NotificationKind): KindStyle = when (kind) {
    NotificationKind.JADWAL -> KindStyle("Jadwal", TkOutlineIcons.Calendar, DashTint, DashBlue, DashBlue)
    NotificationKind.TUGAS -> KindStyle("Tugas", TkOutlineIcons.Clipboard, DashOrangeSoft, DashOrange, TemanKuliahOrangeDark)
    NotificationKind.AGENDA -> KindStyle("Agenda", TkOutlineIcons.BookOpen, DashOkSoft, DashOkInk, DashOkInk)
}

@Composable
private fun NotificationCard(entry: NotificationEntry, onClick: () -> Unit) {
    val item = entry.item
    val style = styleOf(item.kind)
    val shape = TkShape.card
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = tkShadow(4.dp),
                shape = shape,
                ambientColor = DashBlue.copy(alpha = 0.08f),
                spotColor = DashBlue.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(if (entry.isRead) SurfaceLight else DashTile)
            .border(BorderStroke(1.dp, DashLine), shape)
            .semantics(mergeDescendants = true) {
                stateDescription = if (entry.isRead) "Sudah dibaca" else "Belum dibaca"
            }
            .clickable(role = Role.Button, onClickLabel = "Buka ${style.label.lowercase()}", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("notification_${item.key}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(TkShape.lg)
                .background(style.tile),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.iconTint,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            DashText(
                text = item.title,
                color = DashInk,
                size = 15.sp,
                weight = FontWeight.Bold,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            DashText(
                text = item.subtitle,
                color = TextSecondary,
                size = 13.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 26.dp)
                        .clip(CircleShape)
                        .background(style.tile)
                        .padding(horizontal = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DashText(
                        text = style.label,
                        color = style.pillText,
                        size = 12.sp,
                        weight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(8.dp))
                DashText(
                    text = item.detail,
                    color = TextSecondary,
                    size = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
        if (!entry.isRead) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(DashTint),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(DashBlue)
                )
            }
        }
    }
}
