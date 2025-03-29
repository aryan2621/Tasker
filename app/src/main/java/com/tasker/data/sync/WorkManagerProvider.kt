package com.tasker.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WorkManagerProvider(private val context: Context) {

    companion object {
        private const val PERIODIC_SYNC_WORK_NAME = "periodic_sync"
        private const val CONNECTION_SYNC_WORK_NAME = "connection_sync"

        private const val SYNC_INTERVAL_MINUTES = 30L
        private const val SYNC_FLEX_INTERVAL_MINUTES = 5L

        private const val INITIAL_BACKOFF_SECONDS = 30L
    }

    fun initialize() {
        setupPeriodicSync()
    }

    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES,
            SYNC_FLEX_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                INITIAL_BACKOFF_SECONDS, TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicSyncRequest
            )
    }

    fun scheduleSyncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                CONNECTION_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }

    fun cancelAllWork() {
        WorkManager.getInstance(context).cancelAllWork()
    }
}