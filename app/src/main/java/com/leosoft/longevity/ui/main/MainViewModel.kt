package com.leosoft.longevity.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leosoft.longevity.LongevityApp
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.MealType
import com.leosoft.longevity.data.local.entity.UserGoalsEntity
import com.leosoft.longevity.data.local.entity.WorkoutType
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

    val dashboard: StateFlow<DashboardSummary?> = repository.observeDashboard(LocalDate.now())
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val foods = repository.observeFoods().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val supplements = repository.observeSupplements().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mealEntries = repository.observeMealEntries(LocalDate.now()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun addMeal(foodId: Long, grams: Int, mealType: MealType) {
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

    fun addWater(ml: Int) = viewModelScope.launch { repository.addWater(LocalDate.now(), ml) }

    fun addSupplementLog(supplementId: Long) = viewModelScope.launch {
        repository.addSupplementLog(LocalDate.now(), supplementId, true)
    }

    fun addSleepLog(bedtime: String, wakeTime: String) = viewModelScope.launch {
        repository.addSleepLog(LocalDate.now(), bedtime, wakeTime)
    }

    fun addWorkout(type: WorkoutType, durationMinutes: Int, intensity: Int, notes: String) = viewModelScope.launch {
        repository.addWorkoutLog(LocalDate.now(), type, durationMinutes, intensity, notes)
    }

    fun addTask(title: String, target: String?) = viewModelScope.launch {
        repository.addTaskLog(LocalDate.now(), title, target)
    }

    fun addReminder(type: String, time: String) = viewModelScope.launch {
        repository.addReminderLog(LocalDate.now(), type, time)
    }

    fun updateGoal(goalType: String, value: Int) = viewModelScope.launch {
        repository.updateGoal(goalType, value)
    }
}
