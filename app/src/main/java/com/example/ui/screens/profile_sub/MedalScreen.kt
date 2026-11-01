package com.example.ui.screens.profile_sub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BdTopBar
import com.example.ui.theme.*

data class MedalItem(
    val id: String,
    val title: String,
    val icon: String,
    val isUnlocked: Boolean,
    val condition: String
)

@Composable
fun MedalScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val medals = listOf(
        MedalItem("m1", "Star Host", "🎙️", true, "Hosted 100+ room hours"),
        MedalItem("m2", "PK Gladiator", "⚡", true, "Won 25 live mic duels"),
        MedalItem("m3", "Galaxy Donor", "🌌", true, "Sent 500k+ virtual gift coins"),
        MedalItem("m4", "Prestige SVIP", "👑", true, "Reached SVIP 3 Sovereign"),
        MedalItem("m5", "Festival Champion", "🎪", true, "Top 3 in Carnival event"),
        MedalItem("m6", "CP Love Pledge", "💍", true, "Achieved 10,000 CP loyalty points"),
        MedalItem("m7", "Treasure Master", "🎰", true, "Achieved 45x win in Fruit Game"),
        MedalItem("m8", "Immortal Sovereign", "🦅", false, "Reach SVIP 5 Emperor"),
        MedalItem("m9", "Legendary Idol", "✨", false, "Accumulate 100,000 stage followers")
    )

    Scaffold(
        topBar = {
            BdTopBar(
                title = "Honor & Achievement Medals",
                onBackClick = onBack,
                showActions = false
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            items(medals) { medal ->
                Surface(
                    color = if (medal.isUnlocked) DarkSurfaceCard else Color(0x33150D27),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (medal.isUnlocked) BrightGold else Color(0x22FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medal_${medal.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (medal.isUnlocked) Color(0x33FFB703) else Color(0x22FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = medal.icon, fontSize = 28.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = medal.title,
                            color = if (medal.isUnlocked) Color.White else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = medal.condition,
                            color = TextMuted,
                            fontSize = 9.sp,
                            maxLines = 2,
                            lineHeight = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
