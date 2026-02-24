package com.leosoft.longevity.reminders

import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.GoalPlanEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.MealType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DailyCoachReminderEvaluatorTest {
    @Test
    fun `returns all deficits when actuals are below targets`() {
        val meal = MealEntryEntity(
            id = 1,
            date = LocalDate.of(2026, 1, 10),
            time = LocalDateTime.of(2026, 1, 10, 12, 0),
            mealType = MealType.LUNCH,
            foodId = 7,
            grams = 100
        )
        val food = FoodEntity(id = 7, name = "Test", kcalPer100g = 100, protein = 5f, carbs = 10f, fat = 3f, fiber = 1f, ironMg = 1f)
        val microGoals = listOf(
            GoalPlanEntity(id = 1, goalType = "iron", target = 8, cadence = "daily", createdAt = LocalDateTime.now())
        )

        val deficits = DailyCoachReminderEvaluator.evaluateDeficits(
            waterActualMl = 1200,
            waterTargetMl = 2500,
            proteinActual = 60f,
            proteinTarget = 120f,
            carbsActual = 100f,
            carbsTarget = 180f,
            fatActual = 30f,
            fatTarget = 60f,
            fiberActual = 10f,
            fiberTarget = 30f,
            meals = listOf(meal),
            foodsById = mapOf(food.id to food),
            micronutrientGoalPlans = microGoals
        )

        assertTrue(deficits.waterBehind)
        assertTrue(deficits.macroBehind)
        assertTrue(deficits.microBehind)
    }

    @Test
    fun `logging reminder trigger alternates 11 and 20`() {
        val morning = DailyCoachReminderScheduler.nextTriggerAt(
            now = LocalDateTime.of(2026, 1, 10, 9, 0),
            eventType = DailyCoachEventType.LOGGING_REMINDER
        )
        val evening = DailyCoachReminderScheduler.nextTriggerAt(
            now = LocalDateTime.of(2026, 1, 10, 14, 0),
            eventType = DailyCoachEventType.LOGGING_REMINDER
        )

        assertTrue(morning.hour == 11 && morning.minute == 0)
        assertTrue(evening.hour == 20 && evening.minute == 0)
        assertFalse(evening.toLocalDate().isAfter(LocalDate.of(2026, 1, 10)))
    }
}
