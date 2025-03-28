package com.tasker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tasker.ui.screens.MainScreen
import com.tasker.ui.theme.TaskerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getLongExtra("taskId", -1L)

        setContent {
            TaskerTheme {
                MainScreen(
                    startTaskId = taskId,
                )
            }
        }
    }
}
