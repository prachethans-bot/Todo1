package com.todo.foradhd.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todo.foradhd.data.PlanStep
import com.todo.foradhd.data.QuickNote
import com.todo.foradhd.data.SavedTask
import com.todo.foradhd.ui.theme.GlassCard
import com.todo.foradhd.ui.theme.SilverPrimaryButton
import com.todo.foradhd.ui.theme.VinaiTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Circular progress donut chart visualizing completed vs pending tasks and subtasks for the day.
 */
@Composable
fun DailyProgressDonutCard(
    tasks: List<SavedTask>,
    planSteps: List<PlanStep>,
    modifier: Modifier = Modifier
) {
    val colors = VinaiTheme.colors
    val completedTasks = tasks.count { it.completed }
    val pendingTasks = tasks.count { !it.completed }
    val completedSubtasks = planSteps.count { it.completed }
    val pendingSubtasks = planSteps.count { !it.completed }

    val totalItems = completedTasks + pendingTasks + completedSubtasks + pendingSubtasks
    val doneItems = completedTasks + completedSubtasks
    val targetFraction = if (totalItems > 0) doneItems.toFloat() / totalItems.toFloat() else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "daily_donut"
    )
    val pct = (targetFraction * 100f).roundToInt()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(96.dp)) {
                    val strokePx = 10.dp.toPx()
                    drawArc(
                        color = colors.ringTrack,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokePx)
                    )
                    if (animatedFraction > 0f) {
                        drawArc(
                            color = colors.accent,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedFraction,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$pct%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text("today", fontSize = 10.sp, color = colors.textMuted)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("DAILY PROGRESS", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = if (totalItems == 0) "Ready when you are" else "$doneItems of $totalItems completed",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot(color = colors.accent, label = "Done ($doneItems)")
                    LegendDot(color = colors.ringTrack, label = "Pending (${totalItems - doneItems})")
                }
                if (planSteps.isNotEmpty()) {
                    Text(
                        text = "Includes $completedSubtasks/${planSteps.size} breakdown subtasks",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    val colors = VinaiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = colors.textSecondary)
    }
}

/**
 * Weekly Summary analytics card driven by real task history, streaks, and timestamps.
 */
@Composable
fun WeeklySummaryCard(
    tasks: List<SavedTask>,
    streak: Int,
    points: Int,
    modifier: Modifier = Modifier
) {
    val colors = VinaiTheme.colors
    val summary = remember(tasks) { computeWeeklySummary(tasks) }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Insights, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    Text("WEEKLY SUMMARY", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                    Text("$streak day streak · $points pts", fontSize = 11.sp, color = colors.textSecondary)
                }
            }

            Spacer(Modifier.height(14.dp))

            val maxCount = summary.dayCounts.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                summary.dayCounts.forEach { (dayLabel, count) ->
                    val ratio = count.toFloat() / maxCount.toFloat()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (count > 0) {
                            Text("$count", fontSize = 10.sp, color = colors.textSecondary)
                            Spacer(Modifier.height(3.dp))
                        }
                        Canvas(
                            modifier = Modifier
                                .width(16.dp)
                                .height(48.dp)
                        ) {
                            drawRoundRect(
                                color = colors.ringTrack,
                                topLeft = Offset.Zero,
                                size = size,
                                cornerRadius = CornerRadius(6.dp.toPx())
                            )
                            if (count > 0) {
                                val barH = (size.height * ratio).coerceAtLeast(6.dp.toPx())
                                drawRoundRect(
                                    color = colors.accent,
                                    topLeft = Offset(0f, size.height - barH),
                                    size = Size(size.width, barH),
                                    cornerRadius = CornerRadius(6.dp.toPx())
                                )
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(dayLabel, fontSize = 10.sp, color = colors.textMuted, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("THIS WEEK", fontSize = 9.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
                    Text("${summary.weeklyCompleted} completed", fontSize = 12.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PEAK FOCUS TIME", fontSize = 9.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
                    Text(summary.productiveWindow, fontSize = 12.sp, color = colors.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private data class WeeklySummaryData(
    val dayCounts: List<Pair<String, Int>>,
    val weeklyCompleted: Int,
    val productiveWindow: String
)

private fun computeWeeklySummary(tasks: List<SavedTask>): WeeklySummaryData {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val countsByDate = mutableMapOf<LocalDate, Int>()
    val bucketCounts = mutableMapOf("Morning" to 0, "Afternoon" to 0, "Evening" to 0, "Night" to 0)

    tasks.filter { it.completed }.forEach { task ->
        val iso = task.completedAt ?: task.createdAt
        runCatching {
            val zdt = Instant.parse(iso).atZone(zone)
            val date = zdt.toLocalDate()
            countsByDate[date] = (countsByDate[date] ?: 0) + 1
            val hour = zdt.hour
            val bucket = when (hour) {
                in 5..11 -> "Morning"
                in 12..16 -> "Afternoon"
                in 17..21 -> "Evening"
                else -> "Night"
            }
            bucketCounts[bucket] = (bucketCounts[bucket] ?: 0) + 1
        }
    }

    val dayPairs = days.map { d ->
        d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) to (countsByDate[d] ?: 0)
    }
    val weeklyDone = days.sumOf { countsByDate[it] ?: 0 }
    val topWindow = bucketCounts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: "Flexible"
    return WeeklySummaryData(dayPairs, weeklyDone, topWindow)
}

/**
 * Persistent Quick Notes section at the bottom of Today for fleeting thoughts / brain dumps.
 */
@Composable
fun QuickNotesSection(
    notes: List<QuickNote>,
    input: String,
    onInputChange: (String) -> Unit,
    onAddNote: () -> Unit,
    onDeleteNote: (String) -> Unit,
    onPromoteToTask: (QuickNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VinaiTheme.colors
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.EditNote, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(17.dp))
                Text("QUICK NOTES · BRAIN DUMP", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Park fleeting thoughts here without turning them into tasks yet.",
                fontSize = 11.sp,
                color = colors.textMuted
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text("Jot down a thought…", color = colors.textMuted, fontSize = 13.sp) },
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
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_note_input")
                )
                SilverPrimaryButton(
                    onClick = onAddNote,
                    enabled = input.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("add_quick_note_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Save note", tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                }
            }

            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notes.forEach { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.subtleOverlay)
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = note.text,
                                fontSize = 12.sp,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.elevated)
                                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                    .clickable { onPromoteToTask(note) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("Task", fontSize = 10.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Make task", tint = colors.textPrimary, modifier = Modifier.size(11.dp))
                                }
                            }
                            IconButton(
                                onClick = { onDeleteNote(note.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete note", tint = colors.textMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
