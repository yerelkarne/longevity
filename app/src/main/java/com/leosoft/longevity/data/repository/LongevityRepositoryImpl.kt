package com.leosoft.longevity.data.repository

import com.leosoft.longevity.data.local.dao.ActivityDao
import com.leosoft.longevity.data.local.dao.GoalsDao
import com.leosoft.longevity.data.local.dao.LifeDao
import com.leosoft.longevity.data.local.dao.NutritionDao
import com.leosoft.longevity.data.local.dao.QuickAddDao
import com.leosoft.longevity.data.local.dao.ScoresDao
import com.leosoft.longevity.data.local.dao.SupplementsDao
import com.leosoft.longevity.data.local.dao.WaterDao
import com.leosoft.longevity.data.local.entity.DailyScoreEntity
import com.leosoft.longevity.data.local.entity.ConflictResolution
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.GoalPlanEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.MealNutritionRecordEntity
import com.leosoft.longevity.data.local.entity.RecordSource
import com.leosoft.longevity.data.local.entity.ReminderLogEntity
import com.leosoft.longevity.data.local.entity.SleepLogEntity
import com.leosoft.longevity.data.local.entity.StepsLogEntity
import com.leosoft.longevity.data.local.entity.SupplementEntity
import com.leosoft.longevity.data.local.entity.SupplementLogEntity
import com.leosoft.longevity.data.local.entity.SyncState
import com.leosoft.longevity.data.local.entity.TaskLogEntity
import com.leosoft.longevity.data.local.entity.UserGoalsEntity
import com.leosoft.longevity.data.local.entity.WaterLogEntity
import com.leosoft.longevity.data.local.entity.WorkoutLogEntity
import com.leosoft.longevity.data.local.entity.WorkoutType
import com.leosoft.longevity.domain.model.DashboardSummary
import com.leosoft.longevity.domain.model.DayData
import com.leosoft.longevity.domain.repository.LongevityRepository
import com.leosoft.longevity.domain.usecase.CalculateDailyScoreUseCase
import com.leosoft.longevity.domain.usecase.CalculateMacroTotalsUseCase
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private data class DashboardInputs(
    val score: DailyScoreEntity?,
    val steps: StepsLogEntity?,
    val waterLogs: List<WaterLogEntity>,
    val sleep: SleepLogEntity?,
    val meals: List<MealEntryEntity>,
    val foods: List<FoodEntity>,
    val supplementLogs: List<SupplementLogEntity>,
    val goals: UserGoalsEntity?
)

class LongevityRepositoryImpl(
    private val nutritionDao: NutritionDao,
    private val waterDao: WaterDao,
    private val supplementsDao: SupplementsDao,
    private val lifeDao: LifeDao,
    private val activityDao: ActivityDao,
    private val goalsDao: GoalsDao,
    private val scoresDao: ScoresDao,
    private val quickAddDao: QuickAddDao,
    private val healthConnectAdapter: HealthConnectAdapter,
    private val calculateDailyScore: CalculateDailyScoreUseCase,
    private val calculateMacroTotals: CalculateMacroTotalsUseCase
) : LongevityRepository {

    override fun observeGoals(): Flow<UserGoalsEntity?> = goalsDao.observeGoals()
    override suspend fun saveGoals(goals: UserGoalsEntity) { goalsDao.upsertGoals(goals) }
    override fun observeFoods(): Flow<List<FoodEntity>> = nutritionDao.observeFoods()
    override fun observeFoodsWithNutrition(): Flow<List<FoodEntity>> = nutritionDao.observeFoodsWithNutrition()
    override fun observeSupplements(): Flow<List<SupplementEntity>> = supplementsDao.observeSupplements()
    override fun observeMealEntries(date: LocalDate): Flow<List<MealEntryEntity>> = nutritionDao.observeMealEntries(date)

    override suspend fun addMealEntry(entry: MealEntryEntity) {
        val mealEntryId = nutritionDao.insertMealEntry(entry)
        nutritionDao.getFoodById(entry.foodId)?.let { food ->
            nutritionDao.insertMealNutritionRecord(
                MealNutritionRecordEntity(
                    mealEntryId = mealEntryId,
                    date = entry.date,
                    foodId = food.id,
                    grams = entry.grams,
                    protein = nutrientValuePerGram(food.protein, entry.grams),
                    carbs = nutrientValuePerGram(food.carbs, entry.grams),
                    fat = nutrientValuePerGram(food.fat, entry.grams),
                    fiber = nutrientValuePerGram(food.fiber, entry.grams),
                    ironMg = nutrientValuePerGram(food.ironMg, entry.grams),
                    magnesiumMg = nutrientValuePerGram(food.magnesiumMg, entry.grams),
                    potassiumMg = nutrientValuePerGram(food.potassiumMg, entry.grams),
                    vitaminDUi = nutrientValuePerGram(food.vitaminDUi, entry.grams),
                    omega3Mg = nutrientValuePerGram(food.omega3Mg, entry.grams),
                    createdAt = LocalDateTime.now(),
                    syncState = SyncState.PENDING_UPLOAD
                )
            )
        }
        recalculateScore(entry.date)
    }

    override suspend fun updateMealEntry(entry: MealEntryEntity) {
        nutritionDao.updateMealEntry(id = entry.id, foodId = entry.foodId, grams = entry.grams)
        nutritionDao.deleteMealNutritionRecordByMealEntryId(entry.id)
        nutritionDao.getFoodById(entry.foodId)?.let { food ->
            nutritionDao.insertMealNutritionRecord(
                MealNutritionRecordEntity(
                    mealEntryId = entry.id,
                    date = entry.date,
                    foodId = food.id,
                    grams = entry.grams,
                    protein = nutrientValuePerGram(food.protein, entry.grams),
                    carbs = nutrientValuePerGram(food.carbs, entry.grams),
                    fat = nutrientValuePerGram(food.fat, entry.grams),
                    fiber = nutrientValuePerGram(food.fiber, entry.grams),
                    ironMg = nutrientValuePerGram(food.ironMg, entry.grams),
                    magnesiumMg = nutrientValuePerGram(food.magnesiumMg, entry.grams),
                    potassiumMg = nutrientValuePerGram(food.potassiumMg, entry.grams),
                    vitaminDUi = nutrientValuePerGram(food.vitaminDUi, entry.grams),
                    omega3Mg = nutrientValuePerGram(food.omega3Mg, entry.grams),
                    createdAt = LocalDateTime.now(),
                    syncState = SyncState.PENDING_UPLOAD
                )
            )
        }
        recalculateScore(entry.date)
    }

    override suspend fun deleteMealEntry(id: Long, date: LocalDate) {
        nutritionDao.deleteMealNutritionRecordByMealEntryId(id)
        nutritionDao.deleteMealEntry(id)
        recalculateScore(date)
    }

    override suspend fun addCustomFood(name: String): Long {
        return nutritionDao.insertFood(
            FoodEntity(
                name = name,
                kcalPer100g = 0,
                protein = 0f,
                carbs = 0f,
                fat = 0f,
                fiber = 0f
            )
        )
    }

    override suspend fun addWater(date: LocalDate, amountMl: Int) {
        waterDao.insert(WaterLogEntity(date = date, time = LocalDateTime.now(), amountMl = amountMl, syncState = SyncState.PENDING_UPLOAD))
        recalculateScore(date)
    }

    override fun observeWaterLogs(date: LocalDate): Flow<List<WaterLogEntity>> = waterDao.observeByDate(date)

    override suspend fun addSteps(log: StepsLogEntity) {
        activityDao.upsertSteps(log.copy(syncState = SyncState.PENDING_UPLOAD))
        recalculateScore(log.date)
    }

    override fun observeWeeklySteps(endDate: LocalDate): Flow<List<StepsLogEntity>> {
        val start = endDate.minusDays(6)
        return activityDao.observeStepsBetween(start, endDate)
    }

    override fun observeMonthlyStepsTotal(monthDate: LocalDate): Flow<Int> {
        val start = monthDate.withDayOfMonth(1)
        val end = monthDate.withDayOfMonth(monthDate.lengthOfMonth())
        return activityDao.observeStepsTotalBetween(start, end)
    }

    override suspend fun addSupplementLog(date: LocalDate, supplementId: Long, taken: Boolean) {
        supplementsDao.insertLog(SupplementLogEntity(date = date, time = LocalDateTime.now(), supplementId = supplementId, taken = taken))
        recalculateScore(date)
    }

    override fun observeSupplementLogs(date: LocalDate): Flow<List<SupplementLogEntity>> = supplementsDao.observeLogs(date)

    override fun observeSleepLogs(): Flow<List<SleepLogEntity>> = lifeDao.observeSleepLogs()

    override suspend fun addSleepLog(date: LocalDate, bedtime: String, wakeTime: String) {
        val bed = LocalTime.parse(bedtime)
        val wake = LocalTime.parse(wakeTime)
        val bedDateTime = LocalDateTime.of(date, bed)
        val wakeDateTime = LocalDateTime.of(if (wake.isBefore(bed)) date.plusDays(1) else date, wake)
        val duration = Duration.between(bedDateTime, wakeDateTime).toMinutes().toInt().coerceAtLeast(0)
        lifeDao.insertSleepLog(SleepLogEntity(date = date, bedtime = bedDateTime, wakeTime = wakeDateTime, durationMinutes = duration, syncState = SyncState.PENDING_UPLOAD))
        recalculateScore(date)
    }

    override suspend fun addWorkoutLog(date: LocalDate, type: WorkoutType, durationMinutes: Int, intensity: Int, notes: String) {
        activityDao.insertWorkout(WorkoutLogEntity(date = date, time = LocalDateTime.now(), type = type, durationMinutes = durationMinutes, intensity = intensity, notes = notes, syncState = SyncState.PENDING_UPLOAD))
        recalculateScore(date)
    }

    override suspend fun syncWithHealthConnect(options: LongevityRepository.ExternalSyncOptions): LongevityRepository.ExternalSyncResult {
        if (!healthConnectAdapter.isAvailable()) {
            return LongevityRepository.ExternalSyncResult(0, 0, 0, "Health Connect kullanılamıyor")
        }
        return runCatching {
            val now = LocalDateTime.now()
            var uploaded = 0
            var imported = 0

            if (options.steps) {
                activityDao.getPendingStepUploads().forEach { step ->
                    val start = step.date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val end = step.date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val id = healthConnectAdapter.insertSteps(start, end, step.steps.toLong())
                    if (id != null) {
                        activityDao.updateStepSyncState(step.date, SyncState.SYNCED, id, now)
                        uploaded++
                    }
                }
            }
            if (options.hydration) {
                waterDao.getPendingUploads().forEach { item ->
                    val id = healthConnectAdapter.insertHydration(item.time.atZone(ZoneId.systemDefault()).toInstant(), item.amountMl.toDouble())
                    if (id != null) {
                        waterDao.updateSyncState(item.id, SyncState.SYNCED, id, now)
                        uploaded++
                    }
                }
            }
            if (options.sleep) {
                lifeDao.getPendingUploads().forEach { item ->
                    val id = healthConnectAdapter.insertSleep(item.bedtime.atZone(ZoneId.systemDefault()).toInstant(), item.wakeTime.atZone(ZoneId.systemDefault()).toInstant())
                    if (id != null) {
                        lifeDao.updateSyncState(item.id, SyncState.SYNCED, id, now)
                        uploaded++
                    }
                }
            }
            if (options.exercise) {
                activityDao.getPendingWorkoutUploads().forEach { item ->
                    val start = item.time.atZone(ZoneId.systemDefault()).toInstant()
                    val end = item.time.plusMinutes(item.durationMinutes.toLong()).atZone(ZoneId.systemDefault()).toInstant()
                    val id = healthConnectAdapter.insertExercise(start, end, item.type, item.notes)
                    if (id != null) {
                        activityDao.updateWorkoutSyncState(item.id, SyncState.SYNCED, id, now)
                        uploaded++
                    }
                }
            }
            if (options.nutrition) {
                nutritionDao.getPendingNutritionUploads().forEach { item ->
                    val calories = ((item.protein + item.carbs) * 4f + (item.fat * 9f)).toDouble()
                    val id = healthConnectAdapter.insertNutrition(
                        item.createdAt.atZone(ZoneId.systemDefault()).toInstant(),
                        item.protein.toDouble(),
                        item.carbs.toDouble(),
                        item.fat.toDouble(),
                        calories
                    )
                    if (id != null) {
                        nutritionDao.updateNutritionSyncState(item.id, SyncState.SYNCED, id, now)
                        uploaded++
                    }
                }
            }

            val importStartDate = LocalDate.now().minusDays(options.importDays)
            val start = healthConnectAdapter.dayStart(importStartDate)
            val end = healthConnectAdapter.dayEnd(LocalDate.now())

            if (options.steps) {
                healthConnectAdapter.readSteps(start, end).forEach { record ->
                    val date = healthConnectAdapter.instantToLocalDateTime(record.endTime).toLocalDate()
                    val existing = activityDao.getSteps(date)
                    if (existing == null || shouldApplyRemote(existing.updatedAt, record.metadata.lastModifiedTime, options.conflictResolution)) {
                        activityDao.upsertSteps(
                            StepsLogEntity(
                                date = date,
                                steps = record.count.toInt(),
                                goal = existing?.goal ?: 10000,
                                updatedAt = healthConnectAdapter.instantToLocalDateTime(record.metadata.lastModifiedTime),
                                source = RecordSource.HEALTH_CONNECT,
                                syncState = SyncState.SYNCED,
                                hcRecordId = record.metadata.id,
                                lastSyncedAt = now
                            )
                        )
                        imported++
                    }
                }
            }
            LongevityRepository.ExternalSyncResult(uploaded, imported, 0)
        }.getOrElse {
            LongevityRepository.ExternalSyncResult(0, 0, 0, it.message)
        }
    }

    private fun shouldApplyRemote(localUpdatedAt: LocalDateTime, remoteUpdatedAt: java.time.Instant, rule: ConflictResolution): Boolean {
        return when (rule) {
            ConflictResolution.LOCAL_PRIORITY -> false
            ConflictResolution.HEALTH_CONNECT_PRIORITY -> true
            ConflictResolution.LAST_WRITE_WINS -> remoteUpdatedAt.isAfter(localUpdatedAt.atZone(ZoneId.systemDefault()).toInstant())
        }
    }

    override suspend fun addTaskLog(date: LocalDate, title: String, targetText: String?) {
        quickAddDao.insertTask(TaskLogEntity(date = date, title = title, targetText = targetText, createdAt = LocalDateTime.now()))
    }

    override fun observeGoalPlans() = quickAddDao.observeGoalPlans()

    override suspend fun addGoalPlan(goalType: String, target: Int, cadence: String): Long {
        return quickAddDao.insertGoalPlan(
            GoalPlanEntity(
                goalType = goalType,
                target = target,
                cadence = cadence,
                createdAt = LocalDateTime.now()
            )
        )
    }

    override suspend fun updateGoalPlan(id: Long, goalType: String, target: Int, cadence: String) {
        quickAddDao.updateGoalPlan(id, goalType, target, cadence)
    }

    override suspend fun deleteGoalPlan(id: Long) {
        quickAddDao.deleteGoalPlan(id)
    }

    override suspend fun clearGoalPlans() {
        quickAddDao.clearGoalPlans()
    }

    override fun observeReminders() = quickAddDao.observeReminders()

    override suspend fun addReminderLog(date: LocalDate, reminderType: String, reminderTime: String, cadence: String, intervalHours: Int?): Long {
        return quickAddDao.insertReminder(
            ReminderLogEntity(
                date = date,
                reminderType = reminderType,
                reminderTime = reminderTime,
                cadence = cadence,
                intervalHours = intervalHours,
                createdAt = LocalDateTime.now()
            )
        )
    }

    override suspend fun updateReminderLog(id: Long, reminderType: String, reminderTime: String, cadence: String, intervalHours: Int?) {
        quickAddDao.updateReminder(id, reminderType, reminderTime, cadence, intervalHours)
    }

    override suspend fun deleteReminderLog(id: Long) {
        quickAddDao.deleteReminder(id)
    }

    override suspend fun updateGoal(goalType: String, value: Int) {
        val current = goalsDao.getGoals() ?: defaultGoals()
        val updated = when (goalType) {
            "water" -> current.copy(waterTargetMl = value)
            "steps" -> current.copy(stepsTarget = value)
            "protein" -> current.copy(proteinTarget = value.toFloat())
            "carbs" -> current.copy(carbsTarget = value.toFloat())
            "fat" -> current.copy(fatTarget = value.toFloat())
            "fiber" -> current.copy(fiberTarget = value.toFloat())
            "sleep" -> current.copy(sleepTargetMinutes = value)
            "supplements" -> current.copy(supplementsPerDayTarget = value)
            else -> current
        }
        goalsDao.upsertGoals(updated)
        recalculateScore(LocalDate.now())
    }

    override fun observeDailyScore(date: LocalDate): Flow<DailyScoreEntity?> = scoresDao.observeByDate(date)

    override fun observeDashboard(date: LocalDate): Flow<DashboardSummary> {
        val partialFlow: Flow<DashboardInputs> = combine(scoresDao.observeByDate(date), activityDao.observeSteps(date)) { score, steps ->
            DashboardInputs(score, steps, emptyList(), null, emptyList(), emptyList(), emptyList(), null)
        }
            .combine(waterDao.observeByDate(date)) { partial, waterLogs -> partial.copy(waterLogs = waterLogs) }
            .combine(lifeDao.observeSleep(date)) { partial, sleep -> partial.copy(sleep = sleep) }
            .combine(nutritionDao.observeMealEntries(date)) { partial, meals -> partial.copy(meals = meals) }
            .combine(nutritionDao.observeFoods()) { partial, foods -> partial.copy(foods = foods) }
            .combine(supplementsDao.observeLogs(date)) { partial, logs -> partial.copy(supplementLogs = logs) }
            .combine(goalsDao.observeGoals()) { partial, goals -> partial.copy(goals = goals) }

        return partialFlow.combine(goalsDao.observeGoals()) { partial, latestGoals ->
            val safeGoals = latestGoals ?: partial.goals ?: defaultGoals()
            val totals = calculateMacroTotals(partial.meals, partial.foods.associateBy { it.id })
            val stepsValue = partial.steps?.steps ?: 0
            val waterTotal = partial.waterLogs.sumOf { it.amountMl }
            val sleepMinutes = partial.sleep?.durationMinutes ?: 0
            val workoutMinutesByType = activityDao.getWorkouts(date)
                .groupBy { it.type.name.lowercase() }
                .mapValues { (_, logs) -> logs.sumOf { it.durationMinutes } }
            DashboardSummary(
                score = partial.score,
                steps = stepsValue,
                waterMl = waterTotal,
                sleepMinutes = sleepMinutes,
                supplementsTaken = partial.supplementLogs.count { it.taken },
                workoutMinutesByType = workoutMinutesByType,
                macroTotals = totals,
                pendingTasks = buildList {
                    if (stepsValue < safeGoals.stepsTarget) add("${safeGoals.stepsTarget} adım tamamla")
                    if (waterTotal < safeGoals.waterTargetMl) add("Su hedefini tamamla")
                    if (sleepMinutes < safeGoals.sleepTargetMinutes) add("Uyku hedefini tuttur")
                    if (totals.protein < safeGoals.proteinTarget) add("Protein hedefine yaklaş")
                }
            )
        }
    }

    override suspend fun recalculateScore(date: LocalDate) = withContext(Dispatchers.IO) {
        val goals = goalsDao.getGoals() ?: defaultGoals()
        val meals = nutritionDao.getMealEntries(date)
        val foods = nutritionDao.observeFoods().first().associateBy { it.id }
        val dayData = DayData(
            meals = meals,
            foodsById = foods,
            waterMl = waterDao.getWaterTotal(date),
            steps = activityDao.getSteps(date)?.steps ?: 0,
            workouts = activityDao.getWorkouts(date),
            sleep = lifeDao.getSleep(date),
            supplementsTaken = supplementsDao.takenCount(date),
            goals = goals
        )
        scoresDao.upsert(calculateDailyScore(dayData, date))
    }


    override suspend fun ensureCoreFoods() {
        val defaults = listOf(
            FoodEntity(name = "Yumurta", kcalPer100g = 155, protein = 13f, carbs = 1.1f, fat = 11f, fiber = 0f, vitaminDUi = 82f),
            FoodEntity(name = "Zeytin", kcalPer100g = 115, protein = 0.8f, carbs = 6.3f, fat = 10.7f, fiber = 3.2f, ironMg = 3.3f, potassiumMg = 42f)
        )
        defaults.forEach { food ->
            val existing = nutritionDao.getFoodByName(food.name)
            if (existing == null) {
                nutritionDao.insertFood(food)
            } else {
                nutritionDao.updateFoodNutritionById(
                    id = existing.id,
                    kcalPer100g = food.kcalPer100g,
                    protein = food.protein,
                    carbs = food.carbs,
                    fat = food.fat,
                    fiber = food.fiber,
                    ironMg = food.ironMg,
                    magnesiumMg = food.magnesiumMg,
                    potassiumMg = food.potassiumMg,
                    vitaminDUi = food.vitaminDUi,
                    omega3Mg = food.omega3Mg
                )
            }
        }
    }


    private fun nutrientValuePerGram(valuePer100g: Float, grams: Int): Float = valuePer100g * (grams / 100f)

    private fun defaultGoals() = UserGoalsEntity(
        proteinTarget = 120f,
        carbsTarget = 180f,
        fatTarget = 60f,
        fiberTarget = 30f,
        waterTargetMl = 2000,
        stepsTarget = 10000,
        sleepTargetMinutes = 480,
        wakeTime = "07:00",
        bedTime = "23:00",
        supplementsPerDayTarget = 2
    )
}
