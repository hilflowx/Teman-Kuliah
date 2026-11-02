package com.temankuliah.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.icons.ArrowBack
import com.temankuliah.app.ui.theme.BorderLight
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.TkSize
import com.temankuliah.app.ui.theme.TkText
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import com.temankuliah.app.ui.theme.TkSpace

/*
 * Komponen bersama sistem desain TemanKuliah (iOS-inspired, soft blue, modern).
 */

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = TkShape.md,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = TemanKuliahBlue,
        contentColor = Color.White
    ),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.pressScale(interactionSource).heightIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = TkShape.md,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = TemanKuliahBlueDark),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = BorderStroke(1.dp, BorderLight),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.pressScale(interactionSource).heightIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/** Warna kolom isian: transparan dengan indikator garis bawah atau border */
@Composable
fun tkFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedIndicatorColor = TemanKuliahBlue,
    unfocusedIndicatorColor = BorderLight,
    errorIndicatorColor = TemanKuliahRedDark,
    focusedLabelColor = TemanKuliahBlueDark,
    unfocusedLabelColor = TextSecondary,
    errorLabelColor = TemanKuliahRedDark,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = TemanKuliahBlue
)

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    @Suppress("UNUSED_PARAMETER") shape: Shape = RectangleShape,
    colors: TextFieldColors = tkFieldColors()
) {
    androidx.compose.material3.TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = RectangleShape,
        colors = colors
    )
}

/** Judul bagian dengan aksi seragam (mis. "Lihat semua"). */
@Composable
fun TkSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(actionLabel, color = TemanKuliahBlueDark, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Kartu putih bersih modern: latar putih tanpa garis tepi kasar, tanpa elevasi berlebih, sudut membulat 16dp.
 */
@Composable
fun TkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = SurfaceLight,
    shape: Shape = TkShape.card,
    border: BorderStroke? = BorderStroke(1.dp, DividerLight),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = if (onClick != null) modifier.pressScale() else modifier,
        shape = shape,
        color = color,
        border = border,
        shadowElevation = 0.dp
    ) {
        if (onClick != null) {
            Box(Modifier.clickable(role = Role.Button, onClick = onClick)) {
                Column(content = content)
            }
        } else {
            Column(content = content)
        }
    }
}

/** Inset grouped list card gaya iOS untuk mengelompokkan baris menu atau opsi. */
@Composable
fun TkGroupedList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        shadowElevation = 0.dp
    ) {
        Column(content = content)
    }
}

/**
 * Kontrol tersegmentasi (Segmented Control) gaya iOS untuk beralih filter atau mode.
 */
@Composable
fun <T> TkSegmentedControl(
    items: List<T>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String = { it.toString() }
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .height(IntrinsicSize.Min),
        shape = TkShape.md,
        color = TemanKuliahBlueLight
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) TemanKuliahBlueDark else TextSecondary,
                    animationSpec = tween(150, easing = FastOutSlowInEasing),
                    label = "segmentedText"
                )
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) SurfaceLight else Color.Transparent,
                    animationSpec = tween(150, easing = FastOutSlowInEasing),
                    label = "segmentedBg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(TkShape.sm)
                        .background(backgroundColor)
                        .clickable(
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(item),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Ubin ikon bulat bernuansa soft blue. */
@Composable
fun TkIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = TemanKuliahBlue,
    background: Color = TemanKuliahBlueLight,
    shape: Shape = CircleShape
) {
    Surface(modifier = modifier.size(size), shape = shape, color = background) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** Pil status atau label kecil. */
@Composable
fun TkPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = TemanKuliahBlueLight,
    content: Color = TemanKuliahBlueDark,
    shape: Shape = TkShape.sm
) {
    Surface(modifier = modifier, shape = shape, color = container) {
        Text(
            text,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/**
 * Keadaan kosong (Empty State): informatif, bersih, dan mendukung tombol aksi.
 */
@Composable
fun TkEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    if (compact) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TkIconTile(icon, size = 38.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(actionLabel, color = TemanKuliahBlueDark, style = MaterialTheme.typography.titleMedium)
                }
            } else if (action != null) {
                action()
            }
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TkSpace.xl, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TkIconTile(icon, size = 52.dp)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAction,
                shape = TkShape.md
            ) {
                Text(actionLabel)
            }
        } else if (action != null) {
            action()
        }
    }
}

/** Pesan galat formulir. */
@Composable
fun TkFormError(message: String?) {
    if (message != null) {
        Text(
            message,
            color = TemanKuliahRedDark,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

/** Tombol persegi membulat berbingkai untuk header (kembali, tutup). Ukuran dan bingkai sama di seluruh aplikasi. */
@Composable
fun TkHeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = TkShape.md,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        modifier = modifier.size(TkSize.headerButton).pressScale()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Judul + keterangan; dipakai header halaman, sub-halaman, dan dialog layar penuh agar tipografinya seragam. */
@Composable
fun TkTitleBlock(
    title: String,
    supporting: String?,
    titleStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.semantics { heading() }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = titleStyle,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (supporting != null) {
            Text(
                supporting,
                style = TkText.supporting,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Header tab utama (Jadwal, Tugas, Proyek, Akademik, Profil): judul 24sp rata kiri, keterangan, dan slot aksi
 * opsional di kanan. Tanpa padding horizontal sendiri; pemanggil memberi margin halaman [TkSpace.page].
 */
@Composable
fun TkPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TkTitleBlock(
            title = title,
            supporting = supporting,
            titleStyle = TkText.pageTitle,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Header sub-halaman: tombol kembali berbingkai di kiri, judul 18sp dan keterangan di sebelahnya. */
@Composable
fun TkSubHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    backDescription: String = "Kembali",
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TkSpace.page, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TkHeaderIconButton(icon = Icons.Default.ArrowBack, contentDescription = backDescription, onClick = onBack)
        Spacer(Modifier.width(12.dp))
        TkTitleBlock(
            title = title,
            supporting = supporting,
            titleStyle = TkText.screenTitle,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Isi AlertDialog yang bisa di-scroll agar teks panjang atau font besar tidak terpotong dan tombol tetap terlihat. */
@Composable
fun TkScrollableDialogText(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), content = content)
}

/**
 * Dialog konfirmasi bergaya iOS: sudut membulat 18dp, latar putih bersih,
 * tombol teks sejajar di bagian bawah dipisahkan garis pemisah tipis.
 */
@Composable
fun TkAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmLabel: String = "Ya",
    dismissLabel: String = "Batal",
    confirmColor: Color = TemanKuliahBlueDark,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        val reducedMotion = rememberReducedMotion()
        var appeared by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(Unit) { appeared = true }
        val dialogScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (appeared || reducedMotion) 1f else 0.92f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = MotionTokens.DampingBouncy,
                stiffness = MotionTokens.StiffnessMediumLow
            ),
            label = "dialogScale"
        )
        val dialogAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (appeared || reducedMotion) 1f else 0f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
            label = "dialogAlpha"
        )
        Surface(
            shape = TkShape.lg,
            color = SurfaceLight,
            shadowElevation = 0.dp,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    scaleX = dialogScale
                    scaleY = dialogScale
                    alpha = dialogAlpha
                }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Isi bisa di-scroll agar tombol aksi tetap terlihat pada layar pendek atau font besar.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(DividerLight)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(onClick = onDismissRequest, role = Role.Button),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dismissLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(0.8.dp)
                            .fillMaxHeight()
                            .background(DividerLight)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(onClick = onConfirm, role = Role.Button),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDestructive) TemanKuliahRedDark else confirmColor
                        )
                    }
                }
            }
        }
    }
}

