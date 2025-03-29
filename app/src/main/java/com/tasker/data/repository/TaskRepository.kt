package com.tasker.data.repository

import com.tasker.data.model.SyncStatus
import com.tasker.data.model.Task
import com.tasker.data.model.TaskProgress
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface TaskRepository {
    // Existing methods
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun getTaskById(taskId: Long): Task?
    fun getAllTasks(): Flow<List<Task>>
    fun getPendingTasks(): Flow<List<Task>>
    fun getCompletedTasks(): Flow<List<Task>>
    fun getTasksByTimeRange(startTime: Date, endTime: Date): Flow<List<Task>>
    fun getAcceptedTasks(): Flow<List<Task>>

    suspend fun insertProgress(progress: TaskProgress): Long
    suspend fun updateProgress(progress: TaskProgress)
    fun getProgressForTask(taskId: Long): Flow<List<TaskProgress>>
    fun getProgressByDateRange(startDate: Date, endDate: Date): Flow<List<TaskProgress>>
    fun getProgressByDate(date: Date): Flow<List<TaskProgress>>
    fun getCompletedTasksCountForDay(date: Date): Flow<Int>

    suspend fun syncData(): Boolean
    suspend fun syncProgressData(): Boolean
    suspend fun getUnsyncedProgress(): List<TaskProgress>
    suspend fun markProgressAsSynced(progressId: Long)

    // New methods for enhanced offline capabilities
    suspend fun markTaskSynced(taskId: Long, serverUpdatedAt: Date?)
    suspend fun markTaskLocallyDeleted(taskId: Long)
    suspend fun getPendingSyncTasks(): List<Task>
    suspend fun getPendingDeleteTasks(): List<Task>
    suspend fun getConflictTasks(): List<Task>
    suspend fun resolveConflict(localTask: Task, serverTask: Task): Task
    suspend fun cleanupDeletedTasks()
    suspend fun logSyncError(taskId: Long, error: String)
    suspend fun syncById(taskId: Long): Boolean
    suspend fun resyncFailedTasks(): Boolean
    suspend fun getSyncStatus(): Flow<Map<SyncStatus, Int>>
}