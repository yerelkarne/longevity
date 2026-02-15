package com.leosoft.longevity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leosoft.longevity.data.local.entity.StepsLogEntity
import com.leosoft.longevity.data.local.entity.WorkoutLogEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSteps(log: StepsLogEntity)

    @Query("SELECT * FROM steps_logs WHERE date = :date LIMIT 1")
    fun observeSteps(date: LocalDate): Flow<StepsLogEntity?>

    @Query("SELECT * FROM steps_logs WHERE date = :date LIMIT 1")
    suspend fun getSteps(date: LocalDate): StepsLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(log: WorkoutLogEntity)

    @Query("SELECT * FROM workout_logs WHERE date = :date")
    fun observeWorkouts(date: LocalDate): Flow<List<WorkoutLogEntity>>

    @Query("SELECT * FROM workout_logs WHERE date = :date")
    suspend fun getWorkouts(date: LocalDate): List<WorkoutLogEntity>
}
