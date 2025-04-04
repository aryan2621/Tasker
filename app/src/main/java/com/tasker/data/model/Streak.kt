package com.tasker.data.model
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_streaks")
data class UserStreak(
    @PrimaryKey
    val userId: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: Date? = null,

    // Enhanced offline sync support
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val lastSyncAttempt: Date? = null,
    val serverUpdatedAt: Date? = null,
    val syncErrorMessage: String? = null,
    val localModifiedAt: Date = Date(),
    val isDeletedLocally: Boolean = false
)