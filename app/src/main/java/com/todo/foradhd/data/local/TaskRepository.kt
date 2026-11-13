package com.todo.foradhd.data.local

import com.todo.foradhd.data.SavedTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val dao: TaskDao) {
    fun ownerKey(userId: String?): String = userId?.takeIf { it.isNotBlank() } ?: "guest"

    fun observeTasks(userId: String?): Flow<List<SavedTask>> =
        dao.observeTasks(ownerKey(userId)).map { list -> list.map { it.toDomain() } }

    suspend fun getTasksOnce(userId: String?): List<SavedTask> =
        dao.getTasksOnce(ownerKey(userId)).map { it.toDomain() }

    suspend fun getAllPendingAlarmTasks(): List<SavedTask> =
        dao.getAllPendingAlarmTasks().map { it.toDomain() }

    suspend fun upsert(task: SavedTask, userId: String?) {
        dao.upsertTask(TaskEntity.fromDomain(task, ownerKey(userId)))
    }

    suspend fun upsertAll(tasks: List<SavedTask>, userId: String?) {
        val key = ownerKey(userId)
        dao.upsertAll(tasks.map { TaskEntity.fromDomain(it, key) })
    }

    suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }
}
