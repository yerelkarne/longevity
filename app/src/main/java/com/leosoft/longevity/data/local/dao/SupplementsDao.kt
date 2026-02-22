package com.leosoft.longevity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leosoft.longevity.data.local.entity.SupplementEntity
import com.leosoft.longevity.data.local.entity.SupplementLogEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementsDao {
    @Query("SELECT * FROM supplements ORDER BY name")
    fun observeSupplements(): Flow<List<SupplementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplement(supplement: SupplementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SupplementLogEntity)

    @Query("SELECT * FROM supplement_logs WHERE date = :date ORDER BY time DESC")
    fun observeLogs(date: LocalDate): Flow<List<SupplementLogEntity>>

    @Query("SELECT * FROM supplement_logs ORDER BY date DESC, time DESC")
    fun observeAllLogs(): Flow<List<SupplementLogEntity>>

    @Query("SELECT COUNT(*) FROM supplement_logs WHERE date = :date AND taken = 1")
    suspend fun takenCount(date: LocalDate): Int
}
