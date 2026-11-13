package com.todo.foradhd.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todo.foradhd.data.local.AppDatabase
import com.todo.foradhd.data.local.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = TaskRepository(AppDatabase.getInstance(context).taskDao())
                val tasks = repo.getAllPendingAlarmTasks()
                AlarmScheduler.syncAlarmsOnLaunch(context, tasks)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
