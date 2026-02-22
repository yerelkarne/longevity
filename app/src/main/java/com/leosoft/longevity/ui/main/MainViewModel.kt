package com.leosoft.longevity.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leosoft.longevity.LongevityApp
import android.content.Intent
import com.leosoft.longevity.data.local.ProfilePreferences
import com.leosoft.longevity.data.local.HealthSyncPreferences
import com.leosoft.longevity.data.local.entity.ConflictResolution
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.MealType
import com.leosoft.longevity.data.local.entity.StepsLogEntity
import com.leosoft.longevity.data.local.entity.SleepLogEntity
import com.leosoft.longevity.data.local.entity.SupplementLogEntity
import com.leosoft.longevity.data.local.entity.WaterLogEntity
import com.leosoft.longevity.data.local.entity.UserGoalsEntity
import com.leosoft.longevity.data.local.entity.WorkoutLogEntity
import com.leosoft.longevity.data.local.entity.WorkoutType
import com.leosoft.longevity.domain.model.DashboardSummary
import com.leosoft.longevity.steps.StepTrackerManager
import com.leosoft.longevity.steps.StepTrackingService
import com.leosoft.longevity.data.repository.HealthConnectAdapter
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

enum class WeightGoalMode { REACH_IDEAL, MAINTAIN }

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
    val omega3Mg: Int,
    val idealWeightKg: Float,
    val weightPlanSummary: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LongevityApp
    private val repository = app.repository
    private val healthConnectAdapter = HealthConnectAdapter(application)

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
    val sleepLogs: StateFlow<List<SleepLogEntity>> = repository.observeSleepLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val menstrualCycleLogs = repository.observeMenstrualCycleLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pulseMeasurements = repository.observePulseMeasurements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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

    val userGoals: StateFlow<UserGoalsEntity?> = repository.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val workoutLogs: StateFlow<List<WorkoutLogEntity>> = repository.observeAllWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklySteps = repository.observeWeeklySteps(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyStepsTotal = repository.observeMonthlyStepsTotal(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val stepTrackingState = app.preferences.stepTrackingState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.leosoft.longevity.data.local.StepTrackingState())

    val healthSyncPreferences: StateFlow<HealthSyncPreferences> = app.preferences.healthSyncPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HealthSyncPreferences())

    val healthConnectAvailable: Boolean get() = healthConnectAdapter.isAvailable()
    val healthConnectInstallable: Boolean get() = healthConnectAdapter.isInstallable()
    val healthConnectPermissions = healthConnectAdapter.allPermissions
    private val _healthPermissionsGranted = MutableStateFlow(false)
    val healthPermissionsGranted: StateFlow<Boolean> = _healthPermissionsGranted

    val usesEstimatedTracking = StepTrackerManager(application, repository, app.preferences).usesEstimatedTracking

    init {
        ensureCoreFoods()
        refreshHealthPermissions()
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

    fun buildPersonalizedTargets(age: Int, heightCm: Int, weightKg: Float, gender: String, goalMode: WeightGoalMode = WeightGoalMode.REACH_IDEAL): PersonalizedTargets {
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
        val maintenanceCalories = (bmr * activityFactor).coerceAtLeast(1300f)
        val idealWeight = ((heightCm / 100f) * (heightCm / 100f) * 22f).coerceIn(45f, 120f)
        val bmi = weightKg / ((heightCm / 100f) * (heightCm / 100f))

        val calorieMultiplier = when (goalMode) {
            WeightGoalMode.MAINTAIN -> 1f
            WeightGoalMode.REACH_IDEAL -> when {
                weightKg < idealWeight * 0.97f -> if (bmi < 18.5f) 1.18f else 1.12f
                weightKg > idealWeight * 1.03f -> if (bmi >= 30f) 0.78f else 0.85f
                else -> 1f
            }
        }
        val targetCalories = (maintenanceCalories * calorieMultiplier).coerceIn(1300f, 3600f)

        val protein = when (goalMode) {
            WeightGoalMode.MAINTAIN -> (weightKg * 1.3f)
            WeightGoalMode.REACH_IDEAL -> when {
                weightKg > idealWeight * 1.03f -> idealWeight * 1.7f
                weightKg < idealWeight * 0.97f -> idealWeight * 1.5f
                else -> weightKg * 1.35f
            }
        }.coerceIn(70f, 220f)

        val fatRatio = when {
            calorieMultiplier < 1f -> 0.30f
            calorieMultiplier > 1f -> 0.26f
            else -> 0.28f
        }
        val fat = (targetCalories * fatRatio / 9f).coerceIn(40f, 120f)
        val carbs = ((targetCalories - ((protein * 4f) + (fat * 9f))) / 4f).coerceAtLeast(100f)
        val fiber = (targetCalories / 1000f * 14f).coerceIn(25f, 45f)
        val water = ((weightKg * 33f) + (heightCm * 2f)).toInt().coerceIn(1800, 4500)
        val steps = ((heightCm * 20f) + (age * 35f)).toInt().coerceIn(7000, 13000)
        val sleep = if (age < 18) 540 else if (age < 65) 480 else 450
        val iron = if (gender == "female" && age in 18..50) 18 else 8

        val planSummary = when (goalMode) {
            WeightGoalMode.MAINTAIN -> "maintain"
            WeightGoalMode.REACH_IDEAL -> when {
                calorieMultiplier > 1f -> "gain"
                calorieMultiplier < 1f -> "lose"
                else -> "maintain"
            }
        }

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
            omega3Mg = if (gender == "male") 1600 else 1100,
            idealWeightKg = idealWeight,
            weightPlanSummary = planSummary
        )
    }

    fun createPersonalizedGoals(age: Int, heightCm: Int, weightKg: Float, gender: String, goalMode: WeightGoalMode, onComplete: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val success = runCatching {
            app.preferences.saveProfile(age, heightCm, weightKg, gender)
            clearMenstrualLogsIfNotFemale(gender)
            val targets = buildPersonalizedTargets(age, heightCm, weightKg, gender, goalMode)
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
        clearMenstrualLogsIfNotFemale(gender)
    }

    private suspend fun clearMenstrualLogsIfNotFemale(gender: String) {
        if (gender != "female") {
            repository.clearMenstrualCycleLogs()
        }
    }

    private fun syncOptionsForGrantedPermissions(
        settings: HealthSyncPreferences,
        grantedPermissions: Set<String>
    ): com.leosoft.longevity.domain.repository.LongevityRepository.ExternalSyncOptions {
        val canSyncSteps = settings.stepsEnabled && grantedPermissions.containsAll(healthConnectAdapter.stepsPermissions)
        val canSyncSleep = settings.sleepEnabled && grantedPermissions.containsAll(healthConnectAdapter.sleepPermissions)
        val canSyncExercise = settings.exerciseEnabled && grantedPermissions.containsAll(healthConnectAdapter.exercisePermissions)
        val canSyncNutrition = settings.nutritionEnabled && grantedPermissions.containsAll(healthConnectAdapter.nutritionPermissions)
        val canSyncHydration = settings.hydrationEnabled && grantedPermissions.containsAll(healthConnectAdapter.hydrationPermissions)
        val canSyncMenstruation = grantedPermissions.containsAll(healthConnectAdapter.menstruationPermissions)

        return com.leosoft.longevity.domain.repository.LongevityRepository.ExternalSyncOptions(
            hydration = canSyncHydration,
            sleep = canSyncSleep,
            steps = canSyncSteps,
            exercise = canSyncExercise,
            nutrition = canSyncNutrition,
            menstruation = canSyncMenstruation,
            conflictResolution = ConflictResolution.LAST_WRITE_WINS,
            importDays = 30
        )
    }

    private suspend fun autoSyncHealthConnectIfEnabled() {
        val settings = healthSyncPreferences.value
        if (!settings.enabled || !healthConnectAvailable) return

        val grantedPermissions = healthConnectAdapter.grantedPermissions()
        val options = syncOptionsForGrantedPermissions(settings, grantedPermissions)
        val hasAnyEnabledScope = options.hydration || options.sleep || options.steps || options.exercise || options.nutrition || options.menstruation
        if (!hasAnyEnabledScope) {
            _healthPermissionsGranted.value = false
            return
        }

        repository.syncWithHealthConnect(options)
        _healthPermissionsGranted.value = true
        app.preferences.updateHealthSyncPreferences { it.copy(lastSyncAt = LocalDateTime.now()) }
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
            autoSyncHealthConnectIfEnabled()
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
            autoSyncHealthConnectIfEnabled()
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

    fun addWater(ml: Int) = viewModelScope.launch {
        repository.addWater(selectedNutritionDate.value, ml)
        autoSyncHealthConnectIfEnabled()
    }

    fun addSteps(steps: Int) = viewModelScope.launch {
        repository.addSteps(StepsLogEntity(date = LocalDate.now(), steps = steps, updatedAt = LocalDateTime.now()))
        autoSyncHealthConnectIfEnabled()
    }

    fun addSupplementLog(supplementId: Long) = viewModelScope.launch {
        repository.addSupplementLog(selectedNutritionDate.value, supplementId, true)
    }

    fun addSleepLog(bedtime: String, wakeTime: String) = viewModelScope.launch {
        repository.addSleepLog(LocalDate.now(), bedtime, wakeTime)
        autoSyncHealthConnectIfEnabled()
    }

    fun addWorkout(type: WorkoutType, durationMinutes: Int, intensity: Int, notes: String) = viewModelScope.launch {
        repository.addWorkoutLog(LocalDate.now(), type, durationMinutes, intensity, notes)
        autoSyncHealthConnectIfEnabled()
    }

    fun addMenstrualCycleLog(periodStartDate: LocalDate, cycleLengthDays: Int = 28, periodLengthDays: Int = 5) = viewModelScope.launch {
        repository.addMenstrualCycleLog(periodStartDate, cycleLengthDays, periodLengthDays)
        autoSyncHealthConnectIfEnabled()
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

    fun setStepsGoal(goal: Int) = viewModelScope.launch {
        repository.updateGoal("steps", goal)
    }

    fun setForegroundStepTracking(enabled: Boolean) = viewModelScope.launch {
        app.preferences.setForegroundTrackingEnabled(enabled)
        val intent = Intent(getApplication(), StepTrackingService::class.java)
        if (enabled) getApplication<Application>().startForegroundService(intent)
        else getApplication<Application>().stopService(intent)
    }

    fun ensureCoreFoods() = viewModelScope.launch {
        repository.ensureCoreFoods()
    }

    private fun requiredPermissions(settings: HealthSyncPreferences): Set<String> = buildSet {
        if (settings.stepsEnabled) addAll(healthConnectAdapter.stepsPermissions)
        if (settings.sleepEnabled) addAll(healthConnectAdapter.sleepPermissions)
        if (settings.exerciseEnabled) addAll(healthConnectAdapter.exercisePermissions)
        if (settings.nutritionEnabled) addAll(healthConnectAdapter.nutritionPermissions)
        if (settings.hydrationEnabled) addAll(healthConnectAdapter.hydrationPermissions)
        addAll(healthConnectAdapter.menstruationPermissions)
    }

    suspend fun hasHealthPermissions(settings: HealthSyncPreferences = healthSyncPreferences.value): Boolean {
        val required = requiredPermissions(settings)
        if (required.isEmpty()) return true
        return healthConnectAdapter.grantedPermissions().containsAll(required)
    }

    suspend fun missingHealthPermissions(): Set<String> {
        val granted = healthConnectAdapter.grantedPermissions()
        return healthConnectPermissions - granted
    }

    fun refreshHealthPermissions() = viewModelScope.launch {
        val settings = healthSyncPreferences.value
        if (!settings.enabled || !healthConnectAvailable) {
            _healthPermissionsGranted.value = false
            return@launch
        }
        val options = syncOptionsForGrantedPermissions(settings, healthConnectAdapter.grantedPermissions())
        _healthPermissionsGranted.value = options.hydration || options.sleep || options.steps || options.exercise || options.nutrition || options.menstruation
    }

    fun permissionsContract() = healthConnectAdapter.permissionsContract()

    fun setHealthSyncEnabled(enabled: Boolean) = viewModelScope.launch {
        app.preferences.updateHealthSyncPreferences {
            if (enabled) {
                it.copy(
                    enabled = true,
                    hydrationEnabled = true,
                    sleepEnabled = true,
                    stepsEnabled = true,
                    exerciseEnabled = true,
                    nutritionEnabled = true
                )
            } else {
                it.copy(enabled = false)
            }
        }
        refreshHealthPermissions()
    }

    fun setHealthScope(key: String, enabled: Boolean) = viewModelScope.launch {
        app.preferences.updateHealthSyncPreferences {
            when (key) {
                "water" -> it.copy(hydrationEnabled = enabled)
                "sleep" -> it.copy(sleepEnabled = enabled)
                "steps" -> it.copy(stepsEnabled = enabled)
                "exercise" -> it.copy(exerciseEnabled = enabled)
                "nutrition" -> it.copy(nutritionEnabled = enabled)
                else -> it
            }
        }
        refreshHealthPermissions()
    }

    fun syncNow() = viewModelScope.launch {
        if (!healthSyncPreferences.value.enabled) return@launch
        autoSyncHealthConnectIfEnabled()
    }


    fun addPulseMeasurement(bpm: Int, quality: Int, confidenceLabel: String, measurementSeconds: Int) = viewModelScope.launch {
        repository.addPulseMeasurement(bpm, quality, confidenceLabel, measurementSeconds)
    }
}
