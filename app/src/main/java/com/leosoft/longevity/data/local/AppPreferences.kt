package com.leosoft.longevity.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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

class AppPreferences(private val context: Context) {
    private val onboardingKey = booleanPreferencesKey("onboarding_done")
    private val ageKey = intPreferencesKey("profile_age")
    private val heightKey = intPreferencesKey("profile_height_cm")
    private val weightKey = floatPreferencesKey("profile_weight_kg")
    private val genderKey = stringPreferencesKey("profile_gender")

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[onboardingKey] ?: false }

    val profilePreferences: Flow<ProfilePreferences> = context.dataStore.data.map { prefs ->
        ProfilePreferences(
            age = prefs[ageKey] ?: 0,
            heightCm = prefs[heightKey] ?: 0,
            weightKg = prefs[weightKey] ?: 0f,
            gender = prefs[genderKey] ?: "unspecified"
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
}
