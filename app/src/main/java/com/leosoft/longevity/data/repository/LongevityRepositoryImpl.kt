package com.leosoft.longevity.data.repository

import com.leosoft.longevity.data.local.dao.ActivityDao
import com.leosoft.longevity.data.local.dao.GoalsDao
import com.leosoft.longevity.data.local.dao.LifeDao
import com.leosoft.longevity.data.local.dao.NutritionDao
import com.leosoft.longevity.data.local.dao.ScoresDao
import com.leosoft.longevity.data.local.dao.SupplementsDao
import com.leosoft.longevity.data.local.dao.WaterDao
import com.leosoft.longevity.data.local.entity.DailyScoreEntity
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.SleepLogEntity
import com.leosoft.longevity.data.local.entity.StepsLogEntity
import com.leosoft.longevity.data.local.entity.UserGoalsEntity
import com.leosoft.longevity.data.local.entity.WaterLogEntity
import com.leosoft.longevity.domain.model.DashboardSummary
import com.leosoft.longevity.domain.model.DayData
import com.leosoft.longevity.domain.repository.LongevityRepository
import com.leosoft.longevity.domain.usecase.CalculateDailyScoreUseCase
import com.leosoft.longevity.domain.usecase.CalculateMacroTotalsUseCase
import java.time.LocalDate
import java.time.LocalDateTime
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
    private val calculateDailyScore: CalculateDailyScoreUseCase,
    private val calculateMacroTotals: CalculateMacroTotalsUseCase
) : LongevityRepository {

    override fun observeGoals(): Flow<UserGoalsEntity?> = goalsDao.observeGoals()

    override suspend fun saveGoals(goals: UserGoalsEntity) {
        goalsDao.upsertGoals(goals)
    }

    override fun observeFoods(): Flow<List<FoodEntity>> = nutritionDao.observeFoods()

    override fun observeMealEntries(date: LocalDate): Flow<List<MealEntryEntity>> = nutritionDao.observeMealEntries(date)

    override suspend fun addMealEntry(entry: MealEntryEntity) {
        nutritionDao.insertMealEntry(entry)
        recalculateScore(entry.date)
    }

    override suspend fun addWater(date: LocalDate, amountMl: Int) {
        waterDao.insert(WaterLogEntity(date = date, time = LocalDateTime.now(), amountMl = amountMl))
        recalculateScore(date)
    }

    override suspend fun addSteps(log: StepsLogEntity) {
        activityDao.upsertSteps(log)
        recalculateScore(log.date)
    }

    override fun observeDailyScore(date: LocalDate): Flow<DailyScoreEntity?> = scoresDao.observeByDate(date)

    override fun observeDashboard(date: LocalDate): Flow<DashboardSummary> {
        val partialFlow: Flow<DashboardInputs> = combine(
            scoresDao.observeByDate(date),
            activityDao.observeSteps(date)
        ) { score, steps ->
            DashboardInputs(
                score = score,
                steps = steps,
                waterLogs = emptyList(),
                sleep = null,
                meals = emptyList(),
                foods = emptyList(),
                goals = null
            )
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
