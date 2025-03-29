package com.tasker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tasker.data.domain.ActiveTaskUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * BroadcastReceiver to handle task actions from notifications
 */
class TaskNotificationReceiver : BroadcastReceiver(), KoinComponent {

    private val activeTaskUseCase: ActiveTaskUseCase by inject()

    companion object {
        const val ACTION_PAUSE_TASK = "com.tasker.ACTION_PAUSE_TASK"
        const val ACTION_RESUME_TASK = "com.tasker.ACTION_RESUME_TASK"
        const val ACTION_COMPLETE_TASK = "com.tasker.ACTION_COMPLETE_TASK"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1L)
        if (taskId == -1L) return

        when (intent.action) {
            ACTION_PAUSE_TASK -> handlePauseTask(context, taskId)
            ACTION_RESUME_TASK -> handleResumeTask(context, taskId)
            ACTION_COMPLETE_TASK -> handleCompleteTask(context, taskId)
        }
    }

    private fun handlePauseTask(context: Context, taskId: Long) {
        // Pause background music
        MusicService.pauseMusic(context)

        // Get current task info and update notification
        CoroutineScope(Dispatchers.IO).launch {
            val task = activeTaskUseCase.getTaskById(taskId) ?: return@launch

            // TODO: Need to store the remaining time somewhere for proper pausing
            // For now, we'll just update the UI to paused state

            // Send broadcast to inform active task about the pause
            val broadcastIntent = Intent("com.tasker.TASK_STATE_CHANGED").apply {
                putExtra("taskId", taskId)
                putExtra("action", "pause")
            }
            context.sendBroadcast(broadcastIntent)
        }
    }

    private fun handleResumeTask(context: Context, taskId: Long) {
        // Resume background music
        MusicService.resumeMusic(context)

        // Get current task info and update notification
        CoroutineScope(Dispatchers.IO).launch {
            val task = activeTaskUseCase.getTaskById(taskId) ?: return@launch

            // Send broadcast to inform active task about the resume
            val broadcastIntent = Intent("com.tasker.TASK_STATE_CHANGED").apply {
                putExtra("taskId", taskId)
                putExtra("action", "resume")
            }
            context.sendBroadcast(broadcastIntent)
        }
    }

    private fun handleCompleteTask(context: Context, taskId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            // Mark task as completed
            val task = activeTaskUseCase.getTaskById(taskId) ?: return@launch

            // We don't have all the timing information from the notification
            // For simplicity, we'll just mark it completed with estimated duration
            activeTaskUseCase.completeTask(
                taskId = taskId,
                startTime = System.currentTimeMillis() - (task.durationMinutes * 60 * 1000),
                endTime = System.currentTimeMillis(),
                totalPausedTime = 0
            )

            // Clear the notification
            RunPendingTaskService.clearRunningTaskNotification(context, taskId)

            // Show completion notification
            RunPendingTaskService.showTaskCompletedNotification(context, task)

            // Stop the music
            MusicService.stopMusic(context)

            // Send broadcast to inform active task about completion
            val broadcastIntent = Intent("com.tasker.TASK_STATE_CHANGED").apply {
                putExtra("taskId", taskId)
                putExtra("action", "complete")
            }
            context.sendBroadcast(broadcastIntent)
        }
    }
}