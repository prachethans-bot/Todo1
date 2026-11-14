package com.todo.foradhd.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.todo.foradhd.MainActivity
import com.todo.foradhd.R
import com.todo.foradhd.data.SavedTask
import java.time.Instant
import kotlin.math.PI
import kotlin.math.sin

object AlarmScheduler {
    const val CHANNEL_ID = "vinai-exact-alarms-v2"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_REPEAT_INDEX = "extra_repeat_index"
    const val EXTRA_IS_PLAN_STEP = "extra_is_plan_step"
    const val EXTRA_TIME_LABEL = "extra_time_label"

    private const val REPEAT_EVERY_MS = 5L * 60L * 1000L
    private const val MAX_REPEATS = 12

    fun getCustomSoundUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.vinai_alarm_chime}")

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val soundUri = getCustomSoundUri(context)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.alarm_channel_desc)
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFFE6EBF2.toInt()
                setSound(soundUri, attrs)
                setBypassDnd(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun getNotificationIdsForTask(taskId: String): List<Int> {
        val hash = taskId.fold(0) { acc, c -> acc * 31 + c.code } and 0x7FFFFFFF
        val base = (hash % 1_000_000) + 1
        return List(MAX_REPEATS) { i -> base * 30 + i }
    }

    fun scheduleTaskAlarm(
        context: Context,
        taskId: String,
        title: String,
        triggerAtMillis: Long,
        timeLabel: String? = null
    ) {
        createNotificationChannel(context)
        cancelTaskAlarm(context, taskId)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val ids = getNotificationIdsForTask(taskId)

        ids.forEachIndexed { index, reqCode ->
            val at = triggerAtMillis + index * REPEAT_EVERY_MS
            if (at <= now) return@forEachIndexed
            val pending = buildAlarmPendingIntent(
                context = context,
                requestCode = reqCode,
                taskId = taskId,
                title = title,
                repeatIndex = index,
                isPlanStep = false,
                timeLabel = timeLabel
            )
            scheduleExactOrInexact(
                context = context,
                alarmManager = alarmManager,
                triggerAtMs = at,
                pending = pending,
                useAlarmClock = (index == 0),
                taskId = taskId,
                title = title
            )
        }
    }

    fun cancelTaskAlarm(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        getNotificationIdsForTask(taskId).forEach { reqCode ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
            notifManager.cancel(reqCode)
        }
    }

    fun schedulePlanStepNotifications(context: Context, steps: List<Pair<String, Long>>): List<Int> {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val ids = mutableListOf<Int>()
        steps.forEachIndexed { index, (title, triggerAtMs) ->
            if (triggerAtMs <= now + 500) return@forEachIndexed
            val reqCode = 900_000 + index + ((now % 10_000).toInt())
            ids.add(reqCode)
            val pending = buildAlarmPendingIntent(
                context = context,
                requestCode = reqCode,
                taskId = "plan_step_$reqCode",
                title = title,
                repeatIndex = index,
                isPlanStep = true,
                timeLabel = null
            )
            scheduleExactOrInexact(
                context = context,
                alarmManager = alarmManager,
                triggerAtMs = triggerAtMs,
                pending = pending,
                useAlarmClock = false,
                taskId = "plan_step_$reqCode",
                title = title
            )
        }
        return ids
    }

    fun cancelPlanStepNotifications(context: Context, ids: List<Int>) {
        if (ids.isEmpty()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ids.forEach { reqCode ->
            val pending = PendingIntent.getBroadcast(
                context,
                reqCode,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
            notifManager.cancel(reqCode)
        }
    }

    fun syncAlarmsOnLaunch(context: Context, tasks: List<SavedTask>) {
        val now = System.currentTimeMillis()
        tasks.forEach { task ->
            if (task.completed || task.reminderAt.isNullOrBlank()) return@forEach
            val targetMs = runCatching { Instant.parse(task.reminderAt).toEpochMilli() }.getOrNull() ?: return@forEach
            val chainEnd = targetMs + (MAX_REPEATS - 1) * REPEAT_EVERY_MS
            if (chainEnd > now) {
                val label = task.reminderTime?.let { format12h(it) }
                scheduleTaskAlarm(context, task.id, task.title, targetMs, label)
            }
        }
    }

    private fun format12h(time24: String): String {
        val parts = time24.split(":")
        val hh = parts.getOrNull(0)?.toIntOrNull() ?: return time24
        val mm = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val period = if (hh >= 12) "PM" else "AM"
        val h12 = if (hh % 12 == 0) 12 else hh % 12
        return "$h12:${mm.toString().padStart(2, '0')} $period"
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    private fun scheduleExactOrInexact(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMs: Long,
        pending: PendingIntent,
        useAlarmClock: Boolean,
        taskId: String,
        title: String
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
            } else if (useAlarmClock) {
                val showIntent = buildOpenTaskIntent(context, taskId, title)
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMs, showIntent), pending)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
        }
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        requestCode: Int,
        taskId: String,
        title: String,
        repeatIndex: Int,
        isPlanStep: Boolean,
        timeLabel: String?
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_REPEAT_INDEX, repeatIndex)
            putExtra(EXTRA_IS_PLAN_STEP, isPlanStep)
            if (!timeLabel.isNullOrBlank()) putExtra(EXTRA_TIME_LABEL, timeLabel)
            putExtra("request_code", requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildOpenTaskIntent(context: Context, taskId: String, title: String, timeLabel: String? = null): PendingIntent {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            if (!timeLabel.isNullOrBlank()) putExtra(EXTRA_TIME_LABEL, timeLabel)
        }
        return PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

object AlarmSoundPlayer {
    fun playCustomChime(context: Context) {
        Thread {
            val played = runCatching {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context.applicationContext, AlarmScheduler.getCustomSoundUri(context))
                    prepare()
                    start()
                }
                player.setOnCompletionListener { it.release() }
                true
            }.getOrDefault(false)

            if (!played) {
                playTone(freqHz = 740.0, durationMs = 320, volume = 0.25f)
            }
        }.start()
    }

    fun playTone(freqHz: Double = 740.0, durationMs: Int = 320, volume: Float = 0.25f) {
        Thread {
            runCatching {
                val sampleRate = 22050
                val count = (sampleRate * durationMs) / 1000
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val env = when {
                        i < count / 10 -> i.toFloat() / (count / 10f)
                        i > count * 0.7 -> ((count - i).toFloat() / (count * 0.3f)).coerceAtLeast(0f)
                        else -> 1f
                    }
                    val angle = 2.0 * PI * i * freqHz / sampleRate
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * volume * env).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 80L)
                track.release()
            }
        }.start()
    }

    fun vibrate(context: Context, pattern: LongArray = longArrayOf(0, 120, 90, 140)) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }
}
