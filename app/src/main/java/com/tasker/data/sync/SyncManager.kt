package com.tasker.data.sync

import android.util.Log
import com.tasker.data.model.SyncStatus
import com.tasker.data.repository.AchievementRepository
import com.tasker.data.repository.AuthRepository
import com.tasker.data.repository.StreakRepository
import com.tasker.data.repository.TaskRepository
import com.tasker.util.ConnectionState
import com.tasker.util.NetworkConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class SyncManager(
    private val taskRepository: TaskRepository,
    private val achievementRepository: AchievementRepository,
    private val streakRepository: StreakRepository,
    private val authRepository: AuthRepository,
    private val networkConnectivityObserver: NetworkConnectivityObserver
) {
    private val TAG = "SyncManager"

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Track sync state for each data type
    private val isSyncingTasks = AtomicBoolean(false)
    private val isSyncingProgress = AtomicBoolean(false)
    private val isSyncingAchievements = AtomicBoolean(false)
    private val isSyncingStreaks = AtomicBoolean(false)

    // Global syncing flag
    private val isSyncing = AtomicBoolean(false)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Track sync results for each data type
    private val _taskSyncResult = MutableStateFlow<SyncResult?>(null)
    val taskSyncResult: StateFlow<SyncResult?> = _taskSyncResult.asStateFlow()

    private val _progressSyncResult = MutableStateFlow<SyncResult?>(null)
    val progressSyncResult: StateFlow<SyncResult?> = _progressSyncResult.asStateFlow()

    private val _achievementSyncResult = MutableStateFlow<SyncResult?>(null)
    val achievementSyncResult: StateFlow<SyncResult?> = _achievementSyncResult.asStateFlow()

    private val _streakSyncResult = MutableStateFlow<SyncResult?>(null)
    val streakSyncResult: StateFlow<SyncResult?> = _streakSyncResult.asStateFlow()

    init {
        syncScope.launch {
            networkConnectivityObserver.observe().collectLatest { state ->
                if (state is ConnectionState.Available) {
                    Log.d(TAG, "Network connected (${state.type}). Attempting to sync...")
                    attemptSync()
                } else {
                    Log.d(TAG, "Network disconnected")
                }
            }
        }
    }

    // Main sync method that coordinates all sync operations
    suspend fun syncAll(): Boolean = withContext(Dispatchers.IO) {
        if (!networkConnectivityObserver.isConnected()) {
            Log.d(TAG, "Cannot sync: No network connection")
            _syncState.value = SyncState.Error("No network connection")
            return@withContext false
        }

        if (!authRepository.isUserAuthenticated()) {
            Log.d(TAG, "Cannot sync: User not authenticated")
            _syncState.value = SyncState.Error("User not authenticated")
            return@withContext false
        }

        if (!isSyncing.compareAndSet(false, true)) {
            Log.d(TAG, "Sync already in progress, skipping")
            return@withContext false
        }

        try {
            _syncState.value = SyncState.Syncing("Syncing data...")
            Log.d(TAG, "Starting sync process...")

            // Run all sync operations
            val tasksSuccess = syncTasks()
            val progressSuccess = syncProgress()
            val achievementsSuccess = syncAchievements()
            val streakSuccess = syncStreaks()

            val allSuccess = tasksSuccess && progressSuccess && achievementsSuccess && streakSuccess
            _syncState.value = if (allSuccess) {
                SyncState.Success("Sync completed successfully")
            } else {
                SyncState.Error("Some sync operations failed")
            }

            return@withContext allSuccess

        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            _syncState.value = SyncState.Error("Sync error: ${e.message}")
            return@withContext false
        } finally {
            isSyncing.set(false)
        }
    }

    // Sync tasks data
    suspend fun syncTasks(): Boolean {
        if (!networkConnectivityObserver.isConnected() || !authRepository.isUserAuthenticated()) {
            _taskSyncResult.value = SyncResult(false, "Network unavailable or not authenticated")
            return false
        }

        if (!isSyncingTasks.compareAndSet(false, true)) {
            Log.d(TAG, "Task sync already in progress")
            return false
        }

        try {
            _syncState.value = SyncState.Syncing("Syncing tasks...")
            val success = taskRepository.syncData()

            val message = if (success) "Tasks synced successfully" else "Task sync had errors"
            _taskSyncResult.value = SyncResult(success, message)
            Log.d(TAG, message)

            return success
        } catch (e: Exception) {
            Log.e(TAG, "Task sync error: ${e.message}", e)
            _taskSyncResult.value = SyncResult(false, "Task sync error: ${e.message}")
            return false
        } finally {
            isSyncingTasks.set(false)
        }
    }

    // Sync task progress data
    suspend fun syncProgress(): Boolean {
        if (!networkConnectivityObserver.isConnected() || !authRepository.isUserAuthenticated()) {
            _progressSyncResult.value = SyncResult(false, "Network unavailable or not authenticated")
            return false
        }

        if (!isSyncingProgress.compareAndSet(false, true)) {
            Log.d(TAG, "Progress sync already in progress")
            return false
        }

        try {
            _syncState.value = SyncState.Syncing("Syncing progress...")
            val success = taskRepository.syncProgressData()

            val message = if (success) "Progress synced successfully" else "Progress sync had errors"
            _progressSyncResult.value = SyncResult(success, message)
            Log.d(TAG, message)

            return success
        } catch (e: Exception) {
            Log.e(TAG, "Progress sync error: ${e.message}", e)
            _progressSyncResult.value = SyncResult(false, "Progress sync error: ${e.message}")
            return false
        } finally {
            isSyncingProgress.set(false)
        }
    }

    // Sync achievements data
    suspend fun syncAchievements(): Boolean {
        if (!networkConnectivityObserver.isConnected() || !authRepository.isUserAuthenticated()) {
            _achievementSyncResult.value = SyncResult(false, "Network unavailable or not authenticated")
            return false
        }

        if (!isSyncingAchievements.compareAndSet(false, true)) {
            Log.d(TAG, "Achievements sync already in progress")
            return false
        }

        try {
            _syncState.value = SyncState.Syncing("Syncing achievements...")
            val success = achievementRepository.syncAchievements()

            val message = if (success) "Achievements synced successfully" else "Achievements sync had errors"
            _achievementSyncResult.value = SyncResult(success, message)
            Log.d(TAG, message)

            return success
        } catch (e: Exception) {
            Log.e(TAG, "Achievements sync error: ${e.message}", e)
            _achievementSyncResult.value = SyncResult(false, "Achievements sync error: ${e.message}")
            return false
        } finally {
            isSyncingAchievements.set(false)
        }
    }

    // Sync user streak data
    suspend fun syncStreaks(): Boolean {
        if (!networkConnectivityObserver.isConnected() || !authRepository.isUserAuthenticated()) {
            _streakSyncResult.value = SyncResult(false, "Network unavailable or not authenticated")
            return false
        }

        if (!isSyncingStreaks.compareAndSet(false, true)) {
            Log.d(TAG, "Streaks sync already in progress")
            return false
        }

        try {
            _syncState.value = SyncState.Syncing("Syncing streaks...")

            // Get all streaks that need syncing
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _streakSyncResult.value = SyncResult(false, "No user ID available")
                return false
            }

            // First sync any pending local changes
            val unsyncedStreaks = streakRepository.getUnsyncedStreaks()
            var allSuccess = true

            if (unsyncedStreaks.isNotEmpty()) {
                for (streak in unsyncedStreaks) {
                    val success = streakRepository.syncUserStreak(streak)
                    if (!success) {
                        allSuccess = false
                    }
                }
            }

            // Then fetch and merge remote data
            val remoteStreak = streakRepository.fetchUserStreak(userId)

            val message = if (allSuccess) "Streaks synced successfully" else "Streak sync had errors"
            _streakSyncResult.value = SyncResult(allSuccess, message)
            Log.d(TAG, message)

            return allSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Streak sync error: ${e.message}", e)
            _streakSyncResult.value = SyncResult(false, "Streak sync error: ${e.message}")
            return false
        } finally {
            isSyncingStreaks.set(false)
        }
    }

    // Attempt to sync any specific data type
    fun syncSpecificData(dataType: SyncDataType) {
        syncScope.launch {
            when (dataType) {
                SyncDataType.TASKS -> syncTasks()
                SyncDataType.PROGRESS -> syncProgress()
                SyncDataType.ACHIEVEMENTS -> syncAchievements()
                SyncDataType.STREAKS -> syncStreaks()
            }
        }
    }

    // Attempt to sync all data
    fun attemptSync() {
        syncScope.launch {
            if (!isSyncing.get() && networkConnectivityObserver.isConnected() &&
                authRepository.isUserAuthenticated()) {
                syncAll()
            }
        }
    }

    // Get counts of items needing sync for each data type
    suspend fun getSyncCounts(): Map<SyncDataType, Int> {
        val counts = mutableMapOf<SyncDataType, Int>()

        // Only try to get counts if we're authenticated
        if (authRepository.isUserAuthenticated()) {
            val userId = authRepository.getCurrentUserId() ?: return counts

            // Count tasks needing sync
            val pendingTasks = taskRepository.getPendingSyncTasks().size
            counts[SyncDataType.TASKS] = pendingTasks

            // Count progress entries needing sync
            val pendingProgress = taskRepository.getUnsyncedProgress().size
            counts[SyncDataType.PROGRESS] = pendingProgress

            // Count achievements needing sync
            val pendingAchievements = achievementRepository.getUnsyncedAchievements().size
            counts[SyncDataType.ACHIEVEMENTS] = pendingAchievements

            // Count streaks needing sync
            val pendingStreaks = streakRepository.getUnsyncedStreaks().size
            counts[SyncDataType.STREAKS] = pendingStreaks
        }

        return counts
    }

    fun cleanup() {
        syncScope.cancel()
    }
}

// Data class to hold sync results for a specific data type
data class SyncResult(
    val success: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Enum to represent different types of data to sync
enum class SyncDataType {
    TASKS,
    PROGRESS,
    ACHIEVEMENTS,
    STREAKS
}

sealed class SyncState {
    data object Idle : SyncState()
    data class Syncing(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}