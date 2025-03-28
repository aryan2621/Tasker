package com.tasker.data.model

data class SpecificTaskProgress(
    val completedSessions: Int,
    val totalDuration: Int,
    val progressList: List<TaskProgress>
)