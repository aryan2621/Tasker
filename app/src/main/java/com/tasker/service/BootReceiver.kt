package com.tasker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tasker.data.repository.TaskRepository
import com.tasker.util.AlarmUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Date

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val taskRepository: TaskRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all pending tasks
            CoroutineScope(Dispatchers.IO).launch {
                val pendingTasks = taskRepository.getPendingTasks().first()

                for (task in pendingTasks) {
                    // Only reschedule if the reminder time is in the future
                    if (task.reminderTime.after(Date())) {
                        AlarmUtils.scheduleAlarm(
                            context,
                            task.id,
                            task.reminderTime,
                            task.recurrence
                        )
                    }
                }
            }
        }
    }
}