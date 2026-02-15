package com.leosoft.longevity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leosoft.longevity.data.local.entity.DailyScoreEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoresDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(score: DailyScoreEntity)

    @Query("SELECT * FROM daily_scores WHERE date = :date")
    fun observeByDate(date: LocalDate): Flow<DailyScoreEntity?>
}
