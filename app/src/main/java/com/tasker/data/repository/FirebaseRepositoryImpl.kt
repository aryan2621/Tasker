package com.tasker.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tasker.data.model.Achievement
import com.tasker.data.model.AchievementType
import com.tasker.data.model.Task
import com.tasker.data.model.TaskProgress
import com.tasker.data.model.UserStreak
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseRepositoryImpl : FirebaseRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val tag = "FirebaseRepositoryImpl"

    private val tasksCollection = "tasks"
    private val progressCollection = "task_progress"
    private val achievementsCollection = "achievements"
    private val streaksCollection = "user_streaks"

    override suspend fun syncTasks(tasks: List<Task>): List<Task> {
        val userId = getCurrentUserId() ?: return emptyList()
        val syncedTaskIds = mutableListOf<Long>()

        // Upload tasks to Firestore
        tasks.forEach { task ->
            try {
                val taskMap = mapTaskToMap(task, userId)

                firestore.collection(tasksCollection)
                    .document("${userId}_${task.id}")
                    .set(taskMap)
                    .await()

                syncedTaskIds.add(task.id)
            } catch (e: Exception) {
                // Handle exception
            }
        }

        return tasks.filter { it.id in syncedTaskIds }
    }

    override suspend fun syncTaskProgress(progressList: List<TaskProgress>): List<TaskProgress> {
        val userId = getCurrentUserId() ?: return emptyList()
        val syncedProgressIds = mutableListOf<Long>()

        // Upload progress to Firestore
        progressList.forEach { progress ->
            try {
                val progressMap = mapProgressToMap(progress, userId)

                firestore.collection(progressCollection)
                    .document("${userId}_${progress.id}")
                    .set(progressMap)
                    .await()

                syncedProgressIds.add(progress.id)
            } catch (e: Exception) {
                // Handle exception
            }
        }

        return progressList.filter { it.id in syncedProgressIds }
    }

    override suspend fun fetchUserTasks(): List<Task> {
        val userId = getCurrentUserId() ?: return emptyList()
        val result = mutableListOf<Task>()

        try {
            val snapshot = firestore.collection(tasksCollection)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (document in snapshot.documents) {
                val task = mapDocumentToTask(document)
                if (task != null) {
                    result.add(task)
                }
            }
        } catch (e: Exception) {
            // Handle exception
        }

        return result
    }

    override suspend fun fetchUserTaskProgress(): List<TaskProgress> {
        val userId = getCurrentUserId() ?: return emptyList()
        val result = mutableListOf<TaskProgress>()

        try {
            val snapshot = firestore.collection(progressCollection)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (document in snapshot.documents) {
                val progress = mapDocumentToProgress(document)
                if (progress != null) {
                    result.add(progress)
                }
            }
        } catch (e: Exception) {
            // Handle exception
        }

        return result
    }

    override suspend fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    override suspend fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun mapTaskToMap(task: Task, userId: String): Map<String, Any> {
        return mapOf(
            "id" to task.id,
            "userId" to userId,
            "title" to task.title,
            "description" to task.description,
            "category" to task.category.name,
            "priority" to task.priority.name,
            "recurrence" to task.recurrence.name,
            "reminderTime" to task.reminderTime.time,
            "durationMinutes" to task.durationMinutes,
            "isCompleted" to task.isCompleted,
            "isAccepted" to task.isAccepted,
            "isRejected" to task.isRejected,
            "createdAt" to task.createdAt.time,
            "updatedAt" to task.updatedAt.time,
            "completedAt" to (task.completedAt?.time ?: 0)
        )
    }

    private fun mapDocumentToTask(document: com.google.firebase.firestore.DocumentSnapshot): Task? {
        return try {
            val data = document.data ?: return null
            Task(
                id = (data["id"] as Long),
                title = data["title"] as String,
                userId = data["userId"] as String,
                description = data["description"] as String,
                category = enumValueOf(data["category"] as String),
                priority = enumValueOf(data["priority"] as String),
                recurrence = enumValueOf(data["recurrence"] as String),
                reminderTime = Date(data["reminderTime"] as Long),
                durationMinutes = (data["durationMinutes"] as Long).toInt(),
                isCompleted = data["isCompleted"] as Boolean,
                isAccepted = data["isAccepted"] as Boolean,
                isRejected = data["isRejected"] as Boolean,
                createdAt = Date(data["createdAt"] as Long),
                updatedAt = Date(data["updatedAt"] as Long),
                completedAt = if (data["completedAt"] as Long > 0) Date(data["completedAt"] as Long) else null,
                isSynced = true
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun mapProgressToMap(progress: TaskProgress, userId: String): Map<String, Any> {
        return mapOf(
            "id" to progress.id,
            "userId" to userId,
            "taskId" to progress.taskId,
            "date" to progress.date.time,
            "isCompleted" to progress.isCompleted,
            "startTime" to (progress.startTime ?: 0),
            "endTime" to (progress.endTime ?: 0),
            "durationCompleted" to (progress.durationCompleted ?: 0)
        )
    }

    private fun mapDocumentToProgress(document: com.google.firebase.firestore.DocumentSnapshot): TaskProgress? {
        return try {
            val data = document.data ?: return null
            TaskProgress(
                id = (data["id"] as Long),
                taskId = (data["taskId"] as Long),
                date = Date(data["date"] as Long),
                isCompleted = data["isCompleted"] as Boolean,
                startTime = data["startTime"] as Long?,
                endTime = data["endTime"] as Long?,
                durationCompleted = (data["durationCompleted"] as Long?)?.toInt(),
                isSynced = true
            )
        } catch (e: Exception) {
            null
        }
    }
    override suspend fun deleteTask(taskId: Long): Boolean {
        val userId = getCurrentUserId() ?: return false

        return try {
            firestore.collection(tasksCollection)
                .document("${userId}_${taskId}")
                .delete()
                .await()

            true
        } catch (e: Exception) {
            // Handle exception
            false
        }
    }
    override suspend fun syncAchievements(achievements: List<Achievement>): List<Achievement> {
        val userId = getCurrentUserId() ?: return emptyList()
        val syncedAchievementIds = mutableListOf<Long>()

        // Upload achievements to Firestore
        achievements.forEach { achievement ->
            try {
                val achievementMap = mapAchievementToMap(achievement, userId)

                firestore.collection(achievementsCollection)
                    .document("${userId}_${achievement.id}")
                    .set(achievementMap)
                    .await()

                syncedAchievementIds.add(achievement.id)
                Log.d(tag, "Synced achievement ${achievement.id} to Firebase")
            } catch (e: Exception) {
                Log.e(tag, "Error syncing achievement ${achievement.id}: ${e.message}", e)
            }
        }

        return achievements.filter { it.id in syncedAchievementIds }
    }

    override suspend fun fetchUserAchievements(): List<Achievement> {
        val userId = getCurrentUserId() ?: return emptyList()
        val result = mutableListOf<Achievement>()

        try {
            val snapshot = firestore.collection(achievementsCollection)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (document in snapshot.documents) {
                val achievement = mapDocumentToAchievement(document)
                if (achievement != null) {
                    result.add(achievement)
                }
            }
            Log.d(tag, "Fetched ${result.size} achievements from Firebase")
        } catch (e: Exception) {
            Log.e(tag, "Error fetching achievements: ${e.message}", e)
        }

        return result
    }

    override suspend fun syncUserStreak(streak: UserStreak): Boolean {
        val userId = getCurrentUserId() ?: return false

        return try {
            val streakMap = mapStreakToMap(streak)

            firestore.collection(streaksCollection)
                .document(userId)  // UserStreak uses userId as primary key
                .set(streakMap)
                .await()

            Log.d(tag, "Synced user streak to Firebase")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error syncing user streak: ${e.message}", e)
            false
        }
    }

    override suspend fun fetchUserStreak(userId: String): UserStreak? {
        if (userId.isEmpty()) return null

        return try {
            val document = firestore.collection(streaksCollection)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val streak = mapDocumentToStreak(document)
                Log.d(tag, "Fetched user streak from Firebase")
                streak
            } else {
                Log.d(tag, "No user streak found in Firebase")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching user streak: ${e.message}", e)
            null
        }
    }

    private fun mapAchievementToMap(achievement: Achievement, userId: String): Map<String, Any> {
        return mapOf(
            "id" to achievement.id,
            "userId" to userId,
            "type" to achievement.type.name,
            "title" to achievement.title,
            "description" to achievement.description,
            "earnedAt" to achievement.earnedAt.time
        )
    }

    private fun mapDocumentToAchievement(document: com.google.firebase.firestore.DocumentSnapshot): Achievement? {
        return try {
            val data = document.data ?: return null
            Achievement(
                id = (data["id"] as Long),
                userId = data["userId"] as String,
                type = AchievementType.valueOf(data["type"] as String),
                title = data["title"] as String,
                description = data["description"] as String,
                earnedAt = Date(data["earnedAt"] as Long),
                isSynced = true
            )
        } catch (e: Exception) {
            Log.e(tag, "Error mapping document to achievement: ${e.message}", e)
            null
        }
    }

    private fun mapStreakToMap(streak: UserStreak): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to streak.userId,
            "currentStreak" to streak.currentStreak,
            "longestStreak" to streak.longestStreak
        )

        // Add lastCompletedDate if not null
        streak.lastCompletedDate?.let {
            map["lastCompletedDate"] = it.time
        }

        return map
    }

    private fun mapDocumentToStreak(document: com.google.firebase.firestore.DocumentSnapshot): UserStreak? {
        return try {
            val data = document.data ?: return null

            val lastCompletedDate = if (data.containsKey("lastCompletedDate") && data["lastCompletedDate"] != null) {
                Date(data["lastCompletedDate"] as Long)
            } else {
                null
            }

            UserStreak(
                userId = data["userId"] as String,
                currentStreak = (data["currentStreak"] as Long).toInt(),
                longestStreak = (data["longestStreak"] as Long).toInt(),
                lastCompletedDate = lastCompletedDate,
                isSynced = true
            )
        } catch (e: Exception) {
            Log.e(tag, "Error mapping document to streak: ${e.message}", e)
            null
        }
    }
}