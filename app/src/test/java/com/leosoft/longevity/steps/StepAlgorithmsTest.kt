package com.leosoft.longevity.steps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepAlgorithmsTest {

    @Test
    fun `day rollover resets baseline`() {
        val result = StepCounterCalculator.process(
            date = "2026-02-20",
            rawSensorValue = 1050f,
            savedBaselineDate = "2026-02-19",
            savedBaseline = 1000f,
            savedLastRaw = 1040f
        )
        assertEquals(0, result.todaySteps)
        assertEquals(1050f, result.baseline)
    }

    @Test
    fun `device reboot updates baseline when raw decreases`() {
        val result = StepCounterCalculator.process(
            date = "2026-02-20",
            rawSensorValue = 50f,
            savedBaselineDate = "2026-02-20",
            savedBaseline = 1000f,
            savedLastRaw = 1200f
        )
        assertTrue(result.rebootDetected)
        assertEquals(0, result.todaySteps)
        assertEquals(50f, result.baseline)
    }

    @Test
    fun `accelerometer detector debounces close peaks`() {
        val detector = AccelerometerStepEstimator(minStepIntervalMs = 300L)
        val first = detector.detectStep(2f, 10f, 10f, 1_000L)
        val second = detector.detectStep(2f, 10f, 10f, 1_100L)
        assertTrue(first)
        assertEquals(false, second)
    }
}
