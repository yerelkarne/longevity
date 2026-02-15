package com.leosoft.longevity.domain.model

import com.leosoft.longevity.data.local.entity.DailyScoreEntity
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.SleepLogEntity
import com.leosoft.longevity.data.local.entity.UserGoalsEntity
import com.leosoft.longevity.data.local.entity.WorkoutLogEntity

data class MacroTotals(
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float
)

data class ScoreBreakdown(
    val nutrition: Float,
    val hydration: Float,
    val activity: Float,
    val sleep: Float,
    val supplements: Float,
    val total: Float,
    val bestArea: String,
    val weakestArea: String
)

data class DayData(
    val meals: List<MealEntryEntity>,
    val foodsById: Map<Long, FoodEntity>,
    val waterMl: Int,
    val steps: Int,
    val workouts: List<WorkoutLogEntity>,
    val sleep: SleepLogEntity?,
    val supplementsTaken: Int,
    val goals: UserGoalsEntity
)

data class DashboardSummary(
    val score: DailyScoreEntity?,
    val steps: Int,
    val waterMl: Int,
    val sleepMinutes: Int,
    val macroTotals: MacroTotals,
    val pendingTasks: List<String>
)
