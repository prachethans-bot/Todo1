package com.todo.foradhd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todo.foradhd.data.PlanStatus
import com.todo.foradhd.data.PlanStepKind
import com.todo.foradhd.ui.VinaiUiState
import com.todo.foradhd.ui.theme.GlassBorder
import com.todo.foradhd.ui.theme.GlassBorderStrong
import com.todo.foradhd.ui.theme.GlassCard
import com.todo.foradhd.ui.theme.GlassOutlineButton
import com.todo.foradhd.ui.theme.ObsidianInputBg
import com.todo.foradhd.ui.theme.SilverPrimary
import com.todo.foradhd.ui.theme.SilverPrimaryButton
import com.todo.foradhd.ui.theme.SilverSecondary
import com.todo.foradhd.ui.theme.TextMuted
import com.todo.foradhd.ui.theme.TextOnSilver
import com.todo.foradhd.ui.theme.TextPrimary
import com.todo.foradhd.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun TimedPlanTab(
    state: VinaiUiState,
    onGoalChange: (String) -> Unit,
    onTotalMinutesChange: (Int) -> Unit,
    onCreatePlan: () -> Unit,
    onChangeStepMinutes: (Int, Int) -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text("FOCUS MODE", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Text("Build a timed plan", style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                text = "Small steps, planned breaks\nand reminders along the way.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = TextMuted,
                textAlign = TextAlign.End
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("WHAT DO YOU WANT TO FINISH?", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.planGoal,
                    onValueChange = onGoalChange,
                    placeholder = { Text("e.g. Study one chapter of maths", color = TextMuted) },
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
                    Text("TOTAL TIME", style = MaterialTheme.typography.labelSmall)
                    val hrs = state.planTotalMinutes / 60
                    val mins = state.planTotalMinutes % 60
                    val label = if (hrs > 0) {
                        if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
                    } else {
                        "${state.planTotalMinutes} min"
                    }
                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Slider(
                    value = state.planTotalMinutes.toFloat(),
                    onValueChange = { raw ->
                        val snapped = ((raw / 15f).roundToInt() * 15).coerceIn(15, 240)
                        onTotalMinutesChange(snapped)
                    },
                    valueRange = 15f..240f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = SilverPrimary,
                        activeTrackColor = SilverSecondary,
                        inactiveTrackColor = Color(0x22FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_duration_slider")
                )

                SilverPrimaryButton(
                    onClick = onCreatePlan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_plan_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextOnSilver,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Create my plan", color = TextOnSilver, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (state.planSteps.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    val remainingTotal = state.planSteps
                        .drop(state.planCurrentIndex)
                        .sumOf { it.minutes }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("YOUR PLAN", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(state.planGoal, style = MaterialTheme.typography.titleMedium)
                        }
                        Text("$remainingTotal min left", fontSize = 11.sp, color = TextMuted)
                    }

                    if (state.planStatus == PlanStatus.RUNNING) {
                        val activeStep = state.planSteps.getOrNull(state.planCurrentIndex)
                        val totalSecs = (state.planStepRemainingMs / 1000L).coerceAtLeast(0L)
                        val mm = (totalSecs / 60L).toString().padStart(2, '0')
                        val ss = (totalSecs % 60L).toString().padStart(2, '0')
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0x0DFFFFFF))
                                .border(1.dp, GlassBorder, RoundedCornerShape(15.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NEXT STEP · ${activeStep?.title.orEmpty()}",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "$mm:$ss",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Countdown updates while this screen is open",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.planSteps.forEachIndexed { index, step ->
                            val isActive = index == state.planCurrentIndex && state.planStatus == PlanStatus.RUNNING
                            val isPast = index < state.planCurrentIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (isPast) 0.5f else 1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isActive) Color(0x18FFFFFF) else Color(0x07FFFFFF))
                                    .border(
                                        1.dp,
                                        if (isActive) GlassBorderStrong else Color(0x1EFFFFFF),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = (index + 1).toString().padStart(2, '0'),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SilverSecondary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(step.title, fontSize = 12.sp, color = TextPrimary, lineHeight = 16.sp)
                                    Text(
                                        text = if (step.kind == PlanStepKind.BREAK) "BREAK" else "FOCUS STEP",
                                        fontSize = 9.sp,
                                        letterSpacing = 0.08.sp,
                                        color = TextMuted
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    OutlinedTextField(
                                        value = step.minutes.toString(),
                                        onValueChange = { raw ->
                                            val num = raw.filter { it.isDigit() }.toIntOrNull() ?: 1
                                            onChangeStepMinutes(index, num)
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0x16FFFFFF),
                                            unfocusedContainerColor = Color(0x12FFFFFF),
                                            focusedBorderColor = GlassBorderStrong,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.width(62.dp)
                                    )
                                    Text("min", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.planStatus != PlanStatus.RUNNING) {
                            val label = when (state.planStatus) {
                                PlanStatus.PAUSED -> "Resume"
                                PlanStatus.FINISHED -> "Start again"
                                else -> "Start plan"
                            }
                            SilverPrimaryButton(
                                onClick = onStartOrResume,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("plan_start_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = TextOnSilver, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(label, color = TextOnSilver, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            enabled = state.planStatus == PlanStatus.RUNNING,
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
                                .weight(1f)
                                .testTag("plan_stop_button")
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stop", fontSize = 12.sp)
                        }
                    }
                }

                if (state.planNotice.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(state.planNotice, fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Android app: local reminders alert you for each scheduled step even when locked.",
                        fontSize = 10.sp,
                        color = TextMuted,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
