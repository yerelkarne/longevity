package com.leosoft.longevity.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.leosoft.longevity.data.local.entity.ConflictResolution
import java.time.LocalDate
import java.time.LocalDateTime
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

data class HealthSyncPreferences(
    val enabled: Boolean = false,
    val hydrationEnabled: Boolean = true,
    val sleepEnabled: Boolean = true,
    val stepsEnabled: Boolean = true,
    val exerciseEnabled: Boolean = true,
    val nutritionEnabled: Boolean = true,
    val conflictResolution: ConflictResolution = ConflictResolution.LAST_WRITE_WINS,
    val lastSyncAt: LocalDateTime? = null
)

class AppPreferences(private val context: Context) {
    private val onboardingKey = booleanPreferencesKey("onboarding_done")
    private val medicalDisclaimerAcceptedKey = booleanPreferencesKey("medical_disclaimer_accepted")
    private val ageKey = intPreferencesKey("profile_age")
    private val heightKey = intPreferencesKey("profile_height_cm")
    private val weightKey = floatPreferencesKey("profile_weight_kg")
    private val genderKey = stringPreferencesKey("profile_gender")

    private val baselineDateKey = stringPreferencesKey("step_counter_baseline_date")
    private val baselineRawValueKey = longPreferencesKey("step_counter_baseline_raw")
    private val lastRawSensorValueKey = longPreferencesKey("step_counter_last_raw")
    private val foregroundTrackingEnabledKey = booleanPreferencesKey("steps_foreground_enabled")
    private val hcSyncEnabledKey = booleanPreferencesKey("hc_sync_enabled")
    private val hcHydrationEnabledKey = booleanPreferencesKey("hc_hydration_enabled")
    private val hcSleepEnabledKey = booleanPreferencesKey("hc_sleep_enabled")
    private val hcStepsEnabledKey = booleanPreferencesKey("hc_steps_enabled")
    private val hcExerciseEnabledKey = booleanPreferencesKey("hc_exercise_enabled")
    private val hcNutritionEnabledKey = booleanPreferencesKey("hc_nutrition_enabled")
    private val hcConflictRuleKey = stringPreferencesKey("hc_conflict_rule")
    private val hcLastSyncAtKey = stringPreferencesKey("hc_last_sync_at")
    private val appLanguageKey = stringPreferencesKey("app_language")

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[onboardingKey] ?: false }
    val medicalDisclaimerAccepted: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[medicalDisclaimerAcceptedKey] ?: false }

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

    val appLanguage: Flow<String> = context.dataStore.data.map { prefs -> prefs[appLanguageKey] ?: "tr" }

    val healthSyncPreferences: Flow<HealthSyncPreferences> = context.dataStore.data.map { prefs ->
        HealthSyncPreferences(
            enabled = prefs[hcSyncEnabledKey] ?: false,
            hydrationEnabled = prefs[hcHydrationEnabledKey] ?: true,
            sleepEnabled = prefs[hcSleepEnabledKey] ?: true,
            stepsEnabled = prefs[hcStepsEnabledKey] ?: true,
            exerciseEnabled = prefs[hcExerciseEnabledKey] ?: true,
            nutritionEnabled = prefs[hcNutritionEnabledKey] ?: true,
            conflictResolution = prefs[hcConflictRuleKey]?.let(ConflictResolution::valueOf) ?: ConflictResolution.LAST_WRITE_WINS,
            lastSyncAt = prefs[hcLastSyncAtKey]?.let(LocalDateTime::parse)
        )
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[onboardingKey] = done }
    }

    suspend fun setMedicalDisclaimerAccepted(accepted: Boolean) {
        context.dataStore.edit { it[medicalDisclaimerAcceptedKey] = accepted }
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

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[appLanguageKey] = languageCode
        }
    }

    suspend fun updateHealthSyncPreferences(update: (HealthSyncPreferences) -> HealthSyncPreferences) {
        val current = healthSyncPreferences.first()
        val next = update(current)
        context.dataStore.edit { prefs ->
            prefs[hcSyncEnabledKey] = next.enabled
            prefs[hcHydrationEnabledKey] = next.hydrationEnabled
            prefs[hcSleepEnabledKey] = next.sleepEnabled
            prefs[hcStepsEnabledKey] = next.stepsEnabled
            prefs[hcExerciseEnabledKey] = next.exerciseEnabled
            prefs[hcNutritionEnabledKey] = next.nutritionEnabled
            prefs[hcConflictRuleKey] = next.conflictResolution.name
            next.lastSyncAt?.let { prefs[hcLastSyncAtKey] = it.toString() } ?: prefs.remove(hcLastSyncAtKey)
        }
    }

    suspend fun getStepTrackingState(): StepTrackingState = stepTrackingState.first()
}
