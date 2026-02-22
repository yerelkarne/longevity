package com.leosoft.longevity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leosoft.longevity.data.local.entity.SyncState
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

    @Query("SELECT * FROM steps_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun observeStepsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<StepsLogEntity>>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM steps_logs WHERE date BETWEEN :startDate AND :endDate")
    fun observeStepsTotalBetween(startDate: LocalDate, endDate: LocalDate): Flow<Int>

    @Query("SELECT * FROM steps_logs WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun getPendingStepUploads(): List<StepsLogEntity>

    @Query("UPDATE steps_logs SET syncState = :state, hcRecordId = :hcRecordId, lastSyncedAt = :syncedAt WHERE date = :date")
    suspend fun updateStepSyncState(date: LocalDate, state: SyncState, hcRecordId: String?, syncedAt: java.time.LocalDateTime)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(log: WorkoutLogEntity)

    @Query("SELECT * FROM workout_logs WHERE date = :date")
    fun observeWorkouts(date: LocalDate): Flow<List<WorkoutLogEntity>>

    @Query("SELECT * FROM workout_logs ORDER BY date DESC, time DESC")
    fun observeAllWorkouts(): Flow<List<WorkoutLogEntity>>

    @Query("SELECT * FROM workout_logs WHERE date = :date")
    suspend fun getWorkouts(date: LocalDate): List<WorkoutLogEntity>

    @Query("SELECT * FROM workout_logs WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun getPendingWorkoutUploads(): List<WorkoutLogEntity>

    @Query("SELECT * FROM workout_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getWorkoutsBetween(startDate: LocalDate, endDate: LocalDate): List<WorkoutLogEntity>

    @Query("UPDATE workout_logs SET syncState = :state, hcRecordId = :hcRecordId, lastSyncedAt = :syncedAt WHERE id = :id")
    suspend fun updateWorkoutSyncState(id: Long, state: SyncState, hcRecordId: String?, syncedAt: java.time.LocalDateTime)
}
