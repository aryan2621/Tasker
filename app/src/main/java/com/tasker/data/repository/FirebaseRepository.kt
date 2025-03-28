package com.tasker.data.repository

import com.tasker.data.model.Achievement
import com.tasker.data.model.Task
import com.tasker.data.model.TaskProgress
import com.tasker.data.model.UserStreak


interface FirebaseRepository {
    suspend fun syncTasks(tasks: List<Task>): List<Task>
    suspend fun syncTaskProgress(progressList: List<TaskProgress>): List<TaskProgress>
    suspend fun fetchUserTasks(): List<Task>
    suspend fun fetchUserTaskProgress(): List<TaskProgress>
    suspend fun isUserSignedIn(): Boolean
    suspend fun getCurrentUserId(): String?
    suspend fun deleteTask(taskId: Long): Boolean

    suspend fun syncAchievements(achievements: List<Achievement>): List<Achievement>
    suspend fun fetchUserAchievements(): List<Achievement>
    suspend fun syncUserStreak(streak: UserStreak): Boolean
    suspend fun fetchUserStreak(userId: String): UserStreak?



}