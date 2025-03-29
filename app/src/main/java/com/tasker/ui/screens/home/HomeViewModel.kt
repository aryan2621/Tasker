package com.tasker.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.data.domain.DeleteTaskUseCase
import com.tasker.data.domain.GetTaskStatsUseCase
import com.tasker.data.domain.GetTasksUseCase
import com.tasker.data.domain.RunTaskUseCase
import com.tasker.data.domain.SyncTasksUseCase
import com.tasker.data.model.Task
import com.tasker.data.model.TaskStats
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
    private val runTaskUseCase: RunTaskUseCase by inject()

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
    fun runTaskNow(taskId: Long, context: Context) {
        viewModelScope.launch {
            runTaskUseCase.executeImmediately(taskId, context)
        }
    }

    init {
        viewModelScope.launch {
            onRefreshTrigger()
        }
    }
}
