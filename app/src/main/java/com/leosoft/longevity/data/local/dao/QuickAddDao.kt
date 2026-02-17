package com.leosoft.longevity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leosoft.longevity.data.local.entity.GoalPlanEntity
import com.leosoft.longevity.data.local.entity.ReminderLogEntity
import com.leosoft.longevity.data.local.entity.TaskLogEntity

@Dao
interface QuickAddDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskLogEntity)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalPlan(goal: GoalPlanEntity): Long

    @Query("SELECT * FROM goal_plans ORDER BY createdAt DESC")
    fun observeGoalPlans(): kotlinx.coroutines.flow.Flow<List<GoalPlanEntity>>

    @Query("UPDATE goal_plans SET goalType = :goalType, target = :target, cadence = :cadence WHERE id = :id")
    suspend fun updateGoalPlan(id: Long, goalType: String, target: Int, cadence: String)

    @Query("DELETE FROM goal_plans WHERE id = :id")
    suspend fun deleteGoalPlan(id: Long)

    @Query("DELETE FROM goal_plans")
    suspend fun clearGoalPlans()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderLogEntity): Long

    @Query("SELECT * FROM reminder_logs ORDER BY createdAt DESC")
    fun observeReminders(): kotlinx.coroutines.flow.Flow<List<ReminderLogEntity>>

    @Query("UPDATE reminder_logs SET reminderType = :type, reminderTime = :time, cadence = :cadence, intervalHours = :intervalHours WHERE id = :id")
    suspend fun updateReminder(id: Long, type: String, time: String, cadence: String, intervalHours: Int?)

    @Query("DELETE FROM reminder_logs WHERE id = :id")
    suspend fun deleteReminder(id: Long)
}
