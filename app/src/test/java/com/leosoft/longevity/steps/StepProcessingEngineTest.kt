package com.leosoft.longevity.steps

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepProcessingEngineTest {
    private val engine = StepProcessingEngine()

    @Test
    fun `day rollover resets baseline`() {
        val prev = StepEngineState(date = LocalDate.of(2026, 1, 1), baseline = 1000L, lastRawValue = 1500L)
        val result = engine.processStepCounter(LocalDate.of(2026, 1, 2), 1600L, prev)
        assertEquals(0, result.dailySteps)
        assertEquals(1600L, result.state.baseline)
    }

    @Test
    fun `device restart re-baselines when raw decreases`() {
        val prev = StepEngineState(date = LocalDate.of(2026, 1, 2), baseline = 1200L, lastRawValue = 1800L)
        val result = engine.processStepCounter(LocalDate.of(2026, 1, 2), 100L, prev)
        assertEquals(0, result.dailySteps)
        assertEquals(100L, result.state.baseline)
    }

    @Test
    fun `permission rejected implies foreground only`() {
        assertTrue(engine.shouldTrackInForegroundOnly(permissionGranted = false, backgroundEnabled = true))
    }
}
