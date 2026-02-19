package com.leosoft.longevity.domain.usecase

import com.leosoft.longevity.data.local.entity.DailyScoreEntity
import com.leosoft.longevity.domain.model.DayData
import com.leosoft.longevity.domain.model.ScoreBreakdown
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

class CalculateDailyScoreUseCase(
    private val calculateMacroTotalsUseCase: CalculateMacroTotalsUseCase
) {
    private fun clamp(value: Float): Float = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f

    private fun safeRatio(actual: Float, target: Float): Float =
        if (!actual.isFinite() || !target.isFinite() || target <= 0f) 0f else actual / target

    fun calculateBreakdown(dayData: DayData): ScoreBreakdown {
        val totals = calculateMacroTotalsUseCase(dayData.meals, dayData.foodsById)
        val goals = dayData.goals

        fun closeness(actual: Float, target: Float): Float {
            val ratio = safeRatio(actual, target)
            return clamp(1f - abs(1f - ratio))
        }

        val proteinC = closeness(totals.protein, goals.proteinTarget)
        val carbsC = closeness(totals.carbs, goals.carbsTarget)
        val fatC = closeness(totals.fat, goals.fatTarget)
        val fiberC = closeness(totals.fiber, goals.fiberTarget)
        val nutritionNormalized = ((proteinC * 0.2f) + (carbsC * 0.2f) + (fatC * 0.2f) + (fiberC * 0.4f))
        val nutrition = nutritionNormalized * 35f

        val hydration = clamp(safeRatio(dayData.waterMl.toFloat(), goals.waterTargetMl.toFloat())) * 15f

        val stepsScore = clamp(safeRatio(dayData.steps.toFloat(), goals.stepsTarget.toFloat())) * 18f
        val workoutBonus = dayData.workouts.sumOf { (it.durationMinutes * it.intensity).toDouble() }
            .toFloat().coerceAtMost(210f) / 210f * 7f
        val activity = stepsScore + workoutBonus

        val sleepRatio = safeRatio(dayData.sleep?.durationMinutes?.toFloat() ?: 0f, goals.sleepTargetMinutes.toFloat())
        val sleep = clamp(1f - abs(1f - sleepRatio)) * 20f

        val supplements = clamp(safeRatio(dayData.supplementsTaken.toFloat(), goals.supplementsPerDayTarget.toFloat())) * 5f

        val map = linkedMapOf(
            "Beslenme" to nutrition,
            "Su" to hydration,
            "Aktivite" to activity,
            "Uyku" to sleep,
            "Takviye" to supplements
        )

        return ScoreBreakdown(
            nutrition = nutrition,
            hydration = hydration,
            activity = activity,
            sleep = sleep,
            supplements = supplements,
            total = (nutrition + hydration + activity + sleep + supplements).let { if (it.isFinite()) it else 0f }.coerceIn(0f, 100f),
            bestArea = map.maxBy { it.value }.key,
            weakestArea = map.minBy { it.value }.key
        )
    }

    operator fun invoke(dayData: DayData, date: java.time.LocalDate): DailyScoreEntity {
        val b = calculateBreakdown(dayData)
        return DailyScoreEntity(
            date = date,
            nutritionScore = b.nutrition,
            hydrationScore = b.hydration,
            activityScore = b.activity,
            sleepScore = b.sleep,
            supplementsScore = b.supplements,
            totalScore = (b.total * 10).roundToInt() / 10f,
            updatedAt = LocalDateTime.now()
        )
    }
}
