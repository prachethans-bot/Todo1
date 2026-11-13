package com.todo.foradhd.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todo.foradhd.data.AppTab
import com.todo.foradhd.data.AssistantFallback
import com.todo.foradhd.data.AssistantResponse
import com.todo.foradhd.data.BuddyActivity
import com.todo.foradhd.data.BuddyState
import com.todo.foradhd.data.BuddyViewData
import com.todo.foradhd.data.LocalBuddy
import com.todo.foradhd.data.PlanStatus
import com.todo.foradhd.data.PlanStep
import com.todo.foradhd.data.RewardSettings
import com.todo.foradhd.data.SavedTask
import com.todo.foradhd.data.TaskMode
import com.todo.foradhd.data.UserSession
import com.todo.foradhd.data.local.AppDatabase
import com.todo.foradhd.data.local.PreferencesStore
import com.todo.foradhd.data.local.TaskRepository
import com.todo.foradhd.data.remote.SupabaseApi
import com.todo.foradhd.notifications.AlarmScheduler
import com.todo.foradhd.notifications.AlarmSoundPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class RingingAlarmState(
    val title: String,
    val taskId: String? = null
)

data class LastRewardInfo(
    val taskId: String,
    val points: Int,
    val bonus: Int
)

data class VinaiUiState(
    val showSplash: Boolean = true,
    val tab: AppTab = AppTab.TODAY,
    val todayFormatted: String = "",
    val todayIsoKey: String = "",
    val session: UserSession? = null,
    val supabaseConfigured: Boolean = false,
    val showAccountModal: Boolean = false,
    val isSignUpMode: Boolean = false,
    val authBusy: Boolean = false,
    val authMessage: String = "",

    // Quick add & assistant
    val taskInput: String = "",
    val mode: TaskMode = TaskMode.NEW_TASK,
    val assistantBusy: Boolean = false,
    val assistantResponse: AssistantResponse? = null,
    val estimateMinutes: Int = 2,
    val reminderTime24: String? = null,
    val reminderAtIso: String? = null,
    val clockPickerOpen: Boolean = false,
    val notice: String = "",

    // Tasks
    val tasks: List<SavedTask> = emptyList(),
    val leavingTaskIds: Set<String> = emptySet(),
    val focusedTasks: Set<String> = emptySet(),
    val qualifiedFocusTasks: Set<String> = emptySet(),

    // Verification
    val verificationTask: SavedTask? = null,
    val verificationText: String = "",
    val verificationBusy: Boolean = false,
    val verificationError: String = "",

    // Rewards & settings
    val points: Int = 0,
    val streak: Int = 0,
    val lastDay: String = "",
    val rewardToast: String = "",
    val floatingReward: Pair<String, String>? = null,
    val burstTaskId: String? = null,
    val undoTask: SavedTask? = null,
    val rewardSettings: RewardSettings = RewardSettings(),
    val settingsOpen: Boolean = false,
    val checkInFrequency: Int = 10,

    // Focus session
    val focusTaskId: String? = null,
    val focusMinutes: Int = 25,
    val focusRemainingSeconds: Int = 25 * 60,
    val focusElapsedSeconds: Int = 0,
    val focusPaused: Boolean = false,
    val focusQualified: Boolean = false,
    val focusMicroStep: String = "",
    val focusProgressNote: String = "",
    val checkInOpen: Boolean = false,
    val checkInExpired: Boolean = false,

    // Timed plan
    val planGoal: String = "",
    val planTotalMinutes: Int = 120,
    val planSteps: List<PlanStep> = emptyList(),
    val planStatus: PlanStatus = PlanStatus.READY,
    val planCurrentIndex: Int = 0,
    val planStepRemainingMs: Long = 0L,
    val planNotice: String = "",

    // Buddy
    val buddy: LocalBuddy? = null,
    val buddyNameInput: String = "",
    val buddyBusy: Boolean = false,
    val buddyMessage: String = "",
    val cheerFloat: String = "",
    val incomingBuddyToken: String? = null,
    val buddyViewData: BuddyViewData? = null,
    val buddyViewLoaded: Boolean = false,
    val buddyViewAccepted: Boolean = false,
    val buddyViewDeclined: Boolean = false,
    val buddyViewFeedback: String = "",

    // Ringing alarm
    val ringingAlarm: RingingAlarmState? = null
)

class VinaiViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext: Context = application.applicationContext
    private val prefs = PreferencesStore(appContext)
    private val repository = TaskRepository(AppDatabase.getInstance(appContext).taskDao())
    private val supabase = SupabaseApi()

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<VinaiUiState> = _uiState.asStateFlow()

    private var tasksObserverJob: Job? = null
    private var focusTimerJob: Job? = null
    private var checkInExpireJob: Job? = null
    private var planTimerJob: Job? = null
    private var buddyOwnerPollJob: Job? = null
    private var buddyViewPollJob: Job? = null
    private var alarmBeepJob: Job? = null
    private var undoHideJob: Job? = null

    private var planStepStartedAtMs: Long = 0L
    private var planPausedElapsedMs: Long = 0L
    private var scheduledPlanIds: List<Int> = emptyList()

    private var undoStreakSnapshot: Pair<Int, String>? = null
    private var lastRewardInfo: LastRewardInfo? = null
    private var currentFocusSessionId: String = ""

    init {
        AlarmScheduler.createNotificationChannel(appContext)
        viewModelScope.launch {
            delay(1500L)
            _uiState.update { it.copy(showSplash = false) }
        }
        observeTasksForCurrentUser()
        syncRemoteTasksIfSignedIn()
        startBuddyOwnerPollingIfNeeded()
    }

    private fun createInitialState(): VinaiUiState {
        val now = LocalDate.now()
        val formattedDate = DateTimeFormatter.ofPattern("EEEE, MMMM d").format(now)
        val todayKey = DateTimeFormatter.ISO_LOCAL_DATE.format(now)
        return VinaiUiState(
            todayFormatted = formattedDate,
            todayIsoKey = todayKey,
            session = prefs.getSession(),
            supabaseConfigured = supabase.isConfigured,
            points = prefs.points,
            streak = prefs.streak,
            lastDay = prefs.lastDay,
            rewardSettings = prefs.getRewardSettings(),
            checkInFrequency = prefs.checkInFrequency,
            focusedTasks = prefs.getFocusedTasks(),
            qualifiedFocusTasks = prefs.getQualifiedFocusTasks(),
            buddy = prefs.getLocalBuddy(),
            buddyNameInput = prefs.buddyOwnerName
        )
    }

    private fun observeTasksForCurrentUser() {
        tasksObserverJob?.cancel()
        tasksObserverJob = viewModelScope.launch {
            val userId = _uiState.value.session?.userId
            repository.observeTasks(userId).collectLatest { list ->
                _uiState.update { it.copy(tasks = list) }
                AlarmScheduler.syncAlarmsOnLaunch(appContext, list)
            }
        }
    }

    private fun syncRemoteTasksIfSignedIn() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            val remote = supabase.fetchTasks(session) ?: return@launch
            val local = repository.getTasksOnce(session.userId)
            val byId = local.associateBy { it.id }.toMutableMap()
            remote.forEach { entry ->
                val existing = byId[entry.id]
                byId[entry.id] = if (existing != null && existing.progressNote.isNotBlank() && entry.progressNote.isBlank()) {
                    entry.copy(progressNote = existing.progressNote)
                } else {
                    entry
                }
            }
            val merged = byId.values.sortedByDescending { it.createdAt }.take(100)
            repository.upsertAll(merged, session.userId)
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(settingsOpen = !it.settingsOpen) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(settingsOpen = false) }
    }

    fun openAccountModal() {
        _uiState.update { it.copy(showAccountModal = true, settingsOpen = false, authMessage = "") }
    }

    fun closeAccountModal() {
        _uiState.update { it.copy(showAccountModal = false, authMessage = "") }
    }

    fun setAuthMode(signUp: Boolean) {
        _uiState.update { it.copy(isSignUpMode = signUp, authMessage = "") }
    }

    fun submitAuth(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            _uiState.update { it.copy(authMessage = "Enter a valid email and at least 6 characters for your password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(authBusy = true, authMessage = "") }
            val result = if (_uiState.value.isSignUpMode) {
                supabase.signUp(email, password)
            } else {
                supabase.signIn(email, password)
            }
            when (result) {
                is SupabaseApi.AuthResult.Error -> {
                    _uiState.update { it.copy(authBusy = false, authMessage = result.message) }
                }
                is SupabaseApi.AuthResult.Success -> {
                    if (result.session != null) {
                        prefs.saveSession(result.session)
                        _uiState.update {
                            it.copy(
                                authBusy = false,
                                session = result.session,
                                showAccountModal = false,
                                authMessage = result.message
                            )
                        }
                        observeTasksForCurrentUser()
                        syncRemoteTasksIfSignedIn()
                    } else {
                        _uiState.update { it.copy(authBusy = false, authMessage = result.message) }
                    }
                }
            }
        }
    }

    fun signOut() {
        val current = _uiState.value.session
        viewModelScope.launch {
            supabase.signOut(current)
            prefs.saveSession(null)
            _uiState.update { it.copy(session = null, showAccountModal = false) }
            observeTasksForCurrentUser()
        }
    }

    fun updateTaskInput(text: String) {
        _uiState.update { it.copy(taskInput = text) }
    }

    fun selectMode(mode: TaskMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun openClockPicker() {
        _uiState.update { it.copy(clockPickerOpen = true) }
    }

    fun closeClockPicker() {
        _uiState.update { it.copy(clockPickerOpen = false) }
    }

    fun setReminderTime(time24: String) {
        val nextOccurrence = computeNextOccurrenceIso(time24)
        _uiState.update {
            it.copy(
                reminderTime24 = time24,
                reminderAtIso = nextOccurrence,
                clockPickerOpen = false
            )
        }
    }

    fun clearReminderTime() {
        _uiState.update { it.copy(reminderTime24 = null, reminderAtIso = null) }
    }

    fun setEstimateMinutes(minutes: Int) {
        _uiState.update { it.copy(estimateMinutes = minutes) }
    }

    fun findNextStep() {
        val title = _uiState.value.taskInput.trim()
        if (title.isEmpty() || _uiState.value.assistantBusy) return
        val mode = _uiState.value.mode
        val session = _uiState.value.session

        viewModelScope.launch {
            _uiState.update { it.copy(assistantBusy = true, notice = "", assistantResponse = null) }
            var result: AssistantResponse? = null
            var noticeMsg = ""
            if (supabase.isConfigured && session != null) {
                result = supabase.invokeStepwiseAssistant(title, mode, session)
                if (result == null) {
                    noticeMsg = "The AI service is unavailable. Showing a built-in micro-step instead."
                }
            }
            val finalResp = result ?: AssistantFallback.getDemoResponse(title, mode)
            if (result == null && noticeMsg.isEmpty()) {
                noticeMsg = if (session != null) {
                    "Using the built-in helper for now."
                } else {
                    "Built-in micro-step helper ready. Connect your account for cloud AI."
                }
            }
            _uiState.update {
                it.copy(
                    assistantBusy = false,
                    assistantResponse = finalResp,
                    notice = noticeMsg
                )
            }
        }
    }

    fun discardAssistantResult() {
        _uiState.update { it.copy(assistantResponse = null) }
    }

    fun saveCurrentStep(hasNotificationPermission: Boolean) {
        val resp = _uiState.value.assistantResponse ?: return
        val state = _uiState.value
        val newTask = SavedTask(
            id = UUID.randomUUID().toString(),
            title = resp.optimizedTaskTitle,
            mode = resp.mode,
            microStep = resp.theMicroStep,
            createdAt = Instant.now().toString(),
            completed = false,
            reminderTime = state.reminderTime24,
            reminderAt = state.reminderAtIso,
            estimateMinutes = state.estimateMinutes,
            actualMinutes = null,
            progressNote = ""
        )

        viewModelScope.launch {
            repository.upsert(newTask, state.session?.userId)
            persistRemoteTask(newTask)
            if (!newTask.reminderAt.isNullOrBlank()) {
                val triggerMs = runCatching { Instant.parse(newTask.reminderAt).toEpochMilli() }.getOrNull()
                if (triggerMs != null && triggerMs > System.currentTimeMillis()) {
                    if (hasNotificationPermission) {
                        AlarmScheduler.scheduleTaskAlarm(appContext, newTask.id, newTask.title, triggerMs)
                    }
                }
            }
            val reminderNotice = if (!newTask.reminderAt.isNullOrBlank()) {
                if (hasNotificationPermission) {
                    "Reminder saved. Android will alert you on time."
                } else {
                    "Saved. Enable notification permission in Settings for lock-screen alarms."
                }
            } else {
                "Saved. Your next step is ready when you are."
            }
            _uiState.update {
                it.copy(
                    assistantResponse = null,
                    taskInput = "",
                    reminderTime24 = null,
                    reminderAtIso = null,
                    notice = reminderNotice
                )
            }
        }
    }

    fun deleteTask(taskId: String) {
        AlarmScheduler.cancelTaskAlarm(appContext, taskId)
        val session = _uiState.value.session
        viewModelScope.launch {
            repository.deleteById(taskId)
            if (session != null && supabase.isConfigured) {
                val ok = supabase.deleteTask(taskId, session)
                if (!ok) _uiState.update { it.copy(notice = "Removed on this device. Cloud deletion needs attention.") }
            }
        }
    }

    fun saveTimeFeedback(task: SavedTask, estimateInput: String, actualInput: String) {
        val actual = actualInput.trim().toIntOrNull()
        if (actual == null || actual <= 0) {
            _uiState.update { it.copy(notice = "Enter an actual time greater than zero.") }
            return
        }
        val est = estimateInput.trim().toIntOrNull() ?: task.estimateMinutes
        val updated = task.copy(actualMinutes = actual, estimateMinutes = est)
        viewModelScope.launch {
            repository.upsert(updated, _uiState.value.session?.userId)
            persistRemoteTask(updated)
            _uiState.update { it.copy(notice = "Time note saved. It is information, not a score.") }
        }
    }

    // Verification & completion
    fun openTaskVerification(task: SavedTask) {
        if (task.completed) return
        stopAlarmSound()
        if (_uiState.value.focusTaskId == task.id) {
            _uiState.update { it.copy(focusPaused = true) }
        }
        _uiState.update {
            it.copy(
                verificationTask = task,
                verificationText = "",
                verificationError = ""
            )
        }
    }

    fun closeTaskVerification() {
        if (_uiState.value.verificationBusy) return
        _uiState.update { it.copy(verificationTask = null, verificationText = "", verificationError = "") }
    }

    fun updateVerificationText(text: String) {
        _uiState.update { it.copy(verificationText = text) }
    }

    fun verifyAndCompleteTask() {
        val target = _uiState.value.verificationTask ?: return
        val note = _uiState.value.verificationText.trim()
        if (_uiState.value.verificationBusy) return
        if (note.isEmpty()) {
            _uiState.update { it.copy(verificationError = "Add a little detail so we can verify your step.") }
            return
        }

        val session = _uiState.value.session
        if (supabase.isConfigured && session == null) {
            _uiState.update { it.copy(verificationError = "Sign in to use the AI task checker. Your task is still saved.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(verificationBusy = true, verificationError = "") }

            val approvedAndReason: Pair<Boolean, String> = if (supabase.isConfigured && session != null) {
                when (val res = supabase.invokeVerifyTaskStart(target.title, target.microStep, note, session)) {
                    is SupabaseApi.VerifyResult.Failure -> {
                        _uiState.update { it.copy(verificationBusy = false, verificationError = res.message) }
                        return@launch
                    }
                    is SupabaseApi.VerifyResult.Done -> res.approved to res.reason
                }
            } else {
                val localRes = AssistantFallback.verifyLocally(target.title, target.microStep, note)
                localRes.approved to localRes.reason
            }

            if (!approvedAndReason.first) {
                val reason = approvedAndReason.second.ifBlank {
                    "That update does not clearly match this task yet. Add a relevant detail and try again."
                }
                _uiState.update { it.copy(verificationBusy = false, verificationError = reason) }
                return@launch
            }

            _uiState.update {
                it.copy(
                    verificationBusy = false,
                    verificationTask = null,
                    verificationText = "",
                    verificationError = "",
                    leavingTaskIds = it.leavingTaskIds + target.id
                )
            }
            delay(420L)
            _uiState.update { it.copy(leavingTaskIds = it.leavingTaskIds - target.id) }
            giveReward(target, note)
            _uiState.update { it.copy(notice = "Verified. Your task is complete.") }
        }
    }

    private fun giveReward(item: SavedTask, note: String) {
        val state = _uiState.value
        val today = LocalDate.now()
        val todayKey = DateTimeFormatter.ISO_LOCAL_DATE.format(today)
        val yesterdayKey = DateTimeFormatter.ISO_LOCAL_DATE.format(today.minusDays(1))

        undoStreakSnapshot = state.streak to state.lastDay
        val nextStreak = when (state.lastDay) {
            todayKey -> state.streak
            yesterdayKey -> state.streak + 1
            else -> 1
        }
        val focusBonus = (state.focusTaskId == item.id && state.focusQualified) || (item.id in state.qualifiedFocusTasks)
        val basePoints = if (focusBonus) 15 else 10
        val bonus = if (Math.random() < 0.2) 25 else 0
        val awarded = basePoints + bonus
        val nextPoints = state.points + awarded

        prefs.points = nextPoints
        prefs.streak = nextStreak
        prefs.lastDay = todayKey
        lastRewardInfo = LastRewardInfo(item.id, basePoints, bonus)

        var nextFocused = state.focusedTasks
        var nextQualified = state.qualifiedFocusTasks
        if (focusBonus) {
            nextFocused = nextFocused + item.id
            nextQualified = nextQualified - item.id
            prefs.saveFocusedTasks(nextFocused)
            prefs.saveQualifiedFocusTasks(nextQualified)
        }

        val combinedNote = listOf(item.progressNote, note).filter { it.isNotBlank() }.joinToString("\n\n")
        val updated = item.copy(completed = true, progressNote = combinedNote)

        val settings = state.rewardSettings
        val rewardLabel = if (bonus > 0) "+$basePoints points · +25 bonus!" else "+$basePoints points · nice work"
        val floatLabel = if (bonus > 0) "+$basePoints · +25 bonus" else "+$basePoints"

        if (settings.haptics) {
            AlarmSoundPlayer.vibrate(
                appContext,
                if (bonus > 0) longArrayOf(0, 25, 35, 25) else longArrayOf(0, 18)
            )
        }
        if (settings.sound) {
            AlarmSoundPlayer.playTone(freqHz = if (bonus > 0) 880.0 else 660.0, durationMs = 180, volume = 0.22f)
        }

        _uiState.update {
            it.copy(
                points = nextPoints,
                streak = nextStreak,
                lastDay = todayKey,
                focusedTasks = nextFocused,
                qualifiedFocusTasks = nextQualified,
                undoTask = item,
                rewardToast = if (settings.animations) rewardLabel else "",
                floatingReward = if (settings.animations) (item.id to floatLabel) else null,
                burstTaskId = if (settings.animations) item.id else null
            )
        }

        undoHideJob?.cancel()
        undoHideJob = viewModelScope.launch {
            delay(5000L)
            _uiState.update { cur -> if (cur.undoTask?.id == item.id) cur.copy(undoTask = null) else cur }
        }
        if (settings.animations) {
            viewModelScope.launch {
                delay(1300L)
                _uiState.update { cur ->
                    cur.copy(
                        floatingReward = if (cur.floatingReward?.first == item.id) null else cur.floatingReward,
                        burstTaskId = if (cur.burstTaskId == item.id) null else cur.burstTaskId
                    )
                }
            }
            viewModelScope.launch {
                delay(2200L)
                _uiState.update { it.copy(rewardToast = "") }
            }
        }

        AlarmScheduler.cancelTaskAlarm(appContext, item.id)
        viewModelScope.launch {
            repository.upsert(updated, state.session?.userId)
            persistRemoteTask(updated)
        }
        if (state.focusTaskId == item.id) endFocus(saveQualified = false)
        val currentBuddy = state.buddy
        if (currentBuddy != null && item.id in prefs.getSharedTasks()) {
            viewModelScope.launch {
                supabase.pushBuddyStatus(currentBuddy, "", BuddyActivity.DONE, "done-${System.currentTimeMillis()}")
            }
        }
    }

    fun undoCompletion() {
        val toRestore = _uiState.value.undoTask ?: return
        val restored = toRestore.copy(completed = false)
        val reward = lastRewardInfo?.takeIf { it.taskId == restored.id }
        val deduct = reward?.let { it.points + it.bonus } ?: 10
        val nextPoints = (_uiState.value.points - deduct).coerceAtLeast(0)
        val snapshot = undoStreakSnapshot
        val nextStreak = snapshot?.first ?: _uiState.value.streak
        val nextLastDay = snapshot?.second ?: _uiState.value.lastDay

        prefs.points = nextPoints
        prefs.streak = nextStreak
        prefs.lastDay = nextLastDay

        var nextQualified = _uiState.value.qualifiedFocusTasks
        if (reward?.points == 15) {
            nextQualified = nextQualified + restored.id
            prefs.saveQualifiedFocusTasks(nextQualified)
        }

        lastRewardInfo = null
        undoStreakSnapshot = null
        _uiState.update {
            it.copy(
                undoTask = null,
                points = nextPoints,
                streak = nextStreak,
                lastDay = nextLastDay,
                qualifiedFocusTasks = nextQualified
            )
        }

        viewModelScope.launch {
            repository.upsert(restored, _uiState.value.session?.userId)
            persistRemoteTask(restored)
            if (!restored.reminderAt.isNullOrBlank()) {
                val triggerMs = runCatching { Instant.parse(restored.reminderAt).toEpochMilli() }.getOrNull()
                if (triggerMs != null && triggerMs > System.currentTimeMillis()) {
                    AlarmScheduler.scheduleTaskAlarm(appContext, restored.id, restored.title, triggerMs)
                }
            }
        }
    }

    // Focus session
    fun beginFocus(task: SavedTask) {
        if (task.completed) return
        stopAlarmSound()
        currentFocusSessionId = "focus-${System.currentTimeMillis()}"
        _uiState.update {
            it.copy(
                focusTaskId = task.id,
                focusMinutes = 25,
                focusRemainingSeconds = 25 * 60,
                focusElapsedSeconds = 0,
                focusPaused = false,
                focusQualified = false,
                focusMicroStep = task.microStep,
                focusProgressNote = task.progressNote,
                checkInOpen = false,
                checkInExpired = false
            )
        }
        startFocusTicker()
        pushBuddyFocusUpdate()
    }

    fun selectFocusPreset(minutes: Int) {
        if (_uiState.value.focusElapsedSeconds > 0) return
        _uiState.update {
            it.copy(focusMinutes = minutes, focusRemainingSeconds = minutes * 60)
        }
    }

    fun updateFocusDetail(microStep: String? = null, progressNote: String? = null) {
        val taskId = _uiState.value.focusTaskId ?: return
        _uiState.update { state ->
            state.copy(
                focusMicroStep = microStep ?: state.focusMicroStep,
                focusProgressNote = progressNote ?: state.focusProgressNote
            )
        }
        val current = _uiState.value.tasks.firstOrNull { it.id == taskId } ?: return
        val updated = current.copy(
            microStep = microStep ?: current.microStep,
            progressNote = progressNote ?: current.progressNote
        )
        viewModelScope.launch {
            repository.upsert(updated, _uiState.value.session?.userId)
        }
    }

    fun saveFocusDetailsToCloud() {
        val taskId = _uiState.value.focusTaskId ?: return
        val current = _uiState.value.tasks.firstOrNull { it.id == taskId } ?: return
        val updated = current.copy(
            microStep = _uiState.value.focusMicroStep,
            progressNote = _uiState.value.focusProgressNote
        )
        viewModelScope.launch {
            repository.upsert(updated, _uiState.value.session?.userId)
            persistRemoteTask(updated)
        }
    }

    fun toggleFocusPause() {
        _uiState.update { it.copy(focusPaused = !it.focusPaused) }
        pushBuddyFocusUpdate()
    }

    fun respondToCheckIn(continueSession: Boolean) {
        checkInExpireJob?.cancel()
        _uiState.update {
            it.copy(
                checkInOpen = false,
                checkInExpired = false,
                focusQualified = if (continueSession) true else it.focusQualified,
                focusPaused = !continueSession
            )
        }
        pushBuddyFocusUpdate()
    }

    fun respondToExpiredCheckIn(continueSession: Boolean) {
        _uiState.update {
            it.copy(
                checkInExpired = false,
                focusPaused = !continueSession
            )
        }
        pushBuddyFocusUpdate()
    }

    fun endFocus(saveQualified: Boolean = true) {
        focusTimerJob?.cancel()
        checkInExpireJob?.cancel()
        val state = _uiState.value
        var nextQualified = state.qualifiedFocusTasks
        if (saveQualified && state.focusTaskId != null && state.focusQualified) {
            nextQualified = nextQualified + state.focusTaskId
            prefs.saveQualifiedFocusTasks(nextQualified)
        }
        _uiState.update {
            it.copy(
                focusTaskId = null,
                focusPaused = false,
                checkInOpen = false,
                checkInExpired = false,
                qualifiedFocusTasks = nextQualified
            )
        }
    }

    private fun startFocusTicker() {
        focusTimerJob?.cancel()
        focusTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val s = _uiState.value
                if (s.focusTaskId == null) break
                if (s.focusPaused || s.checkInOpen || s.focusRemainingSeconds <= 0) continue

                val nextRemaining = s.focusRemainingSeconds - 1
                val nextElapsed = s.focusElapsedSeconds + 1
                val checkInTrigger = nextElapsed > 0 && nextElapsed % (s.checkInFrequency * 60) == 0
                val finishedNow = nextRemaining <= 0

                _uiState.update { cur ->
                    cur.copy(
                        focusRemainingSeconds = nextRemaining.coerceAtLeast(0),
                        focusElapsedSeconds = nextElapsed,
                        focusPaused = if (finishedNow) true else cur.focusPaused,
                        focusQualified = if (finishedNow) true else cur.focusQualified,
                        rewardToast = if (finishedNow) "Focus session complete · you showed up" else cur.rewardToast,
                        checkInOpen = if (checkInTrigger) true else cur.checkInOpen
                    )
                }

                if (finishedNow && _uiState.value.rewardSettings.notifySound) {
                    AlarmSoundPlayer.playTone(freqHz = 780.0, durationMs = 260, volume = 0.25f)
                }
                if (checkInTrigger) {
                    if (_uiState.value.rewardSettings.notifySound) {
                        AlarmSoundPlayer.playTone(freqHz = 620.0, durationMs = 200, volume = 0.2f)
                    }
                    startCheckInTimeout()
                }
            }
        }
    }

    private fun startCheckInTimeout() {
        checkInExpireJob?.cancel()
        checkInExpireJob = viewModelScope.launch {
            delay(120_000L)
            _uiState.update {
                if (it.checkInOpen) it.copy(checkInOpen = false, focusPaused = true, checkInExpired = true)
                else it
            }
        }
    }

    private fun pushBuddyFocusUpdate() {
        val currentBuddy = _uiState.value.buddy ?: return
        val focusId = _uiState.value.focusTaskId ?: return
        val task = _uiState.value.tasks.firstOrNull { it.id == focusId } ?: return
        val label = if (focusId in prefs.getSharedTasks()) task.title else "Working on something"
        val activity = if (_uiState.value.focusPaused) BuddyActivity.BREAK else BuddyActivity.FOCUSING
        viewModelScope.launch {
            supabase.pushBuddyStatus(currentBuddy, label, activity, currentFocusSessionId)
        }
    }

    // Comfort settings
    fun updateRewardSetting(transform: (RewardSettings) -> RewardSettings) {
        val next = transform(_uiState.value.rewardSettings)
        prefs.saveRewardSettings(next)
        _uiState.update { it.copy(rewardSettings = next) }
    }

    fun setCheckInFrequency(minutes: Int) {
        prefs.checkInFrequency = minutes
        _uiState.update { it.copy(checkInFrequency = minutes) }
    }

    // Timed plan builder
    fun updatePlanGoal(goal: String) {
        _uiState.update { it.copy(planGoal = goal) }
    }

    fun updatePlanTotalMinutes(minutes: Int) {
        _uiState.update { it.copy(planTotalMinutes = minutes) }
    }

    fun createTimedPlan() {
        val goal = _uiState.value.planGoal.trim()
        if (goal.isEmpty()) {
            _uiState.update { it.copy(planNotice = "Describe the task first.") }
            return
        }
        stopPlanSchedules()
        planTimerJob?.cancel()
        val steps = AssistantFallback.buildTimedPlanSteps(goal, _uiState.value.planTotalMinutes)
        planStepStartedAtMs = 0L
        planPausedElapsedMs = 0L
        _uiState.update {
            it.copy(
                planSteps = steps,
                planCurrentIndex = 0,
                planStatus = PlanStatus.READY,
                planStepRemainingMs = (steps.firstOrNull()?.minutes ?: 0) * 60_000L,
                planNotice = "Plan created. Adjust any step, then start the plan."
            )
        }
    }

    fun changePlanStepMinutes(index: Int, minutes: Int) {
        val clamped = minutes.coerceIn(1, 240)
        val updated = _uiState.value.planSteps.mapIndexed { i, step ->
            if (i == index) step.copy(minutes = clamped) else step
        }
        val wasRunning = _uiState.value.planStatus == PlanStatus.RUNNING
        if (wasRunning) {
            stopPlanSchedules()
            planTimerJob?.cancel()
        }
        _uiState.update {
            it.copy(
                planSteps = updated,
                planStatus = if (wasRunning) PlanStatus.PAUSED else it.planStatus,
                planStepRemainingMs = if (index == it.planCurrentIndex) clamped * 60_000L else it.planStepRemainingMs,
                planNotice = if (wasRunning) "Timing changed. Start again to apply the new schedule." else it.planNotice
            )
        }
    }

    fun startOrResumeTimedPlan(hasNotificationPermission: Boolean) {
        val steps = _uiState.value.planSteps
        if (steps.isEmpty()) return
        val startIndex = if (_uiState.value.planStatus == PlanStatus.FINISHED) 0 else _uiState.value.planCurrentIndex
        stopPlanSchedules()
        val now = System.currentTimeMillis()
        planStepStartedAtMs = now
        planPausedElapsedMs = 0L

        if (hasNotificationPermission) {
            var offsetMs = 0L
            val scheduleList = mutableListOf<Pair<String, Long>>()
            for (i in startIndex until steps.size) {
                val step = steps[i]
                scheduleList.add(step.title to (now + offsetMs))
                offsetMs += step.minutes * 60_000L
            }
            scheduledPlanIds = AlarmScheduler.schedulePlanStepNotifications(appContext, scheduleList)
        }

        _uiState.update {
            it.copy(
                planCurrentIndex = startIndex,
                planStatus = PlanStatus.RUNNING,
                planStepRemainingMs = steps[startIndex].minutes * 60_000L,
                planNotice = if (hasNotificationPermission) {
                    "Plan started. Step reminders are scheduled."
                } else {
                    "Plan started. Allow notifications for background step reminders."
                }
            )
        }
        startPlanTicker()
    }

    fun pauseTimedPlan() {
        if (_uiState.value.planStatus != PlanStatus.RUNNING) return
        planTimerJob?.cancel()
        stopPlanSchedules()
        planPausedElapsedMs = (System.currentTimeMillis() - planStepStartedAtMs).coerceAtLeast(0L)
        _uiState.update {
            it.copy(
                planStatus = PlanStatus.PAUSED,
                planNotice = "Paused. Start again to reschedule the remaining reminders."
            )
        }
    }

    fun skipTimedPlanStep() {
        val state = _uiState.value
        stopPlanSchedules()
        planTimerJob?.cancel()
        if (state.planCurrentIndex >= state.planSteps.lastIndex) {
            _uiState.update { it.copy(planStatus = PlanStatus.FINISHED, planStepRemainingMs = 0L) }
            return
        }
        val nextIdx = state.planCurrentIndex + 1
        planPausedElapsedMs = 0L
        _uiState.update {
            it.copy(
                planCurrentIndex = nextIdx,
                planStatus = PlanStatus.PAUSED,
                planStepRemainingMs = state.planSteps[nextIdx].minutes * 60_000L,
                planNotice = "Skipped ahead. Start the plan again to reschedule remaining reminders."
            )
        }
    }

    fun stopTimedPlan() {
        stopPlanSchedules()
        planTimerJob?.cancel()
        planStepStartedAtMs = 0L
        planPausedElapsedMs = 0L
        _uiState.update {
            it.copy(
                planStatus = PlanStatus.STOPPED,
                planCurrentIndex = 0,
                planStepRemainingMs = (it.planSteps.firstOrNull()?.minutes ?: 0) * 60_000L,
                planNotice = "Plan stopped. Your steps are still here so you can restart."
            )
        }
    }

    private fun startPlanTicker() {
        planTimerJob?.cancel()
        planTimerJob = viewModelScope.launch {
            while (true) {
                delay(500L)
                val s = _uiState.value
                if (s.planStatus != PlanStatus.RUNNING) break
                val step = s.planSteps.getOrNull(s.planCurrentIndex) ?: break
                val elapsed = (System.currentTimeMillis() - planStepStartedAtMs).coerceAtLeast(0L)
                val remaining = (step.minutes * 60_000L - elapsed).coerceAtLeast(0L)
                if (remaining <= 0L) {
                    if (s.planCurrentIndex >= s.planSteps.lastIndex) {
                        stopPlanSchedules()
                        _uiState.update {
                            it.copy(
                                planStatus = PlanStatus.FINISHED,
                                planStepRemainingMs = 0L,
                                planNotice = "Plan complete. You made it through the steps."
                            )
                        }
                        break
                    } else {
                        val nextIdx = s.planCurrentIndex + 1
                        planStepStartedAtMs = System.currentTimeMillis()
                        _uiState.update {
                            it.copy(
                                planCurrentIndex = nextIdx,
                                planStepRemainingMs = s.planSteps[nextIdx].minutes * 60_000L
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(planStepRemainingMs = remaining) }
                }
            }
        }
    }

    private fun stopPlanSchedules() {
        AlarmScheduler.cancelPlanStepNotifications(appContext, scheduledPlanIds)
        scheduledPlanIds = emptyList()
    }

    // Buddy management
    fun updateBuddyName(name: String) {
        _uiState.update { it.copy(buddyNameInput = name.take(40)) }
    }

    fun createBuddyInvite(activityContext: Context) {
        val name = _uiState.value.buddyNameInput.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(buddyMessage = "Add the name your buddy should see.") }
            return
        }
        if (!supabase.isConfigured) {
            _uiState.update { it.copy(buddyMessage = "Connect the cloud project to turn on buddy invitations.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(buddyBusy = true, buddyMessage = "") }
            prefs.buddyOwnerName = name
            val created = supabase.createBuddyInvite(name)
            if (created == null) {
                _uiState.update {
                    it.copy(buddyBusy = false, buddyMessage = "Couldn't create the invite right now. Try again.")
                }
                return@launch
            }
            prefs.saveLocalBuddy(created)
            _uiState.update {
                it.copy(
                    buddyBusy = false,
                    buddy = created,
                    buddyMessage = "Invite created. Share the link with your buddy."
                )
            }
            startBuddyOwnerPollingIfNeeded()
            shareBuddyInvite(activityContext)
        }
    }

    fun shareBuddyInvite(activityContext: Context) {
        val target = _uiState.value.buddy ?: return
        val link = supabase.buildInviteLink(target.token)
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vinai buddy invite", link))

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Vinai · accountability buddy")
            putExtra(Intent.EXTRA_TEXT, "${target.ownerName} invited you to be their buddy: $link")
        }
        runCatching {
            activityContext.startActivity(Intent.createChooser(shareIntent, "Share buddy invite"))
        }.onFailure {
            _uiState.update { it.copy(buddyMessage = "Invite link copied: $link") }
        }
    }

    fun changeBuddySharing(newState: BuddyState) {
        val current = _uiState.value.buddy ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(buddyBusy = true) }
            val ok = supabase.setBuddyState(current, newState)
            if (!ok) {
                _uiState.update {
                    it.copy(buddyBusy = false, buddyMessage = "Could not update buddy sharing. Your setting is unchanged.")
                }
                return@launch
            }
            if (newState == BuddyState.REMOVED) {
                prefs.saveLocalBuddy(null)
                buddyOwnerPollJob?.cancel()
                _uiState.update {
                    it.copy(buddyBusy = false, buddy = null, buddyMessage = "Buddy disconnected.")
                }
            } else {
                val next = current.copy(state = newState)
                prefs.saveLocalBuddy(next)
                _uiState.update {
                    it.copy(
                        buddyBusy = false,
                        buddy = next,
                        buddyMessage = if (newState == BuddyState.PAUSED) "Buddy sharing paused." else "Buddy sharing resumed."
                    )
                }
            }
        }
    }

    private fun startBuddyOwnerPollingIfNeeded() {
        buddyOwnerPollJob?.cancel()
        if (_uiState.value.buddy == null || !supabase.isConfigured) return
        buddyOwnerPollJob = viewModelScope.launch {
            while (true) {
                val current = _uiState.value.buddy ?: break
                val result = supabase.pollOwner(current)
                if (result != null) {
                    val next = current.copy(
                        state = result.state,
                        lastCheerId = maxOf(current.lastCheerId, result.lastId)
                    )
                    if (next != current) {
                        prefs.saveLocalBuddy(next)
                        _uiState.update { it.copy(buddy = next) }
                    }
                    if (result.emojis.isNotEmpty()) {
                        val emoji = result.emojis.last()
                        _uiState.update {
                            it.copy(
                                rewardToast = "Your buddy sent a cheer $emoji",
                                cheerFloat = emoji
                            )
                        }
                        launch {
                            delay(1800L)
                            _uiState.update { it.copy(cheerFloat = "") }
                        }
                        launch {
                            delay(2400L)
                            _uiState.update { it.copy(rewardToast = "") }
                        }
                    }
                }
                delay(8000L)
            }
        }
    }

    // Deep-linked BuddyView
    fun openBuddyViewer(rawInput: String) {
        val token = extractBuddyToken(rawInput) ?: return
        val accepted = prefs.isBuddyAccepted(token)
        _uiState.update {
            it.copy(
                incomingBuddyToken = token,
                buddyViewLoaded = false,
                buddyViewData = null,
                buddyViewAccepted = accepted,
                buddyViewDeclined = false,
                buddyViewFeedback = ""
            )
        }
        buddyViewPollJob?.cancel()
        buddyViewPollJob = viewModelScope.launch {
            while (true) {
                val activeToken = _uiState.value.incomingBuddyToken ?: break
                val data = supabase.fetchBuddyView(activeToken)
                _uiState.update { it.copy(buddyViewData = data, buddyViewLoaded = true) }
                delay(if (_uiState.value.buddyViewAccepted) 5000L else 15000L)
            }
        }
    }

    fun closeBuddyViewer() {
        buddyViewPollJob?.cancel()
        _uiState.update { it.copy(incomingBuddyToken = null) }
    }

    fun acceptBuddyViewerInvite() {
        val token = _uiState.value.incomingBuddyToken ?: return
        viewModelScope.launch {
            val ok = supabase.acceptBuddyInvite(token)
            if (ok) {
                prefs.setBuddyAccepted(token, true)
                _uiState.update { it.copy(buddyViewAccepted = true) }
            }
            val refreshed = supabase.fetchBuddyView(token)
            _uiState.update { it.copy(buddyViewData = refreshed, buddyViewLoaded = true) }
        }
    }

    fun declineBuddyViewerInvite() {
        _uiState.update { it.copy(buddyViewDeclined = true) }
    }

    fun sendBuddyCheer(emoji: String) {
        val token = _uiState.value.incomingBuddyToken ?: return
        viewModelScope.launch {
            val ok = supabase.sendCheer(token, emoji)
            val feedback = if (ok) "Sent $emoji" else "That's plenty of cheers for now 💚"
            val refreshed = supabase.fetchBuddyView(token)
            _uiState.update { it.copy(buddyViewData = refreshed, buddyViewFeedback = feedback) }
            delay(2000L)
            _uiState.update { it.copy(buddyViewFeedback = "") }
        }
    }

    private fun extractBuddyToken(raw: String): String? {
        val trimmed = raw.trim()
        val match = Regex("buddy=([a-f0-9]{16,64})").find(trimmed)?.groupValues?.getOrNull(1)
        if (match != null) return match
        return if (Regex("^[a-f0-9]{16,64}$").matches(trimmed)) trimmed else null
    }

    // Ringing Alarm Modal
    fun triggerRingingAlarm(title: String, taskId: String?) {
        _uiState.update { it.copy(ringingAlarm = RingingAlarmState(title, taskId)) }
        alarmBeepJob?.cancel()
        alarmBeepJob = viewModelScope.launch {
            AlarmSoundPlayer.vibrate(appContext, longArrayOf(0, 180, 120, 180))
            while (true) {
                AlarmSoundPlayer.playTone(freqHz = 740.0, durationMs = 320, volume = 0.25f)
                delay(1500L)
            }
        }
    }

    fun stopAlarmSound() {
        alarmBeepJob?.cancel()
        alarmBeepJob = null
        _uiState.update { it.copy(ringingAlarm = null) }
    }

    fun snoozeRingingAlarm() {
        val taskId = _uiState.value.ringingAlarm?.taskId
        stopAlarmSound()
        if (taskId.isNullOrBlank()) return
        val original = _uiState.value.tasks.firstOrNull { it.id == taskId } ?: return
        val snoozeInstant = Instant.now().plusSeconds(5 * 60L)
        val localTime = snoozeInstant.atZone(ZoneId.systemDefault()).toLocalTime()
        val time24 = DateTimeFormatter.ofPattern("HH:mm").format(localTime)
        val updated = original.copy(
            reminderAt = snoozeInstant.toString(),
            reminderTime = time24
        )
        viewModelScope.launch {
            repository.upsert(updated, _uiState.value.session?.userId)
            persistRemoteTask(updated)
            AlarmScheduler.scheduleTaskAlarm(appContext, updated.id, updated.title, snoozeInstant.toEpochMilli())
            _uiState.update { it.copy(notice = "Snoozed for five minutes.") }
        }
    }

    fun startTaskFromAlarm(explicitTask: SavedTask? = null) {
        val target = explicitTask ?: _uiState.value.ringingAlarm?.taskId?.let { id ->
            _uiState.value.tasks.firstOrNull { it.id == id }
        }
        stopAlarmSound()
        if (target == null) return
        AlarmScheduler.cancelTaskAlarm(appContext, target.id)
        val cleared = target.copy(reminderAt = null, reminderTime = null)
        viewModelScope.launch {
            repository.upsert(cleared, _uiState.value.session?.userId)
            persistRemoteTask(cleared)
        }
        beginFocus(cleared)
    }

    private suspend fun persistRemoteTask(task: SavedTask) {
        val session = _uiState.value.session ?: return
        if (!supabase.isConfigured) return
        val error = supabase.upsertTask(task, session)
        if (error != null) {
            _uiState.update { it.copy(notice = "Saved on this device. Cloud sync: $error") }
        }
    }

    private fun computeNextOccurrenceIso(time24: String): String {
        val parts = time24.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var target = ZonedDateTime.of(now.toLocalDate(), LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59)), zone)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return target.toInstant().toString()
    }

    override fun onCleared() {
        super.onCleared()
        focusTimerJob?.cancel()
        checkInExpireJob?.cancel()
        planTimerJob?.cancel()
        buddyOwnerPollJob?.cancel()
        buddyViewPollJob?.cancel()
        alarmBeepJob?.cancel()
    }
}
