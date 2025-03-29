package com.tasker.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tasker.MainActivity
import com.tasker.R
import com.tasker.data.model.Task
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Service utility to handle running pending tasks directly from the app
 */
object RunPendingTaskService {

    private const val CHANNEL_ID = "pending_tasks_channel"
    private const val PENDING_TASK_NOTIFICATION_ID = 3000
    private var isAppInForeground = true

    // Set app foreground state - call this from activity lifecycle
    fun setAppForegroundState(isInForeground: Boolean) {
        isAppInForeground = isInForeground

        // If app went to foreground, we can dismiss running notifications
        // as the user is now looking at the task in the app
        if (isInForeground) {
            // No need to do anything, the notification will be updated/shown
            // when the app goes to background again
        }
    }

    // Create notification channel for Android O and above
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pending Tasks"
            val descriptionText = "Notifications for pending tasks that can be started now"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText

                setSound(null, null)

                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskRunningNotification(context: Context, task: Task) {
        if (isAppInForeground) {
            MusicService.playAcceptMusic(context)
            return
        }

        // Create intent to open task in progress screen
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_OPEN_TASK_PROGRESS"
            putExtra("taskId", task.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create pause intent
        val pauseIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_PAUSE_TASK
            putExtra("taskId", task.id)
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt() + 1000,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create complete intent
        val completeIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_COMPLETE_TASK
            putExtra("taskId", task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt() + 2000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with actions
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task Running: ${task.title}")
            .setContentText("Duration: ${task.durationMinutes} minutes")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("\"${task.title}\" is now running. Tap to view progress or control the timer."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Make it persistent
            .addAction(
                R.drawable.ic_pause,
                "Pause",
                pausePendingIntent
            )
            .addAction(
                R.drawable.ic_complete,
                "Complete",
                completePendingIntent
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setColorized(true)
            .setColor(0xFF4CAF50.toInt()) // Green color for active tasks
            .build()

        // Show notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use a special notification ID for running tasks
        notificationManager.notify(PENDING_TASK_NOTIFICATION_ID + 5000 + task.id.toInt(), notification)

        // Start playing the task accepted music
        MusicService.playAcceptMusic(context)
    }

    fun updateRunningTaskNotification(context: Context, task: Task, remainingSeconds: Int) {
        // Only update if app is in background
        if (isAppInForeground) return

        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeString = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        // Create intent to open task in progress screen
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_OPEN_TASK_PROGRESS"
            putExtra("taskId", task.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create pause intent
        val pauseIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_PAUSE_TASK
            putExtra("taskId", task.id)
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt() + 1000,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create complete intent
        val completeIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_COMPLETE_TASK
            putExtra("taskId", task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt() + 2000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with updated time
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task Running: ${task.title}")
            .setContentText("Remaining: $timeString")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("\"${task.title}\" is running.\nRemaining time: $timeString"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Make it persistent
            .addAction(
                R.drawable.ic_pause,
                "Pause",
                pausePendingIntent
            )
            .addAction(
                R.drawable.ic_complete,
                "Complete",
                completePendingIntent
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setColorized(true)
            .setColor(0xFF4CAF50.toInt()) // Green color for active tasks
            .build()

        // Show notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(PENDING_TASK_NOTIFICATION_ID + 5000 + task.id.toInt(), notification)
    }

    fun updatePausedTaskNotification(context: Context, task: Task, remainingSeconds: Int) {
        if (isAppInForeground) return

        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeString = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_OPEN_TASK_PROGRESS"
            putExtra("taskId", task.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_RESUME_TASK
            putExtra("taskId", task.id)
        }
        val resumePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt() + 3000,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_COMPLETE_TASK
            putExtra("taskId", task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt() + 2000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task Paused: ${task.title}")
            .setContentText("Paused - Remaining: $timeString")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("\"${task.title}\" is paused.\nRemaining time: $timeString"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Make it persistent
            .addAction(
                R.drawable.ic_play,
                "Resume",
                resumePendingIntent
            )
            .addAction(
                R.drawable.ic_complete,
                "Complete",
                completePendingIntent
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setColorized(true)
            .setColor(0xFFFF9800.toInt()) // Orange color for paused tasks
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(PENDING_TASK_NOTIFICATION_ID + 5000 + task.id.toInt(), notification)
    }

    fun showPendingTaskNotification(context: Context, task: Task) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_OPEN_TASK_PROGRESS"
            putExtra("taskId", task.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeUntilDue = task.reminderTime.time - System.currentTimeMillis()
        val minutesUntilDue = TimeUnit.MILLISECONDS.toMinutes(timeUntilDue)

        val timeMessage = when {
            timeUntilDue < 0 -> "Overdue"
            minutesUntilDue < 60 -> "$minutesUntilDue minutes from now"
            else -> "${TimeUnit.MILLISECONDS.toHours(timeUntilDue)} hours from now"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Start Task: ${task.title}")
            .setContentText("Scheduled: $timeMessage (${task.durationMinutes} min)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("This task is scheduled $timeMessage. Start it now with a ${task.durationMinutes} minute timer?"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_play,
                "Start Now",
                pendingIntent
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(PENDING_TASK_NOTIFICATION_ID + task.id.toInt(), notification)
    }

    fun showTaskCompletedNotification(context: Context, task: Task) {

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_complete)
            .setContentTitle("Task Completed!")
            .setContentText("You've completed: ${task.title}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Congratulations! You've completed \"${task.title}\"."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setColorized(true)
            .setColor(0xFF4CAF50.toInt()) // Green for completed tasks
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(PENDING_TASK_NOTIFICATION_ID + 7000 + task.id.toInt(), notification)
    }

    fun clearRunningTaskNotification(context: Context, taskId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(PENDING_TASK_NOTIFICATION_ID + 5000 + taskId.toInt())
    }

    fun handleNotificationIntent(context: Context, intent: Intent?, onTaskSelected: (Long) -> Unit) {
        if (intent?.action == "ACTION_OPEN_TASK_PROGRESS") {
            val taskId = intent.getLongExtra("taskId", -1L)
            if (taskId != -1L) {
                onTaskSelected(taskId)
            }
        }
    }
}