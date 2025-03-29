package com.tasker.data.domain

import com.tasker.data.model.Task
import com.tasker.data.model.TaskProgress
import com.tasker.data.repository.TaskRepository
import java.util.Date

/**
 * Use case to manage active task operations
 */
class ActiveTaskUseCase(
    private val taskRepository: TaskRepository
) {
    /**
     * Retrieves a task by ID
     */
    suspend fun getTaskById(taskId: Long): Task? {
        return taskRepository.getTaskById(taskId)
    }

    /**
     * Marks a task as completed and creates a progress record
     */
    suspend fun completeTask(
        taskId: Long,
        startTime: Long,
        endTime: Long,
        totalPausedTime: Long
    ): Boolean {
        val task = taskRepository.getTaskById(taskId) ?: return false

        // Calculate actual time spent
        val actualDuration = (endTime - startTime - totalPausedTime) / (60 * 1000)

        // Create progress record
        val progressRecord = TaskProgress(
            taskId = task.id,
            isCompleted = true,
            startTime = startTime,
            endTime = endTime,
            durationCompleted = actualDuration.coerceAtLeast(1).toInt(),
            date = Date()
        )

        // Update task as completed
        taskRepository.updateTask(
            task.copy(
                isCompleted = true,
                completedAt = Date()
            )
        )

        // Insert progress record
        taskRepository.insertProgress(progressRecord)

        return true
    }

    /**
     * Records a cancelled task's progress, if any time was spent
     */
    suspend fun cancelTask(
        taskId: Long,
        startTime: Long,
        endTime: Long,
        totalPausedTime: Long
    ): Boolean {
        val task = taskRepository.getTaskById(taskId) ?: return false

        // Calculate actual time spent
        val actualDuration = (endTime - startTime - totalPausedTime) / (60 * 1000)

        // Only record progress if some time was spent
        if (actualDuration > 0) {
            // Create progress record
            val progressRecord = TaskProgress(
                taskId = task.id,
                isCompleted = false,
                startTime = startTime,
                endTime = endTime,
                durationCompleted = actualDuration.toInt(),
                date = Date()
            )

            // Insert progress record
            taskRepository.insertProgress(progressRecord)
        }

        return true
    }

    /**
     * Sets a task as accepted (active)
     */
    suspend fun acceptTask(taskId: Long): Boolean {
        val task = taskRepository.getTaskById(taskId) ?: return false
        taskRepository.updateTask(task.copy(isAccepted = true))
        return true
    }
}