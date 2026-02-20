package com.leosoft.longevity.steps

import kotlin.math.sqrt

class AccelerometerStepEstimator(
    private val minStepIntervalMs: Long = 300L,
    private val alpha: Float = 0.15f
) {
    private var gravity = 9.81f
    private var avgLinear = 0f
    private var variance = 0.5f
    private var lastStepTime = 0L

    fun detectStep(x: Float, y: Float, z: Float, eventTimeMillis: Long): Boolean {
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val linear = magnitude - gravity
        gravity += alpha * (magnitude - gravity)

        avgLinear = (0.9f * avgLinear) + (0.1f * linear)
        val deviation = linear - avgLinear
        variance = (0.9f * variance) + (0.1f * deviation * deviation)
        val dynamicThreshold = (1.0f + (2.2f * sqrt(variance.toDouble()).toFloat())).coerceAtLeast(1.2f)

        val enoughTimePassed = (eventTimeMillis - lastStepTime) >= minStepIntervalMs
        val crossedThreshold = linear > dynamicThreshold
        if (enoughTimePassed && crossedThreshold) {
            lastStepTime = eventTimeMillis
            return true
        }
        return false
    }
}

data class StepCounterSnapshot(
    val baselineDate: String,
    val baseline: Float,
    val lastRaw: Float,
    val todaySteps: Int,
    val rebootDetected: Boolean
)

object StepCounterCalculator {
    fun process(
        date: String,
        rawSensorValue: Float,
        savedBaselineDate: String,
        savedBaseline: Float,
        savedLastRaw: Float
    ): StepCounterSnapshot {
        val dateChanged = savedBaselineDate != date
        val rebootDetected = !dateChanged && savedLastRaw > 0f && rawSensorValue < savedLastRaw
        val baseline = if (dateChanged || rebootDetected || savedBaseline <= 0f) rawSensorValue else savedBaseline
        val todaySteps = (rawSensorValue - baseline).toInt().coerceAtLeast(0)
        return StepCounterSnapshot(
            baselineDate = date,
            baseline = baseline,
            lastRaw = rawSensorValue,
            todaySteps = todaySteps,
            rebootDetected = rebootDetected
        )
    }
}
