package com.temankuliah.app

import androidx.compose.ui.graphics.Color
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.BorderLight
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TemanKuliahGreenDark
import com.temankuliah.app.ui.theme.TemanKuliahGreenLight
import com.temankuliah.app.ui.theme.TemanKuliahOrangeLight
import com.temankuliah.app.ui.theme.TemanKuliahRedDark
import com.temankuliah.app.ui.theme.TemanKuliahRedLight
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkInk
import com.temankuliah.app.ui.theme.TemanKuliahOrangeDark
import com.temankuliah.app.ui.theme.TextMuted
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorContrastTest {
    @Test
    fun primaryBlueMeetsNormalTextContrastAgainstWhite() {
        assertTrue(contrastRatio(TemanKuliahBlue, Color.White) >= 4.5)
    }

    @Test
    fun filledOrangeControlsMeetNormalTextContrastAgainstWhite() {
        assertTrue(contrastRatio(TemanKuliahOrangeDark, Color.White) >= 4.5)
    }

    @Test
    fun mutedTextMeetsNormalTextContrastAgainstWhite() {
        assertTrue(contrastRatio(TextMuted, Color.White) >= 4.5)
    }

    @Test
    fun timeBadgeTextMeetsContrastAgainstInk() {
        assertTrue(contrastRatio(Color.White, TkInk) >= 4.5)
    }

    @Test
    fun blueTextMeetsContrastOnLightSurfaces() {
        assertTrue(contrastRatio(TemanKuliahBlueDark, TemanKuliahBlueLight) >= 4.5)
        assertTrue(contrastRatio(TemanKuliahBlue, BackgroundLight) >= 4.5)
        assertTrue(contrastRatio(Color.White, TemanKuliahBlueDark) >= 4.5)
    }

    @Test
    fun statusPairsMeetContrast() {
        assertTrue(contrastRatio(TemanKuliahGreenDark, TemanKuliahGreenLight) >= 4.5)
        assertTrue(contrastRatio(TemanKuliahRedDark, TemanKuliahRedLight) >= 4.5)
        assertTrue(contrastRatio(TemanKuliahOrangeDark, TemanKuliahOrangeLight) >= 4.5)
        assertTrue(contrastRatio(Color.White, TemanKuliahRedDark) >= 4.5)
    }

    @Test
    fun secondaryTextMeetsContrastOnPageBackground() {
        assertTrue(contrastRatio(TextSecondary, BackgroundLight) >= 4.5)
        assertTrue(contrastRatio(TextMuted, BackgroundLight) >= 4.5)
    }

    @Test
    fun fieldAndChipOutlineMeetsNonTextContrastAgainstWhite() {
        assertTrue(contrastRatio(BorderLight, Color.White) >= 3.0)
    }

    @Test
    fun darkConflictWarningPairsMeetContrast() {
        // Peringatan jadwal bentrok di tema gelap (AddScheduleDialog): teks pada isian dan kartu.
        assertTrue(contrastRatio(Color.White, Color(0xFFB45309)) >= 4.5)
        assertTrue(contrastRatio(Color(0xFFFFA566), Color(0xFF3A2412)) >= 4.5)
        assertTrue(contrastRatio(Color(0xFFF5A524), Color(0xFF3A2412)) >= 3.0)
    }

    @Test
    fun darkAuthPairsMeetContrast() {
        val surface = Color(0xFF111A2C)
        assertTrue(contrastRatio(Color(0xFFF2F6FF), surface) >= 4.5) // TextMain
        assertTrue(contrastRatio(Color(0xFFB4C3DE), surface) >= 4.5) // TextSecondary
        assertTrue(contrastRatio(Color(0xFF8C9CBA), surface) >= 4.5) // TextMuted
        assertTrue(contrastRatio(Color(0xFFA3B3D1), surface) >= 4.5) // TextLabel
        assertTrue(contrastRatio(Color(0xFF3B8BF5), surface) >= 4.5) // BrandBlue sebagai teks tautan
        assertTrue(contrastRatio(Color(0xFFFF8A8A), Color(0xFF3D1A20)) >= 4.5) // galat
        assertTrue(contrastRatio(Color(0xFF5FE0A8), surface) >= 4.5) // sukses
        assertTrue(contrastRatio(Color(0xFF7686A3), Color(0xFF162238)) >= 3.0) // placeholder
    }

    @Test
    fun darkGradeBadgePairsMeetContrast() {
        assertTrue(contrastRatio(Color(0xFF5FE0A8), Color(0xFF12352A)) >= 4.5)
        assertTrue(contrastRatio(Color(0xFF8CC4FF), Color(0xFF1A2A48)) >= 4.5)
        assertTrue(contrastRatio(Color(0xFFFFA566), Color(0xFF3A2412)) >= 4.5)
        assertTrue(contrastRatio(Color(0xFFFF8A8A), Color(0xFF3D1A20)) >= 4.5)
    }

    @Test
    fun darkFilledButtonsMeetContrast() {
        // Tombol biru gelap #3B8BF5 berteks putih tebal: pengecualian tercatat di PRD 7.4 (≥ 3:1 untuk teks tebal besar).
        assertTrue(contrastRatio(Color.White, Color(0xFF3B8BF5)) >= 3.0)
        // Isian merah/hijau pekat berteks putih wajib ≥ 4,5:1.
        assertTrue(contrastRatio(Color.White, Color(0xFF12805C)) >= 4.5)
        assertTrue(contrastRatio(Color.White, Color(0xFFC93A3A)) >= 4.5)
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        val lighter = maxOf(firstLuminance, secondLuminance)
        val darker = minOf(firstLuminance, secondLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }

        return 0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)
    }
}
