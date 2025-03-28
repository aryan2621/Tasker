package com.tasker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasker.R
import com.tasker.ui.theme.AppTypography
import java.util.*

@Composable
fun GreetingCard(
    modifier: Modifier = Modifier,
    hour: Int,
    taskCount: Int
) {
    val (greeting, iconRes) = when {
        hour < 12 -> Pair("Good Morning", R.drawable.ic_sunrise)
        hour < 17 -> Pair("Good Afternoon", R.drawable.ic_sun)
        else -> Pair("Good Evening", R.drawable.ic_moon)
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = greeting,
                    style = AppTypography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = when (taskCount) {
                        0 -> "You have no tasks for today"
                        1 -> "You have 1 task to complete"
                        else -> "You have $taskCount tasks to complete"
                    },
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color(0xFFFF5722),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}