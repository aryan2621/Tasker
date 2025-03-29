package com.tasker.ui.screens.currenttask

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.data.domain.ActiveTaskUseCase
import com.tasker.data.model.Task
import com.tasker.service.MusicService
import com.tasker.service.RunPendingTaskService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ActiveTaskViewModel : ViewModel(), KoinComponent {
    private var lastContext: Context? = null
    private val activeTaskUseCase: ActiveTaskUseCase by inject()

    private val _uiState = MutableStateFlow(ActiveTaskUiState())
    val uiState: StateFlow<ActiveTaskUiState> = _uiState.asStateFlow()

    private var startTime: Long = 0
    private var pauseStartTime: Long = 0
    private var totalPausedTime: Long = 0

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val task = activeTaskUseCase.getTaskById(taskId)
            if (task != null) {
                startTime = System.currentTimeMillis()

                activeTaskUseCase.acceptTask(taskId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        task = task,
                        remainingSeconds = task.durationMinutes * 60,
                        isRunning = true
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, task = null) }
            }
        }
    }

    fun updateNotification(context: Context) {
        val task = uiState.value.task ?: return

        // Update notification based on running state
        if (uiState.value.isRunning) {
            RunPendingTaskService.updateRunningTaskNotification(
                context = context,
                task = task,
                remainingSeconds = uiState.value.remainingSeconds
            )
        } else {
            RunPendingTaskService.updatePausedTaskNotification(
                context = context,
                task = task,
                remainingSeconds = uiState.value.remainingSeconds
            )
        }
    }

    // Update the tick function to also update notification
    fun tick() {
        _uiState.update {
            val newRemainingSeconds = (it.remainingSeconds - 1).coerceAtLeast(0)
            val shouldComplete = newRemainingSeconds <= 0 && !it.isCompleted

            it.copy(
                remainingSeconds = newRemainingSeconds,
                isCompleted = it.isCompleted || shouldComplete
            )
        }

        // Update notification every 5 seconds to avoid too many updates
        if (uiState.value.remainingSeconds % 5 == 0) {
            // Pass context from LaunchedEffect
            lastContext?.let { updateNotification(it) }
        }
    }


    // Update the pause function to update the notification
    fun pauseTask(context: Context) {
        // Only pause if currently running
        if (!_uiState.value.isRunning) return

        pauseStartTime = System.currentTimeMillis()
        _uiState.update { it.copy(isRunning = false) }

        // Pause music
        MusicService.pauseMusic(context)

        // Update notification to paused state
        lastContext = context
        val task = uiState.value.task ?: return
        RunPendingTaskService.updatePausedTaskNotification(
            context = context,
            task = task,
            remainingSeconds = uiState.value.remainingSeconds
        )
    }

    // Update the resume function to update the notification
    fun resumeTask(context: Context) {
        // Only resume if not already running
        if (_uiState.value.isRunning) return

        if (pauseStartTime > 0) {
            totalPausedTime += System.currentTimeMillis() - pauseStartTime
            pauseStartTime = 0
        }
        _uiState.update { it.copy(isRunning = true) }

        // Resume music
        MusicService.resumeMusic(context)

        // Update notification to running state
        lastContext = context
        val task = uiState.value.task ?: return
        RunPendingTaskService.updateRunningTaskNotification(
            context = context,
            task = task,
            remainingSeconds = uiState.value.remainingSeconds
        )
    }

    // Update the complete function to show completion notification
    fun completeTask(context: Context) {
        // Don't complete if already completed or cancelled
        if (_uiState.value.isCompleted || _uiState.value.isCancelled) return

        lastContext = context

        viewModelScope.launch {
            val task = uiState.value.task ?: return@launch

            // Update UI state first
            _uiState.update { it.copy(isCompleted = true, isRunning = false) }

            val endTime = if (_uiState.value.isRunning) {
                System.currentTimeMillis()
            } else {
                pauseStartTime
            }

            val success = activeTaskUseCase.completeTask(
                taskId = task.id,
                startTime = startTime,
                endTime = endTime,
                totalPausedTime = totalPausedTime
            )

            if (success) {
                // Clear running notification
                RunPendingTaskService.clearRunningTaskNotification(context, task.id)

                // Show completion notification
                RunPendingTaskService.showTaskCompletedNotification(context, task)

                // Stop music
                MusicService.stopMusic(context)
            }
        }
    }

    // Update the cancel function to clear notifications
    fun cancelTask(context: Context) {
        // Don't cancel if already cancelled or completed
        if (_uiState.value.isCancelled || _uiState.value.isCompleted) return

        lastContext = context

        viewModelScope.launch {
            val task = uiState.value.task ?: return@launch

            // Update UI state immediately to show cancellation
            _uiState.update {
                it.copy(
                    isCancelled = true,
                    isRunning = false
                )
            }

            val endTime = if (_uiState.value.isRunning) {
                System.currentTimeMillis()
            } else {
                pauseStartTime
            }

            activeTaskUseCase.cancelTask(
                taskId = task.id,
                startTime = startTime,
                endTime = endTime,
                totalPausedTime = totalPausedTime
            )

            RunPendingTaskService.clearRunningTaskNotification(context, task.id)

            // Stop music
            MusicService.stopMusic(context)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}

data class ActiveTaskUiState(
    val isLoading: Boolean = false,
    val task: Task? = null,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val isCancelled: Boolean = false
)