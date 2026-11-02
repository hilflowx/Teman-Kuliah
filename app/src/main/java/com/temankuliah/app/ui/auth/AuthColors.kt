package com.temankuliah.app.ui.auth

import androidx.compose.ui.graphics.Color
import com.temankuliah.app.ui.theme.tkAdaptive

// Warna khusus layar masuk/daftar; dipakai bersama oleh AuthScreens, LoginContent, RegisterContent.
internal val BrandBlue: Color get() = tkAdaptive(Color(0xFF1F6FD9), Color(0xFF3B8BF5))
internal val BrandBlueDark: Color get() = tkAdaptive(Color(0xFF1858B8), Color(0xFF8CC4FF))
internal val TextMain: Color get() = tkAdaptive(Color(0xFF16335E), Color(0xFFF2F6FF))
internal val TextSecondary: Color get() = tkAdaptive(Color(0xFF2F4A73), Color(0xFFB4C3DE))
internal val TextMuted: Color get() = tkAdaptive(Color(0xFF5B7194), Color(0xFF8C9CBA))
internal val TextSubtle: Color get() = tkAdaptive(Color(0xFF8FA3C0), Color(0xFF7A8CAB))
internal val CardBorderColor: Color get() = tkAdaptive(Color(0xFFDCE9FA), Color(0xFF1F2D47))
internal val InputBorderColor: Color get() = tkAdaptive(Color(0xFFB9CDE8), Color(0xFF2E4166))
internal val InputFocusedBorderColor: Color get() = tkAdaptive(Color(0xFF1F6FD9), Color(0xFF3B8BF5))
internal val InputBgColor: Color get() = tkAdaptive(Color(0xFFF6FAFF), Color(0xFF162238))
internal val SuccessGreen: Color get() = tkAdaptive(Color(0xFF10B981), Color(0xFF34D399))
internal val WarningOrange = Color(0xFFF59E0B)

internal val ErrorRed: Color get() = tkAdaptive(Color(0xFFEF4444), Color(0xFFFF6B6B))
internal val ErrorRedLight: Color get() = tkAdaptive(Color(0xFFFEF2F2), Color(0xFF3D1A20))
internal val ErrorRedBorder: Color get() = tkAdaptive(Color(0xFFFECACA), Color(0xFF7A2E36))
// Tambahan sesuai mockup login/daftar baru.
internal val TextLabel: Color get() = tkAdaptive(Color(0xFF4A6288), Color(0xFFA3B3D1))
internal val ButtonDisabledBlue: Color get() = tkAdaptive(Color(0xFFBDD8F6), Color(0xFF24406F))
internal val SuccessGreenText: Color get() = tkAdaptive(Color(0xFF047857), Color(0xFF5FE0A8))
// Permukaan layar masuk/daftar: putih di tema terang, permukaan kartu di tema gelap.
internal val AuthSurface: Color get() = tkAdaptive(Color.White, Color(0xFF111A2C))
