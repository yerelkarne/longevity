package com.leosoft.longevity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leosoft.longevity.data.local.entity.SyncState
import com.leosoft.longevity.data.local.entity.WaterLogEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WaterLogEntity)

    @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY time DESC")
    fun observeByDate(date: LocalDate): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs ORDER BY date DESC, time DESC")
    fun observeAll(): Flow<List<WaterLogEntity>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_logs WHERE date = :date")
    suspend fun getWaterTotal(date: LocalDate): Int

    @Query("SELECT * FROM water_logs WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun getPendingUploads(): List<WaterLogEntity>

    @Query("SELECT * FROM water_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getBetween(startDate: LocalDate, endDate: LocalDate): List<WaterLogEntity>

    @Query("UPDATE water_logs SET amountMl = :amountMl WHERE id = :id")
    suspend fun updateLogAmount(id: Long, amountMl: Int)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("UPDATE water_logs SET syncState = :state, hcRecordId = :hcRecordId, lastSyncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncState(id: Long, state: SyncState, hcRecordId: String?, syncedAt: java.time.LocalDateTime)
}
