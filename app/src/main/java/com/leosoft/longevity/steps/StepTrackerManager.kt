package com.leosoft.longevity.steps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.leosoft.longevity.data.local.AppPreferences
import com.leosoft.longevity.data.local.entity.StepsLogEntity
import com.leosoft.longevity.domain.repository.LongevityRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class StepTrackerManager(
    context: Context,
    private val repository: LongevityRepository,
    private val preferences: AppPreferences
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val detector = AccelerometerStepDetector()
    private val engine = StepProcessingEngine()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var estimatedDate: LocalDate? = null
    private var estimatedSteps: Int = 0
    private var cachedPersistedDate: LocalDate? = null
    private var cachedPersistedSteps: Int = 0

    val usesEstimatedTracking: Boolean get() = stepCounterSensor == null

    fun hasAnyStepSensor(): Boolean = stepCounterSensor != null || accelerometerSensor != null

    fun start() {
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> scope.launch { handleStepCounter(event.values.firstOrNull()?.toLong() ?: return@launch) }
            Sensor.TYPE_ACCELEROMETER -> scope.launch {
                val x = event.values.getOrNull(0) ?: 0f
                val y = event.values.getOrNull(1) ?: 0f
                val z = event.values.getOrNull(2) ?: 0f
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                handleEstimatedStep(event.timestamp, magnitude)
            }
        }
    }

    private suspend fun handleStepCounter(rawValue: Long) {
        val today = LocalDate.now()
        val state = preferences.getStepTrackingState()

        val result = engine.processStepCounter(
            today = today,
            rawValue = rawValue,
            previous = StepEngineState(
                date = state.baselineDate,
                baseline = state.baselineRawValue,
                lastRawValue = state.lastRawSensorValue
            )
        )
        preferences.saveStepBaseline(result.state.date ?: today, result.state.baseline ?: rawValue)
        preferences.saveLastRawSensorValue(result.state.lastRawValue ?: rawValue)
        persistDaily(today, result.dailySteps)
    }

    private suspend fun handleEstimatedStep(timestampNs: Long, magnitude: Float) {
        val detected = detector.onSample(timestampNs, magnitude)
        if (!detected) return

        val today = LocalDate.now()
        if (estimatedDate != today) {
            estimatedDate = today
            val persisted = repository.getStepsForDate(today)?.steps ?: 0
            estimatedSteps = persisted
        }
        estimatedSteps += 1
        persistDaily(today, estimatedSteps)
    }

    private suspend fun persistDaily(date: LocalDate, steps: Int) {
        val persistedSteps = if (cachedPersistedDate == date) {
            cachedPersistedSteps
        } else {
            (repository.getStepsForDate(date)?.steps ?: 0).also { current ->
                cachedPersistedDate = date
                cachedPersistedSteps = current
            }
        }
        val stableSteps = maxOf(steps, persistedSteps)
        if (stableSteps == persistedSteps) return

        repository.addSteps(
            StepsLogEntity(
                date = date,
                steps = stableSteps,
                goal = 10000,
                distanceKm = stableSteps * 0.0008f,
                caloriesEst = stableSteps * 0.04f,
                updatedAt = LocalDateTime.now()
            )
        )
        cachedPersistedDate = date
        cachedPersistedSteps = stableSteps
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

private class AccelerometerStepDetector {
    private var dynamicThreshold = 11.5f
    private var avg = 9.8f
    private var lastPeakTimestampNs = 0L

    fun onSample(timestampNs: Long, magnitude: Float): Boolean {
        avg = (avg * 0.9f) + (magnitude * 0.1f)
        dynamicThreshold = (avg + 1.2f).coerceIn(10.8f, 14.5f)
        val debounceNs = 280_000_000L
        val isPeak = magnitude > dynamicThreshold
        val debounceOk = timestampNs - lastPeakTimestampNs > debounceNs
        if (isPeak && debounceOk) {
            lastPeakTimestampNs = timestampNs
            return true
        }
        return false
    }
}
