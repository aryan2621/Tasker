package com.tasker.data.domain

import com.tasker.data.model.Achievement
import com.tasker.data.repository.AchievementRepository
import com.tasker.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class GetAchievementsUseCase(
    private val achievementRepository: AchievementRepository,
    private val authRepository: AuthRepository
) {
    suspend fun execute(): Flow<List<Achievement>> {
        val userId = authRepository.getCurrentUserId() ?: return emptyFlow()

        // Sync achievements with remote database
        achievementRepository.syncAchievements()

        return achievementRepository.getAchievementsForUser(userId)
    }
}