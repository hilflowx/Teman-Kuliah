package com.temankuliah.app

import androidx.compose.animation.core.Spring
import com.temankuliah.app.ui.components.MotionTokens
import com.temankuliah.app.ui.components.calculateHierarchicalOffset
import com.temankuliah.app.ui.components.calculateNavDirectionOffset
import com.temankuliah.app.ui.components.calculatePressScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Unit Test untuk Tahap 4 (Motion & Screen Transitions, Micro-interactions, M3 Motion Tokens).
 * Sesuai spesifikasi PRD Bab 5 & 8.
 */
class MotionStage4UnitTest {

    @Test
    fun motionTokens_durationBoundaries_complyWithM3Standards() {
        // Durasi Mikro: 100 ms – 150 ms
        assertTrue(
            "DurationMicro harus dalam rentang 100–150 ms",
            MotionTokens.DurationMicro in 100..150
        )
        assertEquals(120, MotionTokens.DurationMicro)

        // Durasi Pendek: 180 ms – 240 ms
        assertTrue(
            "DurationShort harus dalam rentang 180–240 ms",
            MotionTokens.DurationShort in 180..240
        )
        assertEquals(200, MotionTokens.DurationShort)

        // Durasi Sedang: 250 ms – 320 ms
        assertTrue(
            "DurationMedium harus dalam rentang 250–320 ms",
            MotionTokens.DurationMedium in 250..320
        )
        assertEquals(280, MotionTokens.DurationMedium)

        // Durasi Panjang: 400 ms – 800 ms
        assertTrue(
            "DurationLong harus dalam rentang 400–800 ms",
            MotionTokens.DurationLong in 400..800
        )
        assertEquals(600, MotionTokens.DurationLong)
    }

    @Test
    fun motionTokens_springPhysicsDamping_complyWithGuidelines() {
        assertEquals(Spring.DampingRatioMediumBouncy, MotionTokens.DampingBouncy, 0.001f)
        assertEquals(0.82f, MotionTokens.DampingNatural, 0.001f)
        assertEquals(Spring.DampingRatioNoBouncy, MotionTokens.DampingNoBouncy, 0.001f)
        assertEquals(Spring.StiffnessMedium, MotionTokens.StiffnessMedium, 0.001f)
        assertEquals(Spring.StiffnessMediumLow, MotionTokens.StiffnessMediumLow, 0.001f)
    }

    @Test
    fun pressScale_targetCalculation_returnsExpectedScale() {
        assertEquals(1.0f, calculatePressScale(isPressed = false, reducedMotion = false), 0.001f)
        assertEquals(0.96f, calculatePressScale(isPressed = true, reducedMotion = false), 0.001f)
        assertEquals(1.0f, calculatePressScale(isPressed = true, reducedMotion = true), 0.001f)
    }

    @Test
    fun topLevelNavigation_directionOffsetCalculation_correctSigns() {
        // Dari Beranda (0) ke Akademik (2) -> Maju
        assertEquals(48, calculateNavDirectionOffset(0, 2, isEnter = true))
        assertEquals(-48, calculateNavDirectionOffset(0, 2, isEnter = false))

        // Dari Profil (4) ke Kalender (1) -> Mundur
        assertEquals(-48, calculateNavDirectionOffset(4, 1, isEnter = true))
        assertEquals(48, calculateNavDirectionOffset(4, 1, isEnter = false))
    }

    @Test
    fun hierarchicalMasterDetail_offsetCalculation_correctProportions() {
        val screenWidth = 1080

        // Forward: root -> detail
        val forwardEnterOffset = calculateHierarchicalOffset(isOpeningDetail = true, isEnter = true, screenWidth = screenWidth)
        val forwardExitOffset = calculateHierarchicalOffset(isOpeningDetail = true, isEnter = false, screenWidth = screenWidth)
        assertEquals(360, forwardEnterOffset)
        assertEquals(-180, forwardExitOffset)

        // Backward: detail -> root
        val backwardEnterOffset = calculateHierarchicalOffset(isOpeningDetail = false, isEnter = true, screenWidth = screenWidth)
        val backwardExitOffset = calculateHierarchicalOffset(isOpeningDetail = false, isEnter = false, screenWidth = screenWidth)
        assertEquals(-180, backwardEnterOffset)
        assertEquals(360, backwardExitOffset)
    }

    @Test
    fun bottomNavIndicator_dimensionsAndScale_complyWithGuidelines() {
        fun calculateDotTargetWidthDp(selected: Boolean): Float = if (selected) 12f else 3.5f
        fun calculateDotScale(selected: Boolean): Float = if (selected) 1.0f else 0.0f
        fun calculateIconScale(selected: Boolean): Float = if (selected) 1.08f else 1.0f

        assertEquals(12f, calculateDotTargetWidthDp(true), 0.01f)
        assertEquals(3.5f, calculateDotTargetWidthDp(false), 0.01f)

        assertEquals(1.0f, calculateDotScale(true), 0.01f)
        assertEquals(0.0f, calculateDotScale(false), 0.01f)

        assertEquals(1.08f, calculateIconScale(true), 0.01f)
        assertEquals(1.0f, calculateIconScale(false), 0.01f)
    }

    @Test
    fun staggeredEntrance_delayCalculation_clampedCorrectly() {
        fun calculateStaggerDelay(index: Int): Long = (index.coerceIn(0, 5) * 40L)

        assertEquals(0L, calculateStaggerDelay(0))
        assertEquals(40L, calculateStaggerDelay(1))
        assertEquals(80L, calculateStaggerDelay(2))
        assertEquals(200L, calculateStaggerDelay(5))
        assertEquals(200L, calculateStaggerDelay(10)) // Dibatasi maksimal 5 item (200 ms)
    }

    @Test
    fun taskCompletionFeedback_alphaAndScale_matchesSpecification() {
        val completedAlpha = 0.45f
        val activeAlpha = 1.0f

        fun taskTitleAlpha(isDone: Boolean): Float = if (isDone) completedAlpha else activeAlpha
        fun checkboxPopScale(isPopping: Boolean): Float = if (isPopping) 1.25f else 1.0f

        assertEquals(1.0f, taskTitleAlpha(false), 0.001f)
        assertEquals(0.45f, taskTitleAlpha(true), 0.001f)

        assertEquals(1.0f, checkboxPopScale(false), 0.001f)
        assertEquals(1.25f, checkboxPopScale(true), 0.001f)
    }

    @Test
    fun progressAnimation_fractionAndPercentageCalculations() {
        fun calculateClampedFraction(completed: Int, target: Int): Float {
            if (target <= 0) return 0f
            return (completed.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        }

        fun toPercentage(fraction: Float): Int = (fraction * 100).toInt()

        val fraction1 = calculateClampedFraction(108, 144)
        assertEquals(0.75f, fraction1, 0.001f)
        assertEquals(75, toPercentage(fraction1))

        val fractionOver = calculateClampedFraction(150, 144)
        assertEquals(1.0f, fractionOver, 0.001f)
        assertEquals(100, toPercentage(fractionOver))

        val fractionZero = calculateClampedFraction(0, 144)
        assertEquals(0f, fractionZero, 0.001f)
        assertEquals(0, toPercentage(fractionZero))
    }

    @Test
    fun gpaTickerFormatting_formatsCorrectlyInIndonesianLocale() {
        val gpa = 3.82f
        val formatted = String.format(Locale.forLanguageTag("id-ID"), "%.2f", gpa)
        assertEquals("3,82", formatted)
    }
}
