package com.tasker.data.domain

import com.tasker.data.model.Task
import com.tasker.data.repository.FirebaseRepository
import com.tasker.data.repository.TaskRepository
import com.tasker.ui.screens.home.HomeViewModel.TaskFilter
import com.tasker.ui.screens.home.TaskStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Date

class GetTasksUseCase(
    private val taskRepository: TaskRepository
) {
    fun execute(filterFlow: Flow<TaskFilter>): Flow<List<Task>> {
        return combine(
            taskRepository.getAllTasks(),
            filterFlow
        ) { tasks, filter ->
            when (filter) {
                TaskFilter.ALL -> tasks
                TaskFilter.PENDING -> tasks.filter { !it.isCompleted }
                TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
                TaskFilter.TODAY -> {
                    val today = Date()
                    val startOfDay = Date(today.year, today.month, today.date, 0, 0, 0)
                    val endOfDay = Date(today.year, today.month, today.date, 23, 59, 59)
                    tasks.filter { it.reminderTime in startOfDay..endOfDay }
                }
            }
        }
    }
}

class GetTaskStatsUseCase(
    private val taskRepository: TaskRepository
) {
    fun execute(): Flow<TaskStats> {
        return taskRepository.getAllTasks()
            .combine(taskRepository.getCompletedTasksCountForDay(Date())) { allTasks, completedToday ->
                val pendingCount = allTasks.count { !it.isCompleted }
                val completedCount = allTasks.count { it.isCompleted }
                val totalCount = allTasks.size

                TaskStats(
                    pendingCount = pendingCount,
                    completedCount = completedCount,
                    totalCount = totalCount,
                    completedToday = completedToday
                )
            }
    }
}

class SyncTasksUseCase(
    private val taskRepository: TaskRepository
) {
    suspend fun execute(): Boolean {
        return taskRepository.syncData()
    }
}
class DeleteTaskUseCase(
    private val taskRepository: TaskRepository,
    private val firebaseRepository: FirebaseRepository
) {
    suspend fun execute(task: Task) {
        taskRepository.deleteTask(task)

        try {
            firebaseRepository.deleteTask(task.id)
        } catch (e: Exception) {
        }
    }

    suspend operator fun invoke(task: Task) {
        execute(task)
    }
}