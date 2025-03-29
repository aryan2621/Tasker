package com.tasker.data.repository

import com.tasker.data.db.StreakDao
import com.tasker.data.model.SyncStatus
import com.tasker.data.model.UserStreak
import com.tasker.util.NetworkConnectivityObserver
import kotlinx.coroutines.flow.Flow
import java.util.Date

class StreakRepositoryImpl(
    private val streakDao: StreakDao,
    private val firebaseRepository: FirebaseRepository,
    private val networkConnectivityObserver: NetworkConnectivityObserver
) : StreakRepository {

    override suspend fun getStreakForUser(userId: String): UserStreak? {
        return streakDao.getStreakForUser(userId)
    }

    override fun getStreakForUserFlow(userId: String): Flow<UserStreak?> {
        return streakDao.getStreakForUserFlow(userId)
    }

    override suspend fun insertOrUpdateStreak(userStreak: UserStreak): Long {
        // Always mark local changes as pending upload
        val updatedStreak = userStreak.copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            localModifiedAt = Date()
        )

        val id = streakDao.insertStreak(updatedStreak)

        // Attempt immediate sync if connected
        if (networkConnectivityObserver.isConnected()) {
            syncUserStreak(updatedStreak)
        }

        return id
    }

    override suspend fun syncUserStreak(streak: UserStreak): Boolean {
        if (!networkConnectivityObserver.isConnected()) {
            return false
        }

        return try {
            // Upload the streak to Firebase
            val success = firebaseRepository.syncUserStreak(streak)

            if (success) {
                // Mark as synced in local database
                streakDao.markStreakSynced(
                    userId = streak.userId,
                    status = SyncStatus.SYNCED,
                    lastSyncAttempt = Date(),
                    serverTime = Date()
                )
            }

            success
        } catch (e: Exception) {
            streakDao.updateStreakSyncErrorMessage(streak.userId, e.message ?: "Unknown error")
            false
        }
    }

    override suspend fun fetchUserStreak(userId: String): UserStreak? {
        if (!networkConnectivityObserver.isConnected()) {
            return null
        }

        val remoteStreak = firebaseRepository.fetchUserStreak(userId) ?: return null

        // Get the local version to compare
        val localStreak = streakDao.getStreakForUser(userId)

        if (localStreak == null) {
            // No local streak, insert the remote one
            val streakWithSync = remoteStreak.copy(
                syncStatus = SyncStatus.SYNCED,
                serverUpdatedAt = remoteStreak.lastCompletedDate ?: Date(),
                lastSyncAttempt = Date()
            )
            streakDao.insertStreak(streakWithSync)
            return streakWithSync
        } else if (localStreak.syncStatus == SyncStatus.SYNCED) {
            // Compare update times to decide which to keep
            val localLastUpdated = localStreak.lastCompletedDate ?: Date(0)
            val remoteLastUpdated = remoteStreak.lastCompletedDate ?: Date(0)

            if (remoteLastUpdated.after(localLastUpdated)) {
                // Remote is newer, update local
                val updatedStreak = remoteStreak.copy(
                    syncStatus = SyncStatus.SYNCED,
                    serverUpdatedAt = remoteLastUpdated,
                    lastSyncAttempt = Date()
                )
                streakDao.insertStreak(updatedStreak)
                return updatedStreak
            }
        }
        // Else, local has pending changes, keep local version

        return localStreak
    }

    override suspend fun markStreakSynced(userId: String, serverUpdatedAt: Date?) {
        streakDao.markStreakSynced(
            userId = userId,
            status = SyncStatus.SYNCED,
            lastSyncAttempt = Date(),
            serverTime = serverUpdatedAt
        )
    }

    override suspend fun getSyncStatus(userId: String): SyncStatus? {
        val streak = streakDao.getStreakForUser(userId) ?: return null
        return streak.syncStatus
    }

    override suspend fun getUnsyncedStreaks(): List<UserStreak> {
        return streakDao.getStreaksByStatus(SyncStatus.PENDING_UPLOAD)
    }
}