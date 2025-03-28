package com.tasker.data.domain

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.tasker.data.model.Task
import com.tasker.data.repository.AuthRepository
import com.tasker.data.repository.TaskRepository
import com.tasker.util.AlarmUtils


class SaveTaskUseCase(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository
) {
    suspend fun execute(task: Task, context: Context): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                throw SecurityException("Notification permission not granted")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                throw SecurityException("Exact alarm permission not granted")
            }
        }

        // Make sure the task has the current user's ID
        val userId = authRepository.getCurrentUserId() ?: throw IllegalStateException("User not logged in")
        val taskWithUserId = if (task.id == 0L || task.userId.isNullOrEmpty()) {
            task.copy(userId = userId)
        } else {
            task
        }

        val taskId = if (taskWithUserId.id == 0L) {
            taskRepository.insertTask(taskWithUserId)
        } else {
            taskRepository.updateTask(taskWithUserId)
            taskWithUserId.id
        }

        AlarmUtils.scheduleAlarm(
            context,
            taskId,
            taskWithUserId.reminderTime,
            taskWithUserId.recurrence
        )
        taskRepository.syncData()
        return taskId
    }
}

class GetTaskForEditUseCase(
    private val taskRepository: TaskRepository
) {
    suspend fun execute(taskId: Long): Task? {
        return taskRepository.getTaskById(taskId)
    }
}