package com.leosoft.longevity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leosoft.longevity.data.local.entity.SyncState
import com.leosoft.longevity.data.local.entity.MenstrualCycleLogEntity
import com.leosoft.longevity.data.local.entity.SleepLogEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenstrualCycleLog(log: MenstrualCycleLogEntity)

    @Query("SELECT * FROM menstrual_cycle_logs ORDER BY periodStartDate DESC")
    fun observeMenstrualCycleLogs(): Flow<List<MenstrualCycleLogEntity>>

    @Query("SELECT * FROM menstrual_cycle_logs WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun getPendingMenstrualUploads(): List<MenstrualCycleLogEntity>

    @Query("UPDATE menstrual_cycle_logs SET syncState = :state, hcRecordId = :hcRecordId, lastSyncedAt = :syncedAt WHERE id = :id")
    suspend fun updateMenstrualSyncState(id: Long, state: SyncState, hcRecordId: String?, syncedAt: java.time.LocalDateTime)

    @Query("DELETE FROM menstrual_cycle_logs")
    suspend fun clearMenstrualCycleLogs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(log: SleepLogEntity)

    @Query("SELECT * FROM sleep_logs WHERE date = :date ORDER BY wakeTime DESC LIMIT 1")
    fun observeSleep(date: LocalDate): Flow<SleepLogEntity?>

    @Query("SELECT * FROM sleep_logs ORDER BY wakeTime DESC")
    fun observeSleepLogs(): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs WHERE date = :date ORDER BY wakeTime DESC LIMIT 1")
    suspend fun getSleep(date: LocalDate): SleepLogEntity?

    @Query("SELECT * FROM sleep_logs WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun getPendingUploads(): List<SleepLogEntity>

    @Query("SELECT * FROM sleep_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getBetween(startDate: LocalDate, endDate: LocalDate): List<SleepLogEntity>

    @Query("UPDATE sleep_logs SET syncState = :state, hcRecordId = :hcRecordId, lastSyncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncState(id: Long, state: SyncState, hcRecordId: String?, syncedAt: java.time.LocalDateTime)
}
