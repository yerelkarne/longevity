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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PedometerTracker(
    private val context: Context,
    private val repository: LongevityRepository,
    private val preferences: AppPreferences,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val accelEstimator = AccelerometerStepEstimator()

    var usingFallback: Boolean = false
        private set

    fun hasStepCounterSensor(): Boolean = stepCounter != null

    fun startTracking(forceFallback: Boolean = false) {
        usingFallback = forceFallback || stepCounter == null
        val sensor = if (usingFallback) accelerometer else stepCounter
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopTracking() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val rawValue = event.values.firstOrNull() ?: return
            scope.launch {
                val nowDate = LocalDate.now()
                val prefs = preferences.stepSensorPreferences.first()
                val snapshot = StepCounterCalculator.process(
                    date = nowDate.toString(),
                    rawSensorValue = rawValue,
                    savedBaselineDate = prefs.baselineDate,
                    savedBaseline = prefs.stepCounterBaselineToday,
                    savedLastRaw = prefs.lastRawSensorValue
                )
                if (snapshot.rebootDetected || prefs.baselineDate != nowDate.toString()) {
                    preferences.updateStepCounterBaseline(snapshot.baselineDate, snapshot.baseline, snapshot.lastRaw)
                } else {
                    preferences.updateLastRawSensorValue(snapshot.lastRaw)
                }

                val goals = repository.observeGoals().first()
                val goal = goals?.stepsTarget ?: 10000
                val steps = snapshot.todaySteps
                val distanceKm = (steps * 0.00075f)
                val calories = steps * 0.04f
                repository.addSteps(
                    StepsLogEntity(
                        date = nowDate,
                        steps = steps,
                        goal = goal,
                        distanceKm = distanceKm,
                        caloriesEst = calories,
                        updatedAt = LocalDateTime.now()
                    )
                )
            }
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values.getOrNull(0) ?: return
            val y = event.values.getOrNull(1) ?: return
            val z = event.values.getOrNull(2) ?: return
            val now = System.currentTimeMillis()
            if (!accelEstimator.detectStep(x, y, z, now)) return

            scope.launch {
                val date = LocalDate.now()
                val current = repository.observeDashboard(date).first().steps
                val next = current + 1
                val goals = repository.observeGoals().first()
                val goal = goals?.stepsTarget ?: 10000
                repository.addSteps(
                    StepsLogEntity(
                        date = date,
                        steps = next,
                        goal = goal,
                        distanceKm = next * 0.00075f,
                        caloriesEst = next * 0.04f,
                        updatedAt = LocalDateTime.now()
                    )
                )
                preferences.updateLastAccelerometerStepAt(now)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
