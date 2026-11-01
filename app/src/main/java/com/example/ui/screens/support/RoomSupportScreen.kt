package com.example.ui.screens.support

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
import com.example.model.SupportTarget
import com.example.model.WeeklyRecord
import com.example.ui.components.BdAvatar
import com.example.ui.components.BdTopBar
import com.example.ui.theme.*

@Composable
fun RoomSupportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val supportInfo = remember { MockRepository.roomSupportInfo }

    Scaffold(
        topBar = {
            BdTopBar(
                title = "Room Agency Support",
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
            // Room Overview Metric Card
            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(BrightGold, NeonViolet))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Room ID: ${supportInfo.roomId}",
                                color = BrightGold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33A855F7))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = supportInfo.currentWeek,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = supportInfo.roomLevel,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3 Key Stats (Visitors, Coins, Total Rewards)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SupportMetricPill("Visitors", String.format("%,d", supportInfo.visitors), "👥")
                            SupportMetricPill("Coins Generated", "${supportInfo.coinsGenerated / 1000000.0}M", "🪙")
                            SupportMetricPill("Total Rewards", String.format("%,d", supportInfo.totalRewards), "🎁")
                        }
                    }
                }
            }

            // Room Partners Section
            item {
                Text(
                    text = "🤝 Room Partners & Contribution",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(supportInfo.partners) { partner ->
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
                    modifier = Modifier.fillMaxWidth()
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
                            BdAvatar(seed = partner.avatarSeed, name = partner.name, size = 42.dp)
                            Column {
                                Text(
                                    text = partner.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Share: ${partner.sharePercent}% pool",
                                    color = NeonMagenta,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${String.format("%,d", partner.contributionCoins)} Coins",
                                color = BrightGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Contributed",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Target and Reward Milestone Table
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🎯 Target & Reward Milestone Table",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(supportInfo.targets) { target ->
                TargetMilestoneRow(target = target)
            }

            // Historical Weekly Records
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📜 Historical Weekly Records",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(supportInfo.historicalRecords) { record ->
                HistoricalRecordRow(record = record)
            }
        }
    }
}

@Composable
private fun SupportMetricPill(label: String, value: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$emoji $value", color = BrightGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun TargetMilestoneRow(target: SupportTarget) {
    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (target.isReached) MintEmerald else Color(0x33A855F7)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = target.targetLabel,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (target.isReached) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x3310B981))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "REACHED ✓", color = MintEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x3364748B))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "IN PROGRESS", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "👑 Owner: +${String.format("%,d", target.ownerReward)} Coins",
                    color = BrightGold,
                    fontSize = 11.sp
                )
                Text(
                    text = "🤝 Partner: +${String.format("%,d", target.partnerReward)} Coins",
                    color = NeonViolet,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun HistoricalRecordRow(record: WeeklyRecord) {
    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = record.weekName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "${String.format("%,d", record.visitors)} visitors", color = TextMuted, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${record.coinsGenerated / 1000000.0}M Coins", color = BrightGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "Claimed: ${String.format("%,d", record.rewardClaimed)}", color = MintEmerald, fontSize = 10.sp)
            }
        }
    }
}
