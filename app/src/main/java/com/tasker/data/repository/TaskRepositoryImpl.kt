package com.tasker.data.repository

import android.util.Log
import com.tasker.data.db.StreakManager
import com.tasker.data.db.TaskDao
import com.tasker.data.db.TaskProgressDao
import com.tasker.data.model.SyncStatus
import com.tasker.data.model.Task
import com.tasker.data.model.TaskProgress
import com.tasker.util.NetworkConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.util.Date

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val taskProgressDao: TaskProgressDao,
    private val firebaseRepository: FirebaseRepository,
    private val streakManager: StreakManager,
    private val authRepository: AuthRepository,
    private val networkConnectivityObserver: NetworkConnectivityObserver
) : TaskRepository {

    private val TAG = "TaskRepositoryImpl"

    override suspend fun insertTask(task: Task): Long {
        val userId = authRepository.getCurrentUserId() ?: return 0

        // Ensure task has correct user ID and sync status
        val taskWithUserId = if (task.userId.isNullOrEmpty()) {
            task.copy(
                userId = userId,
                syncStatus = SyncStatus.PENDING_UPLOAD,
                localModifiedAt = Date()
            )
        } else {
            task.copy(
                syncStatus = SyncStatus.PENDING_UPLOAD,
                localModifiedAt = Date()
            )
        }

        val id = taskDao.insertTask(taskWithUserId)

        // Attempt immediate sync if connected
        if (networkConnectivityObserver.isConnected()) {
            syncById(id)
        }

        return id
    }

    override suspend fun updateTask(task: Task): Unit {
        val oldTask = taskDao.getTaskById(task.id)

        // Update task with new sync status
        val updatedTask = task.copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            localModifiedAt = Date()
        )

        taskDao.updateTask(updatedTask)

        // Check if task was completed and update streak if needed
        if (!oldTask?.isCompleted.orFalse() && task.isCompleted) {
            streakManager.updateStreakOnTaskCompletion()
        }

        // Attempt immediate sync if connected
        if (networkConnectivityObserver.isConnected()) {
            syncById(task.id)
        }
    }

    override suspend fun deleteTask(task: Task): Unit {
        // Mark as deleted locally instead of immediate deletion
        markTaskLocallyDeleted(task.id)

        // Attempt immediate sync if connected
        if (networkConnectivityObserver.isConnected()) {
            syncById(task.id)
        }
    }

    override suspend fun markTaskLocallyDeleted(taskId: Long) {
        taskDao.markTaskDeletedLocally(taskId)
    }

    override suspend fun cleanupDeletedTasks() {
        taskDao.deleteLocallyDeletedAndSyncedTasks()
    }

    override suspend fun getTaskById(taskId: Long): Task? {
        return taskDao.getTaskById(taskId)
    }

    override fun getAllTasks(): Flow<List<Task>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskDao.getAllTasksForUser(userId).map { tasks ->
                tasks.filter { !it.isDeletedLocally } // Filter out locally deleted tasks
            }
        }

        // If userId is null, filter in-memory
        return taskDao.getAllTasks().map { tasks ->
            tasks.filter { it.userId == authRepository.cachedUserId && !it.isDeletedLocally }
        }
    }

    override fun getPendingTasks(): Flow<List<Task>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskDao.getPendingTasksForUser(userId).map { tasks ->
                tasks.filter { !it.isDeletedLocally }
            }
        }

        return taskDao.getPendingTasks().map { tasks ->
            tasks.filter { it.userId == authRepository.cachedUserId && !it.isDeletedLocally }
        }
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskDao.getCompletedTasksForUser(userId).map { tasks ->
                tasks.filter { !it.isDeletedLocally }
            }
        }

        return taskDao.getCompletedTasks().map { tasks ->
            tasks.filter { it.userId == authRepository.cachedUserId && !it.isDeletedLocally }
        }
    }

    override fun getTasksByTimeRange(startTime: Date, endTime: Date): Flow<List<Task>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskDao.getTasksByTimeRangeForUser(userId, startTime, endTime).map { tasks ->
                tasks.filter { !it.isDeletedLocally }
            }
        }

        return taskDao.getTasksByTimeRange(startTime, endTime).map { tasks ->
            tasks.filter { it.userId == authRepository.cachedUserId && !it.isDeletedLocally }
        }
    }

    override fun getAcceptedTasks(): Flow<List<Task>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskDao.getAcceptedTasksForUser(userId).map { tasks ->
                tasks.filter { !it.isDeletedLocally }
            }
        }

        return taskDao.getAcceptedTasks().map { tasks ->
            tasks.filter { it.userId == authRepository.cachedUserId && !it.isDeletedLocally }
        }
    }

    // TaskProgress operations
    override suspend fun insertProgress(progress: TaskProgress): Long {
        val progressWithSync = progress.copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            localModifiedAt = Date()
        )
        val id = taskProgressDao.insertProgress(progressWithSync)

        // Attempt immediate sync if connected
        if (networkConnectivityObserver.isConnected()) {
            syncProgressById(id)
        }

        return id
    }

    override suspend fun updateProgress(progress: TaskProgress) {
        val progressWithSync = progress.copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            localModifiedAt = Date()
        )
        taskProgressDao.updateProgress(progressWithSync)

        // Attempt immediate sync if connected
        if (networkConnectivityObserver.isConnected()) {
            syncProgressById(progress.id)
        }
    }

    override fun getProgressForTask(taskId: Long): Flow<List<TaskProgress>> {
        return taskProgressDao.getProgressForTask(taskId).map { progressList ->
            progressList.filter { !it.isDeletedLocally }
        }
    }

    override fun getProgressByDateRange(startDate: Date, endDate: Date): Flow<List<TaskProgress>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskProgressDao.getProgressByDateRangeForUser(userId, startDate, endDate).map { progressList ->
                progressList.filter { !it.isDeletedLocally }
            }
        }

        return taskProgressDao.getProgressByDateRange(startDate, endDate).map { progressList ->
            progressList.filter { !it.isDeletedLocally }
        }
    }

    override fun getProgressByDate(date: Date): Flow<List<TaskProgress>> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskProgressDao.getProgressByDateForUser(userId, date).map { progressList ->
                progressList.filter { !it.isDeletedLocally }
            }
        }

        return taskProgressDao.getProgressByDate(date).map { progressList ->
            progressList.filter { !it.isDeletedLocally }
        }
    }

    override fun getCompletedTasksCountForDay(date: Date): Flow<Int> {
        val userId = authRepository.cachedUserId
        if (userId != null) {
            return taskProgressDao.getCompletedTasksCountForDayForUser(userId, date)
        }

        return taskProgressDao.getCompletedTasksCountForDay(date)
    }

    // Sync operations
    override suspend fun syncData(): Boolean {
        if (!networkConnectivityObserver.isConnected()) {
            Log.d(TAG, "Cannot sync: No network connection")
            return false
        }

        if (!firebaseRepository.isUserSignedIn()) {
            Log.d(TAG, "Cannot sync: User not signed in")
            return false
        }

        try {
            // Handle pending uploads
            val pendingUploads = taskDao.getTasksBySyncStatus(SyncStatus.PENDING_UPLOAD)
            if (pendingUploads.isNotEmpty()) {
                Log.d(TAG, "Syncing ${pendingUploads.size} pending uploads")
                val syncedTasks = firebaseRepository.syncTasks(pendingUploads)

                syncedTasks.forEach { task ->
                    taskDao.markTaskSynced(
                        taskId = task.id,
                        status = SyncStatus.SYNCED,
                        lastSyncAttempt = Date(),
                        serverTime = task.updatedAt
                    )
                }
            }

            // Handle pending deletes
            val pendingDeletes = taskDao.getTasksBySyncStatus(SyncStatus.PENDING_DELETE)
            if (pendingDeletes.isNotEmpty()) {
                Log.d(TAG, "Syncing ${pendingDeletes.size} pending deletes")
                for (task in pendingDeletes) {
                    val success = firebaseRepository.deleteTask(task.id)
                    if (success) {
                        taskDao.markTaskSynced(
                            taskId = task.id,
                            status = SyncStatus.SYNCED,
                            lastSyncAttempt = Date(),
                            serverTime = null
                        )
                    }
                }
            }

            // Fetch remote tasks
            val remoteTasks = firebaseRepository.fetchUserTasks()
            Log.d(TAG, "Fetched ${remoteTasks.size} remote tasks")

            for (remoteTask in remoteTasks) {
                val localTask = taskDao.getTaskById(remoteTask.id)

                if (localTask == null) {
                    // New remote task, insert locally
                    val localCopy = remoteTask.copy(
                        syncStatus = SyncStatus.SYNCED,
                        serverUpdatedAt = remoteTask.updatedAt,
                        lastSyncAttempt = Date()
                    )
                    taskDao.insertTask(localCopy)
                } else if (localTask.syncStatus == SyncStatus.SYNCED) {
                    // If local is synced, remote changes win
                    if (localTask.serverUpdatedAt?.before(remoteTask.updatedAt) == true) {
                        val updatedLocal = remoteTask.copy(
                            syncStatus = SyncStatus.SYNCED,
                            serverUpdatedAt = remoteTask.updatedAt,
                            lastSyncAttempt = Date()
                        )
                        taskDao.updateTask(updatedLocal)
                    }
                } else if (localTask.isDeletedLocally) {
                    // Local was marked for deletion, keep it that way
                    continue
                } else {
                    // Potential conflict - local changes and remote changes
                    if (localTask.localModifiedAt.after(localTask.serverUpdatedAt)) {
                        // Local was modified after last sync

                        if (remoteTask.updatedAt.after(localTask.serverUpdatedAt ?: Date(0))) {
                            // Remote also changed, we have a conflict
                            val resolvedTask = resolveConflict(localTask, remoteTask)
                            taskDao.updateTask(resolvedTask)
                        } else {
                            // Remote hasn't changed since our last sync, local changes win
                            // We'll upload in the next sync cycle
                        }
                    }
                }
            }

            // Clean up tasks that were successfully deleted
            cleanupDeletedTasks()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            return false
        }
    }

    override suspend fun syncProgressData(): Boolean {
        if (!networkConnectivityObserver.isConnected()) {
            Log.d(TAG, "Cannot sync progress: No network connection")
            return false
        }

        try {
            // First sync unsynced local progress to Firebase
            val unsyncedProgress = taskProgressDao.getProgressByStatus(SyncStatus.PENDING_UPLOAD)
            if (unsyncedProgress.isNotEmpty()) {
                Log.d(TAG, "Syncing ${unsyncedProgress.size} pending progress uploads")
                val syncedProgress = firebaseRepository.syncTaskProgress(unsyncedProgress)

                syncedProgress.forEach { progress ->
                    taskProgressDao.markProgressSynced(
                        progressId = progress.id,
                        status = SyncStatus.SYNCED,
                        lastSyncAttempt = Date(),
                        serverTime = progress.date
                    )
                }
            }

            // Handle pending deletes for progress
            val pendingDeletes = taskProgressDao.getProgressByStatus(SyncStatus.PENDING_DELETE)
            if (pendingDeletes.isNotEmpty()) {
                Log.d(TAG, "Syncing ${pendingDeletes.size} pending progress deletes")
                for (progress in pendingDeletes) {
                    val success = firebaseRepository.deleteTaskProgress(progress.id)
                    if (success) {
                        taskProgressDao.markProgressSynced(
                            progressId = progress.id,
                            status = SyncStatus.SYNCED,
                            lastSyncAttempt = Date(),
                            serverTime = null
                        )
                    }
                }
            }

            // Then sync remote progress to local
            val remoteProgress = firebaseRepository.fetchUserTaskProgress()
            Log.d(TAG, "Fetched ${remoteProgress.size} remote progress entries")

            for (remoteEntry in remoteProgress) {
                val localEntry = taskProgressDao.getProgressById(remoteEntry.id)

                if (localEntry == null) {
                    // New remote progress, insert locally
                    val localCopy = remoteEntry.copy(
                        syncStatus = SyncStatus.SYNCED,
                        serverUpdatedAt = remoteEntry.date,
                        lastSyncAttempt = Date()
                    )
                    taskProgressDao.insertProgress(localCopy)
                } else if (localEntry.syncStatus == SyncStatus.SYNCED) {
                    // If local is synced, remote changes win
                    if (localEntry.serverUpdatedAt?.before(remoteEntry.date) == true) {
                        val updatedLocal = remoteEntry.copy(
                            syncStatus = SyncStatus.SYNCED,
                            serverUpdatedAt = remoteEntry.date,
                            lastSyncAttempt = Date()
                        )
                        taskProgressDao.updateProgress(updatedLocal)
                    }
                } else if (localEntry.isDeletedLocally) {
                    // Local was marked for deletion, keep it that way
                    continue
                } else {
                    // Potential conflict - local changes and remote changes
                    if (localEntry.localModifiedAt.after(localEntry.serverUpdatedAt)) {
                        // Local was modified after last sync

                        if (remoteEntry.date.after(localEntry.serverUpdatedAt ?: Date(0))) {
                            // Remote also changed, we have a conflict
                            val resolvedProgress = resolveProgressConflict(localEntry, remoteEntry)
                            taskProgressDao.updateProgress(resolvedProgress)
                        }
                    }
                }
            }

            // Clean up progress entries that were successfully deleted
            taskProgressDao.deleteLocallyDeletedAndSyncedProgress()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Progress sync error: ${e.message}", e)
            return false
        }
    }

    override suspend fun getUnsyncedProgress(): List<TaskProgress> {
        return taskProgressDao.getProgressByStatus(SyncStatus.PENDING_UPLOAD)
    }

    override suspend fun markProgressAsSynced(progressId: Long) {
        taskProgressDao.markProgressSynced(
            progressId = progressId,
            status = SyncStatus.SYNCED,
            lastSyncAttempt = Date(),
            serverTime = Date()
        )
    }

    override suspend fun syncById(taskId: Long): Boolean {
        if (!networkConnectivityObserver.isConnected()) {
            return false
        }

        try {
            val task = taskDao.getTaskById(taskId) ?: return false

            if (task.isDeletedLocally) {
                val deleteSuccess = firebaseRepository.deleteTask(taskId)
                if (deleteSuccess) {
                    taskDao.markTaskSynced(
                        taskId = taskId,
                        status = SyncStatus.SYNCED,
                        serverTime = null
                    )
                }
                return deleteSuccess
            } else if (task.syncStatus == SyncStatus.PENDING_UPLOAD) {
                val syncedTasks = firebaseRepository.syncTasks(listOf(task))
                if (syncedTasks.isNotEmpty()) {
                    val syncedTask = syncedTasks.first()
                    taskDao.markTaskSynced(
                        taskId = taskId,
                        status = SyncStatus.SYNCED,
                        serverTime = syncedTask.updatedAt
                    )
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            logSyncError(taskId, e.message ?: "Unknown error")
            return false
        }
    }

    // This method was missing
    private suspend fun syncProgressById(progressId: Long): Boolean {
        if (!networkConnectivityObserver.isConnected()) {
            return false
        }

        try {
            val progress = taskProgressDao.getProgressById(progressId) ?: return false

            if (progress.isDeletedLocally) {
                val deleteSuccess = firebaseRepository.deleteTaskProgress(progressId)
                if (deleteSuccess) {
                    taskProgressDao.markProgressSynced(
                        progressId = progressId,
                        status = SyncStatus.SYNCED,
                        serverTime = null
                    )
                }
                return deleteSuccess
            } else if (progress.syncStatus == SyncStatus.PENDING_UPLOAD) {
                val syncedEntries = firebaseRepository.syncTaskProgress(listOf(progress))
                if (syncedEntries.isNotEmpty()) {
                    val syncedEntry = syncedEntries.first()
                    taskProgressDao.markProgressSynced(
                        progressId = progressId,
                        status = SyncStatus.SYNCED,
                        serverTime = syncedEntry.date
                    )
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            taskProgressDao.updateProgressSyncErrorMessage(progressId, e.message ?: "Unknown error")
            return false
        }
    }

    override suspend fun getPendingSyncTasks(): List<Task> {
        return taskDao.getTasksBySyncStatus(SyncStatus.PENDING_UPLOAD)
    }

    override suspend fun getPendingDeleteTasks(): List<Task> {
        return taskDao.getTasksBySyncStatus(SyncStatus.PENDING_DELETE)
    }

    override suspend fun getConflictTasks(): List<Task> {
        return taskDao.getTasksBySyncStatus(SyncStatus.CONFLICT)
    }

    override suspend fun markTaskSynced(taskId: Long, serverUpdatedAt: Date?) {
        taskDao.markTaskSynced(taskId, SyncStatus.SYNCED, Date(), serverUpdatedAt)
    }

    override suspend fun logSyncError(taskId: Long, error: String) {
        taskDao.updateSyncErrorMessage(taskId, error)
    }

    override suspend fun resyncFailedTasks(): Boolean {
        if (!networkConnectivityObserver.isConnected()) {
            return false
        }

        val unsyncedTasks = taskDao.getTasksNotSynced()
        if (unsyncedTasks.isEmpty()) {
            return true
        }

        var allSuccess = true
        for (task in unsyncedTasks) {
            val success = syncById(task.id)
            if (!success) {
                allSuccess = false
            }
        }
        return allSuccess
    }

    override suspend fun getSyncStatus(): Flow<Map<SyncStatus, Int>> = flow {
        val counts = mutableMapOf<SyncStatus, Int>()
        SyncStatus.values().forEach { status ->
            counts[status] = taskDao.getTasksBySyncStatus(status).size
        }
        emit(counts)
    }

    override suspend fun resolveConflict(localTask: Task, serverTask: Task): Task {
        // Simple last-write-wins strategy
        // Could be enhanced with field-level merging if needed
        return if (localTask.localModifiedAt.after(serverTask.updatedAt)) {
            // Local changes are newer, but mark for upload
            localTask.copy(
                syncStatus = SyncStatus.PENDING_UPLOAD,
                lastSyncAttempt = Date()
            )
        } else {
            // Server changes are newer
            serverTask.copy(
                syncStatus = SyncStatus.SYNCED,
                serverUpdatedAt = serverTask.updatedAt,
                lastSyncAttempt = Date()
            )
        }
    }

    // This method was missing
    private suspend fun resolveProgressConflict(localProgress: TaskProgress, remoteProgress: TaskProgress): TaskProgress {
        // For progress, we typically want to keep the most complete data
        return if (localProgress.isCompleted && !remoteProgress.isCompleted) {
            // Local shows completion, keep that data but mark for upload
            localProgress.copy(
                syncStatus = SyncStatus.PENDING_UPLOAD,
                lastSyncAttempt = Date()
            )
        } else if (!localProgress.isCompleted && remoteProgress.isCompleted) {
            // Remote shows completion, use that data
            remoteProgress.copy(
                syncStatus = SyncStatus.SYNCED,
                serverUpdatedAt = remoteProgress.date,
                lastSyncAttempt = Date()
            )
        } else if (localProgress.durationCompleted ?: 0 > remoteProgress.durationCompleted ?: 0) {
            // Local has more progress recorded
            localProgress.copy(
                syncStatus = SyncStatus.PENDING_UPLOAD,
                lastSyncAttempt = Date()
            )
        } else {
            // Default to remote data
            remoteProgress.copy(
                syncStatus = SyncStatus.SYNCED,
                serverUpdatedAt = remoteProgress.date,
                lastSyncAttempt = Date()
            )
        }
    }

    // Helper method to handle null booleans
    private fun Boolean?.orFalse(): Boolean = this ?: false
}