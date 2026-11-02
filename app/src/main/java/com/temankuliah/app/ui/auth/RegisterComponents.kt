package com.temankuliah.app.ui.auth

import com.temankuliah.app.ui.theme.TkShape
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.temankuliah.app.ui.icons.TkOutlineIcons
import com.temankuliah.app.ui.theme.Poppins
import com.temankuliah.app.ui.theme.tkAdaptive
import com.temankuliah.app.ui.theme.tkShadow

// Komponen khusus layar Daftar 3 langkah (header biru + stepper, tab kontak, pengukur kekuatan sandi,
// kartu ringkasan). Field teks dan tombol dasar memakai gaya layar Masuk (LoginComponents.kt).

internal val RegisterSheetCorner = 30.dp

private val HeaderTop = Color(0xFF3A8DF2)
private val HeaderBottom = Color(0xFF1F66D0)
private val HeaderCircleColor = Color.White.copy(alpha = 0.07f)
private val SegmentTrack: Color get() = tkAdaptive(Color(0xFFEEF3FB), Color(0xFF162238))
private val MeterTrack: Color get() = tkAdaptive(Color(0xFFE3EAF5), Color(0xFF1F2D47))
private val SoftDivider: Color get() = tkAdaptive(Color(0xFFEDF2FA), Color(0xFF1A2640))
private val SoftSurface: Color get() = tkAdaptive(Color(0xFFEAF2FE), Color(0xFF1A2A48))
private val EditPillBackground: Color get() = tkAdaptive(Color(0xFFE8F0FD), Color(0xFF1A2A48))
private val AmberText: Color get() = tkAdaptive(Color(0xFFB45309), Color(0xFFFFA566))
private val WeakText: Color get() = tkAdaptive(Color(0xFFB91C1C), Color(0xFFFF8A8A))
private const val STRONG_MIN_LENGTH = 8
private const val LONG_PASSWORD_LENGTH = 12
private const val SCORE_GOOD = 3
private const val SCORE_STRONG = 4

/** Teks Poppins ringkas supaya tiap pemanggilan tidak mengulang fontFamily. */
@Composable
internal fun RegText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextMain,
    size: TextUnit = 14.sp,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = Poppins,
        fontSize = size,
        fontWeight = weight,
        textAlign = align,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow
    )
}

// -------------------------------------------------------------------------------------------------
// Header + stepper
// -------------------------------------------------------------------------------------------------

/**
 * Header biru: tombol kembali, "Langkah n dari 3", stepper berlabel, judul, dan subjudul.
 * Digambar di bawah status bar; lembar putih berikutnya menimpa [RegisterSheetCorner] bagian bawahnya.
 */
@Composable
internal fun RegisterHeader(
    step: Int,
    totalSteps: Int,
    title: String,
    subtitle: String,
    backEnabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
            .drawBehind {
                drawCircle(
                    color = HeaderCircleColor,
                    radius = 130.dp.toPx(),
                    center = Offset(size.width - 40.dp.toPx(), 50.dp.toPx())
                )
                drawCircle(
                    color = HeaderCircleColor,
                    radius = 100.dp.toPx(),
                    center = Offset(20.dp.toPx(), size.height * 0.78f)
                )
            }
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = RegisterSheetCorner + 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
                    .clickable(enabled = backEnabled, role = Role.Button, onClick = onBack)
                    .testTag("btn_register_back"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TkOutlineIcons.ChevronLeft,
                    contentDescription = "Kembali",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            RegText(
                text = "Langkah $step dari $totalSteps",
                color = Color.White.copy(alpha = 0.95f),
                size = 13.sp,
                weight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(14.dp))
        RegisterStepper(step = step)
        Spacer(Modifier.height(22.dp))

        Crossfade(targetState = title to subtitle, label = "RegisterHeaderTitle") { (t, sub) ->
            Column {
                RegText(
                    text = t,
                    color = Color.White,
                    size = 26.sp,
                    weight = FontWeight.Bold,
                    lineHeight = 34.sp
                )
                Spacer(Modifier.height(4.dp))
                RegText(
                    text = sub,
                    color = Color.White.copy(alpha = 0.92f),
                    size = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

private val StepLabels = listOf("Data diri", "Akun", "Tinjau")

@Composable
private fun RegisterStepper(step: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        StepLabels.forEachIndexed { index, label ->
            val number = index + 1
            StepNode(number = number, label = label, step = step)
            if (index < StepLabels.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 19.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (step > number) 1f else 0.35f))
                )
            }
        }
    }
}

@Composable
private fun StepNode(number: Int, label: String, step: Int) {
    val done = step > number
    val active = step == number
    Column(
        modifier = Modifier.widthIn(min = 64.dp, max = 88.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (active) {
                Box(Modifier.size(40.dp).background(Color.White.copy(alpha = 0.28f), CircleShape))
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (done || active) Color.White else Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(
                        imageVector = TkOutlineIcons.Check,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    RegText(
                        text = number.toString(),
                        color = if (active) BrandBlue else Color.White.copy(alpha = 0.9f),
                        size = 13.sp,
                        weight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        RegText(
            text = label,
            color = Color.White.copy(alpha = if (active || done) 1f else 0.75f),
            size = 12.sp,
            weight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            align = TextAlign.Center,
            maxLines = 2
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Langkah 1: pemilih semester
// -------------------------------------------------------------------------------------------------

/** Pemilih semester (dropdown) dengan gaya field layar Masuk. */
@Composable
internal fun RegisterSemesterField(
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxSemester: Int = 8
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        RegText(
            text = "Semester",
            size = 13.sp,
            weight = FontWeight.Bold,
            modifier = Modifier.padding(start = 2.dp)
        )
        Spacer(Modifier.height(8.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(LoginShape)
                    .background(if (expanded) AuthSurface else LoginFieldIdleBackground)
                    .border(
                        width = if (expanded) 2.dp else 1.5.dp,
                        color = if (expanded) InputFocusedBorderColor else LoginFieldIdleBorder,
                        shape = LoginShape
                    )
                    .clickable(role = Role.DropdownList) { expanded = true }
                    .padding(horizontal = 16.dp)
                    .testTag("input_register_semester"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RegText(text = selected.toString(), size = 15.sp, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = TkOutlineIcons.ChevronDown,
                    contentDescription = "Pilih semester",
                    tint = BrandBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(AuthSurface)
            ) {
                for (sem in 1..maxSemester) {
                    DropdownMenuItem(
                        text = {
                            RegText(
                                text = "Semester $sem",
                                color = if (sem == selected) BrandBlue else TextMain,
                                weight = if (sem == selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        onClick = {
                            onSelected(sem)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Langkah 2: tab Email / Nomor HP + pengukur kekuatan sandi
// -------------------------------------------------------------------------------------------------

/** Kontrol segmen "Email | Nomor HP". */
@Composable
internal fun RegisterContactTabs(
    isPhone: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .height(IntrinsicSize.Min)
            .clip(TkShape.lg)
            .background(SegmentTrack)
            .padding(4.dp)
    ) {
        ContactTab(
            text = "Email",
            icon = TkOutlineIcons.Mail,
            selected = !isPhone,
            onClick = { onChange(false) },
            modifier = Modifier.weight(1f).testTag("tab_register_email")
        )
        ContactTab(
            text = "Nomor HP",
            icon = TkOutlineIcons.Phone,
            selected = isPhone,
            onClick = { onChange(true) },
            modifier = Modifier.weight(1f).testTag("tab_register_phone")
        )
    }
}

@Composable
private fun ContactTab(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = TkShape.md
    Row(
        modifier = modifier
            .fillMaxHeight()
            .then(if (selected) Modifier.shadow(tkShadow(3.dp), shape, clip = false) else Modifier)
            .clip(shape)
            .background(if (selected) AuthSurface else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) BrandBlue else TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        RegText(
            text = text,
            color = if (selected) BrandBlue else TextMuted,
            size = 14.sp,
            weight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

internal data class PasswordStrength(
    val score: Int,
    val label: String,
    val barColor: Color,
    val textColor: Color,
    val hasMinLength: Boolean,
    val hasLettersAndDigits: Boolean,
    val hasSymbol: Boolean
)

/** Skor 0..4: panjang ≥ 8, huruf + angka, simbol, panjang ≥ 12. Terisi berarti minimal skor 1. */
internal fun passwordStrength(password: String): PasswordStrength {
    val minLength = password.length >= STRONG_MIN_LENGTH
    val lettersAndDigits = password.any { it.isLetter() } && password.any { it.isDigit() }
    val symbol = password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    val long = password.length >= LONG_PASSWORD_LENGTH
    val raw = listOf(minLength, lettersAndDigits, symbol, long).count { it }
    val score = if (password.isEmpty()) 0 else raw.coerceAtLeast(1)
    return when (score) {
        0 -> PasswordStrength(0, "", MeterTrack, TextMuted, minLength, lettersAndDigits, symbol)
        1 -> PasswordStrength(1, "Lemah", ErrorRed, WeakText, minLength, lettersAndDigits, symbol)
        2 -> PasswordStrength(2, "Cukup", WarningOrange, AmberText, minLength, lettersAndDigits, symbol)
        SCORE_GOOD -> PasswordStrength(SCORE_GOOD, "Baik", BrandBlue, BrandBlueDark, minLength, lettersAndDigits, symbol)
        else -> PasswordStrength(SCORE_STRONG, "Kuat", SuccessGreen, SuccessGreenText, minLength, lettersAndDigits, symbol)
    }
}

/** 4 batang kekuatan, label "Kekuatan sandi", dan daftar syarat (✓ terpenuhi / ○ belum). */
@Composable
internal fun PasswordStrengthMeter(password: String, modifier: Modifier = Modifier) {
    val strength = passwordStrength(password)
    Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (index < strength.score) strength.barColor else MeterTrack)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RegText(text = "Kekuatan sandi", color = TextMuted, size = 12.sp)
            if (strength.label.isNotEmpty()) {
                RegText(
                    text = strength.label,
                    color = strength.textColor,
                    size = 12.sp,
                    weight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val rules = listOf(
            strength.hasMinLength to "Min. 8 karakter",
            strength.hasLettersAndDigits to "Huruf & angka",
            strength.hasSymbol to "Simbol"
        )
        Text(
            text = buildAnnotatedString {
                rules.forEachIndexed { index, (ok, label) ->
                    if (index > 0) append("   ")
                    withStyle(
                        SpanStyle(
                            color = if (ok) SuccessGreenText else TextMuted,
                            fontWeight = if (ok) FontWeight.Medium else FontWeight.Normal
                        )
                    ) {
                        append(if (ok) "✓ " else "○ ")
                        append(label)
                    }
                }
            },
            fontFamily = Poppins,
            fontSize = 12.sp,
            lineHeight = 19.sp
        )
    }
}

/** Keterangan di bawah field "Ulangi kata sandi". */
@Composable
internal fun ConfirmPasswordHint(password: String, confirm: String, modifier: Modifier = Modifier) {
    if (confirm.isEmpty()) return
    val match = confirm == password
    RegText(
        text = if (match) "✓ Kata sandi cocok" else "Kata sandi belum cocok",
        color = if (match) SuccessGreenText else AmberText,
        size = 12.sp,
        weight = FontWeight.Medium,
        modifier = modifier.padding(top = 8.dp, start = 2.dp)
    )
}

// -------------------------------------------------------------------------------------------------
// Langkah 3: ringkasan
// -------------------------------------------------------------------------------------------------

/** Kartu profil ringkas: avatar inisial, nama, dan NIM. */
@Composable
internal fun ReviewProfileCard(fullName: String, nim: String, modifier: Modifier = Modifier) {
    val initials = fullName.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TkShape.card)
            .background(SoftSurface)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(tkShadow(8.dp), CircleShape, clip = false, ambientColor = BrandBlue, spotColor = BrandBlue)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF2F87F1), Color(0xFF1E66D3)))),
            contentAlignment = Alignment.Center
        ) {
            RegText(text = initials, color = Color.White, size = 18.sp, weight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            RegText(
                text = fullName.ifBlank { "-" },
                size = 17.sp,
                weight = FontWeight.Bold,
                maxLines = 2
            )
            RegText(
                text = "NIM ${nim.ifBlank { "-" }}",
                color = TextMuted,
                size = 13.sp,
                maxLines = 1
            )
        }
    }
}

/** Bagian ringkasan (mis. "Data kuliah") dengan tombol Ubah dan baris label–nilai. */
@Composable
internal fun ReviewSection(
    title: String,
    rows: List<Pair<String, String>>,
    onEdit: () -> Unit,
    editTag: String,
    modifier: Modifier = Modifier
) {
    val shape = TkShape.card
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AuthSurface)
            .border(1.5.dp, CardBorderColor, shape)
            .padding(horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RegText(text = title, size = 15.sp, weight = FontWeight.Bold)
            RegText(
                text = "Ubah",
                color = BrandBlueDark,
                size = 12.sp,
                weight = FontWeight.Bold,
                modifier = Modifier
                    .clip(TkShape.md)
                    .background(EditPillBackground)
                    .clickable(role = Role.Button, onClick = onEdit)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag(editTag)
            )
        }
        rows.forEach { (label, value) ->
            HorizontalDivider(color = SoftDivider)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                RegText(text = label, color = TextMuted, size = 13.sp)
                RegText(
                    text = value.ifBlank { "-" },
                    size = 13.sp,
                    weight = FontWeight.Bold,
                    align = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Kotak centang persetujuan kebijakan privasi. */
@Composable
internal fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val boxShape = TkShape.sm
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TkShape.md)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 6.dp)
            .testTag("check_register_consent"),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
                .clip(boxShape)
                .background(if (checked) BrandBlue else AuthSurface)
                .then(if (checked) Modifier else Modifier.border(1.5.dp, InputBorderColor, boxShape)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = TkOutlineIcons.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = buildAnnotatedString {
                append("Saya menyetujui ")
                withStyle(SpanStyle(color = BrandBlueDark, fontWeight = FontWeight.Bold)) {
                    append("Kebijakan Privasi")
                }
                append(" dan pengelolaan data akademik TemanKuliah.")
            },
            color = TextMuted,
            fontFamily = Poppins,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Tombol utama + bar bawah
// -------------------------------------------------------------------------------------------------

/** Tombol utama 54 dp: gradasi biru + bayangan saat aktif, biru pucat saat nonaktif. */
@Composable
internal fun RegisterPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val active = enabled && !loading
    val lit = enabled || loading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .then(
                if (lit) {
                    Modifier.shadow(
                        elevation = tkShadow(12.dp),
                        shape = LoginShape,
                        clip = false,
                        ambientColor = BrandBlue.copy(alpha = 0.25f),
                        spotColor = BrandBlue.copy(alpha = 0.45f)
                    )
                } else Modifier
            )
            .clip(LoginShape)
            .then(
                if (lit) Modifier.background(LoginButtonGradient)
                else Modifier.background(ButtonDisabledBlue)
            )
            .clickable(enabled = active, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp)
                RegText(text = "Memproses...", color = Color.White, size = 16.sp, weight = FontWeight.Bold)
            }
        } else {
            RegText(text = text, color = Color.White, size = 16.sp, weight = FontWeight.Bold)
        }
    }
}

/** Bar aksi bawah: garis tipis di atas, tombol utama di dalam. */
@Composable
internal fun RegisterBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().background(AuthSurface).navigationBarsPadding()) {
        HorizontalDivider(color = SoftDivider)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 14.dp)
        ) { content() }
    }
}

/** Tinggi default celah vertikal antarfield pada langkah form. */
internal val RegisterFieldGap: Dp = 16.dp
