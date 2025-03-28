package com.tasker.ui.screens.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.data.domain.DeleteTaskUseCase
import com.tasker.data.domain.GetTaskDetailUseCase
import com.tasker.data.domain.UpdateTaskStatusUseCase
import com.tasker.data.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TaskDetailViewModel : ViewModel(), KoinComponent {
    private val getTaskDetailUseCase: GetTaskDetailUseCase by inject()
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase by inject()
    private val deleteTaskUseCase: DeleteTaskUseCase by inject()

    private val _task = MutableStateFlow<Task?>(null)
    val task: StateFlow<Task?> = _task.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val loadedTask = getTaskDetailUseCase.execute(taskId)
            _task.value = loadedTask
            _isLoading.value = false
        }
    }

    fun markTaskCompleted(completed: Boolean = true) {
        val currentTask = _task.value ?: return

        viewModelScope.launch {
            val updatedTask = updateTaskStatusUseCase.execute(currentTask, completed)
            _task.value = updatedTask
        }
    }

    fun deleteTask() {
        val currentTask = _task.value ?: return

        viewModelScope.launch {
            deleteTaskUseCase.execute(currentTask)
        }
    }
}