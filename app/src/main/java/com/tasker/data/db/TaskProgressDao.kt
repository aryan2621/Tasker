package com.tasker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tasker.data.model.SyncStatus
import com.tasker.data.model.TaskProgress
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TaskProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: TaskProgress): Long

    @Update
    suspend fun updateProgress(progress: TaskProgress)

    @Query("SELECT * FROM task_progress WHERE id = :progressId")
    suspend fun getProgressById(progressId: Long): TaskProgress?

    @Query("SELECT * FROM task_progress WHERE taskId = :taskId ORDER BY date DESC")
    fun getProgressForTask(taskId: Long): Flow<List<TaskProgress>>

    @Query("SELECT tp.* FROM task_progress tp JOIN tasks t ON tp.taskId = t.id WHERE tp.date BETWEEN :startDate AND :endDate")
    fun getProgressByDateRange(startDate: Date, endDate: Date): Flow<List<TaskProgress>>

    @Query("SELECT tp.* FROM task_progress tp JOIN tasks t ON tp.taskId = t.id WHERE t.userId = :userId AND tp.date BETWEEN :startDate AND :endDate")
    fun getProgressByDateRangeForUser(userId: String, startDate: Date, endDate: Date): Flow<List<TaskProgress>>

    @Query("SELECT tp.* FROM task_progress tp JOIN tasks t ON tp.taskId = t.id WHERE tp.date = :date")
    fun getProgressByDate(date: Date): Flow<List<TaskProgress>>

    @Query("SELECT tp.* FROM task_progress tp JOIN tasks t ON tp.taskId = t.id WHERE t.userId = :userId AND tp.date = :date")
    fun getProgressByDateForUser(userId: String, date: Date): Flow<List<TaskProgress>>

    @Query("SELECT COUNT(*) FROM task_progress WHERE date = :date AND isCompleted = 1")
    fun getCompletedTasksCountForDay(date: Date): Flow<Int>

    @Query("SELECT COUNT(*) FROM task_progress tp JOIN tasks t ON tp.taskId = t.id WHERE t.userId = :userId AND tp.date = :date AND tp.isCompleted = 1")
    fun getCompletedTasksCountForDayForUser(userId: String, date: Date): Flow<Int>

    // Enhanced offline sync support
    @Query("UPDATE task_progress SET syncStatus = :status WHERE id = :progressId")
    suspend fun updateProgressSyncStatus(progressId: Long, status: SyncStatus)

    @Query("UPDATE task_progress SET syncStatus = :status, lastSyncAttempt = :lastSyncAttempt, serverUpdatedAt = :serverTime WHERE id = :progressId")
    suspend fun markProgressSynced(progressId: Long, status: SyncStatus = SyncStatus.SYNCED, lastSyncAttempt: Date = Date(), serverTime: Date?)

    @Query("SELECT * FROM task_progress WHERE syncStatus = :syncStatus")
    suspend fun getProgressByStatus(syncStatus: SyncStatus): List<TaskProgress>

    @Query("SELECT * FROM task_progress WHERE syncStatus != :syncStatus")
    suspend fun getProgressNotSynced(syncStatus: SyncStatus = SyncStatus.SYNCED): List<TaskProgress>

    @Query("UPDATE task_progress SET syncErrorMessage = :errorMessage WHERE id = :progressId")
    suspend fun updateProgressSyncErrorMessage(progressId: Long, errorMessage: String?)

    @Query("UPDATE task_progress SET isDeletedLocally = 1, syncStatus = :syncStatus WHERE id = :progressId")
    suspend fun markProgressDeletedLocally(progressId: Long, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE)

    @Query("DELETE FROM task_progress WHERE isDeletedLocally = 1 AND syncStatus = :syncStatus")
    suspend fun deleteLocallyDeletedAndSyncedProgress(syncStatus: SyncStatus = SyncStatus.SYNCED)
}