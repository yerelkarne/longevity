package com.leosoft.longevity

import android.app.Application
import com.leosoft.longevity.data.local.AppPreferences
import com.leosoft.longevity.data.local.LongevityDatabase
import com.leosoft.longevity.data.repository.LongevityRepositoryImpl
import com.leosoft.longevity.domain.repository.LongevityRepository
import com.leosoft.longevity.domain.usecase.CalculateDailyScoreUseCase
import com.leosoft.longevity.domain.usecase.CalculateMacroTotalsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LongevityApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: LongevityRepository
    lateinit var preferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        val db = LongevityDatabase.create(this)
        val macroUseCase = CalculateMacroTotalsUseCase()
        repository = LongevityRepositoryImpl(
            nutritionDao = db.nutritionDao(),
            waterDao = db.waterDao(),
            supplementsDao = db.supplementsDao(),
            lifeDao = db.lifeDao(),
            activityDao = db.activityDao(),
            goalsDao = db.goalsDao(),
            scoresDao = db.scoresDao(),
            calculateDailyScore = CalculateDailyScoreUseCase(macroUseCase),
            calculateMacroTotals = macroUseCase
        )
        preferences = AppPreferences(this)
        appScope.launch { db.nutritionDao().seedFoodsIfEmpty() }
    }
}
