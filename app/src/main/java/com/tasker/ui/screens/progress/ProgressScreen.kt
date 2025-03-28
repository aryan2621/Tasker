package com.tasker.ui.screens.progress

import StatCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tasker.R
import com.tasker.data.model.DailyStat
import com.tasker.data.model.DateRangeType
import com.tasker.data.model.TaskCategory
import com.tasker.data.model.TaskPriority
import com.tasker.data.model.TaskProgress
import com.tasker.ui.theme.CustomCategoryColor
import com.tasker.ui.theme.HealthCategoryColor
import com.tasker.ui.theme.HighPriorityColor
import com.tasker.ui.theme.LowPriorityColor
import com.tasker.ui.theme.MediumPriorityColor
import com.tasker.ui.theme.PersonalCategoryColor
import com.tasker.ui.theme.StudyCategoryColor
import com.tasker.ui.theme.WorkCategoryColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    taskId: Long? = null,
    onBack: () -> Unit
) {
    val viewModel: ProgressViewModel = viewModel()
    val progressData by viewModel.progressData.collectAsState()
    val dateRangeType by viewModel.dateRangeType.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Set the selected task ID if provided
    LaunchedEffect(taskId) {
        viewModel.setSelectedTaskId(taskId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (selectedTask != null)
                                "${selectedTask!!.title} Progress"
                            else
                                "Progress Overview",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Date range selector - only show for overall progress
                    if (selectedTask == null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Time Period",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    SegmentedButton(
                                        selected = dateRangeType == DateRangeType.DAY,
                                        onClick = { viewModel.setDateRangeType(DateRangeType.DAY) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
                                        colors = SegmentedButtonDefaults.colors(
                                            activeContainerColor = MaterialTheme.colorScheme.primary,
                                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("Day")
                                    }
                                    SegmentedButton(
                                        selected = dateRangeType == DateRangeType.WEEK,
                                        onClick = { viewModel.setDateRangeType(DateRangeType.WEEK) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4),
                                        colors = SegmentedButtonDefaults.colors(
                                            activeContainerColor = MaterialTheme.colorScheme.primary,
                                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("Week")
                                    }
                                    SegmentedButton(
                                        selected = dateRangeType == DateRangeType.MONTH,
                                        onClick = { viewModel.setDateRangeType(DateRangeType.MONTH) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4),
                                        colors = SegmentedButtonDefaults.colors(
                                            activeContainerColor = MaterialTheme.colorScheme.primary,
                                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("Month")
                                    }
                                    SegmentedButton(
                                        selected = dateRangeType == DateRangeType.YEAR,
                                        onClick = { viewModel.setDateRangeType(DateRangeType.YEAR) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4),
                                        colors = SegmentedButtonDefaults.colors(
                                            activeContainerColor = MaterialTheme.colorScheme.primary,
                                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("Year")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (selectedTask != null && progressData.specificTaskProgress != null) {
                        val specificProgress = progressData.specificTaskProgress!!

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                StatCard(
                                    title = "Completed Sessions",
                                    value = "${specificProgress.completedSessions}",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                StatCard(
                                    title = "Total Duration",
                                    value = "${specificProgress.totalDuration} min",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Progress history
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Progress History",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // List of progress entries
                                specificProgress.progressList.takeIf { it.isNotEmpty() }?.forEach { progress ->
                                    ProgressHistoryItem(progress)
                                    Spacer(modifier = Modifier.height(8.dp))
                                } ?: run {
                                    Text(
                                        text = "No progress data available for this task.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    } else {
                        // Overall progress

                        // Task completion rate
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(16.dp)),
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
                                Text(
                                    text = "Task Completion Rate",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Circle progress indicator
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(180.dp)
                                        .padding(8.dp)
                                ) {
                                    CircleProgressIndicator(
                                        progress = progressData.taskCompletionRate / 100f,
                                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                        progressColor = MaterialTheme.colorScheme.primary
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${progressData.taskCompletionRate.toInt()}%",
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Text(
                                            text = "Completed",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (progressData.categoryCounts.isNotEmpty()) {
                            // Task categories distribution
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Tasks by Category",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    CategoryDistribution(categoryCounts = progressData.categoryCounts)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        if (progressData.priorityCounts.isNotEmpty()) {
                            // Task priority distribution
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Tasks by Priority",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    PriorityDistribution(priorityCounts = progressData.priorityCounts)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Daily stats
                        if (progressData.dailyStats.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Daily Completion Stats",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    BarChart(
                                        dailyStats = progressData.dailyStats,
                                        axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        barColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressHistoryItem(progress: TaskProgress) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (progress.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(progress.date),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (progress.isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (progress.isCompleted) "Completed" else "Incomplete",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (progress.isCompleted)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (progress.startTime != null && progress.endTime != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_timer),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    val durationMinutes = (progress.endTime - progress.startTime) / (60 * 1000)
                    Text(
                        text = "Duration: ${progress.durationCompleted ?: durationMinutes} minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_schedule),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "Time: ${timeFormat.format(Date(progress.startTime))} - ${timeFormat.format(Date(progress.endTime))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDistribution(categoryCounts: Map<TaskCategory, Int>) {
    val total = categoryCounts.values.sum()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        categoryCounts.forEach { (category, count) ->
            val percentage = (count.toFloat() / total) * 100
            val color = when (category) {
                TaskCategory.WORK -> WorkCategoryColor
                TaskCategory.STUDY -> StudyCategoryColor
                TaskCategory.HEALTH -> HealthCategoryColor
                TaskCategory.PERSONAL -> PersonalCategoryColor
                TaskCategory.CUSTOM -> CustomCategoryColor
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category color indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Category name
                Text(
                    text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(80.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percentage / 100f)
                            .background(color)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Percentage and count
                Text(
                    text = "${percentage.toInt()}% ($count)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(70.dp),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PriorityDistribution(priorityCounts: Map<TaskPriority, Int>) {
    val total = priorityCounts.values.sum()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        priorityCounts.forEach { (priority, count) ->
            val percentage = (count.toFloat() / total) * 100
            val color = when (priority) {
                TaskPriority.HIGH -> HighPriorityColor
                TaskPriority.MEDIUM -> MediumPriorityColor
                TaskPriority.LOW -> LowPriorityColor
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority color indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Priority name
                Text(
                    text = priority.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(80.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percentage / 100f)
                            .background(color)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Percentage and count
                Text(
                    text = "${percentage.toInt()}% ($count)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(70.dp),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BarChart(
    dailyStats: List<DailyStat>,
    axisColor: Color,
    barColor: Color
) {
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    val maxCompletedCount = dailyStats.maxOfOrNull { it.completedCount } ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(top = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Y-axis line
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Draw Y-axis line
                drawLine(
                    color = axisColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw X-axis line
                drawLine(
                    color = axisColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw bars
                val barWidth = size.width / dailyStats.size

                dailyStats.forEachIndexed { index, stat ->
                    val normalizedHeight = if (maxCompletedCount > 0) {
                        (stat.completedCount.toFloat() / maxCompletedCount) * size.height * 0.9f
                    } else {
                        0f
                    }

                    val barHeight = normalizedHeight.coerceAtLeast(4.dp.toPx())
                    val xPos = index * barWidth + barWidth * 0.5f

                    // Draw bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(xPos - (barWidth * 0.3f), size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth * 0.6f, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Draw count number above bar if there's space
                    if (barHeight > 25.dp.toPx() && stat.completedCount > 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            stat.completedCount.toString(),
                            xPos,
                            size.height - barHeight - 8.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = barColor.toArgb()
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 10.sp.toPx()
                                isFakeBoldText = true
                            }
                        )
                    }
                }
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dailyStats.forEach { stat ->
                Text(
                    text = dateFormat.format(stat.date),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CircleProgressIndicator(progress: Float, backgroundColor: Color, progressColor: Color) {
    Canvas(modifier = Modifier.size(180.dp)) {
        // Background circle
        drawArc(
            color = backgroundColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 24f,
                cap = StrokeCap.Round
            )
        )

        // Progress arc
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 24f,
                cap = StrokeCap.Round
            )
        )
    }
}