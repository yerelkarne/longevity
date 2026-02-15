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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class GoalPlanItem(
    val id: Long,
    val goalType: String,
    val target: Int,
    val cadence: String
)

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

    val selectedGoalsDate = MutableStateFlow(LocalDate.now())
    val goalsDashboard: StateFlow<DashboardSummary?> = selectedGoalsDate
        .flatMapLatest { date -> repository.observeDashboard(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _goalPlans = MutableStateFlow<List<GoalPlanItem>>(emptyList())
    val goalPlans: StateFlow<List<GoalPlanItem>> = _goalPlans
    private var nextGoalPlanId: Long = 1L

    val foods = repository.observeFoods().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val nutritiousFoods = repository.observeFoodsWithNutrition().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val supplements = repository.observeSupplements().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedNutritionDate = MutableStateFlow(LocalDate.now())
    val mealEntries = selectedNutritionDate
        .flatMapLatest { date -> repository.observeMealEntries(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        ensureCoreFoods()
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

    fun addMeal(foodId: Long, grams: Int, mealType: MealType) {
        viewModelScope.launch {
            repository.addMealEntry(
                MealEntryEntity(
                    date = selectedNutritionDate.value,
                    time = LocalDateTime.now(),
                    mealType = mealType,
                    foodId = foodId,
                    grams = grams
                )
            )
        }
    }

    fun addMealWithOptionalCustomFood(foodId: Long?, customFoodName: String, grams: Int, mealType: MealType = MealType.SNACK) {
        viewModelScope.launch {
            val resolvedFoodId = if (customFoodName.isNotBlank()) {
                repository.addCustomFood(customFoodName.trim())
            } else {
                foodId ?: return@launch
            }
            repository.addMealEntry(
                MealEntryEntity(
                    date = selectedNutritionDate.value,
                    time = LocalDateTime.now(),
                    mealType = mealType,
                    foodId = resolvedFoodId,
                    grams = grams
                )
            )
        }
    }

    fun addGoalPlan(goalType: String, target: Int, cadence: String) {
        _goalPlans.value = _goalPlans.value + GoalPlanItem(
            id = nextGoalPlanId++,
            goalType = goalType,
            target = target,
            cadence = cadence
        )
    }

    fun setSelectedGoalsDate(date: LocalDate) {
        selectedGoalsDate.value = date
    }

    fun setSelectedNutritionDate(date: LocalDate) {
        selectedNutritionDate.value = date
    }

    fun updateMealEntry(entry: MealEntryEntity) = viewModelScope.launch {
        repository.updateMealEntry(entry)
    }

    fun deleteMealEntry(entry: MealEntryEntity) = viewModelScope.launch {
        repository.deleteMealEntry(entry.id, entry.date)
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

    fun ensureCoreFoods() = viewModelScope.launch {
        repository.ensureCoreFoods()
    }
}
