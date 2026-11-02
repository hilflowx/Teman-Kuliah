package com.temankuliah.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.Poppins
import com.temankuliah.app.ui.theme.tkShadow

// -------------------------------------------------------------------------------------------------
// Teks Poppins
// -------------------------------------------------------------------------------------------------

private val DashLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/** Teks Poppins dengan parameter ringkas; semua teks Beranda lewat sini supaya jenis huruf seragam. */
@Composable
internal fun DashText(
    text: String,
    color: Color,
    size: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    softWrap: Boolean = true
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = weight,
        fontFamily = Poppins,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = TextStyle(lineHeightStyle = DashLineHeightStyle)
    )
}

// -------------------------------------------------------------------------------------------------
// Header
// -------------------------------------------------------------------------------------------------

/** Avatar inisial + sapaan di kiri, tombol notifikasi bulat di kanan. */
@Composable
internal fun DashboardHeader(
    timeGreeting: String,
    firstName: String,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    unreadCount: Int = 0
) {
    val initial = firstName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_greeting_card"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(DashAvatarStart, DashAvatarEnd)))
                .clickable(onClickLabel = "Buka profil", onClick = onOpenProfile)
                .testTag("dashboard_avatar"),
            contentAlignment = Alignment.Center
        ) {
            DashText(initial, Color.White, 18.sp, weight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        ) {
            DashText(
                text = timeGreeting,
                color = DashMuted,
                size = 12.sp,
                lineHeight = 18.sp,
                maxLines = 1
            )
            DashText(
                text = "Halo, $firstName",
                color = DashInk,
                size = 17.sp,
                weight = FontWeight.Bold,
                lineHeight = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        val bellDescription = if (unreadCount > 0) {
            "Notifikasi, $unreadCount belum dibaca"
        } else {
            "Notifikasi dan pengingat"
        }
        Box(
            modifier = Modifier
                .size(48.dp) // area sentuh 48dp; lingkaran visual 42dp di tengah
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onOpenNotifications)
                .semantics { contentDescription = bellDescription }
                .testTag("dashboard_notifications_button"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DashTile)
                    .border(1.dp, DashLine, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TkOutlineIcons.Bell,
                    contentDescription = null,
                    tint = DashBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp, end = 3.dp)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(DashBadgeRing)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(DashOrange)
                        .testTag("dashboard_notifications_badge")
                )
            }
        }
    }
}

/** Tab 3 pilihan berbentuk kapsul: kontainer biru muda, tab aktif biru langit dengan bayangan. */
@Composable
internal fun DashTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    tagPrefix: String = "dashboard_tab_"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 43.dp)
            .height(IntrinsicSize.Min)
            .clip(CircleShape)
            .background(DashTrack)
            .border(1.dp, DashLineSoft, CircleShape)
            .padding(5.dp)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (selected) {
                            Modifier.shadow(
                                elevation = tkShadow(6.dp),
                                shape = CircleShape,
                                ambientColor = DashSky.copy(alpha = 0.30f),
                                spotColor = DashSky.copy(alpha = 0.50f)
                            )
                        } else Modifier
                    )
                    .clip(CircleShape)
                    .background(if (selected) DashSky else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .testTag("$tagPrefix$index"),
                contentAlignment = Alignment.Center
            ) {
                DashText(
                    text = label,
                    color = if (selected) Color.White else DashMuted,
                    size = 13.sp,
                    weight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
