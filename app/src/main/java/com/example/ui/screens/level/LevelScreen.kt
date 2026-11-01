package com.example.ui.screens.level

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.LevelTier
import com.example.ui.components.BdAvatar
import com.example.ui.components.BdTopBar
import com.example.ui.components.LevelBadge
import com.example.ui.theme.*

@Composable
fun LevelScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val user = remember { MockRepository.currentUser }
    val levelTiers = remember { MockRepository.levelTiers }
    val currentXp = 74500L
    val nextLevelXp = 100000L
    val progress = (currentXp.toFloat() / nextLevelXp.toFloat()).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            BdTopBar(
                title = "Level System (1-100)",
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
            // Header: Current Level & XP progress
            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(BrightGold, NeonMagenta))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BdAvatar(
                            seed = user.avatarSeed,
                            name = user.displayName,
                            size = 76.dp,
                            hasFrame = true,
                            frameColor = Brush.sweepGradient(listOf(BrightGold, LuxuryGold, Color(0xFFF59E0B)))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = user.displayName,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LevelBadge(level = user.level)

                        Spacer(modifier = Modifier.height(16.dp))

                        // XP Progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current XP: ${String.format("%,d", currentXp)}",
                                color = BrightGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Next: ${String.format("%,d", nextLevelXp)} XP",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            color = BrightGold,
                            trackColor = Color(0x33FFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .testTag("level_xp_progress_bar")
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${(progress * 100).toInt()}% completed to Level ${user.level + 1}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Upgrade Rewards Overview
            item {
                Text(
                    text = "🌟 Level 49 Upgrade Perks",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RewardPreviewPill("🪙 +2,500 Coins", "Daily Boost", Modifier.weight(1f))
                    RewardPreviewPill("🎨 Sparkle Name", "Chat Effect", Modifier.weight(1f))
                    RewardPreviewPill("💫 Starlight Wave", "Mic Animation", Modifier.weight(1f))
                }
            }

            // Level 1-100 Structure
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🏆 Level 1 - 100 Progression Tiers",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(levelTiers) { tier ->
                LevelTierCard(tier = tier)
            }
        }
    }
}

@Composable
private fun RewardPreviewPill(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = BrightGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun LevelTierCard(
    tier: LevelTier,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("level_tier_${tier.level}")
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                if (tier.isUnlocked) NeonViolet else Color(0x22FFFFFF),
                RoundedCornerShape(18.dp)
            ),
        color = if (tier.isUnlocked) DarkSurfaceCard else Color(0x66150D27),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tier.tierName,
                        color = if (tier.isUnlocked) Color.White else TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (tier.isUnlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x3310B981))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "UNLOCKED", color = MintEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x3364748B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "🔒 LOCKED", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Requires: ${String.format("%,d", tier.xpRequired)} XP",
                    color = BrightGold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rewards: " + tier.rewards.joinToString(", "),
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
