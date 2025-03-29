package com.tasker.data.model

data class ProgressData(
    val dailyStats: List<DailyStat>,
    val taskCompletionRate: Float,
    val categoryCounts: Map<TaskCategory, Int>,
    val priorityCounts: Map<TaskPriority, Int>,
)

enum class DateRangeType {
    DAY, WEEK, MONTH, YEAR
}
data class SpecificTaskProgressData(
    val completedSessions: Int,
    val totalDuration: Long,
    val progressList: List<TaskProgress>
)