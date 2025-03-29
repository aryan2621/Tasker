package com.tasker.ui.screens.currenttask

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tasker.R
import com.tasker.data.model.TaskCategory
import com.tasker.data.model.TaskPriority
import com.tasker.service.MusicService
import com.tasker.ui.theme.CustomCategoryColor
import com.tasker.ui.theme.HealthCategoryColor
import com.tasker.ui.theme.HighPriorityColor
import com.tasker.ui.theme.LowPriorityColor
import com.tasker.ui.theme.MediumPriorityColor
import com.tasker.ui.theme.PersonalCategoryColor
import com.tasker.ui.theme.StudyCategoryColor
import com.tasker.ui.theme.WorkCategoryColor
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTaskProgressScreen(
    taskId: Long,
    onClose: () -> Unit,
    viewModel: ActiveTaskViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    LaunchedEffect(uiState.isRunning, uiState.remainingSeconds) {
        if (uiState.isRunning && uiState.remainingSeconds > 0) {
            delay(1000)
            viewModel.tick()
        } else if (uiState.remainingSeconds <= 0 && !uiState.isCompleted) {
            viewModel.completeTask(context)

            delay(1500)
            onClose()
        }
    }

    // Check for completed or cancelled state to auto-close
    LaunchedEffect(uiState.isCompleted, uiState.isCancelled) {
        if (uiState.isCompleted || uiState.isCancelled) {
            delay(1500)
            onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            MusicService.stopMusic(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Task Progress",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Close progress screen"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                uiState.task == null -> {
                    Text(
                        text = "Task not found",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    val task = uiState.task!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Task info card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Category indicator
                                    val categoryColor = when (task.category) {
                                        TaskCategory.WORK -> WorkCategoryColor
                                        TaskCategory.STUDY -> StudyCategoryColor
                                        TaskCategory.HEALTH -> HealthCategoryColor
                                        TaskCategory.PERSONAL -> PersonalCategoryColor
                                        TaskCategory.CUSTOM -> CustomCategoryColor
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    // Priority indicator
                                    val priorityColor = when (task.priority) {
                                        TaskPriority.HIGH -> HighPriorityColor
                                        TaskPriority.MEDIUM -> MediumPriorityColor
                                        TaskPriority.LOW -> LowPriorityColor
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(priorityColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (task.priority) {
                                                TaskPriority.HIGH -> "H"
                                                TaskPriority.MEDIUM -> "M"
                                                TaskPriority.LOW -> "L"
                                            },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Timer display
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(240.dp)
                        ) {
                            val progress = remember(uiState.remainingSeconds, task.durationMinutes) {
                                if (task.durationMinutes <= 0) 0f else {
                                    uiState.remainingSeconds.toFloat() / (task.durationMinutes * 60f)
                                }
                            }

                            val animatedProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = LinearEasing
                                )
                            )
                            ActiveTaskProgressIndicator(animatedProgress, modifier = Modifier)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Format time as MM:SS
                                val minutes = uiState.remainingSeconds / 60
                                val seconds = uiState.remainingSeconds % 60
                                val timeString = String.format(Locale.US, "%02d:%02d", minutes, seconds)

                                Text(
                                    text = timeString,
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Text(
                                    text = "remaining",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Control buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Pause/Resume button
                            Button(
                                onClick = {
                                    if (uiState.isRunning) viewModel.pauseTask(context)
                                    else viewModel.resumeTask(context)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isRunning)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.primary
                                ),
                                enabled = !uiState.isCompleted && !uiState.isCancelled
                            ) {
                                Icon(
                                    painter = if (uiState.isRunning)
                                        painterResource(id = R.drawable.ic_pause)
                                    else
                                        painterResource(id = R.drawable.ic_play),
                                    contentDescription = if (uiState.isRunning) "Pause" else "Resume"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = if (uiState.isRunning) "Pause" else "Resume")
                            }

                            // Complete button
                            Button(
                                onClick = { viewModel.completeTask(context) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                enabled = !uiState.isCompleted && !uiState.isCancelled
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_complete),
                                    contentDescription = "Complete"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Complete")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Cancel button
                        OutlinedButton(
                            onClick = { viewModel.cancelTask(context) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = SolidColor(MaterialTheme.colorScheme.error)
                            ),
                            enabled = !uiState.isCompleted && !uiState.isCancelled
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveTaskProgressIndicator(
    animatedProgress: Float,
    modifier: Modifier = Modifier
) {
    // Get colors from the theme outside the Canvas scope
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.size(240.dp)) {
        // Background circle
        drawCircle(
            color = backgroundColor,
            radius = size.minDimension / 2,
            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
        )

        // Progress arc
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}