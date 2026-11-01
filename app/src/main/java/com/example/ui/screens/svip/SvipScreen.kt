package com.example.ui.screens.svip

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.model.SvipTier
import com.example.ui.components.BdGradientButton
import com.example.ui.components.BdTopBar
import com.example.ui.components.SvipBadge
import com.example.ui.theme.*

@Composable
fun SvipScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val svipList = remember { MockRepository.svipTiers }
    var selectedLevelIndex by remember { mutableStateOf(2) } // SVIP 3 default
    val currentSelected = svipList[selectedLevelIndex]

    Scaffold(
        topBar = {
            BdTopBar(
                title = "SVIP Prestige Club",
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
            // SVIP Level Selector Tabs (SVIP 1 through SVIP 5 + expansion)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(svipList.indices.toList()) { index ->
                        val tier = svipList[index]
                        val isSelected = selectedLevelIndex == index
                        Box(
                            modifier = Modifier
                                .testTag("svip_tier_tab_${tier.level}")
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Brush.horizontalGradient(listOf(BrightGold, LuxuryGold))
                                    else Brush.horizontalGradient(listOf(DarkSurfaceCard, DarkSurfaceCard))
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White else Color(0x33A855F7),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedLevelIndex = index }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SVIP ${tier.level}",
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Main SVIP Card Display
            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(BrightGold, NeonMagenta, NeonViolet))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Season information tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33FFB703))
                                .border(0.5.dp, BrightGold, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🗓️ ${currentSelected.seasonInfo}",
                                color = BrightGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Emblem preview
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(BrightGold, NeonMagenta, LuxuryGold, BrightGold)
                                    )
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(DarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (currentSelected.level) {
                                        5 -> "👑"
                                        4 -> "🐉"
                                        3 -> "🦅"
                                        2 -> "⚔️"
                                        else -> "🛡️"
                                    },
                                    fontSize = 42.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentSelected.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        SvipBadge(svipLevel = currentSelected.level)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress toward next level
                        val progress = (currentSelected.currentXp.toFloat() / currentSelected.requiredXp.toFloat()).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SVIP Points: ${String.format("%,d", currentSelected.currentXp)}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Goal: ${String.format("%,d", currentSelected.requiredXp)}",
                                color = BrightGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            color = BrightGold,
                            trackColor = Color(0x33FFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual Previews: SVIP Label, SVIP Medal, SVIP Frame
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PreviewItemPill("SVIP Label", "🏷️ Gold Text")
                            PreviewItemPill("SVIP Medal", "🎖️ Sovereign")
                            PreviewItemPill("SVIP Frame", "🖼️ Halo Glow")
                        }
                    }
                }
            }

            // Exclusive Privileges Section
            item {
                Text(
                    text = "👑 SVIP ${currentSelected.level} Exclusive Privileges",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(currentSelected.perks) { perk ->
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "✨", fontSize = 18.sp)
                        Text(
                            text = perk,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Recharge / Upgrade Area
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0x33FFB703),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrightGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚡ Upgrade to SVIP ${currentSelected.level}",
                            color = BrightGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Spend coins in live rooms or support hosts to gain SVIP prestige points",
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        BdGradientButton(
                            text = "Upgrade SVIP Prestige",
                            onClick = { /* Simulated in-app action */ },
                            brush = Brush.horizontalGradient(listOf(BrightGold, LuxuryGold, Color(0xFFD97706))),
                            testTag = "upgrade_svip_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewItemPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33A855F7)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = value.take(2), fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = TextMuted, fontSize = 10.sp)
    }
}
