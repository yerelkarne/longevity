package com.leosoft.longevity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leosoft.longevity.data.local.entity.UserGoalsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalsDao {
    @Query("SELECT * FROM user_goals WHERE id = 1")
    fun observeGoals(): Flow<UserGoalsEntity?>

    @Query("SELECT * FROM user_goals WHERE id = 1")
    suspend fun getGoals(): UserGoalsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: UserGoalsEntity)
}
