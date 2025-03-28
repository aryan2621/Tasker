package com.tasker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.data.domain.DeleteTaskUseCase
import com.tasker.data.domain.GetTaskStatsUseCase
import com.tasker.data.domain.GetTasksUseCase
import com.tasker.data.domain.SyncTasksUseCase
import com.tasker.data.model.Task
import com.tasker.data.repository.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Date

class HomeViewModel : ViewModel(), KoinComponent {

    private val getTasksUseCase: GetTasksUseCase by inject()
    private val getTaskStatsUseCase: GetTaskStatsUseCase by inject()
    private val syncTasksUseCase: SyncTasksUseCase by inject()
    private val deleteTaskUseCase: DeleteTaskUseCase by inject()

    enum class TaskFilter {
        ALL, PENDING, COMPLETED, TODAY
    }

    private val _currentFilter = MutableStateFlow(TaskFilter.PENDING)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter

    val tasksUiState = getTasksUseCase.execute(_currentFilter)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val statsUiState = getTaskStatsUseCase.execute()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TaskStats(0, 0, 0, 0)
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            deleteTaskUseCase.execute(task)
        }
    }

    fun onRefreshTrigger() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                syncTasksUseCase.execute()
                delay(800)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    init {
        viewModelScope.launch {
            onRefreshTrigger()
        }
    }
}

class GetTasksUseCase(
    private val taskRepository: TaskRepository
) {
    fun execute(filterFlow: Flow<HomeViewModel.TaskFilter>): Flow<List<Task>> {
        return combine(
            taskRepository.getAllTasks(),
            filterFlow
        ) { tasks, filter ->
            when (filter) {
                HomeViewModel.TaskFilter.ALL -> tasks
                HomeViewModel.TaskFilter.PENDING -> tasks.filter { !it.isCompleted }
                HomeViewModel.TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
                HomeViewModel.TaskFilter.TODAY -> {
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

data class TaskStats(
    val pendingCount: Int,
    val completedCount: Int,
    val totalCount: Int,
    val completedToday: Int
)