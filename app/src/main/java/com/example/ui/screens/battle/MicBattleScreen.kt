package com.example.ui.screens.battle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MockRepository
import com.example.model.User
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun MicBattleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val pkState = remember { MockRepository.pkBattleState }
    var challengerScore by remember { mutableStateOf(pkState.challengerScore) }
    var defenderScore by remember { mutableStateOf(pkState.defenderScore) }

    val totalScore = (challengerScore + defenderScore).coerceAtLeast(1L)
    val challengerRatio = (challengerScore.toFloat() / totalScore.toFloat()).coerceIn(0.1f, 0.9f)

    Scaffold(
        topBar = {
            BdTopBar(
                title = "⚡ Live PK Mic Battle",
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
            // Battle Arena Card (Two-sided Challenger vs Defender)
            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(ElectricBlue, NeonMagenta))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Countdown Timer Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33EF4444))
                                .border(1.dp, LiveRed, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(text = "⏱️", fontSize = 12.sp)
                            Text(
                                text = "BATTLE COUNTDOWN: 02:45",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Challenger (Blue) vs Defender (Red) Avatars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Challenger Side
                            FighterSpotlight(
                                user = pkState.challenger,
                                label = "CHALLENGER",
                                score = challengerScore,
                                accentColor = ElectricBlue,
                                supporters = pkState.challengerSupporters
                            )

                            // Center VS Animation
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(NeonMagenta, DeepViolet))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "VS",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "LIVE PK",
                                    color = BrightGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Right Defender Side
                            FighterSpotlight(
                                user = pkState.defender,
                                label = "DEFENDER",
                                score = defenderScore,
                                accentColor = NeonMagenta,
                                supporters = pkState.defenderSupporters
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Two-sided Progress Score Bar
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${String.format("%,d", challengerScore)} pts",
                                    color = ElectricBlue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${String.format("%,d", defenderScore)} pts",
                                    color = NeonMagenta,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Custom Dual Progress Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(DarkBackground)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(challengerRatio)
                                        .background(Brush.horizontalGradient(listOf(ElectricBlue, CyberCyan)))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f - challengerRatio)
                                        .background(Brush.horizontalGradient(listOf(NeonMagenta, HotPink)))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Quick Boost Button (Simulated Gift Cheer)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BdGradientButton(
                                text = "Cheer Blue (+500)",
                                onClick = { challengerScore += 500 },
                                brush = Brush.horizontalGradient(listOf(ElectricBlue, Color(0xFF2563EB))),
                                testTag = "cheer_blue_button",
                                modifier = Modifier.weight(1f)
                            )
                            BdGradientButton(
                                text = "Cheer Red (+500)",
                                onClick = { defenderScore += 500 },
                                brush = Brush.horizontalGradient(listOf(NeonMagenta, HotPink)),
                                testTag = "cheer_red_button",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // PK Reward Section (Medal, Frame, Theme, Virtual Coins)
            item {
                Text(
                    text = "🎁 Weekly Battle Victory Rewards",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PkRewardPill("🏅 Battle Medal", "PK Supreme", Modifier.weight(1f))
                    PkRewardPill("🖼️ Flame Frame", "30 Days VIP", Modifier.weight(1f))
                    PkRewardPill("🌌 Galaxy Theme", "Stage Skin", Modifier.weight(1f))
                    PkRewardPill("🪙 50,000 Coins", "Bonus Pot", Modifier.weight(1f))
                }
            }

            // Weekly PK Ranking & Top 10 Users
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    title = "Weekly PK Top 10 Gladiators",
                    actionLabel = "Leaderboard"
                )
            }

            itemsIndexed(MockRepository.onlineCreators) { index, creator ->
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pk_rank_row_$index")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RankMedalBadge(rank = index + 1)
                            BdAvatar(seed = creator.avatarSeed, name = creator.displayName, size = 40.dp)
                            Column {
                                Text(
                                    text = creator.displayName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Win Streak: ${12 - index} matches",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Text(
                            text = "${(150 - index * 12)}k PK pts",
                            color = BrightGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FighterSpotlight(
    user: User,
    label: String,
    score: Long,
    accentColor: Color,
    supporters: List<User>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        BdAvatar(
            seed = user.avatarSeed,
            name = user.displayName,
            size = 60.dp,
            hasFrame = true,
            frameColor = Brush.sweepGradient(listOf(accentColor, Color.White, accentColor))
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = user.displayName,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Supporter avatar bubbles
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            supporters.take(3).forEach { sup ->
                BdAvatar(seed = sup.avatarSeed, name = sup.displayName, size = 22.dp)
            }
        }
    }
}

@Composable
private fun PkRewardPill(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFB703)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = BrightGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextSecondary, fontSize = 9.sp)
        }
    }
}
