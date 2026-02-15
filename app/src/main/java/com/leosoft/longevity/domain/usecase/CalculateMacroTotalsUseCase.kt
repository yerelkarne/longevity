package com.leosoft.longevity.domain.usecase

import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.domain.model.MacroTotals
import kotlin.math.roundToInt

class CalculateMacroTotalsUseCase {
    operator fun invoke(entries: List<MealEntryEntity>, foodsById: Map<Long, FoodEntity>): MacroTotals {
        var calories = 0f
        var protein = 0f
        var carbs = 0f
        var fat = 0f
        var fiber = 0f

        entries.forEach { entry ->
            val food = foodsById[entry.foodId] ?: return@forEach
            val ratio = entry.grams / 100f
            calories += food.kcalPer100g * ratio
            protein += food.protein * ratio
            carbs += food.carbs * ratio
            fat += food.fat * ratio
            fiber += food.fiber * ratio
        }

        return MacroTotals(calories.roundToInt(), protein, carbs, fat, fiber)
    }
}
