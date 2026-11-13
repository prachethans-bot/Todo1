package com.todo.foradhd.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.todo.foradhd.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_TITLE) ?: "Time for your step"
        val repeatIndex = intent.getIntExtra(AlarmScheduler.EXTRA_REPEAT_INDEX, 0)
        val isPlanStep = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_PLAN_STEP, false)
        val requestCode = intent.getIntExtra("request_code", taskId.hashCode())

        AlarmScheduler.createNotificationChannel(context)

        val notifTitle = when {
            isPlanStep && repeatIndex == 0 -> "Your focus step starts now"
            isPlanStep -> "Timed plan · Next step"
            repeatIndex == 0 -> "வினை · Time for your step"
            else -> "வினை · Reminder (repeats every 5m)"
        }

        val openPending = AlarmScheduler.buildOpenTaskIntent(context, taskId, taskTitle)
        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        if (canNotify) {
            val notification = NotificationCompat.Builder(context, AlarmScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.vinai_launcher)
                .setContentTitle(notifTitle)
                .setContentText(taskTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(openPending)
                .addAction(0, "Open task", openPending)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(requestCode, notification)
        }

        val inAppIntent = Intent(ACTION_ALARM_TRIGGERED).apply {
            setPackage(context.packageName)
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            putExtra(AlarmScheduler.EXTRA_TASK_TITLE, taskTitle)
            putExtra(AlarmScheduler.EXTRA_IS_PLAN_STEP, isPlanStep)
        }
        context.sendBroadcast(inAppIntent)
    }

    companion object {
        const val ACTION_ALARM_TRIGGERED = "com.todo.foradhd.ALARM_TRIGGERED"
    }
}
