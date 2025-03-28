package com.tasker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tasker.data.model.Achievement
import com.tasker.data.model.UserStreak
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(userStreak: UserStreak): Long

    @Update
    suspend fun updateStreak(userStreak: UserStreak)

    @Query("SELECT * FROM user_streaks WHERE userId = :userId")
    suspend fun getStreakForUser(userId: String): UserStreak?

    @Query("SELECT * FROM user_streaks WHERE userId = :userId")
    fun getStreakForUserFlow(userId: String): Flow<UserStreak?>
}

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement): Long

    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY earnedAt DESC")
    fun getAchievementsForUser(userId: String): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND type = :type")
    suspend fun getAchievementsByType(userId: String, type: String): List<Achievement>

    @Query("SELECT COUNT(*) FROM achievements WHERE userId = :userId")
    fun getAchievementCount(userId: String): Flow<Int>

    @Query("UPDATE achievements SET isSynced = 1 WHERE id = :achievementId")
    suspend fun markAchievementAsSynced(achievementId: Long)

    @Query("SELECT * FROM achievements WHERE isSynced = 0")
    suspend fun getUnsyncedAchievements(): List<Achievement>
}