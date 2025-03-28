package com.tasker.ui.screens.taskform

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.data.domain.GetTaskForEditUseCase
import com.tasker.data.domain.SaveTaskUseCase
import com.tasker.data.model.Task
import com.tasker.data.model.TaskCategory
import com.tasker.data.model.TaskPriority
import com.tasker.data.model.TaskRecurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Date

class TaskFormViewModel : ViewModel(), KoinComponent {

    private val saveTaskUseCase: SaveTaskUseCase by inject()
    private val getTaskForEditUseCase: GetTaskForEditUseCase by inject()

    // UI state for the form
    private val _formState = MutableStateFlow(TaskFormState())
    val formState: StateFlow<TaskFormState> = _formState.asStateFlow()

    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Task being edited (null for new task)
    private var taskId: Long? = null

    // Load task data if editing
    fun loadTask(taskId: Long) {
        this.taskId = taskId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val task = getTaskForEditUseCase.execute(taskId)
                if (task != null) {
                    _formState.value = TaskFormState(
                        title = task.title,
                        description = task.description,
                        category = task.category,
                        priority = task.priority,
                        recurrence = task.recurrence,
                        reminderDate = task.reminderTime,
                        durationMinutes = task.durationMinutes.toString()
                    )
                } else {
                    _error.value = "Task not found"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load task: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update form field values
    fun updateTitle(title: String) {
        _formState.value = _formState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _formState.value = _formState.value.copy(description = description)
    }

    fun updateCategory(category: TaskCategory) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun updatePriority(priority: TaskPriority) {
        _formState.value = _formState.value.copy(priority = priority)
    }

    fun updateRecurrence(recurrence: TaskRecurrence) {
        _formState.value = _formState.value.copy(recurrence = recurrence)
    }

    fun updateReminderDate(date: Date) {
        _formState.value = _formState.value.copy(reminderDate = date)
    }

    fun updateDuration(duration: String) {
        _formState.value = _formState.value.copy(durationMinutes = duration)
    }

    // Validate form data
    fun validateForm(): Boolean {
        val state = _formState.value

        return state.title.isNotBlank() &&
                state.description.isNotBlank() &&
                state.durationMinutes.toIntOrNull() != null &&
                (state.durationMinutes.toIntOrNull() ?: 0) > 0
    }

    // Save task
    fun saveTask(context: Context, onSuccess: () -> Unit) {
        if (!validateForm()) {
            _error.value = "Please fill all required fields"
            return
        }

        _isLoading.value = true
        _error.value = null

        val state = _formState.value
        val durationMinutes = state.durationMinutes.toIntOrNull() ?: 0

        viewModelScope.launch {
            try {
                val task = Task(
                    id = taskId ?: 0,
                    userId = "", // will get added later at usecase level
                    title = state.title,
                    description = state.description,
                    category = state.category,
                    priority = state.priority,
                    recurrence = state.recurrence,
                    reminderTime = state.reminderDate,
                    durationMinutes = durationMinutes,
                    isCompleted = false,
                    isAccepted = false,
                    isRejected = false,
                    createdAt = Date(),
                    updatedAt = Date()
                )

                saveTaskUseCase.execute(task, context)
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Failed to save task: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

data class TaskFormState(
    val title: String = "",
    val description: String = "",
    val category: TaskCategory = TaskCategory.WORK,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val recurrence: TaskRecurrence = TaskRecurrence.ONCE,
    val reminderDate: Date = Date(System.currentTimeMillis() + 3600000), // 1 hour from now
    val durationMinutes: String = "30"
)