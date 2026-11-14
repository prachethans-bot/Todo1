package com.todo.foradhd.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todo.foradhd.ui.theme.VinaiTheme
import kotlinx.coroutines.delay
import java.time.Instant

private fun formatRemaining(ms: Long): Pair<String, String> {
    if (ms <= 0L) return "Still here when you're ready" to ""
    val totalSecs = ms / 1000L
    val hrs = totalSecs / 3600L
    val mins = (totalSecs % 3600L) / 60L
    val secs = totalSecs % 60L
    return when {
        hrs > 0 -> "${hrs}h ${mins}m" to "left"
        mins > 0 -> "${mins}m ${secs}s" to "left"
        else -> "${secs}s" to "left"
    }
}

@Composable
fun CountdownRing(
    reminderAt: String?,
    createdAt: String? = null,
    isBig: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (reminderAt.isNullOrBlank()) return
    val colors = VinaiTheme.colors

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(reminderAt) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val targetMs = remember(reminderAt) {
        runCatching { Instant.parse(reminderAt).toEpochMilli() }.getOrNull()
    } ?: return
    val createdMs = remember(createdAt, targetMs) {
        createdAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: (targetMs - 3600_000L)
    }

    val remainingMs = targetMs - nowMs
    val isPassed = remainingMs <= 0L
    val totalDuration = (targetMs - createdMs).coerceAtLeast(60_000L)
    val fraction = if (isPassed) 0f else (remainingMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val (text, sub) = formatRemaining(remainingMs)

    if (isBig) {
        Box(
            modifier = modifier.size(112.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(112.dp)) {
                val strokePx = 6.dp.toPx()
                drawArc(
                    color = colors.ringTrack,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokePx)
                )
                drawArc(
                    color = if (isPassed) colors.accentMuted else colors.accent,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isPassed) {
                    Text(
                        text = text,
                        fontSize = 10.sp,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp
                    )
                } else {
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    if (sub.isNotEmpty()) {
                        Text(
                            text = sub,
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val strokePx = 2.5.dp.toPx()
                drawArc(
                    color = colors.ringTrack,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokePx)
                )
                drawArc(
                    color = if (isPassed) colors.accentMuted else colors.accent,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
            Text(
                text = if (isPassed) text else "$text $sub".trim(),
                fontSize = 11.sp,
                color = if (isPassed) colors.textMuted else colors.accent
            )
        }
    }
}
