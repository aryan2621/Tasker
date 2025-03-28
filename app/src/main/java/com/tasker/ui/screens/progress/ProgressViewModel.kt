package com.tasker.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.data.domain.GetProgressDataUseCase
import com.tasker.data.model.DateRangeType
import com.tasker.data.model.ProgressData
import com.tasker.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProgressViewModel : ViewModel(), KoinComponent {

    private val getProgressDataUseCase: GetProgressDataUseCase by inject()
    private val taskRepository: TaskRepository by inject()

    private val _selectedTaskId = MutableStateFlow<Long?>(null)

    private val _dateRangeType = MutableStateFlow(DateRangeType.WEEK)
    val dateRangeType: StateFlow<DateRangeType> = _dateRangeType.asStateFlow()

    val selectedTask = _selectedTaskId.combine(taskRepository.getAllTasks()) { taskId, tasks ->
        if (taskId == null) null else tasks.find { it.id == taskId }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _progressData = MutableStateFlow(
        ProgressData(
            dailyStats = emptyList(),
            taskCompletionRate = 0f,
            categoryCounts = emptyMap(),
            priorityCounts = emptyMap(),
            specificTaskProgress = null
        )
    )
    val progressData: StateFlow<ProgressData> = _progressData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Load data whenever task ID or date range changes
    init {
        viewModelScope.launch {
            combine(_selectedTaskId, _dateRangeType) { taskId, dateRange ->
                Pair(taskId, dateRange)
            }.collect { (taskId, dateRange) ->
                loadProgressData(taskId, dateRange)
            }
        }
    }

    private suspend fun loadProgressData(taskId: Long?, dateRangeType: DateRangeType) {
        _isLoading.value = true
        try {
            val data = getProgressDataUseCase.execute(taskId, dateRangeType)
            _progressData.value = data
        } catch (e: Exception) {
            // Error handling
        } finally {
            _isLoading.value = false
        }
    }

    fun setSelectedTaskId(taskId: Long?) {
        _selectedTaskId.value = taskId
    }

    fun setDateRangeType(type: DateRangeType) {
        _dateRangeType.value = type
    }
}