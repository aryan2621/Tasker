package com.tasker.data.domain

import com.tasker.data.model.DailyStat
import com.tasker.data.model.DateRangeType
import com.tasker.data.model.ProgressData
import com.tasker.data.model.TaskProgress
import com.tasker.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date

class GetProgressDataUseCase(
    private val taskRepository: TaskRepository
) {
    suspend fun execute(taskId: Long? = null, dateRangeType: DateRangeType): ProgressData {
        taskRepository.syncProgressData()

        val startDate = getStartDate(dateRangeType)
        val endDate = Date() // Current date

        // For the simplified version, we'll only handle general progress data
        val progressList = taskRepository.getProgressByDateRange(startDate, endDate).first()
        val dailyStats = calculateDailyStats(progressList, startDate, endDate, dateRangeType)

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

        return ProgressData(
            dailyStats = dailyStats,
            taskCompletionRate = completionRate,
            categoryCounts = categoryCounts,
            priorityCounts = priorityCounts,
        )
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
                // Go back to the first day of the week (assuming Sunday is first day)
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
        endDate: Date,
        dateRangeType: DateRangeType
    ): List<DailyStat> {
        val result = mutableListOf<DailyStat>()
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        // For DAY view, create hourly stats
        if (dateRangeType == DateRangeType.DAY) {
            // Set to beginning of the day
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // Create hourly slots (use fewer slots for better UI)
            // Using 6 slots (4 hour intervals) instead of 24 for cleaner UI
            for (slot in 0 until 6) {
                val hour = slot * 4 // 0, 4, 8, 12, 16, 20
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                val slotStart = calendar.time

                // Move to end of 4-hour slot
                calendar.add(Calendar.HOUR_OF_DAY, 4)
                val slotEnd = calendar.time

                // Find progress for this time slot
                val slotProgress = progressList.filter {
                    it.date.time >= slotStart.time && it.date.time < slotEnd.time
                }

                val completedCount = slotProgress.count { it.isCompleted }
                val totalDuration = slotProgress.sumOf { it.durationCompleted ?: 0 }

                result.add(
                    DailyStat(
                        date = Date(slotStart.time),
                        completedCount = completedCount,
                        totalDuration = totalDuration
                    )
                )

                // Reset for next iteration
                calendar.time = startDate
            }
        } else {
            // For WEEK view, create daily stats
            val endCalendar = Calendar.getInstance()
            endCalendar.time = endDate

            // Set start time to beginning of day
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // Loop through each day in the week
            while (calendar.time.before(endCalendar.time) || isSameDay(calendar.time, endCalendar.time)) {
                val dayStart = calendar.time

                // Find progress for this day
                val dayProgress = progressList.filter {
                    isSameDay(it.date, dayStart)
                }

                val completedCount = dayProgress.count { it.isCompleted }
                val totalDuration = dayProgress.sumOf { it.durationCompleted ?: 0 }

                result.add(
                    DailyStat(
                        date = Date(dayStart.time),
                        completedCount = completedCount,
                        totalDuration = totalDuration
                    )
                )

                // Move to next day
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
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