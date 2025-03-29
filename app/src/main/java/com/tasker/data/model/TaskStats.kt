package com.tasker.data.model

data class TaskStats(
    val pendingCount: Int,
    val completedCount: Int,
    val totalCount: Int,
    val completedToday: Int
)