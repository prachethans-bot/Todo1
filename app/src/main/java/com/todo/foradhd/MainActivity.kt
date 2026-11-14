package com.todo.foradhd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todo.foradhd.notifications.AlarmReceiver
import com.todo.foradhd.notifications.AlarmScheduler
import com.todo.foradhd.ui.VinaiApp
import com.todo.foradhd.ui.VinaiViewModel
import com.todo.foradhd.ui.theme.VinaiTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VinaiViewModel by viewModels()

    private val alarmBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val taskId = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_ID)
            val title = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_TITLE) ?: "Time for your step"
            val isPlanStep = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_PLAN_STEP, false)
            val timeLabel = intent.getStringExtra(AlarmScheduler.EXTRA_TIME_LABEL)
            viewModel.triggerRingingAlarm(
                title = title,
                taskId = if (isPlanStep) null else taskId,
                timeLabel = timeLabel,
                isPlanStep = isPlanStep
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        ContextCompat.registerReceiver(
            this,
            alarmBroadcastReceiver,
            IntentFilter(AlarmReceiver.ACTION_ALARM_TRIGGERED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            val state = viewModel.uiState.collectAsStateWithLifecycle()
            VinaiTheme(isDark = state.value.isDarkTheme) {
                VinaiApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(alarmBroadcastReceiver) }
        super.onDestroy()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val dataUri = intent.data
        val buddyParam = dataUri?.getQueryParameter("buddy")
        if (!buddyParam.isNullOrBlank()) {
            viewModel.openBuddyViewer(buddyParam)
        }
        val alarmTaskId = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_ID)
        val alarmTitle = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_TITLE)
        if (!alarmTaskId.isNullOrBlank() && !alarmTitle.isNullOrBlank()) {
            val isPlan = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_PLAN_STEP, false)
            val timeLabel = intent.getStringExtra(AlarmScheduler.EXTRA_TIME_LABEL)
            viewModel.triggerRingingAlarm(
                title = alarmTitle,
                taskId = if (isPlan) null else alarmTaskId,
                timeLabel = timeLabel,
                isPlanStep = isPlan
            )
        }
    }
}
