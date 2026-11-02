package com.temankuliah.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.TkCard
import com.temankuliah.app.ui.components.TkIconTile
import com.temankuliah.app.ui.icons.ChevronRight
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceVariantLight
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TextMuted
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape

/** Divider between rows of a section, inset to line up with the row text instead of the icon. */
@Composable
internal fun ProfileDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 68.dp, end = 16.dp).height(1.dp).background(DividerLight))
}

@Composable
internal fun ProfileSection(title: String, content: @Composable () -> Unit) {
    TkCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp).semantics { heading() }
        )
        content()
        Spacer(Modifier.height(8.dp))
    }
}

/** One settings row. Tappable (shows a chevron) only when [onClick] is given; [trailing] is a short current value. */
@Composable
internal fun ProfileAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    trailing: String? = null,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick, role = Role.Button) else Modifier.fillMaxWidth()
    Row(rowModifier.padding(horizontal = 16.dp, vertical = 10.dp).heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically) {
        TkIconTile(icon, size = 40.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Surface(
                shape = TkShape.sm,
                color = if (onClick != null) TemanKuliahBlueLight else SurfaceVariantLight
            ) {
                Text(
                    trailing,
                    color = if (onClick != null) TemanKuliahBlueDark else TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp).widthIn(max = 96.dp)
                )
            }
        }
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(22.dp))
        }
    }
}

/** Baris menu Profil: ubin ikon persegi membulat, judul tebal, keterangan, dan chevron di kanan. */
@Composable
internal fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tileColor: Color = TemanKuliahBlueLight,
    iconTint: Color = com.temankuliah.app.ui.theme.TemanKuliahBlue,
    titleColor: Color = TextPrimary
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(com.temankuliah.app.ui.theme.TkShape.lg).background(tileColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Column(
            Modifier.weight(1f).padding(start = 14.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                title,
                color = titleColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(22.dp))
    }
}

/** Pemisah antar baris menu, sejajar dengan teks (melewati ubin ikon 48dp). */
@Composable
internal fun ProfileMenuDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 78.dp, end = 16.dp).height(1.dp).background(DividerLight))
}
