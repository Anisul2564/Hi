package com.example.ui.screens.event

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MockRepository
import com.example.model.EventTask
import com.example.model.EventTaskType
import com.example.ui.components.BdTopBar
import com.example.ui.theme.*

@Composable
fun EventScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val event = remember { MockRepository.communityEvent }
    val tasksList = remember { mutableStateListOf(*event.tasks.toTypedArray()) }

    Scaffold(
        topBar = {
            BdTopBar(
                title = "Carnival Events",
                onBackClick = onBack,
                showActions = false
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Event Hero Banner
            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(BrightGold, NeonMagenta, NeonViolet))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF3B0764), Color(0xFF1E1B4B), DarkBackground)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33EC4899))
                                    .border(0.5.dp, NeonMagenta, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(text = "SEASON FESTIVAL", color = NeonMagenta, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "⏳ ${event.countdownText}",
                                color = BrightGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = event.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = event.subtitle,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Event Rewards Tier Preview
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RewardBadgePill("🎪 Carnival Frame", "14 Days", Modifier.weight(1f))
                            RewardBadgePill("🪙 25,000 Coins", "Task Pot", Modifier.weight(1f))
                            RewardBadgePill("🎖️ Starlight Medal", "Permanent", Modifier.weight(1f))
                        }
                    }
                }
            }

            // Tasks Section Header
            item {
                Text(
                    text = "🎯 Event Mission Tasks",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Task List (Recharge, Mic-Time, Gifting, Invite)
            items(tasksList) { task ->
                EventTaskCard(
                    task = task,
                    onClaim = {
                        val idx = tasksList.indexOfFirst { it.id == task.id }
                        if (idx != -1) {
                            tasksList[idx] = task.copy(isClaimed = true)
                            Toast.makeText(context, "Claimed ${task.rewardCoins} Virtual Coins!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RewardBadgePill(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0x33251842),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFD166)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = BrightGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextMuted, fontSize = 9.sp)
        }
    }
}

@Composable
fun EventTaskCard(
    task: EventTask,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isComplete = task.currentProgress >= task.totalProgress
    val progress = (task.currentProgress.toFloat() / task.totalProgress.toFloat()).coerceIn(0f, 1f)

    val icon = when (task.type) {
        EventTaskType.MIC_TIME -> "🎙️"
        EventTaskType.RECHARGE -> "⚡"
        EventTaskType.GIFTING -> "🎁"
        EventTaskType.INVITE -> "👥"
    }

    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_task_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33A855F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 22.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = task.requirement,
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${task.currentProgress} / ${task.totalProgress}",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "+${String.format("%,d", task.rewardCoins)} Coins",
                            color = BrightGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        color = if (isComplete) MintEmerald else NeonViolet,
                        trackColor = Color(0x33FFFFFF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Claim or status button
            if (task.isClaimed) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x3310B981))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "Claimed", color = MintEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else if (isComplete) {
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = BrightGold),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("claim_task_button_${task.id}")
                ) {
                    Text(text = "Claim", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "In Progress", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}
