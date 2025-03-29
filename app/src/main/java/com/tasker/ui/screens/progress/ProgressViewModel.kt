package com.tasker.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.data.domain.GetProgressDataUseCase
import com.tasker.data.model.DateRangeType
import com.tasker.data.model.ProgressData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProgressViewModel : ViewModel(), KoinComponent {

    private val getProgressDataUseCase: GetProgressDataUseCase by inject()

    private val _dateRangeType = MutableStateFlow(DateRangeType.DAY)
    val dateRangeType: StateFlow<DateRangeType> = _dateRangeType.asStateFlow()

    private val _progressData = MutableStateFlow(
        ProgressData(
            dailyStats = emptyList(),
            taskCompletionRate = 0f,
            categoryCounts = emptyMap(),
            priorityCounts = emptyMap(),
        )
    )
    val progressData: StateFlow<ProgressData> = _progressData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Load data whenever date range changes
    init {
        viewModelScope.launch {
            _dateRangeType
                .collect { dateRange ->
                    loadProgressData(dateRange)
                }
        }
    }

    private suspend fun loadProgressData(dateRangeType: DateRangeType) {
        _isLoading.value = true
        try {
            // Pass null for taskId to get general progress data
            val data = getProgressDataUseCase.execute(taskId = null, dateRangeType)
            _progressData.value = data
        } catch (e: Exception) {
            // Error handling could be added here
        } finally {
            _isLoading.value = false
        }
    }

    fun setDateRangeType(type: DateRangeType) {
        _dateRangeType.value = type
    }
}