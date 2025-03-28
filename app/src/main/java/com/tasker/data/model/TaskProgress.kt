package com.tasker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "task_progress")
data class TaskProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val date: Date = Date(),
    val isCompleted: Boolean,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val durationCompleted: Int? = null,
    val isSynced: Boolean = false
)