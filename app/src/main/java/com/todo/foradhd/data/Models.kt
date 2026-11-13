package com.todo.foradhd.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TaskMode(val label: String) {
    NEW_TASK("New task"),
    NEXT_STEP("Next step"),
    TOO_HARD("Too hard"),
    TIME_CHECK("Time check");

    fun displayTag(): String = name.lowercase().replace('_', ' ')
}

@Serializable
data class AlarmSettings(
    val enabled: Boolean = false,
    val at: String? = null,
    val label: String = "Optional gentle reminder"
)

@Serializable
data class AssistantResponse(
    val mode: TaskMode,
    @SerialName("optimized_task_title") val optimizedTaskTitle: String,
    @SerialName("the_micro_step") val theMicroStep: String,
    @SerialName("dopamine_hook") val dopamineHook: String,
    @SerialName("alarm_settings") val alarmSettings: AlarmSettings = AlarmSettings(),
    @SerialName("time_guess_prompt") val timeGuessPrompt: String,
    @SerialName("time_feedback") val timeFeedback: String
)

@Serializable
data class SavedTask(
    val id: String,
    val title: String,
    val mode: TaskMode,
    val microStep: String,
    val createdAt: String,
    val completed: Boolean,
    val reminderTime: String? = null, // "HH:mm" 24h
    val reminderAt: String? = null,   // ISO-8601 instant
    val estimateMinutes: Int? = null,
    val actualMinutes: Int? = null,
    val progressNote: String = ""
)

enum class PlanStepKind { WORK, BREAK }

enum class PlanStatus { READY, RUNNING, PAUSED, STOPPED, FINISHED }

data class PlanStep(
    val id: String,
    val title: String,
    val minutes: Int,
    val kind: PlanStepKind
)

@Serializable
enum class BuddyActivity(val raw: String) {
    @SerialName("idle") IDLE("idle"),
    @SerialName("focusing") FOCUSING("focusing"),
    @SerialName("break") BREAK("break"),
    @SerialName("done") DONE("done")
}

@Serializable
enum class BuddyState(val raw: String) {
    @SerialName("pending") PENDING("pending"),
    @SerialName("active") ACTIVE("active"),
    @SerialName("paused") PAUSED("paused"),
    @SerialName("removed") REMOVED("removed")
}

@Serializable
data class LocalBuddy(
    val token: String,
    val ownerKey: String,
    val ownerName: String,
    val state: BuddyState,
    val lastCheerId: Long = 0L
)

data class BuddyViewData(
    val ownerName: String,
    val state: BuddyState,
    val taskLabel: String,
    val activity: BuddyActivity,
    val sessionId: String,
    val cheersUsed: Int
)

@Serializable
data class RewardSettings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val animations: Boolean = true,
    val notifySound: Boolean = true
)

@Serializable
data class UserSession(
    val accessToken: String,
    val refreshToken: String = "",
    val userId: String,
    val email: String
)

enum class AppTab(val label: String) {
    TODAY("Today"),
    PLAN("Timed plan"),
    BUDDY("My buddy")
}
