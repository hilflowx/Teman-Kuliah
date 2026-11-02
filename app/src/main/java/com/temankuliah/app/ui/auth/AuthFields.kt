package com.temankuliah.app.ui.auth

import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.temankuliah.app.ui.icons.TkIcons

// Komponen bersama layar masuk/daftar, mengikuti mockup temankuliah-auth-mockup.html:
// label di atas field, field tinggi 56dp berbentuk rounded 16dp dengan border 1,5dp,
// tombol utama setinggi 56dp.

/** Label field; [hintText] tampil rata kanan (mis. kekuatan sandi, "✓ Cocok"). */
@Composable
internal fun AuthFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    hintText: String? = null,
    hintColor: Color = TextMuted
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 18.dp)
            .padding(start = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = TextLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (!hintText.isNullOrEmpty()) {
            Text(text = hintText, color = hintColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Kotak field (latar, border, fokus) yang dipakai field teks maupun pemilih semester. */
@Composable
internal fun AuthFieldSurface(
    focused: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (focused) InputFocusedBorderColor else CardBorderColor,
        label = "AuthFieldBorder"
    )
    val shape = TkShape.md
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (focused) AuthSurface else InputBgColor)
            .border(1.5.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/** Field teks bergaya mockup: label di atas, ikon pemandu opsional, dan aksi di ujung kanan (mis. mata). */
@Composable
internal fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    leadingIcon: ImageVector? = null,
    hintText: String? = null,
    hintColor: Color = TextMuted,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        AuthFieldLabel(text = label, hintText = hintText, hintColor = hintColor)
        Spacer(Modifier.height(8.dp))
        AuthFieldSurface(focused = focused) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (focused) BrandBlue else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused }
                    .then(if (tag != null) Modifier.testTag(tag) else Modifier),
                singleLine = true,
                textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                cursorBrush = SolidColor(BrandBlue),
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = keyboardActions
            )
            trailing?.invoke()
        }
        below?.invoke()
    }
}

/** Field kata sandi dengan tombol mata untuk menampilkan/menyembunyikan. */
@Composable
internal fun AuthPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    leadingIcon: ImageVector? = null,
    hintText: String? = null,
    hintColor: Color = TextMuted,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    below: (@Composable () -> Unit)? = null
) {
    AuthTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        tag = tag,
        leadingIcon = leadingIcon,
        hintText = hintText,
        hintColor = hintColor,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            IconButton(
                onClick = onToggleVisible,
                modifier = Modifier
                    .size(48.dp)
                    .offset(x = 6.dp)
            ) {
                Icon(
                    imageVector = if (visible) TkIcons.VisibilityOff else TkIcons.Visibility,
                    contentDescription = if (visible) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                    tint = if (visible) BrandBlue else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        below = below
    )
}

/** Tombol utama 52dp; saat [loading] menampilkan spinner dan "Memproses...". */
@Composable
internal fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    val active = enabled && !loading
    Button(
        onClick = onClick,
        enabled = active,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = TkShape.md,
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandBlue,
            contentColor = Color.White,
            disabledContainerColor = if (loading) BrandBlue else ButtonDisabledBlue,
            disabledContentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 1.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.5.dp
            )
            Spacer(Modifier.width(8.dp))
            Text("Memproses...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        } else {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

/** Baris "Belum punya akun? Daftar sekarang" / "Sudah punya akun? Masuk di sini". */
@Composable
internal fun AuthSwitchLine(
    prefix: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionTag: String? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(prefix, color = TextMuted, fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = action,
            color = BrandBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .clip(TkShape.sm)
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp, horizontal = 2.dp)
                .then(if (actionTag != null) Modifier.testTag(actionTag) else Modifier)
        )
    }
}
