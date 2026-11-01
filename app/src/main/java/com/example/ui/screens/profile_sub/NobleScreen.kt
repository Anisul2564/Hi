package com.example.ui.screens.profile_sub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BdTopBar
import com.example.ui.theme.*

data class NobleTier(
    val title: String,
    val icon: String,
    val badge: String,
    val perks: List<String>,
    val isCurrent: Boolean
)

@Composable
fun NobleScreen(
    title: String = "Noble Aristocracy",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val nobleTiers = listOf(
        NobleTier("Knight", "🗡️", "Tier 1", listOf("Silver Entrance Name", "Knight Badge", "+5% Room EXP"), false),
        NobleTier("Baron", "🛡️", "Tier 2", listOf("Baron Frame", "Custom Chat Color", "+10% Room EXP"), true),
        NobleTier("Viscount", "⚔️", "Tier 3", listOf("Flying Chat Banner", "Anti-Kick in Stage", "+15% Room EXP"), false),
        NobleTier("Earl", "🦅", "Tier 4", listOf("Chariot Vehicle", "Gold Badge", "+25% Room EXP"), false),
        NobleTier("Duke", "👑", "Tier 5", listOf("Castle Entrance Fanfare", "Duke Medal", "+40% Room EXP"), false),
        NobleTier("King", "⚡", "Tier 6", listOf("Imperial Server Broadcast", "Supreme Crown", "Zero Stage Delay"), false)
    )

    Scaffold(
        topBar = {
            BdTopBar(
                title = title,
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
            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(BrightGold, NeonMagenta))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "👑", fontSize = 38.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Current Status: Baron",
                            color = BrightGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Noble rank grants permanent recognition across all stages",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            items(nobleTiers) { tier ->
                Surface(
                    color = if (tier.isCurrent) Color(0x33A855F7) else DarkSurfaceCard,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (tier.isCurrent) 1.5.dp else 0.5.dp,
                        if (tier.isCurrent) BrightGold else Color(0x33A855F7)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("noble_tier_${tier.title.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = tier.icon, fontSize = 32.sp)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = tier.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x33FFB703))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tier.badge,
                                            color = BrightGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tier.perks.joinToString(" • "),
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (tier.isCurrent) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrightGold)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Active",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
