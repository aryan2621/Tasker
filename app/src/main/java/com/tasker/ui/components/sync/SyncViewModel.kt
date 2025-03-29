package com.tasker.ui.components.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.data.model.SyncStatus
import com.tasker.data.sync.SyncDataType
import com.tasker.data.sync.SyncManager
import com.tasker.data.sync.SyncState
import com.tasker.util.ConnectionState
import com.tasker.util.NetworkConnectivityObserver
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncViewModel : ViewModel(), KoinComponent {
    private val syncManager: SyncManager by inject()

    val syncState: StateFlow<SyncState> = syncManager.syncState

    private val _syncCounts = MutableStateFlow<Map<SyncStatus, Int>>(emptyMap())
    val syncCounts: StateFlow<Map<SyncStatus, Int>> = _syncCounts.asStateFlow()

    private val _hasUnsyncedChanges = MutableStateFlow(false)
    val hasUnsyncedChanges: StateFlow<Boolean> = _hasUnsyncedChanges.asStateFlow()

    private val networkConnectivityObserver: NetworkConnectivityObserver by inject()

    val isNetworkConnected = networkConnectivityObserver.connectionState
        .map { it is ConnectionState.Available }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    val totalUnsyncedItems = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            updateSyncCounts()

            while (currentCoroutineContext().isActive) {
                try {
                    val counts = syncManager.getSyncCounts()
                    totalUnsyncedItems.value = counts.values.sum()
                } catch (e: Exception) {
                }
                delay(5000)
            }
        }
    }

    private fun updateSyncCounts() {
        val taskRepository: com.tasker.data.repository.TaskRepository by inject()

        viewModelScope.launch {
            taskRepository.getSyncStatus().collectLatest { counts ->
                _syncCounts.value = counts
                _hasUnsyncedChanges.value = counts.entries.any {
                    it.key != SyncStatus.SYNCED && it.value > 0
                }
            }
        }
    }

    private suspend fun getSyncCounts(): Map<SyncDataType, Int> {
        return syncManager.getSyncCounts()
    }

    fun syncNow() {
        viewModelScope.launch {
            syncManager.syncAll()
        }
    }

    fun retryFailedSync() {
        viewModelScope.launch {
            val taskRepository: com.tasker.data.repository.TaskRepository by inject()
            taskRepository.resyncFailedTasks()
        }
    }
}