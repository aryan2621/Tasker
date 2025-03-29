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

    // Enhanced offline sync support
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val lastSyncAttempt: Date? = null,
    val serverUpdatedAt: Date? = null,
    val syncErrorMessage: String? = null,
    val localModifiedAt: Date = Date(),
    val isDeletedLocally: Boolean = false
)