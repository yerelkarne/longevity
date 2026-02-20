package com.leosoft.longevity.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "longevity_prefs")

data class ProfilePreferences(
    val age: Int = 0,
    val heightCm: Int = 0,
    val weightKg: Float = 0f,
    val gender: String = "unspecified"
)

data class StepSensorPreferences(
    val baselineDate: String = "",
    val stepCounterBaselineToday: Float = 0f,
    val lastRawSensorValue: Float = 0f,
    val lastAccelerometerStepAt: Long = 0L,
    val fallbackMode: Boolean = false
)

class AppPreferences(private val context: Context) {
    private val onboardingKey = booleanPreferencesKey("onboarding_done")
    private val ageKey = intPreferencesKey("profile_age")
    private val heightKey = intPreferencesKey("profile_height_cm")
    private val weightKey = floatPreferencesKey("profile_weight_kg")
    private val genderKey = stringPreferencesKey("profile_gender")
    private val baselineDateKey = stringPreferencesKey("step_baseline_date")
    private val stepBaselineKey = floatPreferencesKey("step_counter_baseline_today")
    private val lastRawSensorValueKey = floatPreferencesKey("step_last_raw_sensor_value")
    private val lastAccelStepAtKey = longPreferencesKey("step_last_accelerometer_step_at")
    private val fallbackModeKey = booleanPreferencesKey("step_fallback_mode")

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[onboardingKey] ?: false }

    val profilePreferences: Flow<ProfilePreferences> = context.dataStore.data.map { prefs ->
        ProfilePreferences(
            age = prefs[ageKey] ?: 0,
            heightCm = prefs[heightKey] ?: 0,
            weightKg = prefs[weightKey] ?: 0f,
            gender = prefs[genderKey] ?: "unspecified"
        )
    }

    val stepSensorPreferences: Flow<StepSensorPreferences> = context.dataStore.data.map { prefs ->
        StepSensorPreferences(
            baselineDate = prefs[baselineDateKey] ?: "",
            stepCounterBaselineToday = prefs[stepBaselineKey] ?: 0f,
            lastRawSensorValue = prefs[lastRawSensorValueKey] ?: 0f,
            lastAccelerometerStepAt = prefs[lastAccelStepAtKey] ?: 0L,
            fallbackMode = prefs[fallbackModeKey] ?: false
        )
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[onboardingKey] = done }
    }

    suspend fun saveProfile(age: Int, heightCm: Int, weightKg: Float, gender: String) {
        context.dataStore.edit { prefs ->
            prefs[ageKey] = age
            prefs[heightKey] = heightCm
            prefs[weightKey] = weightKg
            prefs[genderKey] = gender
        }
    }

    suspend fun updateStepCounterBaseline(date: String, baseline: Float, lastRawValue: Float) {
        context.dataStore.edit { prefs ->
            prefs[baselineDateKey] = date
            prefs[stepBaselineKey] = baseline
            prefs[lastRawSensorValueKey] = lastRawValue
            prefs[fallbackModeKey] = false
        }
    }

    suspend fun updateLastRawSensorValue(value: Float) {
        context.dataStore.edit { it[lastRawSensorValueKey] = value }
    }

    suspend fun updateLastAccelerometerStepAt(timestampMillis: Long) {
        context.dataStore.edit {
            it[lastAccelStepAtKey] = timestampMillis
            it[fallbackModeKey] = true
        }
    }
}
