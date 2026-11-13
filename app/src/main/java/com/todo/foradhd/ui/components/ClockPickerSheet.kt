package com.todo.foradhd.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.todo.foradhd.ui.theme.GlassBorder
import com.todo.foradhd.ui.theme.GlassOutlineButton
import com.todo.foradhd.ui.theme.RingLime
import com.todo.foradhd.ui.theme.SilverPrimaryButton
import com.todo.foradhd.ui.theme.TextMuted
import com.todo.foradhd.ui.theme.TextOnSilver
import com.todo.foradhd.ui.theme.TextPrimary
import com.todo.foradhd.ui.theme.TextSecondary
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class DialStep { HOUR, MINUTE }

@Composable
fun ClockPickerDialog(
    initialTime24: String?,
    onSet: (String) -> Unit,
    onCancel: () -> Unit
) {
    val initValues = remember(initialTime24) {
        if (!initialTime24.isNullOrBlank() && ":" in initialTime24) {
            val parts = initialTime24.split(":")
            val h = parts[0].toIntOrNull() ?: 9
            val m = parts[1].toIntOrNull() ?: 0
            val isPm = h >= 12
            val h12 = if (h % 12 == 0) 12 else h % 12
            Triple(h12, m.coerceIn(0, 59), isPm)
        } else {
            val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val isPm = h >= 12
            val h12 = if (h % 12 == 0) 12 else h % 12
            Triple(h12, 0, isPm)
        }
    }

    var step by remember { mutableStateOf(DialStep.HOUR) }
    var hour12 by remember { mutableIntStateOf(initValues.first) }
    var minute by remember { mutableIntStateOf(initValues.second) }
    var isPm by remember { mutableStateOf(initValues.third) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF252525), Color(0xFF090909))))
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFF777777))
                )
                Spacer(Modifier.height(16.dp))

                // Digital readout header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TimeDigitBox(
                        text = hour12.toString().padStart(2, '0'),
                        selected = step == DialStep.HOUR,
                        onClick = { step = DialStep.HOUR },
                        modifier = Modifier.testTag("clock_hour_digit")
                    )
                    Text(":", fontSize = 28.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 4.dp))
                    TimeDigitBox(
                        text = minute.toString().padStart(2, '0'),
                        selected = step == DialStep.MINUTE,
                        onClick = { step = DialStep.MINUTE },
                        modifier = Modifier.testTag("clock_minute_digit")
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isPm) "PM" else "AM",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (step == DialStep.HOUR) "Step 1: Pick the hour" else "Step 2: Pick the minutes",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Spacer(Modifier.height(16.dp))

                // Circular 260dp dial
                val dialDp = 260.dp
                val dialRadiusDp = 96.dp
                val density = LocalDensity.current
                val dialPx = with(density) { dialDp.toPx() }
                val radiusPx = with(density) { dialRadiusDp.toPx() }
                val centerPx = dialPx / 2f

                fun updateFromOffset(pos: Offset, commitStep: Boolean) {
                    val dx = pos.x - centerPx
                    val dy = pos.y - centerPx
                    var deg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI) + 90.0
                    if (deg < 0) deg += 360.0
                    if (step == DialStep.HOUR) {
                        var h = (deg / 30.0).roundToInt()
                        if (h == 0 || h == 12) h = 12
                        hour12 = h
                        if (commitStep) step = DialStep.MINUTE
                    } else {
                        minute = (deg / 6.0).roundToInt() % 60
                    }
                }

                Box(
                    modifier = Modifier
                        .size(dialDp)
                        .clip(CircleShape)
                        .background(Color(0x0DFFFFFF))
                        .pointerInput(step) {
                            detectTapGestures { offset ->
                                updateFromOffset(offset, commitStep = true)
                            }
                        }
                        .pointerInput(step) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (step == DialStep.HOUR) step = DialStep.MINUTE
                                }
                            ) { change, _ ->
                                change.consume()
                                updateFromOffset(change.position, commitStep = false)
                            }
                        }
                ) {
                    val angleDeg = if (step == DialStep.HOUR) (hour12 % 12) * 30f else minute * 6f
                    val angleRad = Math.toRadians((angleDeg - 90f).toDouble())
                    val handEndX = centerPx + radiusPx * cos(angleRad).toFloat()
                    val handEndY = centerPx + radiusPx * sin(angleRad).toFloat()

                    Canvas(modifier = Modifier.size(dialDp)) {
                        drawCircle(color = RingLime, radius = 4.dp.toPx(), center = Offset(centerPx, centerPx))
                        drawLine(
                            color = RingLime,
                            start = Offset(centerPx, centerPx),
                            end = Offset(handEndX, handEndY),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(color = RingLime, radius = 19.dp.toPx(), center = Offset(handEndX, handEndY))
                    }

                    val items = if (step == DialStep.HOUR) {
                        listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
                    } else {
                        listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
                    }

                    val nodeSizePx = with(density) { 44.dp.toPx() }
                    items.forEach { value ->
                        val deg = if (step == DialStep.HOUR) (value % 12) * 30.0 - 90.0 else value * 6.0 - 90.0
                        val rad = Math.toRadians(deg)
                        val x = centerPx + radiusPx * cos(rad).toFloat() - nodeSizePx / 2f
                        val y = centerPx + radiusPx * sin(rad).toFloat() - nodeSizePx / 2f
                        val isSelected = if (step == DialStep.HOUR) hour12 == value else minute == value

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFDDDDDD) else Color.Transparent)
                                .clickable {
                                    if (step == DialStep.HOUR) {
                                        hour12 = value
                                        step = DialStep.MINUTE
                                    } else {
                                        minute = value
                                    }
                                }
                                .semantics {
                                    contentDescription = if (step == DialStep.HOUR) "Hour $value" else "$value minutes"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (step == DialStep.HOUR) value.toString() else value.toString().padStart(2, '0'),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF111111) else TextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // AM / PM toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AmPmChip(label = "AM", selected = !isPm, onClick = { isPm = false }, modifier = Modifier.testTag("clock_am_btn"))
                    AmPmChip(label = "PM", selected = isPm, onClick = { isPm = true }, modifier = Modifier.testTag("clock_pm_btn"))
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassOutlineButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).testTag("clock_cancel_btn")
                    ) {
                        Text("Cancel")
                    }
                    SilverPrimaryButton(
                        onClick = {
                            var h24 = hour12 % 12
                            if (isPm) h24 += 12
                            val hh = h24.toString().padStart(2, '0')
                            val mm = minute.toString().padStart(2, '0')
                            onSet("$hh:$mm")
                        },
                        modifier = Modifier.weight(1f).testTag("clock_set_btn")
                    ) {
                        Text("Set", color = TextOnSilver, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeDigitBox(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0x1BFFFFFF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun AmPmChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0x1BFFFFFF) else Color.Transparent)
            .border(1.dp, if (selected) GlassBorder else Color(0x20FFFFFF), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 12.sp, color = TextPrimary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
