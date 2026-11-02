package com.temankuliah.app

import com.temankuliah.app.domain.runCatchingCancellable
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RunCatchingCancellableTest {
    @Test
    fun `returns the value on success`() {
        assertEquals(7, runCatchingCancellable { 7 }.getOrNull())
    }

    @Test
    fun `wraps ordinary errors as failure`() {
        val result = runCatchingCancellable { error("rusak") }
        assertTrue(result.isFailure)
        assertEquals("rusak", result.exceptionOrNull()?.message)
    }

    @Test
    fun `rethrows coroutine cancellation instead of swallowing it`() {
        try {
            runCatchingCancellable { throw CancellationException("dibatalkan") }
            fail("CancellationException harus dilempar ulang")
        } catch (expected: CancellationException) {
            assertEquals("dibatalkan", expected.message)
        }
    }
}
