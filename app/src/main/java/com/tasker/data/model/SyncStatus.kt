package com.tasker.data.model

enum class SyncStatus {
    SYNCED,        // Fully synced with server
    PENDING_UPLOAD, // Local changes need uploading
    PENDING_DELETE, // Marked for deletion on next sync
    CONFLICT,      // Conflict detected with server version
    ERROR          // Sync failed with error
}