package com.tasker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tasker.data.model.TaskRecurrence
import com.tasker.service.AlarmReceiver
import java.util.Calendar
import java.util.Date

object AlarmUtils {

    private const val REQUEST_CODE_PREFIX = 100

    fun scheduleAlarm(context: Context, taskId: Long, alarmTime: Date, recurrence: TaskRecurrence) {
        // Add validation for alarm time
        if (alarmTime.time <= System.currentTimeMillis()) {
            return  // Don't schedule if the time has already passed
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("recurrence", recurrence.name)  // Store the recurrence type in the intent
        }

        // Create a unique request code for this task
        val requestCode = REQUEST_CODE_PREFIX + taskId.toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        when (recurrence) {
            TaskRecurrence.ONCE -> {
                // Set one-time alarm
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime.time,
                    pendingIntent
                )
            }
            TaskRecurrence.DAILY -> {
                // Set daily recurring alarm
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime.time,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
            TaskRecurrence.WEEKLY -> {
                // Set weekly recurring alarm
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime.time,
                    7 * AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
            TaskRecurrence.MONTHLY -> {
                // For monthly recurrence, we'll need to handle the next occurrence
                // in the AlarmReceiver after the first alarm triggers
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime.time,
                    pendingIntent
                )
            }
        }
    }

    fun calculateNextMonthlyAlarm(baseTime: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = baseTime

        // Store the current day of month
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        // Move to next month
        calendar.add(Calendar.MONTH, 1)

        // Adjust for shorter months (e.g., Jan 31 -> Feb 28)
        val maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (dayOfMonth > maxDaysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, maxDaysInMonth)
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }

        return calendar.time
    }

    fun cancelAlarm(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val requestCode = REQUEST_CODE_PREFIX + taskId.toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}