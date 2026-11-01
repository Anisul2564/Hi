package com.example.ui.screens.ranking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MockRepository
import com.example.model.RankingCategory
import com.example.model.RankingEntry
import com.example.model.RankingTimeframe
import com.example.ui.components.BdAvatar
import com.example.ui.components.BdTopBar
import com.example.ui.components.LevelBadge
import com.example.ui.components.RankMedalBadge
import com.example.ui.theme.*

@Composable
fun RankingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedCategory by remember { mutableStateOf(RankingCategory.HONOR) }
    var selectedTimeframe by remember { mutableStateOf(RankingTimeframe.DAY) }
    var showRulesModal by remember { mutableStateOf(false) }

    val entries = remember { MockRepository.rankingEntries }
    val top1 = entries.getOrNull(0)
    val top2 = entries.getOrNull(1)
    val top3 = entries.getOrNull(2)
    val remainingList = entries.drop(3)

    Scaffold(
        topBar = {
            BdTopBar(
                title = "Hall of Fame Rankings",
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
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Category Tabs (Honor, Charm, Room)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RankingCategory.values().forEach { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ranking_category_${cat.name.lowercase()}")
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) Brush.horizontalGradient(listOf(BrightGold, LuxuryGold))
                                    else Brush.horizontalGradient(listOf(DarkSurfaceCard, DarkSurfaceCard))
                                )
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (cat) {
                                    RankingCategory.HONOR -> "👑 Honor"
                                    RankingCategory.CHARM -> "💖 Charm"
                                    RankingCategory.ROOM -> "🏛️ Room"
                                },
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Timeframe Selector & Rules Action
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RankingTimeframe.values().forEach { tf ->
                            val isSelected = tf == selectedTimeframe
                            Box(
                                modifier = Modifier
                                    .testTag("ranking_timeframe_${tf.name.lowercase()}")
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NeonViolet else DarkSurfaceCard)
                                    .clickable { selectedTimeframe = tf }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tf.displayName,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Ranking Rules Trigger Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .testTag("ranking_rules_button")
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showRulesModal = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Rules",
                            tint = BrightGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Rules",
                            color = BrightGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Podium for Top 1, Top 2, Top 3
            item {
                Spacer(modifier = Modifier.height(10.dp))
                PodiumSection(top1 = top1, top2 = top2, top3 = top3)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Top 4 through 10 List
            itemsIndexed(remainingList) { index, entry ->
                RankingRowItem(
                    entry = entry,
                    rankNumber = index + 4,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }

    // Ranking Rules Modal Dialog
    if (showRulesModal) {
        AlertDialog(
            onDismissRequest = { showRulesModal = false },
            containerColor = DarkSurfaceCard,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "📜", fontSize = 22.sp)
                    Text(
                        text = "Ranking Rules",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RuleExplanationCard(
                        title = "👑 Honor Ranking",
                        explanation = "Total virtual coins spent on gifts sent across BD PARTY rooms. Resets daily, weekly, and monthly."
                    )
                    RuleExplanationCard(
                        title = "💖 Charm Ranking",
                        explanation = "Total virtual diamonds and gift rewards received by the host/creator from supporters."
                    )
                    RuleExplanationCard(
                        title = "🏛️ Room Ranking",
                        explanation = "Total virtual coins spent on gifts inside this specific room stage by all visitors combined."
                    )
                    Text(
                        text = "Note: BD PARTY rankings represent in-app virtual achievements. Rewards are in-game virtual perks only.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showRulesModal = false },
                    modifier = Modifier.testTag("close_ranking_rules_button")
                ) {
                    Text("Got It", color = BrightGold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun RuleExplanationCard(title: String, explanation: String) {
    Column {
        Text(text = title, color = BrightGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = explanation, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun PodiumSection(
    top1: RankingEntry?,
    top2: RankingEntry?,
    top3: RankingEntry?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Top 2 (Silver)
        if (top2 != null) {
            PodiumPedestal(
                entry = top2,
                rank = 2,
                crown = "🥈",
                pedestalHeight = 110.dp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.weight(1f)
            )
        }

        // Top 1 (Gold - Center & Elevated)
        if (top1 != null) {
            PodiumPedestal(
                entry = top1,
                rank = 1,
                crown = "👑",
                pedestalHeight = 140.dp,
                color = BrightGold,
                modifier = Modifier.weight(1.15f)
            )
        }

        // Top 3 (Bronze)
        if (top3 != null) {
            PodiumPedestal(
                entry = top3,
                rank = 3,
                crown = "🥉",
                pedestalHeight = 95.dp,
                color = Color(0xFFCD7F32),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PodiumPedestal(
    entry: RankingEntry,
    rank: Int,
    crown: String,
    pedestalHeight: androidx.compose.ui.unit.Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = crown, fontSize = if (rank == 1) 26.sp else 20.sp)
        Spacer(modifier = Modifier.height(2.dp))
        BdAvatar(
            seed = entry.user.avatarSeed,
            name = entry.user.displayName,
            size = if (rank == 1) 56.dp else 46.dp,
            hasFrame = true,
            frameColor = Brush.sweepGradient(listOf(color, NeonViolet, color))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.user.displayName,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${String.format("%,d", entry.score / 1000)}K",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Pedestal Column
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.35f), DarkSurfaceCard)
                    )
                )
                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "#$rank",
                    color = color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.rewardTitle,
                    color = TextSecondary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RankingRowItem(
    entry: RankingEntry,
    rankNumber: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ranking_item_$rankNumber")
            .clip(RoundedCornerShape(14.dp))
            .border(0.5.dp, Color(0x33A855F7), RoundedCornerShape(14.dp)),
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(14.dp)
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                RankMedalBadge(rank = rankNumber)

                BdAvatar(
                    seed = entry.user.avatarSeed,
                    name = entry.user.displayName,
                    size = 42.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.user.displayName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LevelBadge(level = entry.user.level)
                    }
                    Text(
                        text = entry.rewardTitle,
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%,d", entry.score),
                    color = BrightGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Score",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
