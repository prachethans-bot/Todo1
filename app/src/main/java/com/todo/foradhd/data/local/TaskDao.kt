package com.todo.foradhd.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE ownerKey = :ownerKey ORDER BY createdAt DESC LIMIT 100")
    fun observeTasks(ownerKey: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE ownerKey = :ownerKey ORDER BY createdAt DESC LIMIT 100")
    suspend fun getTasksOnce(ownerKey: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE completed = 0 AND reminderAt IS NOT NULL")
    suspend fun getAllPendingAlarmTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}
