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
import com.todo.foradhd.data.PlanStepKind
import com.todo.foradhd.data.QuickNote
import com.todo.foradhd.data.RewardSettings
import com.todo.foradhd.data.SavedBreakdownState
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
    val taskId: String? = null,
    val timeLabel: String? = null,
    val isPlanStep: Boolean = false
)

data class LastRewardInfo(
    val taskId: String,
    val points: Int,
    val bonus: Int
)

data class VinaiUiState(
    val showSplash: Boolean = true,
    val isDarkTheme: Boolean = true,
    val tab: AppTab = AppTab.TODAY,
    val todayFormatted: String = "",
    val todayIsoKey: String = "",
    val session: UserSession? = null,
    val supabaseConfigured: Boolean = false,
    val showAccountModal: Boolean = false,
    val isSignUpMode: Boolean = false,
    val authBusy: Boolean = false,
    val authMessage: String = "",

    // Category filter
    val selectedCategory: String = "All",

    // Add / Edit Task sheet
    val taskSheetOpen: Boolean = false,
    val editingTask: SavedTask? = null,
    val taskDraftTitle: String = "",
    val taskDraftMicroStep: String = "",
    val taskDraftCategory: String = "Personal",
    val taskDraftCategoryManual: Boolean = false,
    val taskDraftRequiresVerification: Boolean = false,
    val taskDraftEstimateMinutes: Int = 10,
    val reminderTime24: String? = null,
    val reminderAtIso: String? = null,
    val clockPickerOpen: Boolean = false,
    val assistantBusy: Boolean = false,
    val assistantResponse: AssistantResponse? = null,
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
    val focusStartedFromCheckoff: Boolean = false,
    val focusMicroStep: String = "",
    val focusProgressNote: String = "",
    val checkInOpen: Boolean = false,
    val checkInExpired: Boolean = false,

    // Timed plan / AI Task Breakdown
    val planGoal: String = "",
    val planTotalMinutes: Int = 120,
    val planStrictTimeLimit: Boolean = false,
    val planParentTaskId: String? = null,
    val planSteps: List<PlanStep> = emptyList(),
    val planStatus: PlanStatus = PlanStatus.READY,
    val planCurrentIndex: Int = 0,
    val planStepRemainingMs: Long = 0L,
    val planNotice: String = "",

    // Quick Notes
    val quickNotes: List<QuickNote> = emptyList(),
    val quickNoteInput: String = "",

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
    private var planStepBaseDurationMs: Long = 0L
    private var scheduledPlanIds: List<Int> = emptyList()

    private var undoStreakSnapshot: Pair<Int, String>? = null
    private var lastRewardInfo: LastRewardInfo? = null
    private var currentFocusSessionId: String = ""

    init {
        AlarmScheduler.createNotificationChannel(appContext)
        viewModelScope.launch {
            delay(1350L)
            _uiState.update { it.copy(showSplash = false) }
        }
        observeTasksForCurrentUser()
        syncRemoteTasksIfSignedIn()
        startBuddyOwnerPollingIfNeeded()
        resumePersistedBreakdownTickerIfNeeded()
    }

    private fun createInitialState(): VinaiUiState {
        val now = LocalDate.now()
        val formattedDate = DateTimeFormatter.ofPattern("EEE, MMM d").format(now)
        val todayKey = DateTimeFormatter.ISO_LOCAL_DATE.format(now)
        val savedPlan = prefs.getBreakdownState()
        val restoredStatus = when (savedPlan?.status) {
            PlanStatus.RUNNING -> PlanStatus.PAUSED // Safe resume after process kill
            else -> savedPlan?.status ?: PlanStatus.READY
        }
        return VinaiUiState(
            isDarkTheme = prefs.isDarkTheme,
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
            quickNotes = prefs.getQuickNotes(),
            planGoal = savedPlan?.goal ?: "",
            planTotalMinutes = savedPlan?.totalMinutes ?: 120,
            planStrictTimeLimit = savedPlan?.strictTimeLimit ?: false,
            planParentTaskId = savedPlan?.parentTaskId,
            planSteps = savedPlan?.steps ?: emptyList(),
            planStatus = restoredStatus,
            planCurrentIndex = savedPlan?.currentIndex ?: 0,
            planStepRemainingMs = savedPlan?.stepRemainingMs ?: 0L,
            planNotice = if (savedPlan != null && savedPlan.steps.isNotEmpty() && restoredStatus == PlanStatus.PAUSED) {
                "Your unfinished breakdown is saved. Tap Resume to pick up where you left off."
            } else "",
            buddy = prefs.getLocalBuddy(),
            buddyNameInput = prefs.buddyOwnerName
        )
    }

    private fun resumePersistedBreakdownTickerIfNeeded() {
        val s = _uiState.value
        if (s.planSteps.isNotEmpty() && s.planStepRemainingMs > 0L) {
            planStepBaseDurationMs = s.planStepRemainingMs
        }
    }

    private fun persistBreakdownState() {
        val s = _uiState.value
        if (s.planSteps.isEmpty() && s.planGoal.isBlank()) {
            prefs.saveBreakdownState(null)
        } else {
            prefs.saveBreakdownState(
                SavedBreakdownState(
                    goal = s.planGoal,
                    totalMinutes = s.planTotalMinutes,
                    steps = s.planSteps,
                    status = s.planStatus,
                    currentIndex = s.planCurrentIndex,
                    stepRemainingMs = s.planStepRemainingMs,
                    parentTaskId = s.planParentTaskId,
                    strictTimeLimit = s.planStrictTimeLimit
                )
            )
        }
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
                byId[entry.id] = if (existing != null) {
                    entry.copy(
                        progressNote = existing.progressNote.ifBlank { entry.progressNote },
                        requiresVerification = existing.requiresVerification,
                        category = existing.category,
                        completedAt = existing.completedAt
                    )
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

    fun toggleTheme() {
        val next = !_uiState.value.isDarkTheme
        prefs.isDarkTheme = next
        _uiState.update { it.copy(isDarkTheme = next) }
    }

    fun setDarkTheme(dark: Boolean) {
        prefs.isDarkTheme = dark
        _uiState.update { it.copy(isDarkTheme = dark) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
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

    // Add / Edit Task Sheet
    fun openAddTaskSheet(initialTitle: String = "") {
        val autoTag = if (initialTitle.isNotBlank()) AssistantFallback.autoTagTask(initialTitle) else "Personal"
        _uiState.update {
            it.copy(
                taskSheetOpen = true,
                editingTask = null,
                taskDraftTitle = initialTitle,
                taskDraftMicroStep = "",
                taskDraftCategory = autoTag,
                taskDraftCategoryManual = false,
                taskDraftRequiresVerification = false,
                taskDraftEstimateMinutes = 10,
                reminderTime24 = null,
                reminderAtIso = null,
                assistantResponse = null
            )
        }
    }

    fun openEditTaskSheet(task: SavedTask) {
        _uiState.update {
            it.copy(
                taskSheetOpen = true,
                editingTask = task,
                taskDraftTitle = task.title,
                taskDraftMicroStep = task.microStep,
                taskDraftCategory = task.category,
                taskDraftCategoryManual = true,
                taskDraftRequiresVerification = task.requiresVerification,
                taskDraftEstimateMinutes = task.estimateMinutes ?: 10,
                reminderTime24 = task.reminderTime,
                reminderAtIso = task.reminderAt,
                assistantResponse = null
            )
        }
    }

    fun closeTaskSheet() {
        _uiState.update {
            it.copy(
                taskSheetOpen = false,
                editingTask = null,
                assistantResponse = null
            )
        }
    }

    fun updateTaskDraftTitle(text: String) {
        _uiState.update { cur ->
            val nextCategory = if (cur.taskDraftCategoryManual) {
                cur.taskDraftCategory
            } else {
                AssistantFallback.autoTagTask(text, cur.taskDraftMicroStep)
            }
            cur.copy(taskDraftTitle = text, taskDraftCategory = nextCategory)
        }
    }

    fun updateTaskDraftMicroStep(text: String) {
        _uiState.update { cur ->
            val nextCategory = if (cur.taskDraftCategoryManual) {
                cur.taskDraftCategory
            } else {
                AssistantFallback.autoTagTask(cur.taskDraftTitle, text)
            }
            cur.copy(taskDraftMicroStep = text, taskDraftCategory = nextCategory)
        }
    }

    fun selectTaskDraftCategory(category: String) {
        _uiState.update { it.copy(taskDraftCategory = category, taskDraftCategoryManual = true) }
    }

    fun toggleTaskDraftRequiresVerification(required: Boolean) {
        _uiState.update { it.copy(taskDraftRequiresVerification = required) }
    }

    fun setTaskDraftEstimate(minutes: Int) {
        _uiState.update { it.copy(taskDraftEstimateMinutes = minutes) }
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

    fun generateMicroStepSuggestion() {
        val title = _uiState.value.taskDraftTitle.trim()
        if (title.isEmpty() || _uiState.value.assistantBusy) return
        val session = _uiState.value.session
        viewModelScope.launch {
            _uiState.update { it.copy(assistantBusy = true) }
            val result = if (supabase.isConfigured && session != null) {
                supabase.invokeStepwiseAssistant(title, TaskMode.NEW_TASK, session)
            } else null
            val finalResp = result ?: AssistantFallback.getDemoResponse(title, TaskMode.NEW_TASK)
            _uiState.update {
                it.copy(
                    assistantBusy = false,
                    assistantResponse = finalResp,
                    taskDraftMicroStep = it.taskDraftMicroStep.ifBlank { finalResp.theMicroStep }
                )
            }
        }
    }

    fun saveTaskFromSheet(hasNotificationPermission: Boolean) {
        val state = _uiState.value
        val rawTitle = state.taskDraftTitle.trim()
        if (rawTitle.isEmpty()) return

        val existing = state.editingTask
        val microStep = state.taskDraftMicroStep.trim().ifEmpty {
            AssistantFallback.getDemoResponse(rawTitle, TaskMode.NEW_TASK).theMicroStep
        }
        val category = state.taskDraftCategory.ifBlank {
            AssistantFallback.autoTagTask(rawTitle, microStep)
        }

        val savedTask = if (existing != null) {
            existing.copy(
                title = rawTitle,
                microStep = microStep,
                category = category,
                requiresVerification = state.taskDraftRequiresVerification,
                reminderTime = state.reminderTime24,
                reminderAt = state.reminderAtIso,
                estimateMinutes = state.taskDraftEstimateMinutes
            )
        } else {
            SavedTask(
                id = UUID.randomUUID().toString(),
                title = rawTitle,
                mode = TaskMode.NEW_TASK,
                microStep = microStep,
                createdAt = Instant.now().toString(),
                completed = false,
                reminderTime = state.reminderTime24,
                reminderAt = state.reminderAtIso,
                estimateMinutes = state.taskDraftEstimateMinutes,
                actualMinutes = null,
                progressNote = "",
                requiresVerification = state.taskDraftRequiresVerification,
                category = category,
                completedAt = null
            )
        }

        viewModelScope.launch {
            repository.upsert(savedTask, state.session?.userId)
            persistRemoteTask(savedTask)

            AlarmScheduler.cancelTaskAlarm(appContext, savedTask.id)
            if (!savedTask.completed && !savedTask.reminderAt.isNullOrBlank()) {
                val triggerMs = runCatching { Instant.parse(savedTask.reminderAt).toEpochMilli() }.getOrNull()
                if (triggerMs != null && triggerMs > System.currentTimeMillis() && hasNotificationPermission) {
                    val label = savedTask.reminderTime?.let { format12h(it) }
                    AlarmScheduler.scheduleTaskAlarm(appContext, savedTask.id, savedTask.title, triggerMs, label)
                }
            }

            val timeText = savedTask.reminderTime?.let { format12h(it) }
            val noticeMsg = when {
                !savedTask.reminderAt.isNullOrBlank() && hasNotificationPermission && timeText != null ->
                    "Alarm set for $timeText: \"${savedTask.title}\"."
                !savedTask.reminderAt.isNullOrBlank() ->
                    "Saved. Enable notification permission in Settings for exact alarms."
                existing != null -> "Task updated."
                else -> "Task added to your runway."
            }

            _uiState.update {
                it.copy(
                    taskSheetOpen = false,
                    editingTask = null,
                    taskDraftTitle = "",
                    taskDraftMicroStep = "",
                    taskDraftRequiresVerification = false,
                    reminderTime24 = null,
                    reminderAtIso = null,
                    assistantResponse = null,
                    notice = noticeMsg
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

    /**
     * CHANGE 2: Optional verification chosen per task.
     * - requiresVerification == false: completes immediately with normal reward.
     * - requiresVerification == true: starts focus timer & verification flow; completes only after verification succeeds.
     */
    fun onTaskCheckboxClicked(task: SavedTask) {
        if (task.completed) return
        if (!task.requiresVerification) {
            viewModelScope.launch {
                _uiState.update { it.copy(leavingTaskIds = it.leavingTaskIds + task.id) }
                delay(320L)
                _uiState.update { it.copy(leavingTaskIds = it.leavingTaskIds - task.id) }
                giveReward(task, note = "")
                _uiState.update { it.copy(notice = "Completed! Nice momentum.") }
            }
        } else {
            beginFocus(task, fromCheckoff = true)
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
        viewModelScope.launch {
            _uiState.update { it.copy(verificationBusy = true, verificationError = "") }

            val approvedAndReason: Pair<Boolean, String> = if (supabase.isConfigured && session != null) {
                when (val res = supabase.invokeVerifyTaskStart(target.title, target.microStep, note, session)) {
                    is SupabaseApi.VerifyResult.Failure -> {
                        val localRes = AssistantFallback.verifyLocally(target.title, target.microStep, note)
                        localRes.approved to localRes.reason
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
            delay(360L)
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
            todayKey -> state.streak.coerceAtLeast(1)
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
        val updated = item.copy(
            completed = true,
            progressNote = combinedNote,
            completedAt = Instant.now().toString()
        )

        val settings = state.rewardSettings
        val rewardLabel = if (bonus > 0) "+$basePoints points · +25 bonus!" else "+$basePoints points · nice work"
        val floatLabel = if (bonus > 0) "+$basePoints · +25 bonus" else "+$basePoints"

        if (settings.haptics) {
            AlarmSoundPlayer.vibrate(
                appContext,
                if (bonus > 0) longArrayOf(0, 30, 40, 30) else longArrayOf(0, 22)
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
        val restored = toRestore.copy(completed = false, completedAt = null)
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
                    val label = restored.reminderTime?.let { format12h(it) }
                    AlarmScheduler.scheduleTaskAlarm(appContext, restored.id, restored.title, triggerMs, label)
                }
            }
        }
    }

    // Focus session
    fun beginFocus(task: SavedTask, fromCheckoff: Boolean = false) {
        if (task.completed) return
        stopAlarmSound()
        currentFocusSessionId = "focus-${System.currentTimeMillis()}"
        val defaultMins = (task.estimateMinutes ?: 25).coerceIn(5, 45)
        _uiState.update {
            it.copy(
                focusTaskId = task.id,
                focusMinutes = defaultMins,
                focusRemainingSeconds = defaultMins * 60,
                focusElapsedSeconds = 0,
                focusPaused = false,
                focusQualified = fromCheckoff || task.requiresVerification,
                focusStartedFromCheckoff = fromCheckoff,
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
                focusStartedFromCheckoff = false,
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
                    AlarmSoundPlayer.playCustomChime(appContext)
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

    // AI Task Breakdown / Timed Plan Builder
    fun updatePlanGoal(goal: String) {
        _uiState.update { it.copy(planGoal = goal) }
    }

    fun updatePlanTotalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 480)
        _uiState.update { it.copy(planTotalMinutes = clamped) }
    }

    fun togglePlanStrictTime(strict: Boolean) {
        _uiState.update { it.copy(planStrictTimeLimit = strict) }
    }

    fun rescaleExistingPlan(newTotalMinutes: Int) {
        val clamped = newTotalMinutes.coerceIn(5, 480)
        val currentSteps = _uiState.value.planSteps
        if (currentSteps.isEmpty()) {
            _uiState.update { it.copy(planTotalMinutes = clamped) }
            return
        }
        stopPlanSchedules()
        planTimerJob?.cancel()
        val rescaled = AssistantFallback.rescaleBreakdown(currentSteps, clamped)
        val curIdx = _uiState.value.planCurrentIndex.coerceIn(0, rescaled.lastIndex)
        val nextRem = rescaled[curIdx].minutes * 60_000L
        planStepBaseDurationMs = nextRem
        _uiState.update {
            it.copy(
                planTotalMinutes = clamped,
                planSteps = rescaled,
                planStatus = if (it.planStatus == PlanStatus.RUNNING) PlanStatus.PAUSED else it.planStatus,
                planStepRemainingMs = nextRem,
                planNotice = "Subtask durations adjusted to fit ${clamped}m."
            )
        }
        persistBreakdownState()
    }

    fun breakDownExistingTask(task: SavedTask) {
        stopPlanSchedules()
        planTimerJob?.cancel()
        val total = (task.estimateMinutes ?: 60).coerceIn(10, 240)
        val steps = AssistantFallback.buildTimedPlanSteps(task.title, total, strictTimeLimit = false)
        val firstMs = (steps.firstOrNull()?.minutes ?: 0) * 60_000L
        planStepStartedAtMs = 0L
        planStepBaseDurationMs = firstMs
        _uiState.update {
            it.copy(
                tab = AppTab.PLAN,
                planGoal = task.title,
                planTotalMinutes = total,
                planParentTaskId = task.id,
                planSteps = steps,
                planCurrentIndex = 0,
                planStatus = PlanStatus.READY,
                planStepRemainingMs = firstMs,
                planNotice = "Breakdown ready for \"${task.title}\". Customize steps or tap Start."
            )
        }
        persistBreakdownState()
    }

    fun createTimedPlan() {
        val goal = _uiState.value.planGoal.trim()
        if (goal.isEmpty()) {
            _uiState.update { it.copy(planNotice = "Describe the task you want to break down first.") }
            return
        }
        stopPlanSchedules()
        planTimerJob?.cancel()
        val steps = AssistantFallback.buildTimedPlanSteps(
            task = goal,
            totalMinutes = _uiState.value.planTotalMinutes,
            strictTimeLimit = _uiState.value.planStrictTimeLimit
        )
        val firstMs = (steps.firstOrNull()?.minutes ?: 0) * 60_000L
        planStepStartedAtMs = 0L
        planStepBaseDurationMs = firstMs
        _uiState.update {
            it.copy(
                planSteps = steps,
                planCurrentIndex = 0,
                planStatus = PlanStatus.READY,
                planStepRemainingMs = firstMs,
                planNotice = "Breakdown ready. Edit, reorder, or add steps anytime, then tap Start."
            )
        }
        persistBreakdownState()
    }

    fun addPlanStep(title: String, description: String, minutes: Int) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val newStep = PlanStep(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            description = description.trim(),
            minutes = minutes.coerceIn(1, 240),
            kind = PlanStepKind.WORK
        )
        val updated = _uiState.value.planSteps + newStep
        _uiState.update {
            it.copy(
                planSteps = updated,
                planTotalMinutes = updated.sumOf { s -> s.minutes }.coerceIn(5, 480),
                planNotice = "Added step \"$cleanTitle\"."
            )
        }
        persistBreakdownState()
    }

    fun editPlanStep(index: Int, title: String, description: String, minutes: Int) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val clamped = minutes.coerceIn(1, 240)
        val updated = _uiState.value.planSteps.mapIndexed { i, step ->
            if (i == index) step.copy(title = cleanTitle, description = description.trim(), minutes = clamped) else step
        }
        val wasRunning = _uiState.value.planStatus == PlanStatus.RUNNING
        if (wasRunning && index == _uiState.value.planCurrentIndex) {
            stopPlanSchedules()
            planTimerJob?.cancel()
        }
        val newRem = if (index == _uiState.value.planCurrentIndex) clamped * 60_000L else _uiState.value.planStepRemainingMs
        if (index == _uiState.value.planCurrentIndex) planStepBaseDurationMs = newRem
        _uiState.update {
            it.copy(
                planSteps = updated,
                planTotalMinutes = updated.sumOf { s -> s.minutes }.coerceIn(5, 480),
                planStatus = if (wasRunning && index == it.planCurrentIndex) PlanStatus.PAUSED else it.planStatus,
                planStepRemainingMs = newRem,
                planNotice = "Step updated."
            )
        }
        persistBreakdownState()
    }

    fun deletePlanStep(index: Int) {
        val current = _uiState.value.planSteps
        if (index !in current.indices || current.size <= 1) return
        stopPlanSchedules()
        planTimerJob?.cancel()
        val updated = current.filterIndexed { i, _ -> i != index }
        val nextIdx = _uiState.value.planCurrentIndex.coerceIn(0, updated.lastIndex)
        val nextRem = updated[nextIdx].minutes * 60_000L
        planStepBaseDurationMs = nextRem
        _uiState.update {
            it.copy(
                planSteps = updated,
                planCurrentIndex = nextIdx,
                planTotalMinutes = updated.sumOf { s -> s.minutes }.coerceIn(5, 480),
                planStatus = if (it.planStatus == PlanStatus.RUNNING) PlanStatus.PAUSED else it.planStatus,
                planStepRemainingMs = nextRem,
                planNotice = "Step removed."
            )
        }
        persistBreakdownState()
    }

    fun movePlanStep(index: Int, delta: Int) {
        val list = _uiState.value.planSteps.toMutableList()
        val target = index + delta
        if (index !in list.indices || target !in list.indices) return
        stopPlanSchedules()
        planTimerJob?.cancel()
        val item = list.removeAt(index)
        list.add(target, item)
        val newCurrent = when (_uiState.value.planCurrentIndex) {
            index -> target
            target -> index
            else -> _uiState.value.planCurrentIndex
        }
        val nextRem = list[newCurrent].minutes * 60_000L
        planStepBaseDurationMs = nextRem
        _uiState.update {
            it.copy(
                planSteps = list,
                planCurrentIndex = newCurrent,
                planStatus = if (it.planStatus == PlanStatus.RUNNING) PlanStatus.PAUSED else it.planStatus,
                planStepRemainingMs = nextRem
            )
        }
        persistBreakdownState()
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
        val nextRem = if (index == _uiState.value.planCurrentIndex) clamped * 60_000L else _uiState.value.planStepRemainingMs
        if (index == _uiState.value.planCurrentIndex) planStepBaseDurationMs = nextRem
        _uiState.update {
            it.copy(
                planSteps = updated,
                planTotalMinutes = updated.sumOf { s -> s.minutes }.coerceIn(5, 480),
                planStatus = if (wasRunning) PlanStatus.PAUSED else it.planStatus,
                planStepRemainingMs = nextRem,
                planNotice = if (wasRunning) "Timing changed. Start again to apply." else it.planNotice
            )
        }
        persistBreakdownState()
    }

    fun startOrResumeTimedPlan(hasNotificationPermission: Boolean) {
        val steps = _uiState.value.planSteps
        if (steps.isEmpty()) return
        val wasFinished = _uiState.value.planStatus == PlanStatus.FINISHED
        val workingSteps = if (wasFinished) steps.map { it.copy(completed = false) } else steps
        val startIndex = if (wasFinished) {
            0
        } else {
            val cur = _uiState.value.planCurrentIndex.coerceIn(0, workingSteps.lastIndex)
            if (workingSteps[cur].completed) {
                workingSteps.indexOfFirst { !it.completed }.takeIf { it >= 0 } ?: 0
            } else cur
        }

        val initialRemainingMs = if (!wasFinished && _uiState.value.planStatus == PlanStatus.PAUSED && _uiState.value.planStepRemainingMs > 1000L) {
            _uiState.value.planStepRemainingMs
        } else {
            workingSteps[startIndex].minutes * 60_000L
        }

        stopPlanSchedules()
        val now = System.currentTimeMillis()
        planStepStartedAtMs = now
        planStepBaseDurationMs = initialRemainingMs

        if (hasNotificationPermission) {
            var offsetMs = initialRemainingMs
            val scheduleList = mutableListOf<Pair<String, Long>>()
            scheduleList.add(workingSteps[startIndex].title to (now + offsetMs))
            for (i in (startIndex + 1) until workingSteps.size) {
                if (!workingSteps[i].completed) {
                    offsetMs += workingSteps[i].minutes * 60_000L
                    scheduleList.add(workingSteps[i].title to (now + offsetMs))
                }
            }
            scheduledPlanIds = AlarmScheduler.schedulePlanStepNotifications(appContext, scheduleList)
        }

        _uiState.update {
            it.copy(
                planSteps = workingSteps,
                planCurrentIndex = startIndex,
                planStatus = PlanStatus.RUNNING,
                planStepRemainingMs = initialRemainingMs,
                planNotice = if (hasNotificationPermission) {
                    "Focused on step ${startIndex + 1}. You'll get a gentle chime when this block ends."
                } else {
                    "Timer running. Enable notifications in Settings for background step alerts."
                }
            )
        }
        persistBreakdownState()
        startPlanTicker()
    }

    fun pauseTimedPlan() {
        if (_uiState.value.planStatus != PlanStatus.RUNNING) return
        planTimerJob?.cancel()
        stopPlanSchedules()
        val elapsed = (System.currentTimeMillis() - planStepStartedAtMs).coerceAtLeast(0L)
        val remaining = (planStepBaseDurationMs - elapsed).coerceAtLeast(0L)
        planStepBaseDurationMs = remaining
        _uiState.update {
            it.copy(
                planStatus = PlanStatus.PAUSED,
                planStepRemainingMs = remaining,
                planNotice = "Paused. Your progress is saved."
            )
        }
        persistBreakdownState()
    }

    fun completeCurrentPlanStep(hasNotificationPermission: Boolean) {
        val state = _uiState.value
        val idx = state.planCurrentIndex
        if (idx !in state.planSteps.indices) return
        completePlanStepAt(idx, hasNotificationPermission)
    }

    fun completePlanStepAt(index: Int, hasNotificationPermission: Boolean) {
        val state = _uiState.value
        if (index !in state.planSteps.indices) return
        stopPlanSchedules()
        planTimerJob?.cancel()

        val wasCompleted = state.planSteps[index].completed
        val updated = state.planSteps.mapIndexed { i, step ->
            if (i == index) step.copy(completed = !wasCompleted) else step
        }

        if (!wasCompleted) {
            if (state.rewardSettings.haptics) {
                AlarmSoundPlayer.vibrate(appContext, longArrayOf(0, 22))
            }
            if (state.rewardSettings.sound) {
                AlarmSoundPlayer.playTone(freqHz = 720.0, durationMs = 150, volume = 0.2f)
            }
            val nextPts = state.points + 5
            prefs.points = nextPts
            _uiState.update { it.copy(points = nextPts, rewardToast = "+5 pts · subtask done!") }
            viewModelScope.launch {
                delay(1800L)
                _uiState.update { cur -> if (cur.rewardToast == "+5 pts · subtask done!") cur.copy(rewardToast = "") else cur }
            }
        }

        val nextIncompleteIdx = updated.indexOfFirst { !it.completed }
        if (nextIncompleteIdx == -1) {
            // All subtasks complete!
            if (state.rewardSettings.sound) {
                AlarmSoundPlayer.playCustomChime(appContext)
            }
            _uiState.update {
                it.copy(
                    planSteps = updated,
                    planStatus = PlanStatus.FINISHED,
                    planStepRemainingMs = 0L,
                    planNotice = "All subtasks finished! Amazing focus."
                )
            }
            persistBreakdownState()
            // Also complete parent task if linked & doesn't require essay verification
            val parentId = state.planParentTaskId
            if (parentId != null) {
                val parent = state.tasks.firstOrNull { it.id == parentId && !it.completed }
                if (parent != null && !parent.requiresVerification) {
                    giveReward(parent, "Completed via AI Task Breakdown")
                }
            }
            return
        }

        val nextRem = updated[nextIncompleteIdx].minutes * 60_000L
        val wasRunning = state.planStatus == PlanStatus.RUNNING
        planStepBaseDurationMs = nextRem
        _uiState.update {
            it.copy(
                planSteps = updated,
                planCurrentIndex = nextIncompleteIdx,
                planStepRemainingMs = nextRem,
                planStatus = if (wasRunning) PlanStatus.RUNNING else PlanStatus.PAUSED,
                planNotice = "Step completed! Moving to: ${updated[nextIncompleteIdx].title}"
            )
        }
        persistBreakdownState()
        if (wasRunning) {
            startOrResumeTimedPlan(hasNotificationPermission)
        }
    }

    fun skipTimedPlanStep() {
        val state = _uiState.value
        stopPlanSchedules()
        planTimerJob?.cancel()
        val nextIdx = ((state.planCurrentIndex + 1) until state.planSteps.size)
            .firstOrNull { !state.planSteps[it].completed }
        if (nextIdx == null) {
            _uiState.update { it.copy(planStatus = PlanStatus.FINISHED, planStepRemainingMs = 0L) }
            persistBreakdownState()
            return
        }
        val nextRem = state.planSteps[nextIdx].minutes * 60_000L
        planStepBaseDurationMs = nextRem
        _uiState.update {
            it.copy(
                planCurrentIndex = nextIdx,
                planStatus = PlanStatus.PAUSED,
                planStepRemainingMs = nextRem,
                planNotice = "Skipped ahead to step ${nextIdx + 1}. Tap Resume when ready."
            )
        }
        persistBreakdownState()
    }

    fun stopTimedPlan() {
        stopPlanSchedules()
        planTimerJob?.cancel()
        planStepStartedAtMs = 0L
        val resetSteps = _uiState.value.planSteps.map { it.copy(completed = false) }
        val firstRem = (resetSteps.firstOrNull()?.minutes ?: 0) * 60_000L
        planStepBaseDurationMs = firstRem
        _uiState.update {
            it.copy(
                planSteps = resetSteps,
                planStatus = PlanStatus.STOPPED,
                planCurrentIndex = 0,
                planStepRemainingMs = firstRem,
                planNotice = "Plan reset. Your steps are saved here anytime."
            )
        }
        persistBreakdownState()
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
                val remaining = (planStepBaseDurationMs - elapsed).coerceAtLeast(0L)
                if (remaining <= 0L) {
                    if (s.rewardSettings.notifySound) {
                        AlarmSoundPlayer.playCustomChime(appContext)
                    }
                    if (s.rewardSettings.haptics) {
                        AlarmSoundPlayer.vibrate(appContext, longArrayOf(0, 140, 80, 140))
                    }
                    // Pause at 00:00 and invite user to mark complete, continue, or move next
                    _uiState.update {
                        it.copy(
                            planStatus = PlanStatus.PAUSED,
                            planStepRemainingMs = 0L,
                            planNotice = "Time's up for \"${step.title}\"! Complete step, add time, or skip to next."
                        )
                    }
                    persistBreakdownState()
                    break
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

    // Quick Notes
    fun updateQuickNoteInput(text: String) {
        _uiState.update { it.copy(quickNoteInput = text) }
    }

    fun addQuickNote() {
        val text = _uiState.value.quickNoteInput.trim()
        if (text.isEmpty()) return
        val note = QuickNote(
            id = UUID.randomUUID().toString(),
            text = text,
            createdAt = Instant.now().toString()
        )
        val updated = listOf(note) + _uiState.value.quickNotes
        prefs.saveQuickNotes(updated)
        _uiState.update { it.copy(quickNotes = updated, quickNoteInput = "") }
    }

    fun deleteQuickNote(noteId: String) {
        val updated = _uiState.value.quickNotes.filterNot { it.id == noteId }
        prefs.saveQuickNotes(updated)
        _uiState.update { it.copy(quickNotes = updated) }
    }

    fun convertQuickNoteToTask(note: QuickNote) {
        deleteQuickNote(note.id)
        openAddTaskSheet(initialTitle = note.text)
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
            putExtra(Intent.EXTRA_SUBJECT, "வினை · accountability buddy")
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
                val cur = _uiState.value.buddy ?: break
                val res = supabase.pollOwner(cur)
                if (res != null) {
                    var updated = cur
                    if (res.state != cur.state) {
                        updated = updated.copy(state = res.state)
                    }
                    if (res.lastId > cur.lastCheerId) {
                        updated = updated.copy(lastCheerId = res.lastId)
                    }
                    if (updated != cur) {
                        prefs.saveLocalBuddy(updated)
                        _uiState.update { it.copy(buddy = updated) }
                    }
                    if (res.emojis.isNotEmpty()) {
                        _uiState.update { it.copy(cheerFloat = res.emojis.last()) }
                        delay(1800L)
                        _uiState.update { it.copy(cheerFloat = "") }
                    }
                }
                delay(5000L)
            }
        }
    }

    fun openBuddyViewer(rawTokenOrLink: String) {
        val token = extractBuddyToken(rawTokenOrLink) ?: return
        _uiState.update {
            it.copy(
                incomingBuddyToken = token,
                buddyViewLoaded = false,
                buddyViewAccepted = prefs.isBuddyAccepted(token),
                buddyViewDeclined = false,
                buddyViewFeedback = ""
            )
        }
        buddyViewPollJob?.cancel()
        buddyViewPollJob = viewModelScope.launch {
            while (_uiState.value.incomingBuddyToken == token) {
                val data = supabase.fetchBuddyView(token)
                _uiState.update { it.copy(buddyViewData = data, buddyViewLoaded = true) }
                delay(4000L)
            }
        }
    }

    fun closeBuddyViewer() {
        buddyViewPollJob?.cancel()
        _uiState.update {
            it.copy(incomingBuddyToken = null, buddyViewData = null, buddyViewLoaded = false, buddyViewFeedback = "")
        }
    }

    fun acceptBuddyViewerInvite() {
        val token = _uiState.value.incomingBuddyToken ?: return
        viewModelScope.launch {
            val ok = supabase.acceptBuddyInvite(token)
            if (ok) {
                prefs.setBuddyAccepted(token, true)
                val refreshed = supabase.fetchBuddyView(token)
                _uiState.update {
                    it.copy(buddyViewAccepted = true, buddyViewData = refreshed)
                }
            }
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
    fun triggerRingingAlarm(
        title: String,
        taskId: String?,
        timeLabel: String? = null,
        isPlanStep: Boolean = false
    ) {
        _uiState.update {
            it.copy(ringingAlarm = RingingAlarmState(title, taskId, timeLabel, isPlanStep))
        }
        alarmBeepJob?.cancel()
        alarmBeepJob = viewModelScope.launch {
            AlarmSoundPlayer.vibrate(appContext, longArrayOf(0, 220, 120, 220))
            while (true) {
                AlarmSoundPlayer.playCustomChime(appContext)
                delay(2400L)
            }
        }
    }

    fun dismissRingingAlarm() {
        if (_uiState.value.rewardSettings.haptics) {
            AlarmSoundPlayer.vibrate(appContext, longArrayOf(0, 25))
        }
        stopAlarmSound()
    }

    fun stopAlarmSound() {
        alarmBeepJob?.cancel()
        alarmBeepJob = null
        _uiState.update { it.copy(ringingAlarm = null) }
    }

    fun snoozeRingingAlarm(minutes: Int = 5) {
        val taskId = _uiState.value.ringingAlarm?.taskId
        if (_uiState.value.rewardSettings.haptics) {
            AlarmSoundPlayer.vibrate(appContext, longArrayOf(0, 20, 40, 20))
        }
        stopAlarmSound()
        if (taskId.isNullOrBlank()) return
        val original = _uiState.value.tasks.firstOrNull { it.id == taskId } ?: return
        val snoozeInstant = Instant.now().plusSeconds(minutes * 60L)
        val localTime = snoozeInstant.atZone(ZoneId.systemDefault()).toLocalTime()
        val time24 = DateTimeFormatter.ofPattern("HH:mm").format(localTime)
        val timeLabel = format12h(time24)
        val updated = original.copy(
            reminderAt = snoozeInstant.toString(),
            reminderTime = time24
        )
        viewModelScope.launch {
            repository.upsert(updated, _uiState.value.session?.userId)
            persistRemoteTask(updated)
            AlarmScheduler.scheduleTaskAlarm(appContext, updated.id, updated.title, snoozeInstant.toEpochMilli(), timeLabel)
            _uiState.update { it.copy(notice = "Snoozed for $minutes minutes (until $timeLabel).") }
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

    private fun format12h(time24: String): String {
        val parts = time24.split(":")
        val hh = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val mm = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val period = if (hh >= 12) "PM" else "AM"
        val h12 = if (hh % 12 == 0) 12 else hh % 12
        return "$h12:${mm.toString().padStart(2, '0')} $period"
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
