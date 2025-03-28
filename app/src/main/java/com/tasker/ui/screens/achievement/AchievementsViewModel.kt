package com.tasker.ui.screens.achievement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.data.domain.GetAchievementsUseCase
import com.tasker.data.model.Achievement
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AchievementsViewModel : ViewModel(), KoinComponent {

    private val getAchievementsUseCase: GetAchievementsUseCase by inject()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                getAchievementsUseCase.execute().collect { achievementList ->
                    _achievements.value = achievementList.sortedByDescending { it.earnedAt }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load achievements: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        loadAchievements()
    }
}