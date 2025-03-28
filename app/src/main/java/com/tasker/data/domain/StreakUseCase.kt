package com.tasker.data.domain

import com.tasker.data.model.Achievement
import com.tasker.data.model.AchievementType
import com.tasker.data.model.UserStreak
import com.tasker.data.repository.AchievementRepository
import com.tasker.data.repository.AuthRepository
import com.tasker.data.repository.StreakRepository
import com.tasker.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class UpdateStreakUseCase(
    private val authRepository: AuthRepository,
    private val streakRepository: StreakRepository,
    private val achievementRepository: AchievementRepository,
    private val taskRepository: TaskRepository
) {
    suspend fun execute() {
        val userId = authRepository.getCurrentUserId() ?: return

        // Get current streak or create new one
        val currentStreak = streakRepository.getStreakForUser(userId) ?: UserStreak(userId = userId)
        val calendar = Calendar.getInstance()
        val today = calendar.time

        // Check last completed date
        val lastCompletedDate = currentStreak.lastCompletedDate
        val newStreak: UserStreak

        if (lastCompletedDate == null) {
            // First completion
            newStreak = currentStreak.copy(
                currentStreak = 1,
                longestStreak = 1,
                lastCompletedDate = today
            )
        } else {
            // Calculate days between
            val diffInMillis = today.time - lastCompletedDate.time
            val diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)

            // Check if streak continues or is broken
            newStreak = when {
                // Same day, no streak update
                diffInDays == 0L -> currentStreak

                // Next day, streak continues
                diffInDays == 1L -> currentStreak.copy(
                    currentStreak = currentStreak.currentStreak + 1,
                    longestStreak = maxOf(currentStreak.longestStreak, currentStreak.currentStreak + 1),
                    lastCompletedDate = today
                )

                // Streak broken
                else -> currentStreak.copy(
                    currentStreak = 1,
                    lastCompletedDate = today
                )
            }
        }

        // Save updated streak
        streakRepository.insertOrUpdateStreak(newStreak)

        // Sync with Firebase
        streakRepository.syncUserStreak(newStreak)

        // Check for achievements
        checkForAchievements(userId, newStreak)
    }

    private suspend fun checkForAchievements(userId: String, streak: UserStreak) {
        // Streak milestone achievements
        val streakMilestones = listOf(3, 7, 14, 30, 60, 90, 180, 365)
        for (milestone in streakMilestones) {
            if (streak.currentStreak == milestone) {
                // Check if this achievement already exists
                val existingAchievements = achievementRepository.getAchievementsByType(
                    userId,
                    AchievementType.STREAK_MILESTONE.name
                )

                val alreadyAwarded = existingAchievements.any {
                    it.description.contains("$milestone day")
                }

                if (!alreadyAwarded) {
                    // Award new achievement
                    val achievement = Achievement(
                        userId = userId,
                        type = AchievementType.STREAK_MILESTONE,
                        title = "$milestone Day Streak!",
                        description = "You've completed tasks for $milestone days in a row!"
                    )
                    achievementRepository.insertAchievement(achievement)
                }
            }
        }

        // Check for task count achievements
        val completedTasks = taskRepository.getCompletedTasks().first()
        val taskCountMilestones = listOf(10, 50, 100, 500, 1000)

        for (milestone in taskCountMilestones) {
            if (completedTasks.size == milestone) {
                // Check if this achievement already exists
                val existingAchievements = achievementRepository.getAchievementsByType(
                    userId,
                    AchievementType.TASK_COUNT.name
                )

                val alreadyAwarded = existingAchievements.any {
                    it.description.contains("$milestone tasks")
                }

                if (!alreadyAwarded) {
                    // Award new achievement
                    val achievement = Achievement(
                        userId = userId,
                        type = AchievementType.TASK_COUNT,
                        title = "Task Master: $milestone",
                        description = "You've completed $milestone tasks!"
                    )
                    achievementRepository.insertAchievement(achievement)
                }
            }
        }

        // Check for category master achievements
        val categoryCounts = completedTasks
            .groupBy { it.category }
            .mapValues { it.value.size }

        for ((category, count) in categoryCounts) {
            if (count == 20) {
                // Check if this achievement already exists
                val existingAchievements = achievementRepository.getAchievementsByType(
                    userId,
                    AchievementType.CATEGORY_MASTER.name
                )

                val categoryName = category.name.lowercase().replaceFirstChar { it.uppercase() }
                val alreadyAwarded = existingAchievements.any {
                    it.description.contains(categoryName)
                }

                if (!alreadyAwarded) {
                    // Award new achievement
                    val achievement = Achievement(
                        userId = userId,
                        type = AchievementType.CATEGORY_MASTER,
                        title = "$categoryName Expert",
                        description = "You've completed 20 $categoryName tasks!"
                    )
                    achievementRepository.insertAchievement(achievement)
                }
            }
        }
    }
}