package com.temankuliah.app.ui.auth

import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.Poppins
import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.tkShadow

// Komponen khusus layar Masuk (mockup baru). Layar Daftar dan Buat Sandi tetap memakai AuthFields.kt.

internal val LoginShape = TkShape.md
internal val LoginFieldIdleBorder: Color get() = tkAdaptive(Color(0xFFDFE9F8), Color(0xFF25365A))
internal val LoginFieldValidBorder: Color get() = tkAdaptive(Color(0xFF8EE3B0), Color(0xFF2F8F68))
internal val LoginFieldIdleBackground: Color get() = tkAdaptive(Color(0xFFF4F8FE), Color(0xFF162238))
internal val LoginFocusGlow: Color get() = tkAdaptive(Color(0xFFE5EFFC), Color(0xFF1F3A66))
internal val LoginPlaceholder: Color get() = tkAdaptive(Color(0xFF8E9BB0), Color(0xFF7686A3))
internal val LoginOutlinedBorder: Color get() = tkAdaptive(Color(0xFFCFE0F8), Color(0xFF2E4166))
internal val LoginButtonGradient = Brush.linearGradient(listOf(Color(0xFF2F87F1), Color(0xFF1E66D3)))

/** Field teks layar Masuk: label tebal di atas, ikon outline, placeholder, dan cincin cahaya biru saat fokus. */
@Composable
internal fun LoginTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    tag: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    valid: Boolean = false,
    invalid: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = when {
            focused -> InputFocusedBorderColor
            invalid -> ErrorRedBorder
            valid -> LoginFieldValidBorder
            else -> LoginFieldIdleBorder
        },
        label = "LoginFieldBorder"
    )
    val glowColor by animateColorAsState(
        targetValue = if (focused) LoginFocusGlow else Color.Transparent,
        label = "LoginFieldGlow"
    )
    val textStyle = TextStyle(fontFamily = Poppins, color = TextMain, fontSize = 15.sp)

    Column(modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextMain,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 2.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                // Cincin cahaya 4 dp digambar di luar kotak tanpa mengubah tata letak.
                .drawBehind {
                    val g = 4.dp.toPx()
                    drawRoundRect(
                        color = glowColor,
                        topLeft = Offset(-g, -g),
                        size = Size(size.width + 2 * g, size.height + 2 * g),
                        cornerRadius = CornerRadius(14.dp.toPx() + g)
                    )
                }
                .clip(LoginShape)
                .background(if (focused) AuthSurface else LoginFieldIdleBackground)
                .border(if (focused) 2.dp else 1.5.dp, borderColor, LoginShape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (focused) BrandBlue else TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(text = placeholder, style = textStyle.copy(color = LoginPlaceholder), maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                        .then(if (tag != null) Modifier.testTag(tag) else Modifier),
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(BrandBlue),
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions(
                        capitalization = capitalization,
                        keyboardType = keyboardType,
                        imeAction = imeAction
                    ),
                    keyboardActions = keyboardActions
                )
            }
            trailing?.invoke()
        }
    }
}

/** Field kata sandi dengan tombol mata (outline) untuk menampilkan atau menyembunyikan. */
@Composable
internal fun LoginPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    valid: Boolean = false,
    invalid: Boolean = false
) {
    LoginTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = TkOutlineIcons.Lock,
        modifier = modifier,
        tag = tag,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        valid = valid,
        invalid = invalid,
        visualTransformation = if (visible) VisualTransformation.None
        else PasswordVisualTransformation(),
        trailing = {
            IconButton(
                onClick = onToggleVisible,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (visible) TkOutlineIcons.EyeOff else TkOutlineIcons.Eye,
                    contentDescription = if (visible) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                    tint = if (visible) BrandBlue else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    )
}

/** Tombol utama bergradasi biru dengan bayangan lembut dan panah di kanan teks. */
@Composable
internal fun LoginPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .shadow(
                elevation = tkShadow(12.dp),
                shape = LoginShape,
                clip = false,
                ambientColor = BrandBlue.copy(alpha = 0.25f),
                spotColor = BrandBlue.copy(alpha = 0.45f)
            )
            .clip(LoginShape)
            .background(LoginButtonGradient)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp)
                Text("Memproses...", color = Color.White, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Text(text, color = Color.White, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Icon(
                    imageVector = TkOutlineIcons.ArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** Tombol sekunder berlatar putih dengan garis biru muda dan ikon di kiri teks. */
@Composable
internal fun LoginOutlinedButton(
    text: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val content = if (enabled) BrandBlue else TextSubtle
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(LoginShape)
            .background(AuthSurface)
            .border(1.5.dp, LoginOutlinedBorder, LoginShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            Text(text, color = content, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

/** Pembagi "───── atau ─────". */
@Composable
internal fun LoginOrDivider(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(CardBorderColor))
        Text(
            text = text,
            color = TextSubtle,
            fontFamily = Poppins,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Box(Modifier.weight(1f).height(1.dp).background(CardBorderColor))
    }
}
