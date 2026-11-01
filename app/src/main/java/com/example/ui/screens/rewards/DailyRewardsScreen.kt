package com.example.ui.screens.rewards

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.components.BdGradientButton
import com.example.ui.components.BdTopBar
import com.example.ui.theme.*

@Composable
fun DailyRewardsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val rewardsList = remember { mutableStateListOf(*MockRepository.dailyRewards.toTypedArray()) }
    val todayReward = rewardsList.firstOrNull { it.isCurrentDay }
    var streakDays by remember { mutableStateOf(3) }

    Scaffold(
        topBar = {
            BdTopBar(
                title = "7-Day Daily Rewards",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Streak Header Banner
            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(BrightGold, NeonMagenta))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🔥", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$streakDays-Day Check-in Streak!",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Check in every day to claim bonus virtual coins and VIP items",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (todayReward != null && !todayReward.isClaimed) {
                            BdGradientButton(
                                text = "Claim Day ${todayReward.day} Bonus (+${String.format("%,d", todayReward.coinAmount)} Coins) ✨",
                                onClick = {
                                    val idx = rewardsList.indexOfFirst { it.day == todayReward.day }
                                    if (idx != -1) {
                                        rewardsList[idx] = todayReward.copy(isClaimed = true)
                                        streakDays += 1
                                        Toast.makeText(context, "Claimed ${todayReward.coinAmount} Coins + ${todayReward.bonusItem}!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                brush = Brush.horizontalGradient(listOf(BrightGold, LuxuryGold, Color(0xFFD97706))),
                                testTag = "claim_daily_reward_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x3310B981))
                                    .border(1.dp, MintEmerald, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "✓ Today's Reward Claimed! Come back tomorrow at 00:00",
                                    color = MintEmerald,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 7-Day Roadmap Grid / List
            item {
                Text(
                    text = "📅 7-Day Reward Roadmap",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(rewardsList.size) { index ->
                val reward = rewardsList[index]
                val isToday = reward.isCurrentDay

                Surface(
                    color = if (isToday) Color(0x33A855F7) else DarkSurfaceCard,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isToday) 1.5.dp else 0.5.dp,
                        if (isToday) BrightGold else Color(0x33A855F7)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_reward_day_${reward.day}")
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
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (reward.isClaimed) Color(0x3310B981) else Color(0x33FFB703)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (reward.isClaimed) "✓" else "Day ${reward.day}",
                                    color = if (reward.isClaimed) MintEmerald else BrightGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "+${String.format("%,d", reward.coinAmount)} Virtual Coins",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isToday) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(NeonMagenta)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = "TODAY", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "Bonus: ${reward.bonusItem}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // State pill
                        if (reward.isClaimed) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x3310B981))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "Claimed", color = MintEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (isToday) {
                            Button(
                                onClick = {
                                    val idx = rewardsList.indexOfFirst { it.day == reward.day }
                                    if (idx != -1) {
                                        rewardsList[idx] = reward.copy(isClaimed = true)
                                        streakDays += 1
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrightGold),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Claim", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(text = "Locked", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
