package com.leosoft.longevity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.MealNutritionRecordEntity
import com.leosoft.longevity.data.local.entity.SyncState
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {
    @Query("SELECT * FROM foods ORDER BY name")
    fun observeFoods(): Flow<List<FoodEntity>>

    @Query("""
        SELECT * FROM foods
        WHERE (protein > 0 OR carbs > 0 OR fat > 0 OR fiber > 0)
          AND (ironMg > 0 OR magnesiumMg > 0 OR potassiumMg > 0 OR vitaminDUi > 0 OR omega3Mg > 0)
        ORDER BY name
    """)
    fun observeFoodsWithNutrition(): Flow<List<FoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity): Long

    @Query("SELECT * FROM meal_entries WHERE date = :date")
    fun observeMealEntries(date: LocalDate): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries ORDER BY date DESC, time DESC")
    fun observeAllMealEntries(): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries WHERE date = :date")
    suspend fun getMealEntries(date: LocalDate): List<MealEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealEntry(entry: MealEntryEntity): Long

    @Query("UPDATE meal_entries SET foodId = :foodId, grams = :grams WHERE id = :id")
    suspend fun updateMealEntry(id: Long, foodId: Long, grams: Int)

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun deleteMealEntry(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealNutritionRecord(record: MealNutritionRecordEntity)

    @Query("DELETE FROM meal_nutrition_records WHERE mealEntryId = :mealEntryId")
    suspend fun deleteMealNutritionRecordByMealEntryId(mealEntryId: Long)

    @Query("SELECT * FROM meal_nutrition_records WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun getPendingNutritionUploads(): List<MealNutritionRecordEntity>

    @Query("SELECT * FROM meal_nutrition_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getNutritionBetween(startDate: LocalDate, endDate: LocalDate): List<MealNutritionRecordEntity>

    @Query("UPDATE meal_nutrition_records SET syncState = :state, hcRecordId = :hcRecordId, lastSyncedAt = :syncedAt WHERE id = :id")
    suspend fun updateNutritionSyncState(id: Long, state: SyncState, hcRecordId: String?, syncedAt: java.time.LocalDateTime)

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

    @Query("SELECT * FROM foods WHERE name = :name LIMIT 1")
    suspend fun getFoodByName(name: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE id = :id LIMIT 1")
    suspend fun getFoodById(id: Long): FoodEntity?

    @Query("""
        UPDATE foods
        SET kcalPer100g = :kcalPer100g,
            protein = :protein,
            carbs = :carbs,
            fat = :fat,
            fiber = :fiber,
            ironMg = :ironMg,
            magnesiumMg = :magnesiumMg,
            potassiumMg = :potassiumMg,
            vitaminDUi = :vitaminDUi,
            omega3Mg = :omega3Mg
        WHERE id = :id
    """)
    suspend fun updateFoodNutritionById(
        id: Long,
        kcalPer100g: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        fiber: Float,
        ironMg: Float,
        magnesiumMg: Float,
        potassiumMg: Float,
        vitaminDUi: Float,
        omega3Mg: Float
    )
}
