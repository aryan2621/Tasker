package com.tasker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import com.tasker.ui.components.sync.OfflinePill
import com.tasker.ui.components.sync.SyncViewModel
import com.tasker.ui.navigation.Screen
import com.tasker.ui.screens.achievement.AchievementsScreen // Add this import
import com.tasker.ui.screens.auth.AuthViewModel
import com.tasker.ui.screens.auth.LoginScreen
import com.tasker.ui.screens.auth.RegisterScreen
import com.tasker.ui.screens.currenttask.ActiveTaskProgressScreen
import com.tasker.ui.screens.home.HomeScreen
import com.tasker.ui.screens.profile.ProfileScreen
import com.tasker.ui.screens.progress.ProgressScreen
import com.tasker.ui.screens.taskdetail.TaskDetailScreen
import com.tasker.ui.screens.taskform.TaskFormScreen

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    startTaskId: Long = -1L,
    isProgressScreen: Boolean = false // New parameter
) {
    val viewModel: AuthViewModel = viewModel()
    val authState by viewModel.uiState.collectAsState()
    var isInitialAuthCheckDone by remember { mutableStateOf(false) }

    val syncViewModel: SyncViewModel = viewModel()
    val isConnected by syncViewModel.isNetworkConnected.collectAsState(initial = true)

    LaunchedEffect(authState) {
        if (!authState.isLoading) isInitialAuthCheckDone = true
    }

    if (!isInitialAuthCheckDone) {
        LoadingScreen()
        return
    }

    val startDestination = if (authState.isLoggedIn) Screen.Home.route else Screen.Login.route

    Scaffold(
        bottomBar = {
            // Only show bottom bar on main screens
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            val showBottomBar = remember(currentRoute) {
                currentRoute in listOf(Screen.Home.route, Screen.Progress.route, Screen.Profile.route)
            }

            if (showBottomBar) {
                TaskerBottomBar(
                    navController = navController,
                    onAddTaskClick = { navController.navigate(Screen.TaskForm.createRoute()) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(paddingValues)
            ) {
                // Auth screens
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

                composable(Screen.Home.route) {
                    HomeScreen(
                        onTaskClick = { navController.navigate(Screen.TaskDetail.createRoute(it)) },
                        onCreateTask = { navController.navigate(Screen.TaskForm.createRoute()) },
                        onRunTask = { navController.navigate("task_progress/$it") }
                    )
                }

                composable(Screen.TaskProgress.route,
                    arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
                    ActiveTaskProgressScreen(
                        taskId = taskId,
                        onClose = { navController.popBackStack() }
                    )
                }
                composable(Screen.Progress.route) {
                    ProgressScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onBack = { navController.popBackStack() },
                        onLogout = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onNavigateToAchievements = {
                            navController.navigate(Screen.Achievements.route)
                        }
                    )
                }

                // Detail screens
                composable(Screen.TaskDetail.route) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getString("taskId")?.toLong() ?: -1L
                    TaskDetailScreen(
                        taskId = taskId,
                        onEditTask = { navController.navigate(Screen.TaskForm.createRoute(taskId)) },
                        onBack = { navController.popBackStack() },
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

                composable(Screen.Achievements.route) {
                    AchievementsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Offline indicator
            AnimatedVisibility(
                visible = !isConnected,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
            ) {
                OfflinePill(
                    modifier = Modifier.padding(16.dp),
                    onSync = { syncViewModel.syncNow() }
                )
            }
        }
    }

    // Handle navigation based on startTaskId
    LaunchedEffect(startTaskId, authState.isLoggedIn, isProgressScreen) {
        if (startTaskId != -1L && authState.isLoggedIn) {
            if (isProgressScreen) {
                // If this is a progress screen navigation, go to the task progress screen
                navController.navigate("task_progress/$startTaskId")
            } else {
                // Otherwise go to the task detail screen
                navController.navigate(Screen.TaskDetail.createRoute(startTaskId))
            }
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

        Scaffold(
            bottomBar = {
                TaskerBottomBar(
                    navController = innerNavController,
                    onAddTaskClick = { navController.navigate(Screen.TaskForm.createRoute()) }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = innerNavController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onTaskClick = { navController.navigate(Screen.TaskDetail.createRoute(it)) },
                        onCreateTask = { navController.navigate(Screen.TaskForm.createRoute()) },
                        onRunTask = { navController.navigate(Screen.TaskProgress.createRoute(it)) }
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onBack = { innerNavController.popBackStack() },
                        onLogout = { navController.navigate(Screen.Login.route) },
                        onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },

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
            onBack = { navController.popBackStack() }
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

    composable(Screen.Progress.route) { backStackEntry ->
        val taskId = backStackEntry.arguments?.getString("taskId")?.toLong() ?: -1L
        ProgressScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable(Screen.Achievements.route) {
        AchievementsScreen(
            onBack = { navController.popBackStack() },
        )
    }
}

@Composable
fun TaskerBottomBar(
    navController: NavHostController,
    onAddTaskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem("Home", Screen.Home.route, R.drawable.ic_home, isEnabled = true),
        BottomNavItem("Progress", Screen.Progress.route, R.drawable.ic_show_chart, isEnabled = true),
        BottomNavItem("Profile", Screen.Profile.route, R.drawable.ic_person, isEnabled = true)
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                BottomBarItem(
                    item = item,
                    isSelected = currentRoute == item.route,
                    onClick = {
                        if (item.isEnabled && currentRoute != item.route) {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val color = if (isSelected && item.isEnabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (item.isEnabled) 0.7f else 0.3f)

    Column(
        modifier = modifier
            .width(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else
                    Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = item.isEnabled, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = item.icon),
                contentDescription = "${item.title} navigation item${if (isSelected) " (selected)" else ""}",
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedVisibility(visible = isSelected) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp,
                shadow = if (isSelected) Shadow(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    offset = Offset(0f, 1f),
                    blurRadius = 2f
                ) else null
            ),
            color = color,
            maxLines = 1
        )
    }
}
data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: Int,
    val isEnabled: Boolean = true
)