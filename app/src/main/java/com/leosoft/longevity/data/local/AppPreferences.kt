package com.leosoft.longevity.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "longevity_prefs")

data class ProfilePreferences(
    val age: Int = 0,
    val heightCm: Int = 0,
    val weightKg: Float = 0f,
    val gender: String = "unspecified"
)

data class StepTrackingState(
    val baselineDate: LocalDate? = null,
    val baselineRawValue: Long? = null,
    val lastRawSensorValue: Long? = null,
    val isForegroundTrackingEnabled: Boolean = false
)

class AppPreferences(private val context: Context) {
    private val onboardingKey = booleanPreferencesKey("onboarding_done")
    private val ageKey = intPreferencesKey("profile_age")
    private val heightKey = intPreferencesKey("profile_height_cm")
    private val weightKey = floatPreferencesKey("profile_weight_kg")
    private val genderKey = stringPreferencesKey("profile_gender")

    private val baselineDateKey = stringPreferencesKey("step_counter_baseline_date")
    private val baselineRawValueKey = longPreferencesKey("step_counter_baseline_raw")
    private val lastRawSensorValueKey = longPreferencesKey("step_counter_last_raw")
    private val foregroundTrackingEnabledKey = booleanPreferencesKey("steps_foreground_enabled")

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[onboardingKey] ?: false }

    val profilePreferences: Flow<ProfilePreferences> = context.dataStore.data.map { prefs ->
        ProfilePreferences(
            age = prefs[ageKey] ?: 0,
            heightCm = prefs[heightKey] ?: 0,
            weightKg = prefs[weightKey] ?: 0f,
            gender = prefs[genderKey] ?: "unspecified"
        )
    }

    val stepTrackingState: Flow<StepTrackingState> = context.dataStore.data.map { prefs ->
        StepTrackingState(
            baselineDate = prefs[baselineDateKey]?.let(LocalDate::parse),
            baselineRawValue = prefs[baselineRawValueKey],
            lastRawSensorValue = prefs[lastRawSensorValueKey],
            isForegroundTrackingEnabled = prefs[foregroundTrackingEnabledKey] ?: false
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

    suspend fun saveStepBaseline(date: LocalDate, baselineRawValue: Long) {
        context.dataStore.edit { prefs ->
            prefs[baselineDateKey] = date.toString()
            prefs[baselineRawValueKey] = baselineRawValue
        }
    }

    suspend fun saveLastRawSensorValue(rawValue: Long) {
        context.dataStore.edit { prefs ->
            prefs[lastRawSensorValueKey] = rawValue
        }
    }

    suspend fun setForegroundTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[foregroundTrackingEnabledKey] = enabled
        }
    }

    suspend fun getStepTrackingState(): StepTrackingState = stepTrackingState.first()
}
