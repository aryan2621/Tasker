package com.tasker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tasker.R
import com.tasker.ui.navigation.Screen
import com.tasker.ui.screens.auth.AuthViewModel
import com.tasker.ui.screens.auth.LoginScreen
import com.tasker.ui.screens.auth.RegisterScreen
import com.tasker.ui.screens.home.HomeScreen
import com.tasker.ui.screens.profile.ProfileScreen
import com.tasker.ui.screens.progress.ProgressScreen
import com.tasker.ui.screens.taskdetail.TaskDetailScreen
import com.tasker.ui.screens.taskform.TaskFormScreen

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    startTaskId: Long = -1L,
) {
    val viewModel: AuthViewModel = viewModel()
    val authState by viewModel.uiState.collectAsState()
    var isInitialAuthCheckDone by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (!authState.isLoading) isInitialAuthCheckDone = true
    }

    if (!isInitialAuthCheckDone) {
        LoadingScreen()
        return
    }

    val startDestination = if (authState.isLoggedIn) Screen.Home.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        authScreens(navController)
        mainScreens(navController)
    }

    LaunchedEffect(startTaskId, authState.isLoggedIn) {
        if (startTaskId != -1L && authState.isLoggedIn) {
            navController.navigate(Screen.TaskDetail.createRoute(startTaskId))
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

fun NavGraphBuilder.authScreens(navController: NavHostController) {
    composable(Screen.Login.route) {
        LoginScreen(
            onNavigateToRegister = { navController.navigate(Screen.Register.route) },
            onLoginSuccess = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        )
    }
    composable(Screen.Register.route) {
        RegisterScreen(
            onNavigateBack = { navController.popBackStack() },
            onRegisterSuccess = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Register.route) { inclusive = true }
                }
            }
        )
    }
}

fun NavGraphBuilder.mainScreens(navController: NavHostController) {
    composable(Screen.Home.route) {
        val innerNavController = rememberNavController()

        Scaffold(bottomBar = { TaskerBottomBar(innerNavController) }) { innerPadding ->
            NavHost(
                    navController = innerNavController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                            onTaskClick = {
                                navController.navigate(Screen.TaskDetail.createRoute(it))
                            },
                            onAddTaskClick = {
                                navController.navigate(Screen.TaskForm.createRoute())
                            }
                    )
                }
                composable(route = Screen.Progress.route) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getString("taskId")?.toLong() ?: -1L
                    ProgressScreen(
                        taskId = if (taskId == -1L) null else taskId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                            onBack = { innerNavController.popBackStack() },
                            onLogout = { navController.navigate(Screen.Login.route) },
                            onNavigateToAchievements = {
                                navController.navigate(Screen.Achievements.route)
                            }
                    )
                }
            }
        }
    }

    composable(Screen.TaskDetail.route) { backStackEntry ->
        val taskId = backStackEntry.arguments?.getString("taskId")?.toLong() ?: -1L
        TaskDetailScreen(
            taskId = taskId,
            onEditTask = { navController.navigate(Screen.TaskForm.createRoute(taskId)) },
            onBack = { navController.popBackStack() },
            onViewProgress = { navController.navigate(Screen.Progress.createRoute(taskId)) }
        )
    }

    composable(Screen.TaskForm.route) { backStackEntry ->
        val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
        TaskFormScreen(
            taskId = if (taskId == -1L) null else taskId,
            onTaskSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }
}


@Composable
fun TaskerBottomBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Home", Screen.Home.route, R.drawable.ic_home),
        BottomNavItem("Progress", Screen.Progress.route, R.drawable.ic_show_chart),
        BottomNavItem("Profile", Screen.Profile.route, R.drawable.ic_person)
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 8.dp) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = { BottomBarIcon(item, currentRoute == item.route) },
                    label = { BottomBarLabel(item, currentRoute == item.route) },
                    selected = currentRoute == item.route,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.background,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

@Composable
fun BottomBarIcon(item: BottomNavItem, selected: Boolean) {
    if (selected) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = item.icon),
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        Icon(
            painter = painterResource(id = item.icon),
            contentDescription = item.title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun BottomBarLabel(item: BottomNavItem, selected: Boolean) {
    Text(
        text = item.title,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = 12.sp
    )
}

data class BottomNavItem(val title: String, val route: String, val icon: Int)