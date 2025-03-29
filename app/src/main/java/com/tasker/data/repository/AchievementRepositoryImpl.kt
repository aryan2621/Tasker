package com.tasker.data.repository

import com.tasker.data.db.AchievementDao
import com.tasker.data.model.Achievement
import com.tasker.data.model.SyncStatus
import com.tasker.util.NetworkConnectivityObserver
import kotlinx.coroutines.flow.Flow
import java.util.Date

class AchievementRepositoryImpl(
    private val achievementDao: AchievementDao,
    private val authRepository: AuthRepository,
    private val firebaseRepository: FirebaseRepository,
    private val networkConnectivityObserver: NetworkConnectivityObserver
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
        achievementDao.markAchievementSynced(
            achievementId = achievementId,
            status = SyncStatus.SYNCED,
            lastSyncAttempt = Date(),
            serverTime = Date()
        )
    }

    override suspend fun getUnsyncedAchievements(): List<Achievement> {
        return achievementDao.getAchievementsByStatus(SyncStatus.PENDING_UPLOAD)
    }

    override suspend fun syncAchievements(): Boolean {
        try {
            val userId = authRepository.getCurrentUserId() ?: return false

            // Check network connectivity first (assuming you have the observer injected)
            if (!networkConnectivityObserver.isConnected()) {
                return false
            }

            // Handle pending uploads
            val unsyncedAchievements = getUnsyncedAchievements()
            if (unsyncedAchievements.isNotEmpty()) {
                val syncedAchievements = firebaseRepository.syncAchievements(unsyncedAchievements)

                syncedAchievements.forEach { achievement ->
                    achievementDao.markAchievementSynced(
                        achievementId = achievement.id,
                        status = SyncStatus.SYNCED,
                        lastSyncAttempt = Date(),
                        serverTime = achievement.earnedAt
                    )
                }
            }

            // Fetch and merge remote achievements
            val remoteAchievements = firebaseRepository.fetchUserAchievements()
            for (remoteAchievement in remoteAchievements) {
                val localAchievement = achievementDao.getAchievementById(remoteAchievement.id)

                if (localAchievement == null) {
                    // New remote achievement, insert locally
                    val localCopy = remoteAchievement.copy(
                        syncStatus = SyncStatus.SYNCED,
                        serverUpdatedAt = remoteAchievement.earnedAt,
                        lastSyncAttempt = Date()
                    )
                    achievementDao.insertAchievement(localCopy)
                } else if (localAchievement.syncStatus == SyncStatus.SYNCED) {
                    // If local is synced, use the most recent
                    if (localAchievement.serverUpdatedAt?.before(remoteAchievement.earnedAt) == true) {
                        achievementDao.insertAchievement(remoteAchievement.copy(
                            syncStatus = SyncStatus.SYNCED,
                            serverUpdatedAt = remoteAchievement.earnedAt,
                            lastSyncAttempt = Date()
                        ))
                    }
                }
                // No else needed - local changes that are pending should be preserved
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }
}