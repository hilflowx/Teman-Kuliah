package com.temankuliah.app.ui.profile

import com.temankuliah.app.ui.theme.TkShape
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import com.temankuliah.app.ui.theme.tkAdaptive
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temankuliah.app.data.files.StudentFileStore
import com.temankuliah.app.data.local.UserEntity
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.components.TkSubHeader
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkGradientEnd
import com.temankuliah.app.ui.theme.TkGradientStart
import com.temankuliah.app.ui.theme.TkMint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import com.temankuliah.app.ui.theme.tkShadow

/** Latar kartu hero Profil: gradien biru, bayangan biru halus, dan lingkaran lembut di kanan atas. */
internal fun Modifier.profileHeroSurface(): Modifier {
    val shape = TkShape.card
    return this
        .fillMaxWidth()
        .shadow(
            elevation = tkShadow(8.dp),
            shape = shape,
            ambientColor = TkGradientEnd.copy(alpha = 0.15f),
            spotColor = TkGradientEnd.copy(alpha = 0.25f)
        )
        .clip(shape)
        .background(Brush.linearGradient(listOf(TkGradientStart, TkGradientEnd)))
        .drawBehind {
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = 90.dp.toPx(),
                center = Offset(size.width - 6.dp.toPx(), -10.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = 50.dp.toPx(),
                center = Offset(10.dp.toPx(), size.height - 2.dp.toPx())
            )
        }
}

/** Memuat foto profil (dikecilkan maksimal ~512px); null bila belum ada atau tidak terbaca. */
@Composable
internal fun rememberProfileBitmap(user: UserEntity): ImageBitmap? {
    val context = LocalContext.current
    return remember(user.profileImagePath) {
        user.profileImagePath?.let { path ->
            StudentFileStore.fileFor(context, path)?.let { file ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null else {
                    val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
                    val sampleSize = Integer.highestOneBit((longestEdge / 512).coerceAtLeast(1))
                    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize.coerceAtLeast(1) }
                    BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
                }
            }
        }
    }
}

/** Pil translusen putih di atas kartu hero; [showDot] menambahkan titik status mint di kiri. */
@Composable
internal fun HeroPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = Color.White.copy(alpha = 0.22f),
    showDot: Boolean = false
) {
    Row(
        modifier = modifier
            .heightIn(min = 34.dp)
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (showDot) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(TkMint))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Header sub-halaman Profil: memakai [TkSubHeader] agar identik dengan sub-halaman lain. */
@Composable
internal fun ProfileSubHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    backDescription: String = "Kembali ke Profil"
) {
    TkSubHeader(
        title = title,
        onBack = onBack,
        modifier = modifier,
        supporting = supporting,
        backDescription = backDescription
    )
}

/** "12 Agustus 2026" */
internal fun formatLongDate(millis: Long): String =
    SimpleDateFormat("d MMMM yyyy", IndonesianLocale).format(Date(millis))

/** "Hari ini, 17.40", "Kemarin, 09.05", atau "3 Okt 2026, 20.15"; [millis] null berarti belum pernah. */
internal fun formatLastBackup(millis: Long?, now: Long = System.currentTimeMillis()): String {
    if (millis == null) return "Belum pernah"
    val time = SimpleDateFormat("HH.mm", IndonesianLocale).format(Date(millis))
    fun dayStart(value: Long) = Calendar.getInstance().apply {
        timeInMillis = value
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val dayDiff = ((dayStart(now) - dayStart(millis)) / 86_400_000L).toInt()
    return when (dayDiff) {
        0 -> "Hari ini, $time"
        1 -> "Kemarin, $time"
        else -> "${SimpleDateFormat("d MMM yyyy", IndonesianLocale).format(Date(millis))}, $time"
    }
}

/** Satu baris detail: ubin ikon proporsional, label kecil, nilai jelas, dan lencana opsional di kanan. */
@Composable
internal fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null
) {
    val isBlank = value == "Belum diisi" || value.isBlank()
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(TkShape.md).background(com.temankuliah.app.ui.theme.TemanKuliahBlueLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = com.temankuliah.app.ui.theme.TemanKuliahBlue, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isBlank) FontWeight.Normal else FontWeight.SemiBold,
                color = if (isBlank) tkAdaptive(Color(0xFF94A3B8), Color(0xFF6B7C9B)) else TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        badge?.invoke()
    }
}

/** Pemisah antar baris detail dengan indentasi ke teks agar rapi. */
@Composable
internal fun ProfileDetailDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 48.dp).height(1.dp).background(DividerLight))
}

/** Sakelar biru bergaya desain: latar biru saat aktif (kanan ON), abu-biru muda saat mati (kiri OFF), kenop putih berikon. */
@Composable
internal fun ProfileSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = com.temankuliah.app.ui.theme.TemanKuliahBlue,
        checkedBorderColor = com.temankuliah.app.ui.theme.TemanKuliahBlue,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = com.temankuliah.app.ui.theme.BorderLight,
        uncheckedBorderColor = com.temankuliah.app.ui.theme.BorderLight
    )
    val thumbContent: (@Composable () -> Unit) = {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "ON",
                modifier = Modifier.size(SwitchDefaults.IconSize),
                tint = com.temankuliah.app.ui.theme.TemanKuliahBlue
            )
        } else {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "OFF",
                modifier = Modifier.size(SwitchDefaults.IconSize),
                tint = com.temankuliah.app.ui.theme.TextMuted
            )
        }
    }
    if (compact) {
        Box(modifier.size(width = 46.dp, height = 28.dp), contentAlignment = Alignment.Center) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = colors,
                thumbContent = thumbContent,
                modifier = Modifier.scale(0.85f)
            )
        }
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = colors,
            thumbContent = thumbContent,
            modifier = modifier
        )
    }
}

/**
 * Tombol utama gradien biru dengan ikon opsional di kiri teks.
 * Lebarnya mengikuti isi (bukan selebar layar) dan diletakkan di tengah; pakai [fullWidth] hanya bila perlu.
 */
@Composable
internal fun ProfileGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    fullWidth: Boolean = false
) {
    val shape = TkShape.card
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Row(
        modifier = Modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier.widthIn(min = 160.dp, max = 320.dp))
            .shadow(
                elevation = tkShadow(6.dp),
                shape = shape,
                ambientColor = TkGradientEnd.copy(alpha = 0.20f),
                spotColor = TkGradientEnd.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(TkGradientStart, TkGradientEnd)))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 28.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
    }
}
