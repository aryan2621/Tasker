
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasker.R
import com.tasker.data.model.Task
import com.tasker.data.model.TaskCategory
import com.tasker.data.model.TaskPriority
import com.tasker.ui.components.sync.SyncStatusIndicator
import com.tasker.ui.theme.CustomCategoryColor
import com.tasker.ui.theme.HealthCategoryColor
import com.tasker.ui.theme.HighPriorityColor
import com.tasker.ui.theme.LowPriorityColor
import com.tasker.ui.theme.MediumPriorityColor
import com.tasker.ui.theme.PersonalCategoryColor
import com.tasker.ui.theme.StudyCategoryColor
import com.tasker.ui.theme.WorkCategoryColor
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TaskItem(
    task: Task,
    onTaskClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRunNowClick: () -> Unit, // New parameter for running task immediately
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> HighPriorityColor
        TaskPriority.MEDIUM -> MediumPriorityColor
        TaskPriority.LOW -> LowPriorityColor
    }
    val categoryColor = when (task.category) {
        TaskCategory.WORK -> WorkCategoryColor
        TaskCategory.STUDY -> StudyCategoryColor
        TaskCategory.HEALTH -> HealthCategoryColor
        TaskCategory.PERSONAL -> PersonalCategoryColor
        TaskCategory.CUSTOM -> CustomCategoryColor
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTaskClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row with title, category, priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Category indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                        .padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Task title
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (task.isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Description with ellipsis for overflow
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Priority indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(priorityColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time information row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_timer),
                    contentDescription = "Schedule",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = dateFormat.format(task.reminderTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${task.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Only show action buttons for non-completed tasks
            if (!task.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sync status
                    SyncStatusIndicator(
                        syncStatus = task.syncStatus,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Run Now button
                    OutlinedButton(
                        onClick = onRunNowClick,
                        modifier = Modifier
                            .height(36.dp)
                            .padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play),
                            contentDescription = "Run now",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Run Now",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // For completed tasks, just show sync and delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sync status
                    SyncStatusIndicator(
                        syncStatus = task.syncStatus,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Delete button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}