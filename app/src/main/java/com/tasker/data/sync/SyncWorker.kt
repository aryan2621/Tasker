package com.tasker.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val TAG = "SyncWorker"

    private val syncManager: SyncManager by inject()

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting background sync")

        return try {
            val syncSuccess = syncManager.syncAll()

            if (syncSuccess) {
                Log.d(TAG, "Background sync completed successfully")
                Result.success()
            } else {
                Log.d(TAG, "Background sync had errors, scheduling retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Background sync failed with exception: ${e.message}", e)
            Result.retry()
        }
    }
}