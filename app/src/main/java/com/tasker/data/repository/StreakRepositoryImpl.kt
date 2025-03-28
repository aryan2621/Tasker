package com.tasker.data.repository

import com.tasker.data.db.StreakDao
import com.tasker.data.model.UserStreak
import kotlinx.coroutines.flow.Flow

class StreakRepositoryImpl(
    private val streakDao: StreakDao,
    private val firebaseRepository: FirebaseRepository
) : StreakRepository {

    override suspend fun getStreakForUser(userId: String): UserStreak? {
        return streakDao.getStreakForUser(userId)
    }

    override fun getStreakForUserFlow(userId: String): Flow<UserStreak?> {
        return streakDao.getStreakForUserFlow(userId)
    }

    override suspend fun insertOrUpdateStreak(userStreak: UserStreak): Long {
        return streakDao.insertStreak(userStreak)
    }

    override suspend fun syncUserStreak(streak: UserStreak): Boolean {
        return firebaseRepository.syncUserStreak(streak)
    }

    override suspend fun fetchUserStreak(userId: String): UserStreak? {
        return firebaseRepository.fetchUserStreak(userId)
    }
}