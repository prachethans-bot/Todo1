package com.todo.foradhd.ui.components

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.todo.foradhd.ui.RingingAlarmState
import com.todo.foradhd.ui.VinaiUiState
import com.todo.foradhd.ui.theme.ErrorRose
import com.todo.foradhd.ui.theme.GlassBorder
import com.todo.foradhd.ui.theme.GlassBorderStrong
import com.todo.foradhd.ui.theme.GlassCard
import com.todo.foradhd.ui.theme.GlassDivider
import com.todo.foradhd.ui.theme.GlassOutlineButton
import com.todo.foradhd.ui.theme.ObsidianInputBg
import com.todo.foradhd.ui.theme.SilverPrimary
import com.todo.foradhd.ui.theme.SilverPrimaryButton
import com.todo.foradhd.ui.theme.SilverSecondary
import com.todo.foradhd.ui.theme.TextMuted
import com.todo.foradhd.ui.theme.TextOnSilver
import com.todo.foradhd.ui.theme.TextPrimary
import com.todo.foradhd.ui.theme.TextSecondary

@Composable
fun LoadingSplashOverlay() {
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
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF282828), Color(0xFF090909))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier.size(210.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(226.dp)
                        .rotate(angle)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(Color(0x22FFFFFF), SilverSecondary, Color(0x18FFFFFF))
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(198.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.dp, GlassBorderStrong, CircleShape),
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

            Spacer(Modifier.height(34.dp))
            Text("YOUR NEXT SMALL STEP", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "A little less noise.\nA little more you.",
                fontSize = 27.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 32.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text("Making room for your next step.", fontSize = 12.sp, color = TextMuted)
        }
    }
}

@Composable
fun ComfortSettingsCard(
    settings: RewardSettings,
    checkInFrequency: Int,
    accountLabel: String,
    onClose: () -> Unit,
    onUpdateSettings: ((RewardSettings) -> RewardSettings) -> Unit,
    onSelectFrequency: (Int) -> Unit,
    onOpenAccount: () -> Unit
) {
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
                    Icon(Icons.Filled.Close, contentDescription = "Close settings", tint = TextSecondary)
                }
            }

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
                Text("Focus check-in interval", fontSize = 12.sp, color = Color(0xFFC9C9C9))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, color = Color(0xFFC9C9C9))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextOnSilver,
                checkedTrackColor = SilverPrimary,
                uncheckedThumbColor = SilverSecondary,
                uncheckedTrackColor = Color(0x22FFFFFF)
            )
        )
    }
    HorizontalDivider(color = GlassDivider)
}

@Composable
fun AccountDialog(
    state: VinaiUiState,
    onDismiss: () -> Unit,
    onSetMode: (Boolean) -> Unit,
    onSubmit: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD000000))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 26.dp,
                borderColor = GlassBorderStrong
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
                            Icon(Icons.Filled.Close, contentDescription = "Close account", tint = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    if (state.session != null) {
                        Text(state.session.email, fontSize = 14.sp, color = TextPrimary)
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
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x10FFFFFF))
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
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
                                focusedContainerColor = ObsidianInputBg,
                                unfocusedContainerColor = ObsidianInputBg,
                                focusedBorderColor = GlassBorderStrong,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
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
                                focusedContainerColor = ObsidianInputBg,
                                unfocusedContainerColor = ObsidianInputBg,
                                focusedBorderColor = GlassBorderStrong,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
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
                                color = TextOnSilver,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = TextOnSilver, modifier = Modifier.size(16.dp))
                        }
                        if (state.authMessage.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(state.authMessage, fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
                        }
                        if (!state.supabaseConfigured) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Cloud sign-in needs Supabase configuration in AI Studio Secrets.",
                                fontSize = 11.sp,
                                color = TextMuted,
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) Color(0x1BFFFFFF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, color = if (selected) TextPrimary else TextMuted)
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
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF1B1B1B), Color(0xFF080808))))
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = SilverSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Back to tasks", fontSize = 13.sp, color = SilverSecondary)
            }

            Spacer(Modifier.height(18.dp))
            Text("TASK CHECK", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(10.dp))
            Text("Tell us what you did.", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (supabaseConfigured) {
                    "Write what you did or what you're going to do next. The AI checks that your update matches this task."
                } else {
                    "Write what you did or what you're going to do next. A clear, specific update completes your task."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 15.dp) {
                Column {
                    Text("YOUR TASK", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(Modifier.height(6.dp))
                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(task.microStep, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Your update", fontSize = 12.sp, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text("Write freely about what you did or what you plan to do for this task…", color = TextMuted)
                },
                minLines = 5,
                maxLines = 10,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianInputBg,
                    unfocusedContainerColor = ObsidianInputBg,
                    focusedBorderColor = GlassBorderStrong,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verification_text_input")
            )

            if (error.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(error, fontSize = 12.sp, color = ErrorRose, lineHeight = 17.sp)
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
                    color = TextOnSilver,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.Check, contentDescription = null, tint = TextOnSilver, modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Relevant updates complete the task. Unrelated updates leave it open.",
                fontSize = 11.sp,
                color = TextMuted,
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
    Dialog(
        onDismissRequest = onEndSession,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF1B1B1B), Color(0xFF080808))))
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
                    Icon(Icons.Filled.Close, contentDescription = "Close focus session", tint = TextSecondary)
                }
            }

            Text(task.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "One task. One small next action. This screen keeps the same quiet monochrome look.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(Modifier.height(14.dp))
            Text("What do I need to do?", fontSize = 12.sp, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.focusMicroStep,
                onValueChange = onMicroStepChange,
                placeholder = { Text("Write the next small action…", color = TextMuted) },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianInputBg,
                    unfocusedContainerColor = ObsidianInputBg,
                    focusedBorderColor = GlassBorderStrong,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth().testTag("focus_micro_input")
            )

            Spacer(Modifier.height(12.dp))
            Text("What have I done so far?", fontSize = 12.sp, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.focusProgressNote,
                onValueChange = onProgressNoteChange,
                placeholder = { Text("Add progress notes as you go…", color = TextMuted) },
                minLines = 2,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianInputBg,
                    unfocusedContainerColor = ObsidianInputBg,
                    focusedBorderColor = GlassBorderStrong,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth().testTag("focus_progress_input")
            )

            val mm = (state.focusRemainingSeconds / 60).toString().padStart(2, '0')
            val ss = (state.focusRemainingSeconds % 60).toString().padStart(2, '0')
            Spacer(Modifier.height(20.dp))
            Text(
                text = "$mm:$ss",
                fontSize = 48.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
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
                            .background(if (selected) Color(0xFFD5D5D5) else Color(0x12FFFFFF))
                            .border(1.dp, GlassBorder, RoundedCornerShape(999.dp))
                            .clickable(enabled = state.focusElapsedSeconds == 0) { onSelectPreset(mins) }
                            .padding(horizontal = 13.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "$mins min",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) TextOnSilver else TextSecondary
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
                            Text("Yes, continue", color = TextOnSilver, fontWeight = FontWeight.Bold)
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
                            Text("Continue", color = TextOnSilver, fontWeight = FontWeight.Bold)
                        }
                        GlassOutlineButton(onClick = { onExpiredCheckInRespond(false) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Take a break")
                        }
                    }
                }
            }

            if (state.focusRemainingSeconds == 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Focus session complete. You're eligible for 15 points when you verify the task.",
                    fontSize = 12.sp,
                    color = SilverPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(18.dp))
            if (state.focusQualified) {
                SilverPrimaryButton(
                    onClick = onVerifyTask,
                    modifier = Modifier.fillMaxWidth().testTag("focus_verify_button")
                ) {
                    Text("Verify task · 15 pts", color = TextOnSilver, fontWeight = FontWeight.Bold)
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
                    Text("End session")
                }
            }
        }
    }
}

@Composable
fun RingingAlarmDialog(
    alarm: RingingAlarmState,
    onSnooze: () -> Unit,
    onStartFocus: () -> Unit,
    onDismiss: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "alarm")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse"
    )

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            borderColor = GlassBorderStrong
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, GlassBorderStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "Ringing alarm",
                        tint = SilverPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("GENTLE REMINDER", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = alarm.title,
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
                SilverPrimaryButton(onClick = onStartFocus, modifier = Modifier.fillMaxWidth()) {
                    Text("Start focus", color = TextOnSilver, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                if (alarm.taskId != null) {
                    GlassOutlineButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth()) {
                        Text("Snooze 5 min")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                GlassOutlineButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Dismiss")
                }
            }
        }
    }
}
