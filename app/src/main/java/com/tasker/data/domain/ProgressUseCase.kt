package com.tasker.data.domain

import com.tasker.data.model.DailyStat
import com.tasker.data.model.DateRangeType
import com.tasker.data.model.ProgressData
import com.tasker.data.model.SpecificTaskProgress
import com.tasker.data.model.TaskProgress
import com.tasker.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date

class GetProgressDataUseCase(
    private val taskRepository: TaskRepository
) {
    suspend fun execute(taskId: Long?, dateRangeType: DateRangeType): ProgressData {
        taskRepository.syncProgressData()

        val startDate = getStartDate(dateRangeType)
        val endDate = Date() // Current date

        return if (taskId == null) {
            val progressList = taskRepository.getProgressByDateRange(startDate, endDate).first()
            val dailyStats = calculateDailyStats(progressList, startDate, endDate)

            val allTasks = taskRepository.getAllTasks().first()
            val categoryCounts = allTasks.groupBy { it.category }
                .mapValues { it.value.size }
            val priorityCounts = allTasks.groupBy { it.priority }
                .mapValues { it.value.size }

            val completedCount = allTasks.count { it.isCompleted }
            val completionRate = if (allTasks.isNotEmpty()) {
                (completedCount.toFloat() / allTasks.size) * 100
            } else {
                0f
            }

            ProgressData(
                dailyStats = dailyStats,
                taskCompletionRate = completionRate,
                categoryCounts = categoryCounts,
                priorityCounts = priorityCounts,
                specificTaskProgress = null
            )
        } else {
            val task = taskRepository.getTaskById(taskId)
            if (task == null) {
                return ProgressData(
                    dailyStats = emptyList(),
                    taskCompletionRate = 0f,
                    categoryCounts = emptyMap(),
                    priorityCounts = emptyMap(),
                    specificTaskProgress = null
                )
            }

            val progressList = taskRepository.getProgressForTask(taskId).first()
            val specificProgress = SpecificTaskProgress(
                completedSessions = progressList.count { it.isCompleted },
                totalDuration = progressList.sumOf { it.durationCompleted ?: 0 },
                progressList = progressList.sortedByDescending { it.date }
            )

            ProgressData(
                dailyStats = emptyList(),
                taskCompletionRate = 0f,
                categoryCounts = emptyMap(),
                priorityCounts = emptyMap(),
                specificTaskProgress = specificProgress
            )
        }
    }

    private fun getStartDate(dateRangeType: DateRangeType): Date {
        val calendar = Calendar.getInstance()

        when (dateRangeType) {
            DateRangeType.DAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            DateRangeType.WEEK -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.add(Calendar.DAY_OF_MONTH, -(calendar.get(Calendar.DAY_OF_WEEK) - 1))
            }
            DateRangeType.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            DateRangeType.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        }

        return calendar.time
    }

    private fun calculateDailyStats(
        progressList: List<TaskProgress>,
        startDate: Date,
        endDate: Date
    ): List<DailyStat> {
        val result = mutableListOf<DailyStat>()
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        while (calendar.time.before(endDate) || calendar.time == endDate) {
            val currentDate = calendar.time

            val dayProgress = progressList.filter { isSameDay(it.date, currentDate) }
            val completedCount = dayProgress.count { it.isCompleted }
            val totalDuration = dayProgress.sumOf { it.durationCompleted ?: 0 }

            result.add(
                DailyStat(
                    date = Date(currentDate.time),
                    completedCount = completedCount,
                    totalDuration = totalDuration
                )
            )

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return result
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}