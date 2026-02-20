package com.leosoft.longevity.steps

import java.time.LocalDate

data class StepEngineState(
    val date: LocalDate? = null,
    val baseline: Long? = null,
    val lastRawValue: Long? = null
)

data class StepEngineResult(
    val state: StepEngineState,
    val dailySteps: Int
)

class StepProcessingEngine {
    fun processStepCounter(today: LocalDate, rawValue: Long, previous: StepEngineState): StepEngineResult {
        val dayChanged = previous.date != today || previous.baseline == null
        val rebooted = previous.lastRawValue?.let { rawValue < it } == true
        val baseline = if (dayChanged || rebooted) rawValue else (previous.baseline ?: rawValue)
        val steps = (rawValue - baseline).coerceAtLeast(0).toInt()
        return StepEngineResult(
            state = StepEngineState(today, baseline, rawValue),
            dailySteps = steps
        )
    }

    fun shouldTrackInForegroundOnly(permissionGranted: Boolean, backgroundEnabled: Boolean): Boolean {
        return !permissionGranted || !backgroundEnabled
    }
}
