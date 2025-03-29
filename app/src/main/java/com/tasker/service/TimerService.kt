package com.tasker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tasker.MainActivity
import com.tasker.R
import com.tasker.data.model.TaskProgress
import com.tasker.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Date

class TimerService : Service(), KoinComponent {

    private val taskRepository: TaskRepository by inject()
    private var countDownTimer: CountDownTimer? = null
    private var taskId: Long = -1
    private var startTime: Long = 0
    private var remainingTimeMillis: Long = 0
    private var isPaused: Boolean = false

    companion object {
        const val CHANNEL_ID = "timer_service"
        const val NOTIFICATION_ID = 2000
        const val ACTION_STOP = "com.tasker.ACTION_STOP"
        const val ACTION_PAUSE = "com.tasker.ACTION_PAUSE"
        const val ACTION_RESUME = "com.tasker.ACTION_RESUME"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_STOP -> {
                    stopTimer()
                    // Stop background music
                    MusicService.stopMusic(this)
                    stopSelf()
                    return START_NOT_STICKY
                }
                ACTION_PAUSE -> {
                    pauseTimer()
                    return START_STICKY
                }
                ACTION_RESUME -> {
                    resumeTimer()
                    return START_STICKY
                }
            }

            taskId = intent.getLongExtra("taskId", -1)
            val durationMinutes = intent.getIntExtra("durationMinutes", 0)

            if (taskId != -1L && durationMinutes > 0) {
                startForegroundService()
                startTimer(durationMinutes)
            } else {
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()

        // Create the common intents
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create pause intent
        val pauseIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content intent to open the app
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            taskId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Task Timer")
            .setContentText("Task in progress...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_pause, // You'll need to add this icon
                "Pause",
                pausePendingIntent
            )
            .addAction(
                R.drawable.ic_stop,
                "Stop",
                stopPendingIntent
            )
            .build()

        // For Android Q (API 29) and above, specify the foreground service type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID + taskId.toInt(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID + taskId.toInt(), notification)
        }
    }

    private fun updatePausedNotification() {
        // Create resume intent
        val resumeIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getService(
            this,
            2,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create stop intent
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content intent to open the app
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            taskId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Task Timer - PAUSED")
            .setContentText("Task is paused. Resume to continue.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_play, // You'll need to add this icon
                "Resume",
                resumePendingIntent
            )
            .addAction(
                R.drawable.ic_stop,
                "Stop",
                stopPendingIntent
            )
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + taskId.toInt(), notification)
    }

    private fun startTimer(durationMinutes: Int) {
        startTime = System.currentTimeMillis()
        val totalTime = durationMinutes * 60 * 1000L

        // If we're starting fresh, set the remaining time to the full duration
        if (!isPaused) {
            remainingTimeMillis = totalTime
        }

        countDownTimer = object : CountDownTimer(remainingTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Save remaining time in case we need to pause
                remainingTimeMillis = millisUntilFinished
                // Update notification with remaining time
                updateNotification(millisUntilFinished)
            }

            override fun onFinish() {
                // Task completed
                completeTask()
                // Stop background music
                MusicService.stopMusic(this@TimerService)
                stopSelf()
            }
        }.start()
    }

    private fun pauseTimer() {
        // Cancel the current timer but preserve the remaining time
        countDownTimer?.cancel()
        isPaused = true

        // Pause the background music
        MusicService.pauseMusic(this)

        // Update notification to show paused state
        updatePausedNotification()
    }

    private fun resumeTimer() {
        isPaused = false

        // Resume the background music
        MusicService.resumeMusic(this)

        // Restart timer with remaining time
        startTimer(0) // The duration is ignored since we're using remainingTimeMillis
    }

    private fun updateNotification(millisUntilFinished: Long) {
        val minutes = millisUntilFinished / (60 * 1000)
        val seconds = (millisUntilFinished % (60 * 1000)) / 1000

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Task Timer")
            .setContentText("Remaining: $minutes min $seconds sec")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + taskId.toInt(), notification)
    }

    private fun stopTimer() {
        countDownTimer?.cancel()

        if (taskId != -1L) {
            // Record the progress
            val endTime = System.currentTimeMillis()
            val durationCompleted = ((endTime - startTime) / (60 * 1000)).toInt()

            CoroutineScope(Dispatchers.IO).launch {
                // Create progress record
                val progress = TaskProgress(
                    taskId = taskId,
                    isCompleted = false, // Task wasn't completed
                    startTime = startTime,
                    endTime = endTime,
                    durationCompleted = durationCompleted
                )
                taskRepository.insertProgress(progress)
            }
        }
    }

    private fun completeTask() {
        if (taskId != -1L) {
            CoroutineScope(Dispatchers.IO).launch {
                val task = taskRepository.getTaskById(taskId)

                if (task != null) {
                    // Update task as completed
                    taskRepository.updateTask(
                        task.copy(
                            isCompleted = true,
                            completedAt = Date()
                        )
                    )

                    // Create progress record
                    val endTime = System.currentTimeMillis()
                    val progress = TaskProgress(
                        taskId = taskId,
                        isCompleted = true,
                        startTime = startTime,
                        endTime = endTime,
                        durationCompleted = task.durationMinutes
                    )
                    taskRepository.insertProgress(progress)

                    // Show completion notification
                    showCompletionNotification()
                }
            }
        }
    }

    private fun showCompletionNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_complete)
            .setContentTitle("Task Completed")
            .setContentText("Congratulations! You've completed your task.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1000 + taskId.toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Timer"
            val descriptionText = "Shows timer for ongoing tasks"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        // Ensure music is stopped when service is destroyed
        MusicService.stopMusic(this)
        super.onDestroy()
    }
}