package com.tasker.data.db

import androidx.room.TypeConverter
import com.tasker.data.model.TaskCategory
import com.tasker.data.model.TaskPriority
import com.tasker.data.model.TaskRecurrence
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toTaskPriority(value: String) = enumValueOf<TaskPriority>(value)

    @TypeConverter
    fun fromTaskPriority(value: TaskPriority) = value.name

    @TypeConverter
    fun toTaskCategory(value: String) = enumValueOf<TaskCategory>(value)

    @TypeConverter
    fun fromTaskCategory(value: TaskCategory) = value.name

    @TypeConverter
    fun toTaskRecurrence(value: String) = enumValueOf<TaskRecurrence>(value)

    @TypeConverter
    fun fromTaskRecurrence(value: TaskRecurrence) = value.name
}