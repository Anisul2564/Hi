package com.example.ui.screens.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.navigation.Screen
import com.example.ui.theme.*

data class CreateAction(
    val title: String,
    val subtitle: String,
    val icon: String,
    val gradient: Brush,
    val flowTitle: String,
    val testTag: String
)

@Composable
fun CreateScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val createActions = listOf(
        CreateAction(
            title = "Create Voice Room",
            subtitle = "Host live stages, invite speakers & engage listeners",
            icon = "🎙️",
            gradient = Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFC026D3))),
            flowTitle = "Voice Room Setup",
            testTag = "create_voice_room_action"
        ),
        CreateAction(
            title = "Create Community Event",
            subtitle = "Launch room parties, festivals & gifting tournaments",
            icon = "🎪",
            gradient = Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFDC2626))),
            flowTitle = "Community Event Setup",
            testTag = "create_event_action"
        ),
        CreateAction(
            title = "Start PK / Mic Battle",
            subtitle = "Challenge rival creators to real-time cheering duels",
            icon = "⚡",
            gradient = Brush.horizontalGradient(listOf(Color(0xFFDB2777), Color(0xFF9333EA))),
            flowTitle = "Mic Battle Challenge",
            testTag = "create_pk_battle_action"
        ),
        CreateAction(
            title = "Create Announcement",
            subtitle = "Broadcast news, schedule notices & guild updates",
            icon = "📢",
            gradient = Brush.horizontalGradient(listOf(Color(0xFF0D9488), Color(0xFF0284C7))),
            flowTitle = "Server Announcement",
            testTag = "create_announcement_action"
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            BdTopBar(
                title = "Creator Studio",
                showActions = false
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Launch Your Party 🎉",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select what you want to create on BD PARTY today",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        items(createActions.size) { index ->
            val action = createActions[index]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(action.testTag)
                    .clip(RoundedCornerShape(20.dp))
                    .background(action.gradient)
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .clickable {
                        onNavigate(Screen.CreateFlow(action.flowTitle))
                    }
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = action.icon, fontSize = 28.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = action.subtitle,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Host tips card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = DarkSurfaceCard,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Host Guidelines",
                        color = BrightGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Keep stages respectful and community-friendly.\n• Active rooms receive automated discovery boost.\n• Verify your Official Host badge in Profile to unlock 20-seat stages.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
