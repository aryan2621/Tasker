package com.tasker.data.repository

import com.tasker.data.model.UserStreak
import kotlinx.coroutines.flow.Flow

interface StreakRepository {
    suspend fun getStreakForUser(userId: String): UserStreak?
    fun getStreakForUserFlow(userId: String): Flow<UserStreak?>
    suspend fun insertOrUpdateStreak(userStreak: UserStreak): Long
    suspend fun syncUserStreak(streak: UserStreak): Boolean
    suspend fun fetchUserStreak(userId: String): UserStreak?
}