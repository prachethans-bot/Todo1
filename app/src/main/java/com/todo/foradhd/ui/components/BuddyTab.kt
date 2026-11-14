package com.todo.foradhd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todo.foradhd.data.BuddyActivity
import com.todo.foradhd.data.BuddyState
import com.todo.foradhd.ui.VinaiUiState
import com.todo.foradhd.ui.theme.GlassCard
import com.todo.foradhd.ui.theme.GlassOutlineButton
import com.todo.foradhd.ui.theme.ObsidianAtmosphereBackground
import com.todo.foradhd.ui.theme.SilverPrimaryButton
import com.todo.foradhd.ui.theme.VinaiTheme

@Composable
fun BuddyTab(
    state: VinaiUiState,
    completedCount: Int,
    onNameChange: (String) -> Unit,
    onCreateInvite: () -> Unit,
    onShareInvite: () -> Unit,
    onChangeSharing: (BuddyState) -> Unit,
    onOpenFriendToken: (String) -> Unit
) {
    val colors = VinaiTheme.colors
    var friendLinkInput by remember { mutableStateOf("") }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BuddyOrbitCircle("You")
                Text("+", color = colors.textMuted, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 14.dp))
                BuddyOrbitCircle("Buddy")
            }

            Spacer(Modifier.height(20.dp))
            Text("BETTER, TOGETHER", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your accountability buddy",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Not a report card. A friend in your corner. Share focus status and send small cheers.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))
            val buddy = state.buddy
            if (buddy == null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("What should your buddy call you?", fontSize = 12.sp, color = colors.textPrimary)
                    OutlinedTextField(
                        value = state.buddyNameInput,
                        onValueChange = onNameChange,
                        placeholder = { Text("Your name", color = colors.textMuted) },
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
                            .fillMaxWidth()
                            .testTag("buddy_name_input")
                    )
                    SilverPrimaryButton(
                        onClick = onCreateInvite,
                        enabled = !state.buddyBusy && state.supabaseConfigured,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_buddy_invite_button")
                    ) {
                        Text(
                            text = if (state.buddyBusy) "Creating invite…" else "Create buddy invite",
                            color = colors.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.subtleOverlay)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    if (buddy.state == BuddyState.ACTIVE) colors.accent
                                    else colors.textMuted
                                )
                        )
                        Column {
                            Text(
                                text = when (buddy.state) {
                                    BuddyState.PENDING -> "Invite pending"
                                    BuddyState.PAUSED -> "Sharing paused"
                                    else -> "Buddy connected"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(buddy.ownerName, fontSize = 11.sp, color = colors.textMuted)
                        }
                    }

                    SilverPrimaryButton(
                        onClick = onShareInvite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_buddy_invite_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Share invite link", color = colors.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (buddy.state == BuddyState.PAUSED) {
                            GlassOutlineButton(
                                onClick = { onChangeSharing(BuddyState.ACTIVE) },
                                enabled = !state.buddyBusy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Resume sharing", fontSize = 11.sp)
                            }
                        } else {
                            GlassOutlineButton(
                                onClick = { onChangeSharing(BuddyState.PAUSED) },
                                enabled = !state.buddyBusy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pause sharing", fontSize = 11.sp)
                            }
                        }
                        GlassOutlineButton(
                            onClick = { onChangeSharing(BuddyState.REMOVED) },
                            enabled = !state.buddyBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Disconnect", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(colors.subtleOverlay)
                    .border(1.dp, colors.border, RoundedCornerShape(15.dp))
                    .padding(16.dp)
            ) {
                Text("TODAY'S CHECK-IN", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$completedCount small wins. Real progress.",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "I'm taking it one step at a time.",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = state.buddyMessage.ifBlank {
                    if (state.supabaseConfigured) "Your buddy sees only the task status you choose to share."
                    else "Configure Supabase environment keys in AI Studio Secrets to enable cloud buddy invitations."
                },
                fontSize = 11.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            if (state.supabaseConfigured) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = friendLinkInput,
                    onValueChange = { friendLinkInput = it },
                    placeholder = { Text("Paste a friend's invite link or token", color = colors.textMuted) },
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
                Spacer(Modifier.height(8.dp))
                GlassOutlineButton(
                    onClick = { onOpenFriendToken(friendLinkInput) },
                    enabled = friendLinkInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open buddy invite", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun BuddyOrbitCircle(label: String) {
    val colors = VinaiTheme.colors
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(colors.subtleOverlay, colors.surface)))
            .border(1.dp, colors.borderStrong, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
    }
}

@Composable
fun BuddyViewerScreen(
    state: VinaiUiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCheer: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = VinaiTheme.colors
    val view = state.buddyViewData
    val name = view?.ownerName ?: "Your friend"
    val cheers = listOf("👏", "🔥", "💪", "❤️")

    ObsidianAtmosphereBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("வினை · BUDDY", style = MaterialTheme.typography.labelSmall)
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to tasks",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    when {
                        !state.buddyViewLoaded -> Text("Loading…", color = colors.textSecondary, fontSize = 14.sp)
                        view == null -> Text("This invite link isn't active anymore.", color = colors.textSecondary, fontSize = 14.sp)
                        state.buddyViewDeclined -> Text("No problem. Thanks for stopping by.", color = colors.textSecondary, fontSize = 14.sp)
                        view.state == BuddyState.PENDING && !state.buddyViewAccepted -> {
                            Text(
                                text = "$name invited you to be their buddy.",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SilverPrimaryButton(onClick = onAccept, modifier = Modifier.weight(1f)) {
                                    Text("Accept", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                                }
                                GlassOutlineButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                                    Text("No thanks")
                                }
                            }
                        }
                        view.state == BuddyState.PENDING -> Text("Waiting for $name to get started.", color = colors.textSecondary, fontSize = 14.sp)
                        !state.buddyViewAccepted -> Text("This invite has already been used.", color = colors.textSecondary, fontSize = 14.sp)
                        view.state == BuddyState.PAUSED -> Text("Sharing is paused.", color = colors.textSecondary, fontSize = 14.sp)
                        else -> {
                            val left = (3 - view.cheersUsed).coerceAtLeast(0)
                            val canCheer = view.sessionId.isNotEmpty() && left > 0 && view.activity != BuddyActivity.IDLE
                            val statusText = when (view.activity) {
                                BuddyActivity.FOCUSING -> "Focusing"
                                BuddyActivity.BREAK -> "On a break"
                                BuddyActivity.DONE -> "Done"
                                BuddyActivity.IDLE -> ""
                            }
                            when (view.activity) {
                                BuddyActivity.IDLE -> Text("$name isn't in a session right now.", color = colors.textSecondary, fontSize = 14.sp)
                                BuddyActivity.DONE -> Text("$name finished a task!", style = MaterialTheme.typography.headlineMedium)
                                BuddyActivity.FOCUSING, BuddyActivity.BREAK -> Text(
                                    "$name is working on: ${view.taskLabel.ifBlank { "something" }}",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                            if (statusText.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(statusText, fontSize = 12.sp, color = colors.textPrimary)
                            }
                            if (canCheer) {
                                Spacer(Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    cheers.forEach { emoji ->
                                        GlassOutlineButton(
                                            onClick = { onCheer(emoji) },
                                            modifier = Modifier.size(52.dp)
                                        ) {
                                            Text(emoji, fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                            if (state.buddyViewFeedback.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(state.buddyViewFeedback, fontSize = 13.sp, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
