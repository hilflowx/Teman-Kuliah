package com.temankuliah.app.ui.components

import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.icons.Add
import com.temankuliah.app.ui.icons.Clear
import com.temankuliah.app.ui.icons.Search
import com.temankuliah.app.ui.icons.Tune
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TextMuted
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkGradientEnd
import com.temankuliah.app.ui.theme.TkGradientStart
import com.temankuliah.app.ui.theme.tkShadow

/*
 * Rangka bersama halaman Tugas dan Proyek Kuliah (sesuai desain "Redesain Tugas & Proyek Kuliah"):
 * header judul + tombol Tambah, pemilih dua segmen berikon, kartu statistik biru, kolom cari dengan
 * tombol filter, chip filter, dan keadaan kosong berbingkai putus-putus.
 */

/** Tombol Tambah biru berbentuk pil di kanan header; satu-satunya tombol tambah di halaman. */
@Composable
fun TkAddPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Tambah",
    contentDescription: String = label,
    icon: ImageVector = Icons.Default.Add
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = TemanKuliahBlue,
        shadowElevation = tkShadow(6.dp),
        modifier = modifier.semantics { this.contentDescription = contentDescription }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Judul halaman dengan keterangan, dan tombol Tambah di kanan. Tanpa padding horizontal sendiri. */
@Composable
fun TkTitleActionHeader(
    title: String,
    supporting: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    addTag: String? = null,
    addDescription: String = "Tambah"
) {
    TkPageHeader(
        title = title,
        supporting = supporting,
        modifier = modifier,
        trailing = {
            TkAddPillButton(
                onClick = onAdd,
                contentDescription = addDescription,
                modifier = if (addTag != null) Modifier.testTag(addTag) else Modifier
            )
        }
    )
}

data class TkSwitchOption(val key: String, val label: String, val icon: ImageVector)

/** Pemilih dua segmen berikon: segmen terpilih putih berteks biru, sisanya abu-abu di atas latar biru muda. */
@Composable
fun TkIconSwitch(
    options: List<TkSwitchOption>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TkShape.card)
            .background(TemanKuliahBlueLight.copy(alpha = 0.7f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.key == selectedKey
            val tint = if (isSelected) TemanKuliahBlueDark else TextSecondary
            Surface(
                onClick = { onSelect(option.key) },
                shape = TkShape.lg,
                color = if (isSelected) SurfaceLight else Color.Transparent,
                shadowElevation = if (isSelected) tkShadow(2.dp) else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .semantics { role = Role.Tab; this.selected = isSelected }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(option.icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = tint,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Satu angka statistik pada kartu biru. [key] non-null membuat ubin bisa diketuk sebagai filter. */
data class TkHeroStat(val label: String, val value: Int, val key: String? = null)

/** Kartu gradien biru berisi tiga ubin statistik. */
@Composable
fun TkHeroStatsCard(
    stats: List<TkHeroStat>,
    modifier: Modifier = Modifier,
    selectedKey: String? = null,
    onSelect: ((String) -> Unit)? = null
) {
    val shape = TkShape.xl
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = tkShadow(16.dp),
                shape = shape,
                ambientColor = TkGradientEnd.copy(alpha = 0.25f),
                spotColor = TkGradientEnd.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(TkGradientStart, TkGradientEnd)))
            .tkHeroCircles()
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.forEach { stat ->
            val isSelected = stat.key != null && stat.key == selectedKey
            val tileModifier = Modifier.weight(1f)
            val content: @Composable () -> Unit = {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stat.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stat.value.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val color = Color.White.copy(alpha = if (isSelected) 0.34f else 0.16f)
            if (stat.key != null && onSelect != null) {
                Surface(
                    onClick = { onSelect(stat.key) },
                    shape = TkShape.lg,
                    color = color,
                    modifier = tileModifier.semantics { this.selected = isSelected },
                    content = content
                )
            } else {
                Surface(shape = TkShape.lg, color = color, modifier = tileModifier, content = content)
            }
        }
    }
}

/** Kolom cari berbentuk pil dengan tombol filter lanjutan di kanan (bertitik oranye bila filter aktif). */
@Composable
fun TkSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onFilterClick: () -> Unit,
    filterActive: Boolean,
    filterDescription: String,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(22.dp))
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                Box {
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = filterDescription,
                            tint = if (filterActive) TemanKuliahBlueDark else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (filterActive) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TemanKuliahOrangeDark)
                        )
                    }
                }
            }
        },
        singleLine = true,
        shape = TkShape.xl,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceLight,
            unfocusedContainerColor = SurfaceLight,
            focusedBorderColor = TemanKuliahBlue,
            unfocusedBorderColor = DividerLight
        ),
        modifier = modifier
            .fillMaxWidth()
            .let { if (testTag != null) it.testTag(testTag) else it }
    )
}

/** Baris chip pil; [label] memetakan kunci filter internal ke teks tampilan (mis. "Hari Ini" menjadi "Hari ini"). */
@Composable
fun TkPillChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (String) -> String = { it }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                shape = CircleShape,
                color = if (isSelected) TemanKuliahBlue else SurfaceLight,
                border = if (isSelected) null else BorderStroke(1.dp, DividerLight),
                modifier = Modifier.semantics { role = Role.RadioButton; this.selected = isSelected }
            ) {
                Text(
                    text = label(option),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Keadaan kosong: kotak berbingkai putus-putus dengan ubin ikon, judul, dan pesan. */
@Composable
fun TkDashedEmptyCard(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null
) {
    val dashColor = tkAdaptive(Color(0xFFB9D3F2), Color(0xFF2C4268))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(TkShape.card)
            .background(BackgroundLight)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawRoundRect(
                    color = dashColor,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(width = strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                )
            }
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = TkShape.card,
            color = SurfaceLight,
            shadowElevation = tkShadow(6.dp),
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(30.dp))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        footer?.invoke()
    }
}
