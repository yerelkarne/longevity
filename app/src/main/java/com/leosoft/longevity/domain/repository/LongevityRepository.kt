package com.leosoft.longevity.domain.repository

import com.leosoft.longevity.data.local.entity.DailyScoreEntity
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.StepsLogEntity
import com.leosoft.longevity.data.local.entity.SupplementEntity
import com.leosoft.longevity.data.local.entity.UserGoalsEntity
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
    suspend fun addSteps(log: StepsLogEntity)
    suspend fun addSupplementLog(date: LocalDate, supplementId: Long, taken: Boolean)
    suspend fun addSleepLog(date: LocalDate, bedtime: String, wakeTime: String)
    suspend fun addWorkoutLog(date: LocalDate, type: WorkoutType, durationMinutes: Int, intensity: Int, notes: String)
    suspend fun addTaskLog(date: LocalDate, title: String, targetText: String?)
    suspend fun addReminderLog(date: LocalDate, reminderType: String, reminderTime: String)
    suspend fun updateGoal(goalType: String, value: Int)

    fun observeDailyScore(date: LocalDate): Flow<DailyScoreEntity?>
    fun observeDashboard(date: LocalDate): Flow<DashboardSummary>

    suspend fun recalculateScore(date: LocalDate)
    suspend fun ensureCoreFoods()
}
