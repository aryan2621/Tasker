package com.tasker.data.model

import java.util.Date

data class DailyStat(
    val date: Date,
    val completedCount: Int,
    val totalDuration: Int
)