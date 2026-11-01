package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.theme.*

enum class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val screen: Screen,
    val tag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, Screen.Home, "nav_home"),
    ROOMS("Rooms", Icons.Filled.Explore, Icons.Outlined.Explore, Screen.Rooms, "nav_rooms"),
    CREATE("Create", Icons.Filled.Add, Icons.Filled.Add, Screen.Create, "nav_create"),
    MESSAGES("Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, Screen.Messages, "nav_messages"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, Screen.Profile, "nav_profile")
}

/**
 * Premium Edge-to-Edge Bottom Navigation Bar.
 */
@Composable
fun BdBottomNav(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = DarkSurface,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            Brush.horizontalGradient(
                listOf(Color(0x10A855F7), Color(0x33A855F7), Color(0x10EC4899))
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem.values().forEach { item ->
                val isSelected = when (item) {
                    NavItem.HOME -> currentScreen is Screen.Home
                    NavItem.ROOMS -> currentScreen is Screen.Rooms
                    NavItem.CREATE -> currentScreen is Screen.Create
                    NavItem.MESSAGES -> currentScreen is Screen.Messages
                    NavItem.PROFILE -> currentScreen is Screen.Profile
                }

                if (item == NavItem.CREATE) {
                    // Elevated Center "Create Stage" Action Button
                    Box(
                        modifier = Modifier
                            .testTag(item.tag)
                            .size(52.dp)
                            .offset(y = (-6).dp)
                            .shadow(14.dp, CircleShape, spotColor = NeonMagenta)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(NeonMagenta, NeonViolet, BrightGold)
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .clickable { onNavigate(item.screen) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Room",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    val tintColor by animateColorAsState(
                        targetValue = if (isSelected) NeonViolet else TextMuted,
                        animationSpec = tween(200),
                        label = "nav_tint"
                    )

                    Column(
                        modifier = Modifier
                            .testTag(item.tag)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigate(item.screen) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = tintColor,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                        Text(
                            text = item.title,
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(NeonMagenta)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}
