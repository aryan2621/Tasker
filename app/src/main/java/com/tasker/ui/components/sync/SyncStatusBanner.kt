package com.tasker.ui.components.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tasker.R
import com.tasker.data.sync.SyncState

/**
 * Banner that displays global sync status
 */
@Composable
fun SyncStatusBanner(
    syncState: SyncState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (syncState is SyncState.Idle) return  // Don't show anything

    val bannerConfig = when (syncState) {
        is SyncState.Syncing -> BannerConfig(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            iconRes = R.drawable.ic_cloud_sync,
            message = syncState.message
        )
        is SyncState.Success -> BannerConfig(
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            iconRes = R.drawable.ic_cloud_done,
            message = syncState.message
        )
        is SyncState.Error -> BannerConfig(
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer,
            iconRes = R.drawable.ic_error,
            message = syncState.message
        )
        else -> return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bannerConfig.backgroundColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = bannerConfig.iconRes),
            contentDescription = null,
            tint = bannerConfig.textColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = bannerConfig.message,
            color = bannerConfig.textColor,
            modifier = Modifier.weight(1f)
        )
        if (syncState is SyncState.Error) {
            TextButton(onClick = onRetry) {
                Text("Retry", color = bannerConfig.textColor)
            }
        }
    }
}

/**
 * Data class to store banner configurations
 */
data class BannerConfig(
    val backgroundColor: Color,
    val textColor: Color,
    val iconRes: Int,
    val message: String
)
