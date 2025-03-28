package com.tasker.data.domain

import com.tasker.data.model.Task
import com.tasker.data.repository.FirebaseRepository
import com.tasker.data.repository.TaskRepository
import java.util.Date

class GetTaskDetailUseCase(
    private val taskRepository: TaskRepository,
    private val firebaseRepository: FirebaseRepository
) {
    suspend fun execute(taskId: Long): Task? {
        syncRemoteTask(taskId)
        return taskRepository.getTaskById(taskId)
    }

    private suspend fun syncRemoteTask(taskId: Long) {
        try {
            val remoteTasks = firebaseRepository.fetchUserTasks()
            val remoteTask = remoteTasks.find { it.id == taskId }

            if (remoteTask != null) {
                val localTask = taskRepository.getTaskById(taskId)

                if (localTask == null) {
                    taskRepository.insertTask(remoteTask)
                } else if (localTask.updatedAt.before(remoteTask.updatedAt)) {
                    taskRepository.updateTask(remoteTask)
                }
            }
        } catch (e: Exception) {
            // Handle exception
        }
    }
}

class UpdateTaskStatusUseCase(
    private val taskRepository: TaskRepository
) {
    suspend fun execute(task: Task, completed: Boolean): Task {
        val updatedTask = task.copy(
            isCompleted = completed,
            completedAt = if (completed) Date() else null,
            updatedAt = Date()
        )
        taskRepository.updateTask(updatedTask)
        taskRepository.syncData()
        return updatedTask
    }
}

// DeleteTaskUseCase is already defined for the Home component
