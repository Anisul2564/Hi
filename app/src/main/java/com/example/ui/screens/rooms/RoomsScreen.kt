package com.example.ui.screens.rooms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MockRepository
import com.example.data.room.RoomRepository
import com.example.data.wallet.WalletRepository
import com.example.model.RoomCategory
import com.example.model.User
import com.example.model.VoiceRoom
import com.example.ui.components.*
import com.example.ui.navigation.Screen
import com.example.ui.screens.home.RoomCard
import com.example.ui.theme.*

/**
 * Premium BD PARTY Explore Rooms Screen (Phase 1 Redesign).
 *
 * Features:
 * - Refined dark glass search input with real-time room filtering.
 * - Segmented tabs: "🔥 Popular", "⭐ Featured", "✨ New Stages".
 * - Category filter row with vibrant active pills.
 * - Live stage cards rendering real Firestore room telemetry (listeners, speakers, host role).
 * - Empty state with direct "Create Stage" action.
 * - Preserves room creation, joining, and navigation.
 */
@Composable
fun RoomsScreen(
    currentUser: User? = null,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val roomRepository = remember { RoomRepository(context) }
    val walletRepository = remember { WalletRepository() }

    val firestoreRooms by roomRepository.getLiveRoomsFlow().collectAsState(initial = emptyList())
    val userWallet by walletRepository.observeWallet(currentUser?.uid ?: "").collectAsState(initial = null)
    val displayCoins = userWallet?.coinBalance ?: currentUser?.coins ?: 128500L

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RoomCategory.ALL) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Popular, 1: Featured, 2: New

    val fallbackMockRooms = remember { MockRepository.getVoiceRooms() }

    val allRooms = remember(firestoreRooms) {
        if (firestoreRooms.isNotEmpty()) {
            firestoreRooms.map { fs ->
                val cat = try { RoomCategory.valueOf(fs.category) } catch (_: Exception) { RoomCategory.PARTY }
                VoiceRoom(
                    id = fs.roomId,
                    title = fs.title,
                    host = User(
                        id = fs.hostUid,
                        uid = fs.hostUid,
                        bdpId = fs.hostBdpId,
                        displayName = fs.hostUsername,
                        avatarSeed = (fs.hostUid.hashCode() % 6).let { if (it <= 0) 1 else it }
                    ),
                    category = cat,
                    listenersCount = fs.listenerCount,
                    speakersCount = (fs.participantCount - fs.listenerCount).coerceAtLeast(1),
                    seats = emptyList(),
                    isLive = fs.status == "LIVE",
                    isFeatured = true
                )
            }
        } else {
            fallbackMockRooms
        }
    }

    val filteredRooms = remember(allRooms, searchQuery, selectedCategory, selectedTab) {
        allRooms.filter { room ->
            val matchesQuery = searchQuery.isBlank() ||
                    room.title.contains(searchQuery, ignoreCase = true) ||
                    room.host.displayName.contains(searchQuery, ignoreCase = true) ||
                    room.host.id.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == RoomCategory.ALL || room.category == selectedCategory
            val matchesTab = when (selectedTab) {
                1 -> room.isFeatured
                else -> true
            }
            matchesQuery && matchesCategory && matchesTab
        }.let { list ->
            when (selectedTab) {
                0 -> list.sortedByDescending { it.listenersCount }
                2 -> list.reversed()
                else -> list
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Screen.Create) },
                containerColor = NeonMagenta,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("rooms_create_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Live Stage",
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Branded Top Bar
            item {
                BdTopBar(
                    title = "Explore Live Stages",
                    coins = displayCoins,
                    onCoinClick = { onNavigate(Screen.Wallet) },
                    onNotificationClick = { onNavigate(Screen.Messages) }
                )
            }

            // Refined Search Input
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search room title, host name, or ID...",
                                color = TextMuted,
                                fontSize = 12.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = NeonViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard,
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = Color(0x33A855F7),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rooms_search_input")
                    )
                }
            }

            // Discovery Tabs Segment (Popular, Featured, New Stages)
            item {
                val tabLabels = listOf("🔥 Popular", "⭐ Featured", "✨ New Stages")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceCard)
                        .border(0.5.dp, Color(0x22A855F7), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    tabLabels.forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonViolet else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp)
                                .testTag("rooms_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Category Filter Pills
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    items(RoomCategory.values()) { category ->
                        val isSelected = category == selectedCategory
                        val categoryIcon = when (category) {
                            RoomCategory.ALL -> "✨"
                            RoomCategory.PARTY -> "🎉"
                            RoomCategory.SINGING -> "🎤"
                            RoomCategory.GAMING -> "🎮"
                            RoomCategory.CHAT -> "💬"
                        }

                        Box(
                            modifier = Modifier
                                .testTag("rooms_category_${category.name.lowercase()}")
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0x33EC4899) else DarkSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonMagenta else Color(0x1FA855F7),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(text = categoryIcon, fontSize = 11.sp)
                                Text(
                                    text = category.displayName,
                                    color = if (isSelected) NeonMagenta else TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Live Discovery Section Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader(
                    title = "Live Discovery",
                    badgeText = "${filteredRooms.size} Stages",
                    actionLabel = "+ Create",
                    onActionClick = { onNavigate(Screen.Create) }
                )
            }

            // Empty state or Room cards
            if (filteredRooms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurfaceCard)
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22A855F7))
                                    .border(1.dp, NeonViolet.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🎙️", fontSize = 28.sp)
                            }
                            Text(
                                text = if (searchQuery.isNotBlank()) "No stages matching \"$searchQuery\"" else "No live stages in this category yet",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Start your own live party stage and invite friends to the mic!",
                                color = TextMuted,
                                fontSize = 11.5.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            BdGradientButton(
                                text = "Launch Stage Now",
                                onClick = { onNavigate(Screen.Create) },
                                brush = Brush.horizontalGradient(listOf(NeonMagenta, NeonViolet)),
                                testTag = "empty_rooms_create_button"
                            )
                        }
                    }
                }
            } else {
                items(filteredRooms, key = { it.id }) { room ->
                    RoomCard(
                        room = room,
                        onJoin = { onNavigate(Screen.VoiceRoom(room.id)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
