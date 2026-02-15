package com.leosoft.longevity.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcalPer100g: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val ironMg: Float = 0f,
    val magnesiumMg: Float = 0f,
    val potassiumMg: Float = 0f,
    val vitaminDUi: Float = 0f,
    val omega3Mg: Float = 0f
)

@Entity(tableName = "meal_entries")
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val time: LocalDateTime,
    val mealType: MealType,
    val foodId: Long,
    val grams: Int
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val time: LocalDateTime,
    val amountMl: Int
)

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultDoseText: String,
    val notes: String = ""
)

@Entity(tableName = "supplement_logs")
data class SupplementLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val time: LocalDateTime,
    val supplementId: Long,
    val taken: Boolean
)

@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val bedtime: LocalDateTime,
    val wakeTime: LocalDateTime,
    val durationMinutes: Int
)

@Entity(tableName = "steps_logs")
data class StepsLogEntity(
    @PrimaryKey val date: LocalDate,
    val steps: Int,
    val updatedAt: LocalDateTime
)

@Entity(tableName = "workout_logs")
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val time: LocalDateTime,
    val type: WorkoutType,
    val durationMinutes: Int,
    val intensity: Int,
    val notes: String = ""
)

@Entity(tableName = "task_logs")
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val title: String,
    val targetText: String? = null,
    val completed: Boolean = false,
    val createdAt: LocalDateTime
)

@Entity(tableName = "reminder_logs")
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val reminderType: String,
    val reminderTime: String,
    val createdAt: LocalDateTime
)

@Entity(tableName = "user_goals")
data class UserGoalsEntity(
    @PrimaryKey val id: Int = 1,
    val proteinTarget: Float,
    val carbsTarget: Float,
    val fatTarget: Float,
    val fiberTarget: Float,
    val waterTargetMl: Int,
    val stepsTarget: Int,
    val sleepTargetMinutes: Int,
    val wakeTime: String,
    val bedTime: String,
    val supplementsPerDayTarget: Int
)

@Entity(tableName = "daily_scores")
data class DailyScoreEntity(
    @PrimaryKey val date: LocalDate,
    val nutritionScore: Float,
    val hydrationScore: Float,
    val activityScore: Float,
    val sleepScore: Float,
    val supplementsScore: Float,
    val totalScore: Float,
    val updatedAt: LocalDateTime
)
