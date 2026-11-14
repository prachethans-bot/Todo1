package com.todo.foradhd.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todo.foradhd.R
import com.todo.foradhd.data.AppTab
import com.todo.foradhd.data.SavedTask
import com.todo.foradhd.data.TaskCategory
import com.todo.foradhd.ui.components.AccountDialog
import com.todo.foradhd.ui.components.AddEditTaskDialog
import com.todo.foradhd.ui.components.BuddyTab
import com.todo.foradhd.ui.components.BuddyViewerScreen
import com.todo.foradhd.ui.components.ClockPickerDialog
import com.todo.foradhd.ui.components.ComfortSettingsCard
import com.todo.foradhd.ui.components.CountdownRing
import com.todo.foradhd.ui.components.DailyProgressDonutCard
import com.todo.foradhd.ui.components.FocusSessionDialog
import com.todo.foradhd.ui.components.LoadingSplashOverlay
import com.todo.foradhd.ui.components.QuickNotesSection
import com.todo.foradhd.ui.components.RingingAlarmDialog
import com.todo.foradhd.ui.components.TaskVerificationDialog
import com.todo.foradhd.ui.components.TimedPlanTab
import com.todo.foradhd.ui.components.WeeklySummaryCard
import com.todo.foradhd.ui.theme.GlassCard
import com.todo.foradhd.ui.theme.GlassOutlineButton
import com.todo.foradhd.ui.theme.LiquidGlassSurface
import com.todo.foradhd.ui.theme.ObsidianAtmosphereBackground
import com.todo.foradhd.ui.theme.VinaiTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun formatTime12(time24: String): String {
    val parts = time24.split(":")
    val hh = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val mm = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val period = if (hh >= 12) "PM" else "AM"
    val h12 = if (hh % 12 == 0) 12 else hh % 12
    return "$h12:${mm.toString().padStart(2, '0')} $period"
}

private fun formatShortDate(iso: String): String {
    return runCatching {
        val instant = Instant.parse(iso)
        val local = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        DateTimeFormatter.ofPattern("MMM d").format(local)
    }.getOrDefault("")
}

@Composable
fun VinaiApp(viewModel: VinaiViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = VinaiTheme.colors
    val context = LocalContext.current

    fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    var pendingAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val action = pendingAfterPermission
        pendingAfterPermission = null
        action?.invoke()
    }

    fun withNotificationPermission(needsNotification: Boolean, action: () -> Unit) {
        if (!needsNotification || hasNotificationPermission() || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            action()
        } else {
            pendingAfterPermission = action
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (state.incomingBuddyToken != null) {
        BackHandler { viewModel.closeBuddyViewer() }
        BuddyViewerScreen(
            state = state,
            onAccept = { viewModel.acceptBuddyViewerInvite() },
            onDecline = { viewModel.declineBuddyViewerInvite() },
            onCheer = { emoji -> viewModel.sendBuddyCheer(emoji) },
            onClose = { viewModel.closeBuddyViewer() }
        )
        return
    }

    BackHandler(enabled = state.tab != AppTab.TODAY) {
        viewModel.selectTab(AppTab.TODAY)
    }

    val allActiveTasks = remember(state.tasks) { state.tasks.filter { !it.completed } }
    val filteredActiveTasks = remember(allActiveTasks, state.selectedCategory) {
        if (state.selectedCategory == TaskCategory.ALL.label) allActiveTasks
        else allActiveTasks.filter { it.category.equals(state.selectedCategory, ignoreCase = true) }
    }
    val completedTasks = remember(state.tasks) { state.tasks.filter { it.completed } }
    val activeFocusTask = remember(state.tasks, state.focusTaskId) {
        state.tasks.firstOrNull { it.id == state.focusTaskId }
    }

    ObsidianAtmosphereBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        colors.background.copy(alpha = 0.55f),
                                        colors.background.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.tab == AppTab.TODAY) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 600.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                LiquidGlassSurface(
                                    shape = RoundedCornerShape(999.dp),
                                    baseTint = colors.pillBg,
                                    blurRadius = 24.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openAddTaskSheet() }
                                        .testTag("add_task_fab")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 15.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = colors.metallicStart,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Add a Task",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        BottomNavigationDock(
                            selectedTab = state.tab,
                            onSelectTab = { viewModel.selectTab(it) }
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp, bottom = innerPadding.calculateBottomPadding() + 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                    ) {
                        TopHeaderBar(
                            today = state.todayFormatted,
                            onToggleSettings = { viewModel.toggleSettings() },
                            onOpenAccount = { viewModel.openAccountModal() }
                        )

                        AnimatedVisibility(visible = state.settingsOpen) {
                            ComfortSettingsCard(
                                isDarkTheme = state.isDarkTheme,
                                settings = state.rewardSettings,
                                checkInFrequency = state.checkInFrequency,
                                accountLabel = state.session?.let { "Signed in · ${it.email}" } ?: "Sign in / create account",
                                onClose = { viewModel.closeSettings() },
                                onToggleTheme = { viewModel.setDarkTheme(it) },
                                onUpdateSettings = { viewModel.updateRewardSetting(it) },
                                onSelectFrequency = { viewModel.setCheckInFrequency(it) },
                                onOpenAccount = { viewModel.openAccountModal() }
                            )
                        }

                        when (state.tab) {
                            AppTab.TODAY -> {
                                WelcomeSection()

                                HeroArtworkSection(activeCount = allActiveTasks.size)

                                DailyProgressDonutCard(
                                    tasks = state.tasks,
                                    planSteps = state.planSteps
                                )

                                StatsGrid(
                                    points = state.points,
                                    streak = state.streak,
                                    showedUpToday = state.lastDay == state.todayIsoKey
                                )

                                if (state.notice.isNotBlank()) {
                                    Text(
                                        text = state.notice,
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 17.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 6.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text("YOUR RUNWAY", style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Today's tasks", style = MaterialTheme.typography.headlineMedium)
                                    }
                                    Text(
                                        text = "${completedTasks.size}/${state.tasks.size} done",
                                        fontSize = 12.sp,
                                        color = colors.textMuted
                                    )
                                }

                                CategoryFilterBar(
                                    selectedCategory = state.selectedCategory,
                                    tasks = allActiveTasks,
                                    onSelectCategory = { viewModel.selectCategory(it) }
                                )

                                Spacer(Modifier.height(8.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    filteredActiveTasks.forEachIndexed { index, task ->
                                        val isLeaving = task.id in state.leavingTaskIds
                                        AnimatedVisibility(
                                            visible = !isLeaving,
                                            exit = slideOutHorizontally(tween(320)) { it / 2 } + fadeOut(tween(320))
                                        ) {
                                            TaskItemCard(
                                                index = index + 1,
                                                task = task,
                                                isFocused = task.id in state.focusedTasks,
                                                floatingReward = state.floatingReward?.takeIf { it.first == task.id }?.second,
                                                onCheckboxClick = { viewModel.onTaskCheckboxClicked(task) },
                                                onEdit = { viewModel.openEditTaskSheet(task) },
                                                onBreakDown = { viewModel.breakDownExistingTask(task) },
                                                onStartFocus = { viewModel.beginFocus(task) },
                                                onSaveTimeNotes = { est, act -> viewModel.saveTimeFeedback(task, est, act) },
                                                onDelete = { viewModel.deleteTask(task.id) }
                                            )
                                        }
                                    }

                                    if (filteredActiveTasks.isEmpty()) {
                                        EmptyTasksCard(filtered = state.selectedCategory != TaskCategory.ALL.label)
                                    }
                                }

                                if (completedTasks.isNotEmpty()) {
                                    CompletedTasksSection(completedTasks = completedTasks)
                                }

                                WeeklySummaryCard(
                                    tasks = state.tasks,
                                    streak = state.streak,
                                    points = state.points
                                )

                                QuickNotesSection(
                                    notes = state.quickNotes,
                                    input = state.quickNoteInput,
                                    onInputChange = { viewModel.updateQuickNoteInput(it) },
                                    onAddNote = { viewModel.addQuickNote() },
                                    onDeleteNote = { viewModel.deleteQuickNote(it) },
                                    onPromoteToTask = { viewModel.convertQuickNoteToTask(it) }
                                )
                            }

                            AppTab.PLAN -> {
                                TimedPlanTab(
                                    state = state,
                                    onGoalChange = { viewModel.updatePlanGoal(it) },
                                    onTotalMinutesChange = { viewModel.updatePlanTotalMinutes(it) },
                                    onToggleStrictTime = { viewModel.togglePlanStrictTime(it) },
                                    onCreatePlan = { viewModel.createTimedPlan() },
                                    onRescalePlan = { viewModel.rescaleExistingPlan(it) },
                                    onAddStep = { t, d, m -> viewModel.addPlanStep(t, d, m) },
                                    onEditStep = { idx, t, d, m -> viewModel.editPlanStep(idx, t, d, m) },
                                    onDeleteStep = { viewModel.deletePlanStep(it) },
                                    onMoveStep = { idx, delta -> viewModel.movePlanStep(idx, delta) },
                                    onToggleStepComplete = { idx ->
                                        viewModel.completePlanStepAt(idx, hasNotificationPermission())
                                    },
                                    onCompleteCurrentStep = {
                                        viewModel.completeCurrentPlanStep(hasNotificationPermission())
                                    },
                                    onStartOrResume = {
                                        withNotificationPermission(true) {
                                            viewModel.startOrResumeTimedPlan(hasNotificationPermission())
                                        }
                                    },
                                    onPause = { viewModel.pauseTimedPlan() },
                                    onSkip = { viewModel.skipTimedPlanStep() },
                                    onStop = { viewModel.stopTimedPlan() }
                                )
                            }

                            AppTab.BUDDY -> {
                                BuddyTab(
                                    state = state,
                                    completedCount = completedTasks.size,
                                    onNameChange = { viewModel.updateBuddyName(it) },
                                    onCreateInvite = { viewModel.createBuddyInvite(context) },
                                    onShareInvite = { viewModel.shareBuddyInvite(context) },
                                    onChangeSharing = { viewModel.changeBuddySharing(it) },
                                    onOpenFriendToken = { viewModel.openBuddyViewer(it) }
                                )
                            }
                        }

                        Text(
                            text = "Your pace. Your space. Progress over pressure.",
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 22.dp)
                        )
                    }
                }
            }

            // Floating Undo & Reward Toast & Cheer
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (state.tab == AppTab.TODAY) 142.dp else 82.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.undoTask != null) {
                    SilverPillToast(
                        text = "Task completed · Undo",
                        onClick = { viewModel.undoCompletion() },
                        modifier = Modifier.testTag("undo_toast_button")
                    )
                }
                if (state.rewardToast.isNotBlank()) {
                    SilverPillToast(
                        text = state.rewardToast,
                        onClick = null,
                        modifier = Modifier.testTag("reward_toast")
                    )
                }
            }

            if (state.cheerFloat.isNotBlank()) {
                Text(
                    text = state.cheerFloat,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 84.dp, end = 24.dp)
                )
            }

            // Splash overlay
            AnimatedVisibility(
                visible = state.showSplash,
                enter = fadeIn(),
                exit = fadeOut(tween(380))
            ) {
                LoadingSplashOverlay()
            }
        }
    }

    // Add / Edit Task Modal
    if (state.taskSheetOpen) {
        AddEditTaskDialog(
            state = state,
            onTitleChange = { viewModel.updateTaskDraftTitle(it) },
            onMicroStepChange = { viewModel.updateTaskDraftMicroStep(it) },
            onSelectCategory = { viewModel.selectTaskDraftCategory(it) },
            onToggleVerification = { viewModel.toggleTaskDraftRequiresVerification(it) },
            onSelectEstimate = { viewModel.setTaskDraftEstimate(it) },
            onOpenClock = { viewModel.openClockPicker() },
            onClearReminder = { viewModel.clearReminderTime() },
            onSuggestStep = { viewModel.generateMicroStepSuggestion() },
            onSave = {
                val hasReminder = !state.reminderAtIso.isNullOrBlank()
                withNotificationPermission(hasReminder) {
                    viewModel.saveTaskFromSheet(hasNotificationPermission())
                }
            },
            onDismiss = { viewModel.closeTaskSheet() }
        )
    }

    if (state.clockPickerOpen) {
        ClockPickerDialog(
            initialTime24 = state.reminderTime24,
            onSet = { time24 -> viewModel.setReminderTime(time24) },
            onCancel = { viewModel.closeClockPicker() }
        )
    }

    state.verificationTask?.let { target ->
        TaskVerificationDialog(
            task = target,
            text = state.verificationText,
            busy = state.verificationBusy,
            error = state.verificationError,
            supabaseConfigured = state.supabaseConfigured,
            onTextChange = { viewModel.updateVerificationText(it) },
            onVerify = { viewModel.verifyAndCompleteTask() },
            onBack = { viewModel.closeTaskVerification() }
        )
    }

    if (activeFocusTask != null) {
        FocusSessionDialog(
            state = state,
            task = activeFocusTask,
            onMicroStepChange = { viewModel.updateFocusDetail(microStep = it) },
            onProgressNoteChange = { viewModel.updateFocusDetail(progressNote = it) },
            onSelectPreset = { viewModel.selectFocusPreset(it) },
            onCheckInRespond = { cont -> viewModel.respondToCheckIn(cont) },
            onExpiredCheckInRespond = { cont -> viewModel.respondToExpiredCheckIn(cont) },
            onVerifyTask = {
                viewModel.saveFocusDetailsToCloud()
                viewModel.openTaskVerification(activeFocusTask)
            },
            onTogglePause = { viewModel.toggleFocusPause() },
            onEndSession = {
                viewModel.saveFocusDetailsToCloud()
                viewModel.endFocus(saveQualified = !state.focusStartedFromCheckoff)
            }
        )
    }

    if (state.showAccountModal) {
        AccountDialog(
            state = state,
            onDismiss = { viewModel.closeAccountModal() },
            onSetMode = { viewModel.setAuthMode(it) },
            onSubmit = { email, pwd -> viewModel.submitAuth(email, pwd) },
            onSignOut = { viewModel.signOut() }
        )
    }

    state.ringingAlarm?.let { alarm ->
        RingingAlarmDialog(
            alarm = alarm,
            onSnooze = { mins -> viewModel.snoozeRingingAlarm(mins) },
            onStartFocus = { viewModel.startTaskFromAlarm() },
            onDismiss = { viewModel.dismissRingingAlarm() }
        )
    }
}

/**
 * CHANGE 1: Plain-text header showing only "வினை" (no circular logo, no duplicate text, balanced layout).
 */
@Composable
private fun TopHeaderBar(
    today: String,
    onToggleSettings: () -> Unit,
    onOpenAccount: () -> Unit
) {
    val colors = VinaiTheme.colors
    val iconRim = Brush.linearGradient(listOf(colors.glassBorderHighlight, colors.glassBorderShadow))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 27.sp,
                color = colors.metallicStart
            )
            Text(today, fontSize = 12.sp, color = colors.textMuted)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onToggleSettings,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(colors.cardTop, colors.cardBottom)))
                    .border(1.dp, iconRim, CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Comfort settings", tint = colors.primary, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onOpenAccount,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(colors.cardTop, colors.cardBottom)))
                    .border(1.dp, iconRim, CircleShape)
                    .testTag("account_button")
            ) {
                Icon(Icons.Filled.PersonOutline, contentDescription = "Account", tint = colors.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun WelcomeSection() {
    val colors = VinaiTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Text("YOUR PACE. YOUR SPACE.", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildAnnotatedString {
                append("Small steps.\n")
                withStyle(SpanStyle(color = colors.secondary)) {
                    append("Real momentum.")
                }
            },
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "A little structure. No pressure to be perfect.",
            fontSize = 12.sp,
            color = colors.textMuted
        )
        Spacer(Modifier.height(10.dp))
    }
}

/**
 * CHANGE 3 & 4: Enhanced Kadinamaa Iru hero image blended seamlessly into the page background
 * with radial gradient edge masking, zero hard borders, and subtle accent glow.
 */
@Composable
private fun HeroArtworkSection(activeCount: Int) {
    val colors = VinaiTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(275.dp)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Diffused blurred metallic-silver liquid halo behind hero
        Box(
            modifier = Modifier
                .size(250.dp)
                .blur(radius = 28.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.metallicStart.copy(alpha = 0.22f),
                            colors.metallicEnd.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Hero artwork with soft edge fade mask melting black edges into the page
        Image(
            painter = painterResource(R.drawable.img_kadinamaa_hero),
            contentDescription = stringResource(R.string.hero_content_desc),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val radius = size.minDimension * 0.58f
                    drawRect(
                        brush = Brush.radialGradient(
                            0.0f to Color.Black,
                            0.80f to Color.Black,
                            0.94f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Transparent,
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = radius
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        )

        // Liquid glass floating active task counter
        LiquidGlassSurface(
            shape = RoundedCornerShape(14.dp),
            baseTint = colors.glassBase,
            blurRadius = 16.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = activeCount.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.metallicStart
                )
                Text("left today", fontSize = 10.sp, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun StatsGrid(points: Int, streak: Int, showedUpToday: Boolean) {
    val colors = VinaiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassCard(modifier = Modifier.weight(1f), cornerRadius = 20.dp) {
            Column {
                Text("YOUR POINTS", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$points", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("pts", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text("Every small win counts", fontSize = 11.sp, color = colors.textMuted)
            }
        }
        GlassCard(modifier = Modifier.weight(1f), cornerRadius = 20.dp) {
            Column {
                Text("DAILY STREAK", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("$streak", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("days", fontSize = 12.sp, color = colors.textSecondary)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (showedUpToday) "You showed up today" else "A fresh start is waiting",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterBar(
    selectedCategory: String,
    tasks: List<SavedTask>,
    onSelectCategory: (String) -> Unit
) {
    val colors = VinaiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskCategory.entries.forEach { cat ->
            val count = if (cat == TaskCategory.ALL) tasks.size else tasks.count { it.category.equals(cat.label, ignoreCase = true) }
            val active = selectedCategory.equals(cat.label, ignoreCase = true)
            val pillBrush = if (active) {
                Brush.linearGradient(listOf(colors.metallicStart, colors.metallicMid, colors.metallicEnd))
            } else {
                Brush.verticalGradient(listOf(colors.cardTop, colors.cardBottom))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(pillBrush)
                    .border(
                        1.dp,
                        if (active) Brush.verticalGradient(listOf(Color(0xCCFFFFFF), colors.metallicEnd))
                        else Brush.linearGradient(listOf(colors.glassBorderHighlight, colors.glassBorderShadow)),
                        RoundedCornerShape(999.dp)
                    )
                    .clickable { onSelectCategory(cat.label) }
                    .padding(horizontal = 13.dp, vertical = 8.dp)
                    .testTag("filter_${cat.label.lowercase()}")
            ) {
                Text(
                    text = if (count > 0) "${cat.label} ($count)" else cat.label,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) colors.onPrimary else colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationDock(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit
) {
    val colors = VinaiTheme.colors
    LiquidGlassSurface(
        shape = RoundedCornerShape(24.dp),
        baseTint = colors.navBarBg,
        blurRadius = 24.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                val icon = when (tab) {
                    AppTab.TODAY -> Icons.Filled.Home
                    AppTab.PLAN -> Icons.Filled.Schedule
                    AppTab.BUDDY -> Icons.Filled.Group
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (selected) Brush.verticalGradient(listOf(colors.cardTop, colors.subtleOverlay))
                            else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .then(
                            if (selected) Modifier.border(
                                1.dp,
                                Brush.linearGradient(listOf(colors.glassBorderHighlight, colors.glassBorderShadow)),
                                RoundedCornerShape(18.dp)
                            ) else Modifier
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 8.dp)
                        .testTag("tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label,
                        tint = if (selected) colors.metallicStart else colors.textMuted,
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) colors.metallicStart else colors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskItemCard(
    index: Int,
    task: SavedTask,
    isFocused: Boolean,
    floatingReward: String?,
    onCheckboxClick: () -> Unit,
    onEdit: () -> Unit,
    onBreakDown: () -> Unit,
    onStartFocus: () -> Unit,
    onSaveTimeNotes: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    val colors = VinaiTheme.colors
    var timeNotesOpen by remember { mutableStateOf(false) }
    var estimateDraft by remember(task.id, task.estimateMinutes) {
        mutableStateOf(task.estimateMinutes?.toString() ?: "")
    }
    var actualDraft by remember(task.id) { mutableStateOf("") }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        cornerRadius = 18.dp,
        borderColor = if (isFocused) colors.borderStrong else colors.border,
        contentPadding = PaddingValues(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Circular Checkbox (48dp touch area)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCheckboxClick)
                        .testTag("task_check_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(colors.subtleOverlay)
                            .border(1.5.dp, colors.textMuted, CircleShape)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = index.toString().padStart(2, '0'),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted
                        )
                        Text(task.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(task.microStep, fontSize = 12.sp, lineHeight = 17.sp, color = colors.textSecondary)

                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.subtleOverlay)
                                .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(task.category, fontSize = 10.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                        }

                        // Verification badge if enabled
                        if (task.requiresVerification) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.accentMuted)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = "Verification needed", tint = colors.accent, modifier = Modifier.size(11.dp))
                                Text("Verify", fontSize = 10.sp, color = colors.accent, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Text(
                            text = formatShortDate(task.createdAt),
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )

                        if (!task.reminderTime.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsNone,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(formatTime12(task.reminderTime), fontSize = 10.sp, color = colors.accent, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    if (!task.reminderAt.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        CountdownRing(
                            reminderAt = task.reminderAt,
                            createdAt = task.createdAt,
                            isBig = false
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.subtleOverlay)
                                .border(1.dp, colors.border, RoundedCornerShape(9.dp))
                                .clickable(onClick = onStartFocus)
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                                .testTag("start_focus_${task.id}")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = colors.primary, modifier = Modifier.size(13.dp))
                            Text("Focus", fontSize = 11.sp, color = colors.textPrimary)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.subtleOverlay)
                                .border(1.dp, colors.border, RoundedCornerShape(9.dp))
                                .clickable(onClick = onBreakDown)
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.AccountTree, contentDescription = null, tint = colors.accent, modifier = Modifier.size(13.dp))
                            Text("Steps", fontSize = 11.sp, color = colors.textPrimary)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { timeNotesOpen = !timeNotesOpen }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text("Time", fontSize = 11.sp, color = colors.textSecondary)
                            Icon(
                                imageVector = if (timeNotesOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = timeNotesOpen) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = estimateDraft,
                                    onValueChange = { estimateDraft = it.filter(Char::isDigit) },
                                    label = { Text("Estimate (min)", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.inputBg,
                                        unfocusedContainerColor = colors.inputBg,
                                        focusedBorderColor = colors.borderStrong,
                                        unfocusedBorderColor = colors.border,
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = actualDraft,
                                    onValueChange = { actualDraft = it.filter(Char::isDigit) },
                                    label = { Text("Actual (min)", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.inputBg,
                                        unfocusedContainerColor = colors.inputBg,
                                        focusedBorderColor = colors.borderStrong,
                                        unfocusedBorderColor = colors.border,
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            GlassOutlineButton(
                                onClick = {
                                    onSaveTimeNotes(estimateDraft, actualDraft)
                                    actualDraft = ""
                                    timeNotesOpen = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save", fontSize = 11.sp)
                            }
                        }
                    }

                    if (task.actualMinutes != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Estimate ${task.estimateMinutes ?: "—"} min · actual ${task.actualMinutes} min",
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                    }
                }

                // Edit & Delete icons (NO trailing arrow)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp).testTag("edit_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit ${task.title}",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).testTag("delete_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete ${task.title}",
                            tint = colors.textMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            if (!floatingReward.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(floatingReward, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksCard(filtered: Boolean) {
    val colors = VinaiTheme.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (filtered) "No tasks in this category." else "Nothing waiting on you.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text("Tap + Add a Task below whenever you're ready.", fontSize = 12.sp, color = colors.textMuted)
        }
    }
}

@Composable
private fun CompletedTasksSection(completedTasks: List<SavedTask>) {
    val colors = VinaiTheme.colors
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable { expanded = !expanded },
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("COMPLETED · ${completedTasks.size}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    completedTasks.take(10).forEach { task ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(task.title, fontSize = 12.sp, color = colors.textMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SilverPillToast(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = VinaiTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(colors.metallicStart, colors.metallicMid, colors.metallicEnd, colors.metallicMid)
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0xDDFFFFFF), colors.metallicEnd)),
                RoundedCornerShape(999.dp)
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onPrimary)
    }
}
