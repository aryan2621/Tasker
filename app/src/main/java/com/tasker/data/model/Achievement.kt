package com.tasker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val type: AchievementType,
    val title: String,
    val description: String,
    val earnedAt: Date = Date(),
    val isSynced: Boolean = false
)

enum class AchievementType {
    STREAK_MILESTONE,    // Achieved a certain streak length
    TASK_COUNT,          // Completed a certain number of tasks
    CATEGORY_MASTER,     // Completed many tasks in a specific category
    PERFECT_WEEK,        // Completed all tasks in a week
    EARLY_BIRD,          // Completed tasks before scheduled time
    CONSISTENCY          // Completed tasks on same time consistently
}