package com.tasker.ui.components.sync

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tasker.R
import com.tasker.data.model.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusIndicator(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    val (iconRes, tint, label) = when (syncStatus) {
        SyncStatus.SYNCED -> Triple(R.drawable.ic_cloud_done, MaterialTheme.colorScheme.primary, "Synced")
        SyncStatus.PENDING_UPLOAD -> Triple(R.drawable.ic_cloud_sync, MaterialTheme.colorScheme.secondary, "Pending upload")
        SyncStatus.PENDING_DELETE -> Triple(R.drawable.ic_cloud_off, MaterialTheme.colorScheme.error, "Pending deletion")
        SyncStatus.CONFLICT -> Triple(R.drawable.ic_error, MaterialTheme.colorScheme.error, "Conflict detected")
        SyncStatus.ERROR -> Triple(R.drawable.ic_error, MaterialTheme.colorScheme.error, "Sync error")
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(text = label)
            }
        },
        state = rememberTooltipState()
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = tint,
            modifier = modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PreviewSyncStatusIndicator() {
    SyncStatusIndicator(syncStatus = SyncStatus.SYNCED)
}