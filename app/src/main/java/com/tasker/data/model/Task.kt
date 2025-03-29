package com.tasker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

enum class TaskPriority {
    HIGH, MEDIUM, LOW
}

enum class TaskCategory {
    WORK, STUDY, HEALTH, PERSONAL, CUSTOM
}

enum class TaskRecurrence {
    DAILY, WEEKLY, MONTHLY, ONCE
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var userId: String,
    val title: String,
    val description: String,
    val category: TaskCategory,
    val priority: TaskPriority,
    val recurrence: TaskRecurrence,
    val reminderTime: Date,
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val isAccepted: Boolean = false,
    val isRejected: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val completedAt: Date? = null,

    // Enhanced offline sync support
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val lastSyncAttempt: Date? = null,
    val serverUpdatedAt: Date? = null,
    val syncErrorMessage: String? = null,
    val localModifiedAt: Date = Date(),
    val isDeletedLocally: Boolean = false
)