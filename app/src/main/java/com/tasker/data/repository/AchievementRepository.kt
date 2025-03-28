package com.tasker.data.repository

import com.tasker.data.model.Achievement
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    suspend fun getAchievementsForUser(userId: String): Flow<List<Achievement>>
    suspend fun insertAchievement(achievement: Achievement): Long
    suspend fun getAchievementsByType(userId: String, type: String): List<Achievement>
    suspend fun getAchievementCount(userId: String): Flow<Int>
    suspend fun markAchievementAsSynced(achievementId: Long)
    suspend fun getUnsyncedAchievements(): List<Achievement>
    suspend fun syncAchievements(): Boolean
}