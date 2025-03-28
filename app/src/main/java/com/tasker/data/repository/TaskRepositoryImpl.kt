package com.tasker.data.repository

import com.tasker.data.db.StreakManager
import com.tasker.data.db.TaskDao
import com.tasker.data.db.TaskProgressDao
import com.tasker.data.model.Task
import com.tasker.data.model.TaskProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emptyFlow
import java.util.Date

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val taskProgressDao: TaskProgressDao,
    private val firebaseRepository: FirebaseRepository,
    private val streakManager: StreakManager,
    private val authRepository: AuthRepository
) : TaskRepository {

    private fun Boolean?.orFalse(): Boolean = this ?: false

    override suspend fun insertTask(task: Task): Long {
        val userId = authRepository.getCurrentUserId() ?: return 0
        val taskWithUserId = if (task.userId.isNullOrEmpty()) task.copy(userId = userId) else task
        return taskDao.insertTask(taskWithUserId)
    }

    override suspend fun updateTask(task: Task) {
        val oldTask = taskDao.getTaskById(task.id)
        taskDao.updateTask(task)
        if (!oldTask?.isCompleted.orFalse() && task.isCompleted) {
            streakManager.updateStreakOnTaskCompletion()
        }
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    override suspend fun getTaskById(taskId: Long): Task? {
        return taskDao.getTaskById(taskId)
    }

    override fun getAllTasks(): Flow<List<Task>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskDao.getAllTasksForUser(userId)
        }

        // If userId is null, filter in-memory
        return taskDao.getAllTasks().map { tasks ->
            tasks.filter { it.userId == authRepository.cachedUserId }
        }
    }

    override fun getPendingTasks(): Flow<List<Task>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskDao.getPendingTasksForUser(userId)
        }

        return taskDao.getPendingTasks().map { tasks ->
            tasks.filter { it.userId == authRepository.cachedUserId }
        }
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskDao.getCompletedTasksForUser(userId)
        }

        return taskDao.getCompletedTasks().map { tasks ->
            tasks.filter { it.userId == authRepository.cachedUserId }
        }
    }

    override fun getTasksByTimeRange(startTime: Date, endTime: Date): Flow<List<Task>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskDao.getTasksByTimeRangeForUser(userId, startTime, endTime)
        }

        return taskDao.getTasksByTimeRange(startTime, endTime).map { tasks ->
            tasks.filter { it.userId == authRepository.cachedUserId }
        }
    }

    override fun getAcceptedTasks(): Flow<List<Task>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskDao.getAcceptedTasksForUser(userId)
        }

        return taskDao.getAcceptedTasks().map { tasks ->
            tasks.filter { it.userId == authRepository.cachedUserId }
        }
    }

    override suspend fun insertProgress(progress: TaskProgress): Long {
        return taskProgressDao.insertProgress(progress)
    }

    override suspend fun updateProgress(progress: TaskProgress) {
        taskProgressDao.updateProgress(progress)
    }

    override fun getProgressForTask(taskId: Long): Flow<List<TaskProgress>> {
        return taskProgressDao.getProgressForTask(taskId)
    }

    override fun getProgressByDateRange(startDate: Date, endDate: Date): Flow<List<TaskProgress>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskProgressDao.getProgressByDateRangeForUser(userId, startDate, endDate)
        }

        return taskProgressDao.getProgressByDateRange(startDate, endDate)
    }

    override fun getProgressByDate(date: Date): Flow<List<TaskProgress>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskProgressDao.getProgressByDateForUser(userId, date)
        }

        return taskProgressDao.getProgressByDate(date)
    }

    override fun getCompletedTasksCountForDay(date: Date): Flow<Int> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskProgressDao.getCompletedTasksCountForDayForUser(userId, date)
        }

        return taskProgressDao.getCompletedTasksCountForDay(date)
    }

    override suspend fun syncData(): Boolean {
        return try {
            if (!firebaseRepository.isUserSignedIn()) {
                return false
            }
            val unsyncedTasks = taskDao.getUnsyncedTasks()
            val unsyncedProgress = taskProgressDao.getUnsyncedProgress()
            val syncedTasks = firebaseRepository.syncTasks(unsyncedTasks)
            val syncedProgress = firebaseRepository.syncTaskProgress(unsyncedProgress)
            syncedTasks.forEach { task ->
                taskDao.markTaskAsSynced(task.id)
            }
            syncedProgress.forEach { progress ->
                taskProgressDao.markProgressAsSynced(progress.id)
            }
            val remoteTasks = firebaseRepository.fetchUserTasks()
            val remoteProgress = firebaseRepository.fetchUserTaskProgress()
            remoteTasks.forEach { task ->
                val localTask = taskDao.getTaskById(task.id)
                if (localTask == null) {
                    taskDao.insertTask(task)
                } else if (localTask.updatedAt.before(task.updatedAt)) {
                    taskDao.updateTask(task)
                }
            }
            remoteProgress.forEach { progress ->
                taskProgressDao.insertProgress(progress)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun syncProgressData(): Boolean {
        try {
            // First sync unsynced local progress to Firebase
            val unsyncedProgress = getUnsyncedProgress()
            if (unsyncedProgress.isNotEmpty()) {
                val syncedProgress = firebaseRepository.syncTaskProgress(unsyncedProgress)
                syncedProgress.forEach { progress ->
                    markProgressAsSynced(progress.id)
                }
            }

            // Then sync remote progress to local
            val remoteProgress = firebaseRepository.fetchUserTaskProgress()
            remoteProgress.forEach { progress ->
                insertProgress(progress)
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override suspend fun getUnsyncedProgress(): List<TaskProgress> {
        return taskProgressDao.getUnsyncedProgress()
    }

    override suspend fun markProgressAsSynced(progressId: Long) {
        taskProgressDao.markProgressAsSynced(progressId)
    }
}