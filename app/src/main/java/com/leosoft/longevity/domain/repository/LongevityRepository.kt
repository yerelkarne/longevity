package com.leosoft.longevity.domain.repository

import com.leosoft.longevity.data.local.entity.DailyScoreEntity
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.GoalPlanEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.StepsLogEntity
import com.leosoft.longevity.data.local.entity.SupplementEntity
import com.leosoft.longevity.data.local.entity.SupplementLogEntity
import com.leosoft.longevity.data.local.entity.ReminderLogEntity
import com.leosoft.longevity.data.local.entity.UserGoalsEntity
import com.leosoft.longevity.data.local.entity.WaterLogEntity
import com.leosoft.longevity.data.local.entity.WorkoutType
import com.leosoft.longevity.domain.model.DashboardSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface LongevityRepository {
    fun observeGoals(): Flow<UserGoalsEntity?>
    suspend fun saveGoals(goals: UserGoalsEntity)

    fun observeFoods(): Flow<List<FoodEntity>>
    fun observeFoodsWithNutrition(): Flow<List<FoodEntity>>
    fun observeSupplements(): Flow<List<SupplementEntity>>
    fun observeMealEntries(date: LocalDate): Flow<List<MealEntryEntity>>
    suspend fun addMealEntry(entry: MealEntryEntity)
    suspend fun updateMealEntry(entry: MealEntryEntity)
    suspend fun deleteMealEntry(id: Long, date: LocalDate)
    suspend fun addCustomFood(name: String): Long

    suspend fun addWater(date: LocalDate, amountMl: Int)
    fun observeWaterLogs(date: LocalDate): Flow<List<WaterLogEntity>>
    suspend fun addSteps(log: StepsLogEntity)
    suspend fun addSupplementLog(date: LocalDate, supplementId: Long, taken: Boolean)
    fun observeSupplementLogs(date: LocalDate): Flow<List<SupplementLogEntity>>
    suspend fun addSleepLog(date: LocalDate, bedtime: String, wakeTime: String)
    suspend fun addWorkoutLog(date: LocalDate, type: WorkoutType, durationMinutes: Int, intensity: Int, notes: String)
    suspend fun addTaskLog(date: LocalDate, title: String, targetText: String?)
    fun observeGoalPlans(): Flow<List<GoalPlanEntity>>
    suspend fun addGoalPlan(goalType: String, target: Int, cadence: String): Long
    suspend fun updateGoalPlan(id: Long, goalType: String, target: Int, cadence: String)
    suspend fun deleteGoalPlan(id: Long)
    fun observeReminders(): Flow<List<ReminderLogEntity>>
    suspend fun addReminderLog(date: LocalDate, reminderType: String, reminderTime: String, cadence: String, intervalHours: Int?): Long
    suspend fun updateReminderLog(id: Long, reminderType: String, reminderTime: String, cadence: String, intervalHours: Int?)
    suspend fun deleteReminderLog(id: Long)
    suspend fun updateGoal(goalType: String, value: Int)

    fun observeDailyScore(date: LocalDate): Flow<DailyScoreEntity?>
    fun observeDashboard(date: LocalDate): Flow<DashboardSummary>

    suspend fun recalculateScore(date: LocalDate)
    suspend fun ensureCoreFoods()
}
