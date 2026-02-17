package com.leosoft.longevity.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leosoft.longevity.LongevityApp
import com.leosoft.longevity.data.local.ProfilePreferences
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.MealType
import com.leosoft.longevity.data.local.entity.StepsLogEntity
import com.leosoft.longevity.data.local.entity.SupplementLogEntity
import com.leosoft.longevity.data.local.entity.WaterLogEntity
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

data class PersonalizedTargets(
    val waterMl: Int,
    val steps: Int,
    val sleepMinutes: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val fiberGrams: Float,
    val ironMg: Int,
    val magnesiumMg: Int,
    val potassiumMg: Int,
    val vitaminDIu: Int,
    val omega3Mg: Int
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

    val goalsMealEntries: StateFlow<List<MealEntryEntity>> = selectedGoalsDate
        .flatMapLatest { date -> repository.observeMealEntries(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goalPlans: StateFlow<List<GoalPlanItem>> = repository.observeGoalPlans()
        .map { items ->
            items.map { GoalPlanItem(id = it.id, goalType = it.goalType, target = it.target, cadence = it.cadence) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foods = repository.observeFoods().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val nutritiousFoods = repository.observeFoodsWithNutrition().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val supplements = repository.observeSupplements().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminders = repository.observeReminders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedNutritionDate = MutableStateFlow(LocalDate.now())
    val mealEntries = selectedNutritionDate
        .flatMapLatest { date -> repository.observeMealEntries(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterLogs: StateFlow<List<WaterLogEntity>> = selectedNutritionDate
        .flatMapLatest { date -> repository.observeWaterLogs(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supplementLogs: StateFlow<List<SupplementLogEntity>> = selectedNutritionDate
        .flatMapLatest { date -> repository.observeSupplementLogs(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profilePreferences: StateFlow<ProfilePreferences> = app.preferences.profilePreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfilePreferences())

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

    fun buildPersonalizedTargets(age: Int, heightCm: Int, weightKg: Float, gender: String): PersonalizedTargets {
        val activityFactor = when {
            age < 30 -> 1.6f
            age < 50 -> 1.5f
            else -> 1.4f
        }
        val bmr = when (gender) {
            "male" -> (10f * weightKg) + (6.25f * heightCm) - (5f * age) + 5f
            "female" -> (10f * weightKg) + (6.25f * heightCm) - (5f * age) - 161f
            else -> (10f * weightKg) + (6.25f * heightCm) - (5f * age) - 78f
        }
        val estimatedCalories = (bmr * activityFactor).coerceAtLeast(1300f)
        val protein = (weightKg * 1.4f).coerceIn(70f, 210f)
        val fat = (estimatedCalories * 0.28f / 9f).coerceIn(40f, 120f)
        val carbs = ((estimatedCalories - ((protein * 4f) + (fat * 9f))) / 4f).coerceAtLeast(100f)
        val fiber = (estimatedCalories / 1000f * 14f).coerceIn(25f, 45f)
        val water = ((weightKg * 33f) + (heightCm * 2f)).toInt().coerceIn(1800, 4500)
        val steps = ((heightCm * 20f) + (age * 35f)).toInt().coerceIn(7000, 13000)
        val sleep = if (age < 18) 540 else if (age < 65) 480 else 450
        val iron = if (gender == "female" && age in 18..50) 18 else 8

        return PersonalizedTargets(
            waterMl = water,
            steps = steps,
            sleepMinutes = sleep,
            proteinGrams = protein,
            carbsGrams = carbs,
            fatGrams = fat,
            fiberGrams = fiber,
            ironMg = iron,
            magnesiumMg = if (gender == "male") 420 else 320,
            potassiumMg = 3500,
            vitaminDIu = 600,
            omega3Mg = if (gender == "male") 1600 else 1100
        )
    }

    fun createPersonalizedGoals(age: Int, heightCm: Int, weightKg: Float, gender: String, onComplete: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val success = runCatching {
            app.preferences.saveProfile(age, heightCm, weightKg, gender)
            val targets = buildPersonalizedTargets(age, heightCm, weightKg, gender)
            repository.saveGoals(
                UserGoalsEntity(
                    proteinTarget = targets.proteinGrams,
                    carbsTarget = targets.carbsGrams,
                    fatTarget = targets.fatGrams,
                    fiberTarget = targets.fiberGrams,
                    waterTargetMl = targets.waterMl,
                    stepsTarget = targets.steps,
                    sleepTargetMinutes = targets.sleepMinutes,
                    wakeTime = "07:00",
                    bedTime = "23:00",
                    supplementsPerDayTarget = 0
                )
            )

            val goalTargets = listOf(
                "water" to targets.waterMl,
                "steps" to targets.steps,
                "protein" to targets.proteinGrams.toInt(),
                "carbs" to targets.carbsGrams.toInt(),
                "fat" to targets.fatGrams.toInt(),
                "fiber" to targets.fiberGrams.toInt(),
                "sleep" to targets.sleepMinutes,
                "iron" to targets.ironMg,
                "magnesium" to targets.magnesiumMg,
                "potassium" to targets.potassiumMg,
                "vitamin_d" to targets.vitaminDIu,
                "omega3" to targets.omega3Mg
            )

            repository.clearGoalPlans()
            goalTargets.forEach { (type, target) ->
                repository.addGoalPlan(type, target, "daily")
            }
        }.isSuccess

        if (success) {
            runCatching { repository.recalculateScore(LocalDate.now()) }
        }
        onComplete(success)
    }


    fun saveProfile(age: Int, heightCm: Int, weightKg: Float, gender: String) = viewModelScope.launch {
        app.preferences.saveProfile(age, heightCm, weightKg, gender)
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

    fun addGoalPlan(goalType: String, target: Int, cadence: String) = viewModelScope.launch {
        repository.addGoalPlan(goalType, target, cadence)
    }

    fun updateGoalPlan(id: Long, goalType: String, target: Int, cadence: String) = viewModelScope.launch {
        repository.updateGoalPlan(id, goalType, target, cadence)
    }

    fun deleteGoalPlan(id: Long) = viewModelScope.launch {
        repository.deleteGoalPlan(id)
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

    fun addWater(ml: Int) = viewModelScope.launch { repository.addWater(selectedNutritionDate.value, ml) }

    fun addSteps(steps: Int) = viewModelScope.launch {
        repository.addSteps(StepsLogEntity(date = LocalDate.now(), steps = steps, updatedAt = LocalDateTime.now()))
    }

    fun addSupplementLog(supplementId: Long) = viewModelScope.launch {
        repository.addSupplementLog(selectedNutritionDate.value, supplementId, true)
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

    suspend fun addReminder(type: String, time: String, cadence: String, intervalHours: Int?): Long {
        return repository.addReminderLog(LocalDate.now(), type, time, cadence, intervalHours)
    }

    fun updateReminder(id: Long, type: String, time: String, cadence: String, intervalHours: Int?) = viewModelScope.launch {
        repository.updateReminderLog(id, type, time, cadence, intervalHours)
    }

    fun deleteReminder(id: Long) = viewModelScope.launch {
        repository.deleteReminderLog(id)
    }

    fun updateGoal(goalType: String, value: Int) = viewModelScope.launch {
        repository.updateGoal(goalType, value)
    }

    fun ensureCoreFoods() = viewModelScope.launch {
        repository.ensureCoreFoods()
    }
}
