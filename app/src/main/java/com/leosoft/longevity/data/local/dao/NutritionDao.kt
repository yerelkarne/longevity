package com.leosoft.longevity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {
    @Query("SELECT * FROM foods ORDER BY name")
    fun observeFoods(): Flow<List<FoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity): Long

    @Query("SELECT * FROM meal_entries WHERE date = :date")
    fun observeMealEntries(date: LocalDate): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries WHERE date = :date")
    suspend fun getMealEntries(date: LocalDate): List<MealEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealEntry(entry: MealEntryEntity)

    @Transaction
    suspend fun seedFoodsIfEmpty() {
        if (observeFoodsCount() == 0) {
            insertFood(FoodEntity(name = "Yulaf", kcalPer100g = 389, protein = 16.9f, carbs = 66.3f, fat = 6.9f, fiber = 10.6f, magnesiumMg = 177f))
            insertFood(FoodEntity(name = "Yumurta", kcalPer100g = 155, protein = 13f, carbs = 1.1f, fat = 11f, fiber = 0f, vitaminDUi = 82f))
            insertFood(FoodEntity(name = "Somon", kcalPer100g = 208, protein = 20f, carbs = 0f, fat = 13f, fiber = 0f, omega3Mg = 2000f, vitaminDUi = 526f))
            insertFood(FoodEntity(name = "Zeytin", kcalPer100g = 115, protein = 0.8f, carbs = 6.3f, fat = 10.7f, fiber = 3.2f, ironMg = 3.3f, vitaminDUi = 0f, potassiumMg = 42f))
        }
    }

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun observeFoodsCount(): Int
}
