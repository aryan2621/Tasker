package com.tasker.data.repository

import com.tasker.data.model.SyncStatus
import com.tasker.data.model.UserStreak
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface StreakRepository {
    suspend fun getStreakForUser(userId: String): UserStreak?
    fun getStreakForUserFlow(userId: String): Flow<UserStreak?>
    suspend fun insertOrUpdateStreak(userStreak: UserStreak): Long

    // Enhanced offline sync capabilities
    suspend fun syncUserStreak(streak: UserStreak): Boolean
    suspend fun fetchUserStreak(userId: String): UserStreak?
    suspend fun markStreakSynced(userId: String, serverUpdatedAt: Date?)
    suspend fun getSyncStatus(userId: String): SyncStatus?
    suspend fun getUnsyncedStreaks(): List<UserStreak>
}