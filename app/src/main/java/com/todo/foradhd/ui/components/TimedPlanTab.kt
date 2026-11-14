package com.todo.foradhd.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.todo.foradhd.data.PlanStatus
import com.todo.foradhd.data.PlanStep
import com.todo.foradhd.data.PlanStepKind
import com.todo.foradhd.ui.VinaiUiState
import com.todo.foradhd.ui.theme.ApplyDialogBlurBehind
import com.todo.foradhd.ui.theme.GlassCard
import com.todo.foradhd.ui.theme.GlassOutlineButton
import com.todo.foradhd.ui.theme.SilverPrimaryButton
import com.todo.foradhd.ui.theme.VinaiTheme
import kotlin.math.roundToInt

@Composable
fun TimedPlanTab(
    state: VinaiUiState,
    onGoalChange: (String) -> Unit,
    onTotalMinutesChange: (Int) -> Unit,
    onToggleStrictTime: (Boolean) -> Unit,
    onCreatePlan: () -> Unit,
    onRescalePlan: (Int) -> Unit,
    onAddStep: (String, String, Int) -> Unit,
    onEditStep: (Int, String, String, Int) -> Unit,
    onDeleteStep: (Int) -> Unit,
    onMoveStep: (Int, Int) -> Unit,
    onToggleStepComplete: (Int) -> Unit,
    onCompleteCurrentStep: () -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit
) {
    val colors = VinaiTheme.colors
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var addingStep by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text("AI TASK BREAKDOWN", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Text("Big task, small steps", style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                text = "Analyze, customize,\nand finish one step at a time.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = colors.textMuted,
                textAlign = TextAlign.End
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("ENTER YOUR TASK", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.planGoal,
                    onValueChange = onGoalChange,
                    placeholder = {
                        Text(
                            "e.g. Complete a mathematics chapter on Banking, or clean my room",
                            color = colors.textMuted
                        )
                    },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_goal_input")
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AVAILABLE TIME", style = MaterialTheme.typography.labelSmall)
                    val hrs = state.planTotalMinutes / 60
                    val mins = state.planTotalMinutes % 60
                    val label = if (hrs > 0) {
                        if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
                    } else {
                        "${state.planTotalMinutes} min"
                    }
                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }

                Slider(
                    value = state.planTotalMinutes.toFloat(),
                    onValueChange = { raw ->
                        val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(10, 240)
                        onTotalMinutesChange(snapped)
                    },
                    valueRange = 10f..240f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_duration_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Strict time limit", fontSize = 12.sp, color = colors.textSecondary)
                    Switch(
                        checked = state.planStrictTimeLimit,
                        onCheckedChange = onToggleStrictTime,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.secondary,
                            uncheckedTrackColor = colors.subtleOverlay
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SilverPrimaryButton(
                        onClick = onCreatePlan,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_plan_button")
                    ) {
                        Icon(
                            imageVector = if (state.planSteps.isEmpty()) Icons.Filled.AutoAwesome else Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (state.planSteps.isEmpty()) "Break down task" else "Regenerate",
                            color = colors.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    if (state.planSteps.isNotEmpty()) {
                        GlassOutlineButton(
                            onClick = { onRescalePlan(state.planTotalMinutes) },
                            modifier = Modifier.testTag("rescale_plan_button")
                        ) {
                            Text("Fit ${state.planTotalMinutes}m", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (state.planSteps.isNotEmpty()) {
            val totalSteps = state.planSteps.size
            val completedCount = state.planSteps.count { it.completed }
            val remainingCount = totalSteps - completedCount
            val completionPct = ((completedCount.toFloat() / totalSteps.toFloat()) * 100f).roundToInt()
            val remainingMins = state.planSteps.filter { !it.completed }.sumOf { it.minutes }
            val activeStep = state.planSteps.getOrNull(state.planCurrentIndex)

            // Overall Progress Card
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("OVERALL PROGRESS", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(state.planGoal.ifBlank { "Current breakdown" }, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "$completionPct%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (completedCount.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f) },
                        color = colors.accent,
                        trackColor = colors.ringTrack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(99.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$completedCount/$totalSteps done", fontSize = 11.sp, color = colors.textSecondary)
                        Text("$remainingCount left · ~$remainingMins min", fontSize = 11.sp, color = colors.textMuted)
                    }
                }
            }

            // Completion celebration or Active Subtask Focus Card
            if (state.planStatus == PlanStatus.FINISHED || remainingCount == 0) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    borderColor = colors.accent
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Breakdown Complete!", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "You turned a big task into $totalSteps doable wins.",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        GlassOutlineButton(onClick = onStop) {
                            Text("Start another round", fontSize = 12.sp)
                        }
                    }
                }
            } else if (activeStep != null) {
                val breathe = rememberInfiniteTransition(label = "plan_breathe")
                val breatheScale by breathe.animateFloat(
                    initialValue = 0.92f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "breathe_scale"
                )
                val totalSecs = (state.planStepRemainingMs / 1000L).coerceAtLeast(0L)
                val mm = (totalSecs / 60L).toString().padStart(2, '0')
                val ss = (totalSecs % 60L).toString().padStart(2, '0')

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    borderColor = if (state.planStatus == PlanStatus.RUNNING) colors.borderStrong else colors.border
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CURRENT SUBTASK · STEP ${state.planCurrentIndex + 1} OF $totalSteps",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = activeStep.title,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        if (activeStep.description.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = activeStep.description,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        Box(contentAlignment = Alignment.Center) {
                            if (state.planStatus == PlanStatus.RUNNING) {
                                Box(
                                    modifier = Modifier
                                        .size(118.dp)
                                        .scale(breatheScale)
                                        .blur(radius = 24.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(colors.accentMuted, Color.Transparent)
                                            )
                                        )
                                )
                            }
                            Text(
                                text = "$mm:$ss",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        SilverPrimaryButton(
                            onClick = onCompleteCurrentStep,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("plan_complete_step_button")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Complete subtask & move next", color = colors.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.planStatus != PlanStatus.RUNNING) {
                                SilverPrimaryButton(
                                    onClick = onStartOrResume,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("plan_start_button")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (state.planStatus == PlanStatus.PAUSED) "Resume" else "Start",
                                        color = colors.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                GlassOutlineButton(
                                    onClick = onPause,
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("plan_pause_button")
                                ) {
                                    Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Pause", fontSize = 12.sp)
                                }
                            }
                            GlassOutlineButton(
                                onClick = onSkip,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("plan_skip_button")
                            ) {
                                Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Skip", fontSize = 12.sp)
                            }
                            GlassOutlineButton(
                                onClick = onStop,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("plan_stop_button")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Reset", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Editable Subtasks List
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CUSTOMIZE SUBTASKS", style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = { addingStep = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add step", fontSize = 12.sp, color = colors.primary)
                        }
                    }

                    state.planSteps.forEachIndexed { index, step ->
                        val isActive = index == state.planCurrentIndex && !step.completed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (step.completed) 0.55f else 1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) colors.subtleOverlay else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isActive) colors.borderStrong else colors.border,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1-tap checkbox
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (step.completed) colors.accent else colors.subtleOverlay)
                                    .border(1.dp, if (step.completed) colors.accent else colors.borderStrong, RoundedCornerShape(8.dp))
                                    .clickable { onToggleStepComplete(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (step.completed) {
                                    Icon(Icons.Filled.Check, contentDescription = "Done", tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                                } else {
                                    Text(
                                        text = (index + 1).toString().padStart(2, '0'),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    textDecoration = if (step.completed) TextDecoration.LineThrough else TextDecoration.None
                                )
                                if (step.description.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(step.description, fontSize = 11.sp, color = colors.textMuted, lineHeight = 15.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${step.minutes} min · ${if (step.kind == PlanStepKind.BREAK) "BREAK" else "FOCUS"}",
                                    fontSize = 10.sp,
                                    color = colors.accent
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row {
                                    IconButton(
                                        onClick = { onMoveStep(index, -1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", tint = colors.textMuted, modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = { onMoveStep(index, 1) },
                                        enabled = index < state.planSteps.lastIndex,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down", tint = colors.textMuted, modifier = Modifier.size(15.dp))
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = { editingIndex = index },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit step", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                                    }
                                    if (state.planSteps.size > 1) {
                                        IconButton(
                                            onClick = { onDeleteStep(index) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete step", tint = colors.textMuted, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = state.planNotice.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                            Text(state.planNotice, fontSize = 11.sp, color = colors.textSecondary, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }

    editingIndex?.let { idx ->
        val target = state.planSteps.getOrNull(idx)
        if (target != null) {
            PlanStepEditorDialog(
                initialTitle = target.title,
                initialDescription = target.description,
                initialMinutes = target.minutes,
                dialogTitle = "Edit subtask",
                onSave = { title, desc, mins ->
                    onEditStep(idx, title, desc, mins)
                    editingIndex = null
                },
                onDismiss = { editingIndex = null }
            )
        }
    }

    if (addingStep) {
        PlanStepEditorDialog(
            initialTitle = "",
            initialDescription = "",
            initialMinutes = 15,
            dialogTitle = "Add custom subtask",
            onSave = { title, desc, mins ->
                onAddStep(title, desc, mins)
                addingStep = false
            },
            onDismiss = { addingStep = false }
        )
    }
}

@Composable
private fun PlanStepEditorDialog(
    initialTitle: String,
    initialDescription: String,
    initialMinutes: Int,
    dialogTitle: String,
    onSave: (String, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = VinaiTheme.colors
    var title by remember { mutableStateOf(initialTitle) }
    var desc by remember { mutableStateOf(initialDescription) }
    var minsText by remember { mutableStateOf(initialMinutes.toString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ApplyDialogBlurBehind(blurRadiusPx = 46)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.modalScrim)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                borderColor = colors.borderStrong
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(dialogTitle, style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Subtask title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.inputBg,
                            unfocusedContainerColor = colors.inputBg,
                            focusedBorderColor = colors.borderStrong,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Short description (optional)") },
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.inputBg,
                            unfocusedContainerColor = colors.inputBg,
                            focusedBorderColor = colors.borderStrong,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = minsText,
                        onValueChange = { minsText = it.filter(Char::isDigit) },
                        label = { Text("Duration (minutes)") },
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlineButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        SilverPrimaryButton(
                            onClick = {
                                val m = (minsText.toIntOrNull() ?: initialMinutes).coerceIn(1, 240)
                                onSave(title, desc, m)
                            },
                            enabled = title.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, color = colors.onPrimary)
                        }
                    }
                }
            }
        }
    }
}
