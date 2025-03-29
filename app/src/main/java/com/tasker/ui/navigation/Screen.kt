package com.tasker.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: Long) = "task_detail/$taskId"
    }
    data object TaskForm : Screen("task_form?taskId={taskId}") {
        fun createRoute(taskId: Long? = null) =
            if (taskId != null) "task_form?taskId=$taskId" else "task_form"
    }
    data object Progress : Screen("progress?taskId={taskId}") {
        fun createRoute(taskId: Long? = null) =
            if (taskId != null) "progress?taskId=$taskId" else "progress"
    }
    data object TaskProgress : Screen("task_progress/{taskId}") {
        fun createRoute(taskId: Long): String = "task_progress/$taskId"
    }
    data object Profile : Screen("profile")
    data object Achievements : Screen("achievements")
}