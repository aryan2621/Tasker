package com.tasker.ui.screens.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.tasker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit
) {
    val viewModel: ProgressViewModel = viewModel()
    val progressData by viewModel.progressData.collectAsState()
    val dateRangeType by viewModel.dateRangeType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Progress Overview",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Your productivity statistics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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
                            contentDescription = "Navigate back",
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

                    // Time Period Selection Card
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
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { contentDescription = "Select time period for progress" }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SegmentedButton(
                                    selected = dateRangeType == DateRangeType.DAY,
                                    onClick = { viewModel.setDateRangeType(DateRangeType.DAY) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = MaterialTheme.colorScheme.primary,
                                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) { Text("Day") }
                                SegmentedButton(
                                    selected = dateRangeType == DateRangeType.WEEK,
                                    onClick = { viewModel.setDateRangeType(DateRangeType.WEEK) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = MaterialTheme.colorScheme.primary,
                                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) { Text("Week") }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Task Completion Rate Card
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

                    // Category Distribution Card
                    if (progressData.categoryCounts.isNotEmpty()) {
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

                    // Priority Distribution Card
                    if (progressData.priorityCounts.isNotEmpty()) {
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

                    // Daily Completion Stats Card
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
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.semantics { contentDescription = "Daily task completion statistics" }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                BarChart(
                                    dailyStats = progressData.dailyStats,
                                    axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    barColor = MaterialTheme.colorScheme.primary,
                                    dateRangeType = dateRangeType
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
                        contentDescription = "Duration icon", // Added for accessibility
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
                        contentDescription = "Time icon", // Added for accessibility
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
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(80.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = priority.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(80.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
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
    barColor: Color,
    dateRangeType: DateRangeType = DateRangeType.WEEK
) {
    // Use appropriate date format based on the range type
    val dateFormat = when (dateRangeType) {
        DateRangeType.DAY -> SimpleDateFormat("HH:mm", Locale.getDefault()) // Hours for day view
        DateRangeType.WEEK -> SimpleDateFormat("EEE", Locale.getDefault()) // Day of week for week view
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()) // Default format
    }

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
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val barWidth = size.width / dailyStats.size
                            val index = (offset.x / barWidth).toInt().coerceIn(0, dailyStats.size - 1)
                            val stat = dailyStats[index]
                            println("Tapped on ${dateFormat.format(stat.date)}: ${stat.completedCount}")
                        }
                    }
            ) {
                // Draw axes
                drawLine(
                    color = axisColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = axisColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )

                // Calculate bar width based on available space and number of items
                val barWidth = size.width / dailyStats.size

                // Draw grid lines
                val gridLines = 3
                for (i in 1..gridLines) {
                    val y = size.height * (1 - i.toFloat() / gridLines)
                    drawLine(
                        color = axisColor.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }

                // Draw bars
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

                    // Add value labels on top of bars
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