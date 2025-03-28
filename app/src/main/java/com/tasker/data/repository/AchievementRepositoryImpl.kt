package com.tasker.data.repository

import com.tasker.data.db.AchievementDao
import com.tasker.data.model.Achievement
import kotlinx.coroutines.flow.Flow

class AchievementRepositoryImpl(
    private val achievementDao: AchievementDao,
    private val authRepository: AuthRepository,
    private val firebaseRepository: FirebaseRepository
) : AchievementRepository {

    override suspend fun getAchievementsForUser(userId: String): Flow<List<Achievement>> {
        return achievementDao.getAchievementsForUser(userId)
    }

    override suspend fun insertAchievement(achievement: Achievement): Long {
        return achievementDao.insertAchievement(achievement)
    }

    override suspend fun getAchievementsByType(userId: String, type: String): List<Achievement> {
        return achievementDao.getAchievementsByType(userId, type)
    }

    override suspend fun getAchievementCount(userId: String): Flow<Int> {
        return achievementDao.getAchievementCount(userId)
    }

    override suspend fun markAchievementAsSynced(achievementId: Long) {
        achievementDao.markAchievementAsSynced(achievementId)
    }

    override suspend fun getUnsyncedAchievements(): List<Achievement> {
        return achievementDao.getUnsyncedAchievements()
    }

    override suspend fun syncAchievements(): Boolean {
        try {
            val userId = authRepository.getCurrentUserId() ?: return false

            val unsyncedAchievements = getUnsyncedAchievements()
            if (unsyncedAchievements.isNotEmpty()) {
                firebaseRepository.syncAchievements(unsyncedAchievements)

                unsyncedAchievements.forEach { achievement ->
                    markAchievementAsSynced(achievement.id)
                }
            }

            val remoteAchievements = firebaseRepository.fetchUserAchievements()
            remoteAchievements.forEach { achievement ->
                insertAchievement(achievement)
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }
}