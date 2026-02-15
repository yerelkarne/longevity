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
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.ReminderLogEntity
import com.leosoft.longevity.data.local.entity.SleepLogEntity
import com.leosoft.longevity.data.local.entity.StepsLogEntity
import com.leosoft.longevity.data.local.entity.SupplementEntity
import com.leosoft.longevity.data.local.entity.SupplementLogEntity
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
    private val calculateDailyScore: CalculateDailyScoreUseCase,
    private val calculateMacroTotals: CalculateMacroTotalsUseCase
) : LongevityRepository {

    override fun observeGoals(): Flow<UserGoalsEntity?> = goalsDao.observeGoals()
    override suspend fun saveGoals(goals: UserGoalsEntity) { goalsDao.upsertGoals(goals) }
    override fun observeFoods(): Flow<List<FoodEntity>> = nutritionDao.observeFoods()
    override fun observeSupplements(): Flow<List<SupplementEntity>> = supplementsDao.observeSupplements()
    override fun observeMealEntries(date: LocalDate): Flow<List<MealEntryEntity>> = nutritionDao.observeMealEntries(date)

    override suspend fun addMealEntry(entry: MealEntryEntity) {
        nutritionDao.insertMealEntry(entry)
        recalculateScore(entry.date)
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
        waterDao.insert(WaterLogEntity(date = date, time = LocalDateTime.now(), amountMl = amountMl))
        recalculateScore(date)
    }

    override suspend fun addSteps(log: StepsLogEntity) {
        activityDao.upsertSteps(log)
        recalculateScore(log.date)
    }

    override suspend fun addSupplementLog(date: LocalDate, supplementId: Long, taken: Boolean) {
        supplementsDao.insertLog(SupplementLogEntity(date = date, time = LocalDateTime.now(), supplementId = supplementId, taken = taken))
        recalculateScore(date)
    }

    override suspend fun addSleepLog(date: LocalDate, bedtime: String, wakeTime: String) {
        val bed = LocalTime.parse(bedtime)
        val wake = LocalTime.parse(wakeTime)
        val bedDateTime = LocalDateTime.of(date, bed)
        val wakeDateTime = LocalDateTime.of(if (wake.isBefore(bed)) date.plusDays(1) else date, wake)
        val duration = Duration.between(bedDateTime, wakeDateTime).toMinutes().toInt().coerceAtLeast(0)
        lifeDao.insertSleepLog(SleepLogEntity(date = date, bedtime = bedDateTime, wakeTime = wakeDateTime, durationMinutes = duration))
        recalculateScore(date)
    }

    override suspend fun addWorkoutLog(date: LocalDate, type: WorkoutType, durationMinutes: Int, intensity: Int, notes: String) {
        activityDao.insertWorkout(WorkoutLogEntity(date = date, time = LocalDateTime.now(), type = type, durationMinutes = durationMinutes, intensity = intensity, notes = notes))
        recalculateScore(date)
    }

    override suspend fun addTaskLog(date: LocalDate, title: String, targetText: String?) {
        quickAddDao.insertTask(TaskLogEntity(date = date, title = title, targetText = targetText, createdAt = LocalDateTime.now()))
    }

    override suspend fun addReminderLog(date: LocalDate, reminderType: String, reminderTime: String) {
        quickAddDao.insertReminder(ReminderLogEntity(date = date, reminderType = reminderType, reminderTime = reminderTime, createdAt = LocalDateTime.now()))
    }

    override suspend fun updateGoal(goalType: String, value: Int) {
        val current = goalsDao.getGoals() ?: defaultGoals()
        val updated = when (goalType) {
            "water" -> current.copy(waterTargetMl = value)
            "steps" -> current.copy(stepsTarget = value)
            "protein" -> current.copy(proteinTarget = value.toFloat())
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
            DashboardInputs(score, steps, emptyList(), null, emptyList(), emptyList(), null)
        }
            .combine(waterDao.observeByDate(date)) { partial, waterLogs -> partial.copy(waterLogs = waterLogs) }
            .combine(lifeDao.observeSleep(date)) { partial, sleep -> partial.copy(sleep = sleep) }
            .combine(nutritionDao.observeMealEntries(date)) { partial, meals -> partial.copy(meals = meals) }
            .combine(nutritionDao.observeFoods()) { partial, foods -> partial.copy(foods = foods) }
            .combine(goalsDao.observeGoals()) { partial, goals -> partial.copy(goals = goals) }

        return partialFlow.combine(goalsDao.observeGoals()) { partial, latestGoals ->
            val safeGoals = latestGoals ?: partial.goals ?: defaultGoals()
            val totals = calculateMacroTotals(partial.meals, partial.foods.associateBy { it.id })
            val stepsValue = partial.steps?.steps ?: 0
            val waterTotal = partial.waterLogs.sumOf { it.amountMl }
            val sleepMinutes = partial.sleep?.durationMinutes ?: 0
            DashboardSummary(
                score = partial.score,
                steps = stepsValue,
                waterMl = waterTotal,
                sleepMinutes = sleepMinutes,
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
            if (nutritionDao.getFoodByName(food.name) == null) {
                nutritionDao.insertFood(food)
            }
        }
    }

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
