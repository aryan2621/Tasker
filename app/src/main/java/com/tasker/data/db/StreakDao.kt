package com.tasker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tasker.data.model.Achievement
import com.tasker.data.model.SyncStatus
import com.tasker.data.model.UserStreak
import kotlinx.coroutines.flow.Flow
import java.util.Date

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

    // Enhanced offline sync support
    @Query("UPDATE user_streaks SET syncStatus = :status, lastSyncAttempt = :lastSyncAttempt, serverUpdatedAt = :serverTime WHERE userId = :userId")
    suspend fun markStreakSynced(userId: String, status: SyncStatus = SyncStatus.SYNCED, lastSyncAttempt: Date = Date(), serverTime: Date?)

    @Query("UPDATE user_streaks SET syncErrorMessage = :errorMessage WHERE userId = :userId")
    suspend fun updateStreakSyncErrorMessage(userId: String, errorMessage: String?)

    @Query("SELECT * FROM user_streaks WHERE syncStatus = :syncStatus")
    suspend fun getStreaksByStatus(syncStatus: SyncStatus): List<UserStreak>
}
@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement): Long

    @Query("SELECT * FROM achievements WHERE id = :achievementId")
    suspend fun getAchievementById(achievementId: Long): Achievement?

    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY earnedAt DESC")
    fun getAchievementsForUser(userId: String): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND type = :type")
    suspend fun getAchievementsByType(userId: String, type: String): List<Achievement>

    @Query("SELECT COUNT(*) FROM achievements WHERE userId = :userId")
    fun getAchievementCount(userId: String): Flow<Int>

    @Query("UPDATE achievements SET syncStatus = :status, lastSyncAttempt = :lastSyncAttempt, serverUpdatedAt = :serverTime WHERE id = :achievementId")
    suspend fun markAchievementSynced(achievementId: Long, status: SyncStatus = SyncStatus.SYNCED, lastSyncAttempt: Date = Date(), serverTime: Date?)

    @Query("SELECT * FROM achievements WHERE syncStatus = :syncStatus")
    suspend fun getAchievementsByStatus(syncStatus: SyncStatus): List<Achievement>
}