package com.tasker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tasker.data.model.Task
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    @Query("SELECT * FROM tasks ORDER BY reminderTime ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY reminderTime ASC")
    fun getAllTasksForUser(userId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY reminderTime ASC")
    fun getPendingTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 ORDER BY reminderTime ASC")
    fun getPendingTasksForUser(userId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasksForUser(userId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE reminderTime BETWEEN :startTime AND :endTime")
    fun getTasksByTimeRange(startTime: Date, endTime: Date): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND reminderTime BETWEEN :startTime AND :endTime")
    fun getTasksByTimeRangeForUser(userId: String, startTime: Date, endTime: Date): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isAccepted = 1 AND isCompleted = 0")
    fun getAcceptedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isAccepted = 1 AND isCompleted = 0")
    fun getAcceptedTasksForUser(userId: String): Flow<List<Task>>

    @Query("UPDATE tasks SET isSynced = 1 WHERE id = :taskId")
    suspend fun markTaskAsSynced(taskId: Long)

    @Query("SELECT * FROM tasks WHERE isSynced = 0")
    suspend fun getUnsyncedTasks(): List<Task>
}