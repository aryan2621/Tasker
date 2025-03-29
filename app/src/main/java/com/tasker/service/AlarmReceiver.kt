package com.tasker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.tasker.MainActivity
import com.tasker.R
import com.tasker.data.model.TaskRecurrence
import com.tasker.data.repository.TaskRepository
import com.tasker.util.AlarmUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val taskRepository: TaskRepository by inject()

    companion object {
        const val CHANNEL_ID = "task_notifications"
        const val NOTIFICATION_ID = 1000
        const val ACTION_ACCEPT = "com.tasker.ACTION_ACCEPT"
        const val ACTION_REJECT = "com.tasker.ACTION_REJECT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1)
        val recurrenceStr = intent.getStringExtra("recurrence") ?: TaskRecurrence.ONCE.name
        val recurrence = TaskRecurrence.valueOf(recurrenceStr)

        if (taskId != -1L) {
            // Add wake lock to ensure processing completes
            val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tasker:AlarmReceiver")
            wakeLock.acquire(10*60*1000L /*10 minutes*/)

            try {
                when (intent.action) {
                    ACTION_ACCEPT -> handleAcceptAction(context, taskId)
                    ACTION_REJECT -> handleRejectAction(context, taskId)
                    else -> {
                        showTaskNotification(context, taskId)

                        // Handle recurring task if it's monthly (daily/weekly are handled by AlarmManager)
                        if (recurrence == TaskRecurrence.MONTHLY) {
                            handleMonthlyRecurrence(context, taskId)
                        }
                    }
                }
            } finally {
                wakeLock.release()
            }
        }
    }

    private fun handleMonthlyRecurrence(context: Context, taskId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val task = taskRepository.getTaskById(taskId)

            task?.let {
                // Calculate next month's date
                val nextAlarmTime = AlarmUtils.calculateNextMonthlyAlarm(it.reminderTime)

                // Update task with new reminder time
                taskRepository.updateTask(it.copy(reminderTime = nextAlarmTime))

                // Schedule the next monthly alarm
                AlarmUtils.scheduleAlarm(
                    context,
                    taskId,
                    nextAlarmTime,
                    TaskRecurrence.MONTHLY
                )
            }
        }
    }

    private fun showTaskNotification(context: Context, taskId: Long) {
        // Launch a coroutine to get the task details
        CoroutineScope(Dispatchers.IO).launch {
            val task = taskRepository.getTaskById(taskId)

            if (task != null) {
                // Create notification channel for Android O and above
                createNotificationChannel(context)

                // Create accept intent
                val acceptIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ACCEPT
                    putExtra("taskId", taskId)
                }
                val acceptPendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId.toInt() + 1,
                    acceptIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Create reject intent
                val rejectIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_REJECT
                    putExtra("taskId", taskId)
                }
                val rejectPendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId.toInt() + 2,
                    rejectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Content intent to open the app
                val contentIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("taskId", taskId)
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    taskId.toInt(),
                    contentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Build notification
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(task.title)
                    .setContentText(task.description)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(contentPendingIntent)
                    .setAutoCancel(true)
                    .addAction(
                        R.drawable.ic_accept,
                        "Accept",
                        acceptPendingIntent
                    )
                    .addAction(
                        R.drawable.ic_reject,
                        "Reject",
                        rejectPendingIntent
                    )
                    .build()

                // Show notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID + taskId.toInt(), notification)
            }
        }
    }

    private fun handleAcceptAction(context: Context, taskId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val task = taskRepository.getTaskById(taskId)

            if (task != null) {
                // Update task as accepted
                taskRepository.updateTask(task.copy(isAccepted = true))

                // Play accept music in background
                MusicService.playAcceptMusic(context)

                // Start the timer service
                val timerIntent = Intent(context, TimerService::class.java).apply {
                    putExtra("taskId", taskId)
                    putExtra("durationMinutes", task.durationMinutes)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(timerIntent)
                } else {
                    context.startService(timerIntent)
                }

                // Cancel the notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID + taskId.toInt())
            }
        }
    }

    private fun handleRejectAction(context: Context, taskId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val task = taskRepository.getTaskById(taskId)

            if (task != null) {
                // Update task as rejected
                taskRepository.updateTask(task.copy(isRejected = true))

                // Play reject sound
                MusicService.playRejectSound(context)

                // Cancel the notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID + taskId.toInt())
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Reminders"
            val descriptionText = "Notifications for task reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}