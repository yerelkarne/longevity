package com.leosoft.longevity.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.leosoft.longevity.data.local.dao.ActivityDao
import com.leosoft.longevity.data.local.dao.GoalsDao
import com.leosoft.longevity.data.local.dao.LifeDao
import com.leosoft.longevity.data.local.dao.NutritionDao
import com.leosoft.longevity.data.local.dao.ScoresDao
import com.leosoft.longevity.data.local.dao.SupplementsDao
import com.leosoft.longevity.data.local.dao.QuickAddDao
import com.leosoft.longevity.data.local.dao.WaterDao
import com.leosoft.longevity.data.local.entity.DailyScoreEntity
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealEntryEntity
import com.leosoft.longevity.data.local.entity.MealNutritionRecordEntity
import com.leosoft.longevity.data.local.entity.GoalPlanEntity
import com.leosoft.longevity.data.local.entity.SleepLogEntity
import com.leosoft.longevity.data.local.entity.TaskLogEntity
import com.leosoft.longevity.data.local.entity.ReminderLogEntity
import com.leosoft.longevity.data.local.entity.StepsLogEntity
import com.leosoft.longevity.data.local.entity.SupplementEntity
import com.leosoft.longevity.data.local.entity.SupplementLogEntity
import com.leosoft.longevity.data.local.entity.UserGoalsEntity
import com.leosoft.longevity.data.local.entity.WaterLogEntity
import com.leosoft.longevity.data.local.entity.WorkoutLogEntity

@Database(
    entities = [
        FoodEntity::class,
        MealEntryEntity::class,
        MealNutritionRecordEntity::class,
        WaterLogEntity::class,
        SupplementEntity::class,
        SupplementLogEntity::class,
        SleepLogEntity::class,
        StepsLogEntity::class,
        WorkoutLogEntity::class,
        TaskLogEntity::class,
        ReminderLogEntity::class,
        GoalPlanEntity::class,
        UserGoalsEntity::class,
        DailyScoreEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LongevityDatabase : RoomDatabase() {
    abstract fun nutritionDao(): NutritionDao
    abstract fun waterDao(): WaterDao
    abstract fun supplementsDao(): SupplementsDao
    abstract fun lifeDao(): LifeDao
    abstract fun activityDao(): ActivityDao
    abstract fun goalsDao(): GoalsDao
    abstract fun scoresDao(): ScoresDao
    abstract fun quickAddDao(): QuickAddDao

    companion object {
        fun create(context: Context): LongevityDatabase = Room.databaseBuilder(
            context,
            LongevityDatabase::class.java,
            "longevity.db"
        ).addMigrations(MIGRATION_6_7).build()

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                listOf("water_logs", "sleep_logs", "steps_logs", "workout_logs", "meal_nutrition_records").forEach { table ->
                    database.execSQL("ALTER TABLE $table ADD COLUMN source TEXT NOT NULL DEFAULT 'LOCAL'")
                    database.execSQL("ALTER TABLE $table ADD COLUMN syncState TEXT NOT NULL DEFAULT 'NONE'")
                    database.execSQL("ALTER TABLE $table ADD COLUMN hcRecordId TEXT")
                    database.execSQL("ALTER TABLE $table ADD COLUMN lastSyncedAt TEXT")
                }
            }
        }
    }
}
