package com.todo.foradhd.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.todo.foradhd.data.SavedTask
import com.todo.foradhd.data.TaskMode

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val ownerKey: String,
    val title: String,
    val mode: String,
    val microStep: String,
    val createdAt: String,
    val completed: Boolean,
    val reminderTime: String?,
    val reminderAt: String?,
    val estimateMinutes: Int?,
    val actualMinutes: Int?,
    val progressNote: String,
    val requiresVerification: Boolean = false,
    val category: String = "Personal",
    val completedAt: String? = null
) {
    fun toDomain(): SavedTask = SavedTask(
        id = id,
        title = title,
        mode = runCatching { TaskMode.valueOf(mode) }.getOrDefault(TaskMode.NEW_TASK),
        microStep = microStep,
        createdAt = createdAt,
        completed = completed,
        reminderTime = reminderTime,
        reminderAt = reminderAt,
        estimateMinutes = estimateMinutes,
        actualMinutes = actualMinutes,
        progressNote = progressNote,
        requiresVerification = requiresVerification,
        category = category.ifBlank { "Personal" },
        completedAt = completedAt
    )

    companion object {
        fun fromDomain(task: SavedTask, ownerKey: String): TaskEntity = TaskEntity(
            id = task.id,
            ownerKey = ownerKey,
            title = task.title,
            mode = task.mode.name,
            microStep = task.microStep,
            createdAt = task.createdAt,
            completed = task.completed,
            reminderTime = task.reminderTime,
            reminderAt = task.reminderAt,
            estimateMinutes = task.estimateMinutes,
            actualMinutes = task.actualMinutes,
            progressNote = task.progressNote,
            requiresVerification = task.requiresVerification,
            category = task.category.ifBlank { "Personal" },
            completedAt = task.completedAt
        )
    }
}
