package com.temankuliah.app

import com.temankuliah.app.ui.components.AnimationMode
import com.temankuliah.app.ui.components.resolveReducedMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReducedMotionPolicyTest {
    @Test
    fun auto_followsDeviceClass() {
        assertTrue(resolveReducedMotion(AnimationMode.AUTO, systemAnimationsOff = false, lowEndDevice = true))
        assertFalse(resolveReducedMotion(AnimationMode.AUTO, systemAnimationsOff = false, lowEndDevice = false))
    }

    @Test
    fun manualChoiceBeatsAutoDetection() {
        assertFalse(resolveReducedMotion(AnimationMode.ON, systemAnimationsOff = false, lowEndDevice = true))
        assertTrue(resolveReducedMotion(AnimationMode.OFF, systemAnimationsOff = false, lowEndDevice = false))
    }

    @Test
    fun systemAnimationsOff_alwaysWins() {
        assertTrue(resolveReducedMotion(AnimationMode.ON, systemAnimationsOff = true, lowEndDevice = false))
    }

    @Test
    fun fromKey_unknownFallsBackToAuto() {
        assertEquals(AnimationMode.AUTO, AnimationMode.fromKey(null))
        assertEquals(AnimationMode.AUTO, AnimationMode.fromKey("xyz"))
        assertEquals(AnimationMode.OFF, AnimationMode.fromKey("off"))
    }
}
