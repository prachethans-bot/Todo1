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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.todo.foradhd.data.AssistantResponse
import com.todo.foradhd.data.SavedTask
import com.todo.foradhd.data.TaskMode
import com.todo.foradhd.ui.components.BuddyTab
import com.todo.foradhd.ui.components.BuddyViewerScreen
import com.todo.foradhd.ui.components.ClockPickerDialog
import com.todo.foradhd.ui.components.ComfortSettingsCard
import com.todo.foradhd.ui.components.CountdownRing
import com.todo.foradhd.ui.components.FocusSessionDialog
import com.todo.foradhd.ui.components.LoadingSplashOverlay
import com.todo.foradhd.ui.components.RingingAlarmDialog
import com.todo.foradhd.ui.components.TaskVerificationDialog
import com.todo.foradhd.ui.components.TimedPlanTab
import com.todo.foradhd.ui.components.AccountDialog
import com.todo.foradhd.ui.theme.GlassBorder
import com.todo.foradhd.ui.theme.GlassBorderStrong
import com.todo.foradhd.ui.theme.GlassCard
import com.todo.foradhd.ui.theme.GlassDivider
import com.todo.foradhd.ui.theme.GlassOutlineButton
import com.todo.foradhd.ui.theme.ObsidianAtmosphereBackground
import com.todo.foradhd.ui.theme.ObsidianInputBg
import com.todo.foradhd.ui.theme.SilverPrimary
import com.todo.foradhd.ui.theme.SilverPrimaryButton
import com.todo.foradhd.ui.theme.SilverSecondary
import com.todo.foradhd.ui.theme.TextMuted
import com.todo.foradhd.ui.theme.TextOnSilver
import com.todo.foradhd.ui.theme.TextPrimary
import com.todo.foradhd.ui.theme.TextSecondary
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
    val context = LocalContext.current

    fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    var pendingAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
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

    val activeTasks = remember(state.tasks) { state.tasks.filter { !it.completed } }
    val completedTasks = remember(state.tasks) { state.tasks.filter { it.completed } }
    val rightNowTask = remember(activeTasks) {
        activeTasks.firstOrNull { !it.reminderAt.isNullOrBlank() } ?: activeTasks.firstOrNull()
    }
    val activeFocusTask = remember(state.tasks, state.focusTaskId) {
        state.tasks.firstOrNull { it.id == state.focusTaskId }
    }

    ObsidianAtmosphereBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
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

                    WelcomeSection()

                    HeroPosterCard(activeCount = activeTasks.size)

                    StatsGrid(
                        points = state.points,
                        streak = state.streak,
                        showedUpToday = state.lastDay == state.todayIsoKey
                    )

                    TabSelectorRow(
                        selectedTab = state.tab,
                        onSelectTab = { viewModel.selectTab(it) }
                    )

                    AnimatedVisibility(visible = state.settingsOpen) {
                        ComfortSettingsCard(
                            settings = state.rewardSettings,
                            checkInFrequency = state.checkInFrequency,
                            accountLabel = state.session?.let { "Signed in · ${it.email}" } ?: "Sign in / create account",
                            onClose = { viewModel.closeSettings() },
                            onUpdateSettings = { viewModel.updateRewardSetting(it) },
                            onSelectFrequency = { viewModel.setCheckInFrequency(it) },
                            onOpenAccount = { viewModel.openAccountModal() }
                        )
                    }

                    when (state.tab) {
                        AppTab.TODAY -> {
                            QuickAddCard(
                                taskInput = state.taskInput,
                                mode = state.mode,
                                reminderTime24 = state.reminderTime24,
                                busy = state.assistantBusy,
                                onTaskInputChange = { viewModel.updateTaskInput(it) },
                                onSelectMode = { viewModel.selectMode(it) },
                                onOpenClock = { viewModel.openClockPicker() },
                                onClearReminder = { viewModel.clearReminderTime() },
                                onFindStep = { viewModel.findNextStep() }
                            )

                            state.assistantResponse?.let { resp ->
                                AssistantResultCard(
                                    response = resp,
                                    estimateMinutes = state.estimateMinutes,
                                    onSelectEstimate = { viewModel.setEstimateMinutes(it) },
                                    onSave = {
                                        val hasReminder = !state.reminderAtIso.isNullOrBlank()
                                        withNotificationPermission(hasReminder) {
                                            viewModel.saveCurrentStep(hasNotificationPermission())
                                        }
                                    },
                                    onDiscard = { viewModel.discardAssistantResult() }
                                )
                            }

                            if (state.notice.isNotBlank()) {
                                Text(
                                    text = state.notice,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
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
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
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
                                    color = TextMuted
                                )
                            }

                            if (rightNowTask != null) {
                                RightNowCard(
                                    task = rightNowTask,
                                    onStartFocus = { viewModel.startTaskFromAlarm(rightNowTask) }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                activeTasks.forEachIndexed { index, task ->
                                    val isLeaving = task.id in state.leavingTaskIds
                                    AnimatedVisibility(
                                        visible = !isLeaving,
                                        exit = slideOutHorizontally(tween(380)) { it / 2 } + fadeOut(tween(380))
                                    ) {
                                        TaskItemCard(
                                            index = index + 1,
                                            task = task,
                                            isFocused = task.id in state.focusedTasks,
                                            floatingReward = state.floatingReward?.takeIf { it.first == task.id }?.second,
                                            onVerify = { viewModel.openTaskVerification(task) },
                                            onStartFocus = { viewModel.beginFocus(task) },
                                            onSaveTimeNotes = { est, act -> viewModel.saveTimeFeedback(task, est, act) },
                                            onDelete = { viewModel.deleteTask(task.id) }
                                        )
                                    }
                                }

                                if (activeTasks.isEmpty()) {
                                    EmptyTasksCard()
                                }
                            }

                            if (completedTasks.isNotEmpty()) {
                                CompletedTasksSection(completedTasks = completedTasks)
                            }
                        }

                        AppTab.PLAN -> {
                            TimedPlanTab(
                                state = state,
                                onGoalChange = { viewModel.updatePlanGoal(it) },
                                onTotalMinutesChange = { viewModel.updatePlanTotalMinutes(it) },
                                onCreatePlan = { viewModel.createTimedPlan() },
                                onChangeStepMinutes = { idx, mins -> viewModel.changePlanStepMinutes(idx, mins) },
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
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 26.dp)
                    )
                    Spacer(Modifier.height(64.dp))
                }
            }

            // Floating Undo & Reward Toast & Cheer
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp),
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
                exit = fadeOut(tween(400))
            ) {
                LoadingSplashOverlay()
            }
        }
    }

    // Dialogs & full-screen overlays
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
                viewModel.endFocus(saveQualified = true)
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
            onSnooze = { viewModel.snoozeRingingAlarm() },
            onStartFocus = { viewModel.startTaskFromAlarm() },
            onDismiss = { viewModel.stopAlarmSound() }
        )
    }
}

@Composable
private fun TopHeaderBar(
    today: String,
    onToggleSettings: () -> Unit,
    onOpenAccount: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(1.dp, GlassBorderStrong, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.vinai_launcher),
                    contentDescription = stringResource(R.string.brand_logo_desc),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(6.dp)
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = SilverPrimary
                )
                Text(today, fontSize = 12.sp, color = TextMuted)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD6D6D6))
            )
            IconButton(
                onClick = onToggleSettings,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x0DFFFFFF))
                    .border(1.dp, GlassBorder, CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Comfort settings", tint = SilverPrimary, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onOpenAccount,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x0DFFFFFF))
                    .border(1.dp, GlassBorder, CircleShape)
                    .testTag("account_button")
            ) {
                Icon(Icons.Filled.PersonOutline, contentDescription = "Account", tint = SilverPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun WelcomeSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text("YOUR PACE. YOUR SPACE.", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildAnnotatedString {
                append("Small steps.\n")
                withStyle(SpanStyle(color = Color(0xFFC9C9C9))) {
                    append("Real momentum.")
                }
            },
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "A little structure. No pressure to be perfect.",
            fontSize = 12.sp,
            color = TextMuted
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun HeroPosterCard(activeCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(265.dp)
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.stay_hard_hero),
            contentDescription = stringResource(R.string.hero_content_desc),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0x38FFFFFF), Color(0xB8111111))))
                .border(1.dp, GlassBorderStrong, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = activeCount.toString(),
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text("left today", fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun StatsGrid(points: Int, streak: Int, showedUpToday: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassCard(modifier = Modifier.weight(1f), cornerRadius = 20.dp) {
            Column {
                Text("YOUR POINTS", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$points", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = SilverPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("pts", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text("Every small win counts", fontSize = 11.sp, color = TextMuted)
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
                        tint = SilverPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("$streak", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = SilverPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("days", fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (showedUpToday) "You showed up today" else "A fresh start is waiting",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun TabSelectorRow(selectedTab: AppTab, onSelectTab: (AppTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x0BFFFFFF))
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AppTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) Brush.linearGradient(listOf(Color(0x2BFFFFFF), Color(0x0AFFFFFF)))
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelectTab(tab) }
                    .padding(vertical = 12.dp)
                    .testTag("tab_${tab.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Color.White else TextMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAddCard(
    taskInput: String,
    mode: TaskMode,
    reminderTime24: String?,
    busy: Boolean,
    onTaskInputChange: (String) -> Unit,
    onSelectMode: (TaskMode) -> Unit,
    onOpenClock: () -> Unit,
    onClearReminder: () -> Unit,
    onFindStep: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = taskInput,
                onValueChange = onTaskInputChange,
                placeholder = { Text("What needs your focus?", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SilverSecondary, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_input")
            )

            HorizontalDivider(color = GlassDivider)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x66090909))
                        .border(1.dp, Color(0x1CFFFFFF), RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenClock)
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .testTag("set_reminder_button")
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                    Text(
                        text = reminderTime24?.let { formatTime12(it) } ?: "Set time",
                        fontSize = 12.sp,
                        color = SilverPrimary
                    )
                    if (reminderTime24 != null) {
                        Text(
                            text = "×",
                            fontSize = 14.sp,
                            color = TextMuted,
                            modifier = Modifier
                                .clickable(onClick = onClearReminder)
                                .padding(start = 4.dp)
                        )
                    }
                }

                SilverPrimaryButton(
                    onClick = onFindStep,
                    enabled = !busy && taskInput.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("find_step_button")
                ) {
                    Text(
                        text = if (busy) "Thinking…" else "Find my next step",
                        color = TextOnSilver,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    if (!busy) {
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextOnSilver,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TaskMode.entries.forEach { entry ->
                    val active = entry == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) Color(0x18FFFFFF) else Color(0x06FFFFFF))
                            .border(
                                1.dp,
                                if (active) Color(0x70CFCFCF) else Color(0x19FFFFFF),
                                RoundedCornerShape(999.dp)
                            )
                            .clickable { onSelectMode(entry) }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                            .testTag("mode_${entry.name.lowercase()}")
                    ) {
                        Text(
                            text = entry.label,
                            fontSize = 11.sp,
                            color = if (active) TextPrimary else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantResultCard(
    response: AssistantResponse,
    estimateMinutes: Int,
    onSelectEstimate: (Int) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    var estimateMenuOpen by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        cornerRadius = 21.dp,
        borderColor = GlassBorderStrong
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SilverSecondary, modifier = Modifier.size(14.dp))
                Text("YOUR NEXT LITTLE STEP", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(10.dp))
            Text(response.optimizedTaskTitle, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x0AFFFFFF))
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .padding(13.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("01", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Text(response.theMicroStep, fontSize = 13.sp, lineHeight = 20.sp, color = TextPrimary)
            }

            Spacer(Modifier.height(12.dp))
            Text(response.dopamineHook, fontSize = 12.sp, color = Color(0xFFD1D1D1), lineHeight = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(response.timeGuessPrompt, fontSize = 12.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(response.timeFeedback, fontSize = 11.sp, color = TextMuted, lineHeight = 16.sp)

            Spacer(Modifier.height(12.dp))
            Text("My time estimate", fontSize = 11.sp, color = SilverSecondary)
            Spacer(Modifier.height(6.dp))
            Box {
                GlassOutlineButton(
                    onClick = { estimateMenuOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("$estimateMinutes ${if (estimateMinutes == 1) "minute" else "minutes"}", fontSize = 12.sp)
                }
                DropdownMenu(
                    expanded = estimateMenuOpen,
                    onDismissRequest = { estimateMenuOpen = false }
                ) {
                    listOf(1, 2, 5, 10, 15, 25, 45).forEach { mins ->
                        DropdownMenuItem(
                            text = { Text("$mins ${if (mins == 1) "minute" else "minutes"}") },
                            onClick = {
                                onSelectEstimate(mins)
                                estimateMenuOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SilverPrimaryButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).testTag("save_step_button")
                ) {
                    Text("Save this step", color = TextOnSilver, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Check, contentDescription = null, tint = TextOnSilver, modifier = Modifier.size(16.dp))
                }
                TextButton(onClick = onDiscard, modifier = Modifier.testTag("discard_step_button")) {
                    Text("Discard", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RightNowCard(task: SavedTask, onStartFocus: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 14.dp),
        cornerRadius = 20.dp,
        borderColor = Color(0x45FFFFFF)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✳ RIGHT NOW", style = MaterialTheme.typography.labelSmall)
                Text("Current focus", fontSize = 11.sp, color = TextMuted)
            }
            Spacer(Modifier.height(10.dp))
            Text(task.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(task.microStep, fontSize = 12.sp, lineHeight = 18.sp, color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!task.reminderAt.isNullOrBlank()) {
                    CountdownRing(
                        reminderAt = task.reminderAt,
                        createdAt = task.createdAt,
                        isBig = false
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                SilverPrimaryButton(
                    onClick = onStartFocus,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("right_now_start_focus")
                ) {
                    Text("Start focus", color = TextOnSilver, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextOnSilver,
                        modifier = Modifier.size(15.dp)
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
    onVerify: () -> Unit,
    onStartFocus: () -> Unit,
    onSaveTimeNotes: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var timeNotesOpen by remember { mutableStateOf(false) }
    var estimateDraft by remember(task.id, task.estimateMinutes) {
        mutableStateOf(task.estimateMinutes?.toString() ?: "")
    }
    var actualDraft by remember(task.id) { mutableStateOf("") }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        cornerRadius = 17.dp,
        borderColor = if (isFocused) GlassBorderStrong else GlassBorder,
        contentPadding = PaddingValues(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Checkbox button (48dp touch area)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onVerify)
                        .testTag("task_check_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x0DFFFFFF))
                            .border(1.dp, Color(0xFF6A6A6A), RoundedCornerShape(8.dp))
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = index.toString().padStart(2, '0'),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(Modifier.height(5.dp))
                    Text(task.microStep, fontSize = 12.sp, lineHeight = 18.sp, color = TextSecondary)

                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatShortDate(task.createdAt)} · ${task.mode.displayTag()}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        if (!task.reminderTime.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsNone,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(formatTime12(task.reminderTime), fontSize = 10.sp, color = TextMuted)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0x0DFFFFFF))
                                .border(1.dp, GlassBorder, RoundedCornerShape(9.dp))
                                .clickable(onClick = onStartFocus)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("start_focus_${task.id}")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = SilverPrimary, modifier = Modifier.size(14.dp))
                            Text("Start focus", fontSize = 11.sp, color = Color(0xFFD0D0D0))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { timeNotesOpen = !timeNotesOpen }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("Time notes", fontSize = 11.sp, color = TextSecondary)
                            Icon(
                                imageVector = if (timeNotesOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
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
                                        focusedContainerColor = ObsidianInputBg,
                                        unfocusedContainerColor = ObsidianInputBg,
                                        focusedBorderColor = GlassBorderStrong,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
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
                                        focusedContainerColor = ObsidianInputBg,
                                        unfocusedContainerColor = ObsidianInputBg,
                                        focusedBorderColor = GlassBorderStrong,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
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
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete ${task.title}",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!floatingReward.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDDDDDD))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(floatingReward, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SilverSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(10.dp))
        Text("Nothing waiting on you.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Add one tiny step whenever you're ready.", fontSize = 12.sp, color = TextMuted)
    }
}

@Composable
private fun CompletedTasksSection(completedTasks: List<SavedTask>) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Completed · ${completedTasks.size}", fontSize = 12.sp, color = Color(0xFFBDBDBD))
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary,
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
                            tint = SilverSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(task.title, fontSize = 12.sp, color = TextMuted)
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFEEEEEE), Color(0xFFAAAAAA))))
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(999.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101010))
    }
}
