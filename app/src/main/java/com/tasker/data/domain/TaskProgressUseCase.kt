package com.tasker.data.domain

import android.content.Context
import com.tasker.data.model.Task
import com.tasker.data.repository.TaskRepository
import com.tasker.service.RunPendingTaskService
import kotlinx.coroutines.flow.first

/**
 * Use case to run a task immediately or schedule it for running
 */
class RunTaskUseCase(
    private val taskRepository: TaskRepository
) {
    /**
     * Run a task immediately
     * @param taskId The ID of the task to run
     * @param context Context used for showing notifications
     * @return True if successful, false otherwise
     */
    suspend fun executeImmediately(taskId: Long, context: Context): Boolean {
        val task = taskRepository.getTaskById(taskId) ?: return false

        // Mark task as accepted
        taskRepository.updateTask(task.copy(isAccepted = true))

        // Show notification
        RunPendingTaskService.showTaskRunningNotification(context, task)

        return true
    }

    /**
     * Find the next pending task and notify the user
     * @param context Context used for showing notifications
     */
    suspend fun notifyNextPendingTask(context: Context) {
        val pendingTasks = taskRepository.getPendingTasks().first()

        if (pendingTasks.isNotEmpty()) {
            // Sort tasks by priority and due date
            val sortedTasks = pendingTasks.sortedWith(
                compareBy<Task> { it.reminderTime.time - System.currentTimeMillis() }
                    .thenByDescending { it.priority }
            )

            // Get the most relevant task
            val nextTask = sortedTasks.firstOrNull() ?: return

            // Show notification for the task
            RunPendingTaskService.showPendingTaskNotification(context, nextTask)
        }
    }

    /**
     * Get a list of pending tasks that can be run now
     * @return List of pending tasks
     */
    suspend fun getPendingRunnableTasks(): List<Task> {
        return taskRepository.getPendingTasks().first().filter { !it.isCompleted }
    }
}