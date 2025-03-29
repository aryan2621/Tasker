package com.tasker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tasker.data.sync.WorkManagerProvider
import com.tasker.service.RunPendingTaskService
import com.tasker.ui.screens.MainScreen
import com.tasker.ui.theme.TaskerTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val workManagerProvider: WorkManagerProvider by inject()
    private var pendingTaskId: Long = -1L
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        workManagerProvider.initialize()

        RunPendingTaskService.createNotificationChannel(this)
        val taskId = intent.getLongExtra("taskId", -1L)

        handleIntent(intent)?.let {
            pendingTaskId = it
        }

        setContent {
            TaskerTheme {
                MainScreen(
                    startTaskId = if (pendingTaskId != -1L) pendingTaskId else taskId,
                    isProgressScreen = pendingTaskId != -1L
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)?.let { taskId ->
            setContent {
                TaskerTheme {
                    MainScreen(
                        startTaskId = taskId,
                        isProgressScreen = true
                    )
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?): Long? {
        var resultTaskId: Long? = null
        RunPendingTaskService.handleNotificationIntent(
            context = this,
            intent = intent,
            onTaskSelected = { taskId ->
                resultTaskId = taskId
            }
        )
        return resultTaskId
    }
    override fun onResume() {
        super.onResume()
        RunPendingTaskService.setAppForegroundState(true)
    }

    override fun onPause() {
        super.onPause()
        RunPendingTaskService.setAppForegroundState(false)
    }
}