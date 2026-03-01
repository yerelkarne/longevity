package com.leosoft.longevity.reminders

import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.GoalPlanEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity

data class GoalDeficits(
    val waterBehind: Boolean,
    val macroBehind: Boolean,
    val microBehind: Boolean
)

object DailyCoachReminderEvaluator {
    private val micronutrientTypes = setOf("iron", "magnesium", "potassium", "vitamin_d", "omega3")

    fun evaluateDeficits(
        waterActualMl: Int,
        waterTargetMl: Int,
        proteinActual: Float,
        proteinTarget: Float,
        carbsActual: Float,
        carbsTarget: Float,
        fatActual: Float,
        fatTarget: Float,
        fiberActual: Float,
        fiberTarget: Float,
        meals: List<MealEntryEntity>,
        foodsById: Map<Long, FoodEntity>,
        micronutrientGoalPlans: List<GoalPlanEntity>
    ): GoalDeficits {
        val waterBehind = waterActualMl < waterTargetMl

        val macroBehind = proteinActual < proteinTarget || carbsActual < carbsTarget || fatActual < fatTarget || fiberActual < fiberTarget

        val micronutrientGoals = micronutrientGoalPlans
            .filter { it.cadence == "daily" && it.goalType in micronutrientTypes }
            .associateBy { it.goalType }

        var iron = 0f
        var magnesium = 0f
        var potassium = 0f
        var vitaminD = 0f
        var omega3 = 0f

        meals.forEach { meal ->
            val food = foodsById[meal.foodId] ?: return@forEach
            val ratio = meal.grams / 100f
            iron += food.ironMg * ratio
            magnesium += food.magnesiumMg * ratio
            potassium += food.potassiumMg * ratio
            vitaminD += food.vitaminDUi * ratio
            omega3 += food.omega3Mg * ratio
        }

        val microBehind = micronutrientGoals.any { (type, goal) ->
            val actual = when (type) {
                "iron" -> iron
                "magnesium" -> magnesium
                "potassium" -> potassium
                "vitamin_d" -> vitaminD
                "omega3" -> omega3
                else -> Float.MAX_VALUE
            }
            actual < goal.target.toFloat()
        }

        return GoalDeficits(
            waterBehind = waterBehind,
            macroBehind = macroBehind,
            microBehind = microBehind
        )
    }
}
