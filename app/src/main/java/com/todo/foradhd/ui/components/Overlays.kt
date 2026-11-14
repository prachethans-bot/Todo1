package com.todo.foradhd.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.todo.foradhd.R
import com.todo.foradhd.data.RewardSettings
import com.todo.foradhd.data.SavedTask
import com.todo.foradhd.data.TaskCategory
import com.todo.foradhd.ui.RingingAlarmState
import com.todo.foradhd.ui.VinaiUiState
import com.todo.foradhd.ui.theme.ApplyDialogBlurBehind
import com.todo.foradhd.ui.theme.GlassCard
import com.todo.foradhd.ui.theme.GlassOutlineButton
import com.todo.foradhd.ui.theme.SilverPrimaryButton
import com.todo.foradhd.ui.theme.VinaiTheme

private fun formatAlarmTime12(time24: String): String {
    val parts = time24.split(":")
    val hh = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val mm = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val period = if (hh >= 12) "PM" else "AM"
    val h12 = if (hh % 12 == 0) 12 else hh % 12
    return "$h12:${mm.toString().padStart(2, '0')} $period"
}

@Composable
fun LoadingSplashOverlay() {
    val colors = VinaiTheme.colors
    val infinite = rememberInfiniteTransition(label = "splash")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "orbit"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(196.dp)
                        .rotate(angle)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(colors.border, colors.primary, Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.dp, colors.borderStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.vinai_launcher),
                        contentDescription = stringResource(R.string.brand_logo_desc),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
            Text("YOUR NEXT SMALL STEP", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "A little less noise.\nA little more you.",
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 32.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text("Making room for your next step.", fontSize = 12.sp, color = colors.textMuted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskDialog(
    state: VinaiUiState,
    onTitleChange: (String) -> Unit,
    onMicroStepChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleVerification: (Boolean) -> Unit,
    onSelectEstimate: (Int) -> Unit,
    onOpenClock: () -> Unit,
    onClearReminder: () -> Unit,
    onSuggestStep: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = VinaiTheme.colors
    val isEdit = state.editingTask != null
    val formattedAlarm = state.reminderTime24?.let { formatAlarmTime12(it) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ApplyDialogBlurBehind(blurRadiusPx = 46)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.modalScrim)
                .padding(horizontal = 14.dp, vertical = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                cornerRadius = 24.dp,
                borderColor = colors.borderStrong
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(if (isEdit) "EDIT TASK" else "ADD A TASK", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (isEdit) "Update task details" else "What needs your focus?",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = colors.textSecondary)
                        }
                    }

                    // 1. Task Title Input
                    OutlinedTextField(
                        value = state.taskDraftTitle,
                        onValueChange = onTitleChange,
                        placeholder = { Text("e.g. Clean my room", color = colors.textMuted) },
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.inputBg,
                            unfocusedContainerColor = colors.inputBg,
                            focusedBorderColor = colors.borderStrong,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_task_title_input")
                    )

                    // 2. Alarm Time Picker directly below task input
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.subtleOverlay)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenClock)
                            .padding(12.dp)
                            .testTag("set_reminder_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Alarm,
                                    contentDescription = null,
                                    tint = if (formattedAlarm != null) colors.accent else colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = if (formattedAlarm != null) "Alarm set for $formattedAlarm" else "Set exact alarm time",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = if (formattedAlarm != null && state.taskDraftTitle.isNotBlank()) {
                                            "Rings & reminds: \"You have to ${state.taskDraftTitle.trim()} at $formattedAlarm\""
                                        } else {
                                            "Optional · rings on your phone at the exact time"
                                        },
                                        fontSize = 11.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }
                            if (formattedAlarm != null) {
                                TextButton(onClick = onClearReminder) {
                                    Text("Clear", fontSize = 11.sp, color = colors.textMuted)
                                }
                            } else {
                                Icon(Icons.Filled.Schedule, contentDescription = "Pick time", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // 3. Optional Micro-step helper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("First tiny step (optional)", fontSize = 12.sp, color = colors.textSecondary)
                        TextButton(
                            onClick = onSuggestStep,
                            enabled = state.taskDraftTitle.isNotBlank() && !state.assistantBusy
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (state.assistantBusy) "Thinking…" else "AI micro-step", fontSize = 11.sp, color = colors.accent)
                        }
                    }
                    OutlinedTextField(
                        value = state.taskDraftMicroStep,
                        onValueChange = onMicroStepChange,
                        placeholder = { Text("Put the first thing you need within reach.", color = colors.textMuted, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.inputBg,
                            unfocusedContainerColor = colors.inputBg,
                            focusedBorderColor = colors.borderStrong,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 4. Auto-tag Category Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CATEGORY TAG", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = if (state.taskDraftCategoryManual) "Selected" else "AI auto-tagged",
                                fontSize = 10.sp,
                                color = colors.accent
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TaskCategory.assignable.forEach { cat ->
                                val active = state.taskDraftCategory == cat.label
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (active) colors.primary else colors.subtleOverlay)
                                        .border(1.dp, if (active) colors.borderStrong else colors.border, RoundedCornerShape(999.dp))
                                        .clickable { onSelectCategory(cat.label) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) colors.onPrimary else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // 5. CHANGE 2: Verification needed toggle (default OFF)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.subtleOverlay)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = if (state.taskDraftRequiresVerification) colors.accent else colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text("Verification needed", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text(
                                    text = if (state.taskDraftRequiresVerification) {
                                        "Checking off starts focus timer & verification"
                                    } else {
                                        "Checking off completes immediately"
                                    },
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                        Switch(
                            checked = state.taskDraftRequiresVerification,
                            onCheckedChange = onToggleVerification,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onPrimary,
                                checkedTrackColor = colors.primary,
                                uncheckedThumbColor = colors.secondary,
                                uncheckedTrackColor = colors.subtleOverlay
                            ),
                            modifier = Modifier.testTag("verification_needed_switch")
                        )
                    }

                    // Time Estimate chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Estimate", fontSize = 12.sp, color = colors.textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 25, 45).forEach { mins ->
                                val sel = state.taskDraftEstimateMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) colors.primary else colors.subtleOverlay)
                                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                        .clickable { onSelectEstimate(mins) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        fontSize = 11.sp,
                                        color = if (sel) colors.onPrimary else colors.textSecondary,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    SilverPrimaryButton(
                        onClick = onSave,
                        enabled = state.taskDraftTitle.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_task_button")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isEdit) "Save changes" else "Save task",
                            color = colors.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComfortSettingsCard(
    isDarkTheme: Boolean,
    settings: RewardSettings,
    checkInFrequency: Int,
    accountLabel: String,
    onClose: () -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onUpdateSettings: ((RewardSettings) -> RewardSettings) -> Unit,
    onSelectFrequency: (Int) -> Unit,
    onOpenAccount: () -> Unit
) {
    val colors = VinaiTheme.colors
    var freqMenuOpen by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Comfort settings", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onClose, modifier = Modifier.testTag("close_settings_button")) {
                    Icon(Icons.Filled.Close, contentDescription = "Close settings", tint = colors.textSecondary)
                }
            }

            SettingSwitchRow("Pure obsidian black glass", isDarkTheme, onToggleTheme)
            SettingSwitchRow("Reward sounds", settings.sound) { checked ->
                onUpdateSettings { it.copy(sound = checked) }
            }
            SettingSwitchRow("Haptic feedback", settings.haptics) { checked ->
                onUpdateSettings { it.copy(haptics = checked) }
            }
            SettingSwitchRow("Reward animations", settings.animations) { checked ->
                onUpdateSettings { it.copy(animations = checked) }
            }
            SettingSwitchRow("Check-in sound", settings.notifySound) { checked ->
                onUpdateSettings { it.copy(notifySound = checked) }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Focus check-in interval", fontSize = 12.sp, color = colors.textSecondary)
                Box {
                    GlassOutlineButton(onClick = { freqMenuOpen = true }) {
                        Text("$checkInFrequency minutes", fontSize = 11.sp)
                    }
                    DropdownMenu(
                        expanded = freqMenuOpen,
                        onDismissRequest = { freqMenuOpen = false }
                    ) {
                        listOf(5, 10, 15).forEach { mins ->
                            DropdownMenuItem(
                                text = { Text("$mins minutes") },
                                onClick = {
                                    onSelectFrequency(mins)
                                    freqMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            GlassOutlineButton(
                onClick = onOpenAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_account_button")
            ) {
                Text(accountLabel, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = VinaiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, color = colors.textSecondary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.secondary,
                uncheckedTrackColor = colors.subtleOverlay
            )
        )
    }
    HorizontalDivider(color = colors.divider)
}

@Composable
fun AccountDialog(
    state: VinaiUiState,
    onDismiss: () -> Unit,
    onSetMode: (Boolean) -> Unit,
    onSubmit: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    val colors = VinaiTheme.colors
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ApplyDialogBlurBehind(blurRadiusPx = 46)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.modalScrim)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 26.dp,
                borderColor = colors.borderStrong
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("YOUR ACCOUNT", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (state.session != null) "You're connected" else "Connect your account",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close account", tint = colors.textSecondary)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    if (state.session != null) {
                        Text(state.session.email, fontSize = 14.sp, color = colors.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("Your tasks can sync securely across devices.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(18.dp))
                        GlassOutlineButton(
                            onClick = onSignOut,
                            modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sign out")
                        }
                    } else {
                        Text(
                            text = "Sign in to enable connected AI verification, cloud backup and buddy invitations. Your local task list works without an account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.subtleOverlay)
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            AuthTabPill("Sign in", !state.isSignUpMode, { onSetMode(false) }, Modifier.weight(1f))
                            AuthTabPill("Create account", state.isSignUpMode, { onSetMode(true) }, Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.inputBg,
                                unfocusedContainerColor = colors.inputBg,
                                focusedBorderColor = colors.borderStrong,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.inputBg,
                                unfocusedContainerColor = colors.inputBg,
                                focusedBorderColor = colors.borderStrong,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                        )
                        Spacer(Modifier.height(16.dp))
                        SilverPrimaryButton(
                            onClick = { onSubmit(email, password) },
                            enabled = !state.authBusy,
                            modifier = Modifier.fillMaxWidth().testTag("auth_submit_button")
                        ) {
                            Text(
                                text = when {
                                    state.authBusy -> "Connecting…"
                                    state.isSignUpMode -> "Create account"
                                    else -> "Sign in"
                                },
                                color = colors.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                        }
                        if (state.authMessage.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(state.authMessage, fontSize = 12.sp, color = colors.textSecondary, textAlign = TextAlign.Center)
                        }
                        if (!state.supabaseConfigured) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Cloud sign-in needs Supabase configuration in AI Studio Secrets.",
                                fontSize = 11.sp,
                                color = colors.textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthTabPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VinaiTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) colors.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (selected) colors.onPrimary else colors.textMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun TaskVerificationDialog(
    task: SavedTask,
    text: String,
    busy: Boolean,
    error: String,
    supabaseConfigured: Boolean,
    onTextChange: (String) -> Unit,
    onVerify: () -> Unit,
    onBack: () -> Unit
) {
    val colors = VinaiTheme.colors
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ApplyDialogBlurBehind(blurRadiusPx = 54)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xBA040508), colors.modalScrim, Color(0xBA040508))
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !busy, onClick = onBack)
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .testTag("verification_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Back to tasks (keeps task incomplete)", fontSize = 13.sp, color = colors.secondary)
            }

            Spacer(Modifier.height(18.dp))
            Text("TASK VERIFICATION", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(10.dp))
            Text("Tell us what you did.", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (supabaseConfigured) {
                    "Write what you did or completed. Verification completes your task."
                } else {
                    "Write a quick specific note about what you did to complete this task."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 15.dp) {
                Column {
                    Text("YOUR TASK", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                    Spacer(Modifier.height(6.dp))
                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(task.microStep, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Your update", fontSize = 12.sp, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text("Write freely about what you did for this task…", color = colors.textMuted)
                },
                minLines = 5,
                maxLines = 10,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.inputBg,
                    unfocusedContainerColor = colors.inputBg,
                    focusedBorderColor = colors.borderStrong,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verification_text_input")
            )

            if (error.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(error, fontSize = 12.sp, color = colors.error, lineHeight = 17.sp)
            }

            Spacer(Modifier.height(16.dp))
            SilverPrimaryButton(
                onClick = onVerify,
                enabled = !busy && text.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verify_complete_button")
            ) {
                Text(
                    text = if (busy) "Checking…" else "Verify and complete",
                    color = colors.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Cancelling keeps your task in the pending list with nothing lost.",
                fontSize = 11.sp,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun FocusSessionDialog(
    state: VinaiUiState,
    task: SavedTask,
    onMicroStepChange: (String) -> Unit,
    onProgressNoteChange: (String) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onCheckInRespond: (Boolean) -> Unit,
    onExpiredCheckInRespond: (Boolean) -> Unit,
    onVerifyTask: () -> Unit,
    onTogglePause: () -> Unit,
    onEndSession: () -> Unit
) {
    val colors = VinaiTheme.colors
    val breathe = rememberInfiniteTransition(label = "focus_breathe")
    val breatheScale by breathe.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )
    val breatheAlpha by breathe.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_alpha"
    )

    Dialog(
        onDismissRequest = onEndSession,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ApplyDialogBlurBehind(blurRadiusPx = 56)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xBA040508), colors.modalScrim, Color(0xBA040508))
                    )
                )
        ) {
            // Diffused blurred breathing liquid-silver aura
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(290.dp)
                    .scale(breatheScale)
                    .blur(radius = 36.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colors.metallicStart.copy(alpha = breatheAlpha * 0.34f),
                                colors.accentMuted.copy(alpha = breatheAlpha * 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FOCUS SESSION", style = MaterialTheme.typography.labelSmall)
                    IconButton(onClick = onEndSession, modifier = Modifier.testTag("close_focus_button")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close focus session", tint = colors.textSecondary)
                    }
                }

                Text(task.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (state.focusStartedFromCheckoff || task.requiresVerification) {
                        "Verification is required for this task. Focus here, then verify when ready."
                    } else {
                        "Breathe slowly. One task, one calm step at a time."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                Spacer(Modifier.height(14.dp))
                Text("What do I need to do?", fontSize = 12.sp, color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.focusMicroStep,
                    onValueChange = onMicroStepChange,
                    placeholder = { Text("Write the next small action…", color = colors.textMuted) },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.inputBg,
                        unfocusedContainerColor = colors.inputBg,
                        focusedBorderColor = colors.borderStrong,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth().testTag("focus_micro_input")
                )

                Spacer(Modifier.height(12.dp))
                Text("What have I done so far?", fontSize = 12.sp, color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.focusProgressNote,
                    onValueChange = onProgressNoteChange,
                    placeholder = { Text("Add progress notes as you go…", color = colors.textMuted) },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.inputBg,
                        unfocusedContainerColor = colors.inputBg,
                        focusedBorderColor = colors.borderStrong,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth().testTag("focus_progress_input")
                )

                val mm = (state.focusRemainingSeconds / 60).toString().padStart(2, '0')
                val ss = (state.focusRemainingSeconds % 60).toString().padStart(2, '0')
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "$mm:$ss",
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (state.focusPaused) "Paused" else "Inhale · Exhale · Focus",
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    listOf(10, 15, 25, 45).forEach { mins ->
                        val selected = state.focusMinutes == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) colors.primary else colors.subtleOverlay)
                                .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                                .clickable(enabled = state.focusElapsedSeconds == 0) { onSelectPreset(mins) }
                                .padding(horizontal = 13.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "$mins min",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) colors.onPrimary else colors.textSecondary
                            )
                        }
                    }
                }

                if (state.checkInOpen) {
                    Spacer(Modifier.height(16.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 15.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Still on it?", style = MaterialTheme.typography.titleMedium)
                            Text("Quick check-in. No pressure.", style = MaterialTheme.typography.bodySmall)
                            SilverPrimaryButton(onClick = { onCheckInRespond(true) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Yes, continue", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                            }
                            GlassOutlineButton(onClick = { onCheckInRespond(false) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Need a break")
                            }
                        }
                    }
                }

                if (state.checkInExpired) {
                    Spacer(Modifier.height(16.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 15.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Welcome back.", style = MaterialTheme.typography.titleMedium)
                            Text("Want to continue or pause here?", style = MaterialTheme.typography.bodySmall)
                            SilverPrimaryButton(onClick = { onExpiredCheckInRespond(true) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Continue", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                            }
                            GlassOutlineButton(onClick = { onExpiredCheckInRespond(false) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Take a break")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                if (state.focusQualified || task.requiresVerification) {
                    SilverPrimaryButton(
                        onClick = onVerifyTask,
                        modifier = Modifier.fillMaxWidth().testTag("focus_verify_button")
                    ) {
                        Text("Verify & complete", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassOutlineButton(
                        onClick = onTogglePause,
                        modifier = Modifier.weight(1f).testTag("focus_pause_button")
                    ) {
                        Icon(
                            imageVector = if (state.focusPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.focusPaused) "Continue" else "Pause")
                    }
                    GlassOutlineButton(
                        onClick = onEndSession,
                        modifier = Modifier.weight(1f).testTag("focus_end_button")
                    ) {
                        Text(if (state.focusStartedFromCheckoff) "Cancel" else "End session")
                    }
                }
            }
        }
    }
}

@Composable
fun RingingAlarmDialog(
    alarm: RingingAlarmState,
    onSnooze: (Int) -> Unit,
    onStartFocus: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = VinaiTheme.colors
    val infinite = rememberInfiniteTransition(label = "alarm")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "pulse"
    )

    val messageText = when {
        alarm.isPlanStep -> "Allocated time finished for: ${alarm.title}"
        !alarm.timeLabel.isNullOrBlank() -> "You have to ${alarm.title} at ${alarm.timeLabel}"
        else -> "You have to ${alarm.title} now"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ApplyDialogBlurBehind(blurRadiusPx = 50)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.modalScrim)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                borderColor = colors.borderStrong
            ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(colors.subtleOverlay)
                        .border(1.dp, colors.borderStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "Ringing alarm",
                        tint = colors.accent,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (alarm.isPlanStep) "BREAKDOWN STEP ALERT" else "வினை · TASK ALARM",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Ready to take one small step?",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))

                if (!alarm.isPlanStep) {
                    SilverPrimaryButton(onClick = onStartFocus, modifier = Modifier.fillMaxWidth()) {
                        Text("Start focus", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (alarm.taskId != null && !alarm.isPlanStep) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassOutlineButton(
                            onClick = { onSnooze(5) },
                            modifier = Modifier.weight(1f).testTag("snooze_5_button")
                        ) {
                            Text("Snooze 5m", fontSize = 12.sp)
                        }
                        GlassOutlineButton(
                            onClick = { onSnooze(10) },
                            modifier = Modifier.weight(1f).testTag("snooze_10_button")
                        ) {
                            Text("Snooze 10m", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                GlassOutlineButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("dismiss_alarm_button")
                ) {
                    Text("Dismiss")
                }
            }
        }
        }
    }
}
