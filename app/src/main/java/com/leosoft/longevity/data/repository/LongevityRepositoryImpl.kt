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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class LongevityRepositoryImpl(
    private val nutritionDao: NutritionDao,
    private val waterDao: WaterDao,
    private val supplementsDao: SupplementsDao,
    private val lifeDao: LifeDao,
    private val activityDao: ActivityDao,
    private val goalsDao: GoalsDao,
    private val scoresDao: ScoresDao,
    private val calculateDailyScore: CalculateDailyScoreUseCase,
    private val calculateMacroTotals: CalculateMacroTotalsUseCase,
    private val scope: CoroutineScope
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

    override fun observeDashboard(date: LocalDate): Flow<DashboardSummary> = combine(
        scoresDao.observeByDate(date),
        activityDao.observeSteps(date),
        waterDao.observeByDate(date),
        lifeDao.observeSleep(date),
        nutritionDao.observeMealEntries(date),
        nutritionDao.observeFoods(),
        goalsDao.observeGoals()
    ) { score, steps, waterLogs, sleep, meals, foods, goals ->
        val safeGoals = goals ?: defaultGoals()
        val totals = calculateMacroTotals(meals, foods.associateBy { it.id })
        DashboardSummary(
            score = score,
            steps = steps?.steps ?: 0,
            waterMl = waterLogs.sumOf { it.amountMl },
            sleepMinutes = sleep?.durationMinutes ?: 0,
            macroTotals = totals,
            pendingTasks = buildList {
                if ((steps?.steps ?: 0) < safeGoals.stepsTarget) add("${safeGoals.stepsTarget} adım tamamla")
                if (waterLogs.sumOf { it.amountMl } < safeGoals.waterTargetMl) add("Su hedefini tamamla")
                if ((sleep?.durationMinutes ?: 0) < safeGoals.sleepTargetMinutes) add("Uyku hedefini tuttur")
                if (totals.protein < safeGoals.proteinTarget) add("Protein hedefine yaklaş")
            }
        )
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
