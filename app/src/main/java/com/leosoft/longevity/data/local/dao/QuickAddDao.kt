package com.leosoft.longevity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leosoft.longevity.data.local.entity.ReminderLogEntity
import com.leosoft.longevity.data.local.entity.TaskLogEntity

@Dao
interface QuickAddDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderLogEntity)

    @Query("SELECT * FROM reminder_logs ORDER BY createdAt DESC")
    fun observeReminders(): kotlinx.coroutines.flow.Flow<List<ReminderLogEntity>>
}
