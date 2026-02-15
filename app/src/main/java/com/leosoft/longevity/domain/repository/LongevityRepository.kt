package com.leosoft.longevity.domain.repository

import com.leosoft.longevity.data.local.entity.DailyScoreEntity
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.StepsLogEntity
import com.leosoft.longevity.data.local.entity.UserGoalsEntity
import com.leosoft.longevity.domain.model.DashboardSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface LongevityRepository {
    fun observeGoals(): Flow<UserGoalsEntity?>
    suspend fun saveGoals(goals: UserGoalsEntity)

    fun observeFoods(): Flow<List<FoodEntity>>
    fun observeMealEntries(date: LocalDate): Flow<List<MealEntryEntity>>
    suspend fun addMealEntry(entry: MealEntryEntity)

    suspend fun addWater(date: LocalDate, amountMl: Int)
    suspend fun addSteps(log: StepsLogEntity)

    fun observeDailyScore(date: LocalDate): Flow<DailyScoreEntity?>
    fun observeDashboard(date: LocalDate): Flow<DashboardSummary>

    suspend fun recalculateScore(date: LocalDate)
}
