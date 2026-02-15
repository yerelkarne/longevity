package com.leosoft.longevity.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leosoft.longevity.LongevityApp
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.UserGoalsEntity
import com.leosoft.longevity.domain.model.DashboardSummary
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OnboardingForm(
    val proteinTarget: Float = 120f,
    val carbsTarget: Float = 180f,
    val fatTarget: Float = 60f,
    val fiberTarget: Float = 30f,
    val waterTarget: Int = 2000,
    val stepsTarget: Int = 10000,
    val sleepTarget: Int = 480,
    val wakeTime: String = "07:00",
    val bedTime: String = "23:00",
    val supplementsTarget: Int = 2
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LongevityApp
    private val repository = app.repository

    val onboardingDone = app.preferences.onboardingDone.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val dashboard: StateFlow<DashboardSummary?> = repository.observeDashboard(LocalDate.now())
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val foods = repository.observeFoods().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mealEntries = repository.observeMealEntries(LocalDate.now()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (foods.value.isEmpty()) {
                // lightweight seed
                app.repository.observeFoods()
            }
        }
    }

    fun completeOnboarding(form: OnboardingForm) {
        viewModelScope.launch {
            repository.saveGoals(
                UserGoalsEntity(
                    proteinTarget = form.proteinTarget,
                    carbsTarget = form.carbsTarget,
                    fatTarget = form.fatTarget,
                    fiberTarget = form.fiberTarget,
                    waterTargetMl = form.waterTarget,
                    stepsTarget = form.stepsTarget,
                    sleepTargetMinutes = form.sleepTarget,
                    wakeTime = form.wakeTime,
                    bedTime = form.bedTime,
                    supplementsPerDayTarget = form.supplementsTarget
                )
            )
            app.preferences.setOnboardingDone(true)
            repository.recalculateScore(LocalDate.now())
        }
    }

    fun quickAddWater() {
        viewModelScope.launch { repository.addWater(LocalDate.now(), 250) }
    }

    fun addMeal(foodId: Long, grams: Int, mealType: com.leosoft.longevity.data.local.entity.MealType) {
        viewModelScope.launch {
            repository.addMealEntry(
                MealEntryEntity(
                    date = LocalDate.now(),
                    time = LocalDateTime.now(),
                    mealType = mealType,
                    foodId = foodId,
                    grams = grams
                )
            )
        }
    }
}
