package com.tasker.ui.components.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tasker.R
import com.tasker.data.sync.SyncState


/**
 * Component that displays the current sync status
 */
@Composable
fun SyncStatusComponent(
    modifier: Modifier = Modifier,
    syncViewModel: SyncViewModel = viewModel()
) {
    val syncState by syncViewModel.syncState.collectAsState()
    val hasUnsyncedChanges by syncViewModel.hasUnsyncedChanges.collectAsState()

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            syncState is SyncState.Syncing || syncState is SyncState.Error -> {
                SyncStatusBanner(
                    syncState = syncState,
                    onRetry = { syncViewModel.retryFailedSync() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            hasUnsyncedChanges -> {
                SyncStatusPill(
                    onSync = { syncViewModel.syncNow() },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SyncStatusPill(
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onSync),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_cloud_sync),
                contentDescription = "Sync",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp)
            )
            Text(
                text = "Changes to sync",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}