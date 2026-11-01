package com.example.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.ui.theme.*

/**
 * Premium BD PARTY Home Screen (Phase 1 Redesign).
 *
 * Visual Highlights:
 * - Refined international luxury audio social aesthetic.
 * - Deep midnight navy backgrounds with restrained glow accents.
 * - Live stage hero card with live status, listener telemetry, host halo, and instant entry CTA.
 * - Live host carousel with pulsing live rings and verified creator roles.
 * - Compact high-energy entertainment hub (Fruit Game, PK Battle, 7-Day Bonus, Rankings).
 * - Segmented category filters and high-density recommended room cards.
 */
@Composable
fun HomeScreen(
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

    var selectedCategory by remember { mutableStateOf(RoomCategory.ALL) }
    val fallbackMockRooms = remember { MockRepository.getVoiceRooms() }

    // Map real Firestore rooms to VoiceRoom display model if available, otherwise fallback
    val rooms = remember(firestoreRooms) {
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

    val featuredRoom = remember(rooms) { rooms.firstOrNull { it.isFeatured } ?: rooms.first() }
    val onlineCreators = remember { MockRepository.onlineCreators }

    val filteredRooms = remember(selectedCategory, rooms) {
        if (selectedCategory == RoomCategory.ALL) rooms
        else rooms.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        // Branded Header
        item {
            BdTopBar(
                coins = displayCoins,
                onCoinClick = { onNavigate(Screen.Wallet) },
                onSearchClick = { onNavigate(Screen.Rooms) },
                onNotificationClick = { onNavigate(Screen.Messages) }
            )
        }

        // Featured Live Stage Card
        item {
            FeaturedLiveStage(
                room = featuredRoom,
                onEnterStage = { onNavigate(Screen.VoiceRoom(featuredRoom.id)) }
            )
        }

        // Live Hosts & Stars Carousel
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(
                title = "Live Hosts & Stars",
                badgeText = "${onlineCreators.size} LIVE",
                actionLabel = "Explore All",
                onActionClick = { onNavigate(Screen.Rooms) }
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            ) {
                items(onlineCreators) { creator ->
                    CreatorAvatarItem(
                        creator = creator,
                        onClick = { onNavigate(Screen.VoiceRoom(featuredRoom.id)) }
                    )
                }
            }
        }

        // Entertainment Activity Hub (Fruit Game, Mic Battle, Daily Rewards, Rankings)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            EntertainmentHubGrid(onNavigate = onNavigate)
        }

        // Category Pills
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(
                title = "Stage Categories",
                actionLabel = null
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
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
                            .testTag("category_chip_${category.name.lowercase()}")
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) NeonViolet else DarkSurfaceCard)
                            .border(
                                1.dp,
                                if (isSelected) BrightGold.copy(alpha = 0.8f) else Color(0x26A855F7),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = categoryIcon, fontSize = 11.sp)
                            Text(
                                text = category.displayName,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Trending Live Rooms Section
        item {
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader(
                title = "Trending Live Rooms",
                badgeText = "${filteredRooms.size} Active",
                actionLabel = "View More",
                onActionClick = { onNavigate(Screen.Rooms) }
            )
        }

        items(filteredRooms, key = { it.id }) { room ->
            RoomCard(
                room = room,
                onJoin = { onNavigate(Screen.VoiceRoom(room.id)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * High-end Featured Live Stage Card.
 */
@Composable
private fun FeaturedLiveStage(
    room: VoiceRoom,
    onEnterStage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(16.dp, shape = RoundedCornerShape(24.dp), spotColor = NeonViolet)
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        BrightGold.copy(alpha = 0.8f),
                        NeonMagenta.copy(alpha = 0.7f),
                        NeonViolet.copy(alpha = 0.6f),
                        ElectricCyan.copy(alpha = 0.4f)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        // Attractive room cover image
        Image(
            painter = painterResource(id = BdAssets.getRoomCoverDrawable(room.category, room.id, room.title)),
            contentDescription = room.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Dark gradient scrim for strong typography contrast
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xD91E1035),
                            Color(0xF0130B24),
                            Color(0xFA090514)
                        )
                    )
                )
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // Top badges row: Live Indicator + Category + Listener count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BdLiveIndicator(label = "FEATURED STAGE", showWave = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33A855F7))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = room.category.displayName,
                            color = NeonMagenta,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x66000000))
                        .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = "🎧", fontSize = 11.sp)
                    Text(
                        text = String.format("%,d", room.listenersCount),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stage Title
            Text(
                text = room.title,
                color = Color.White,
                fontSize = 17.5.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 23.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Host identity and Enter CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BdAvatar(
                        seed = room.host.avatarSeed,
                        name = room.host.displayName,
                        size = 46.dp,
                        hasFrame = true,
                        isLive = true
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = room.host.displayName,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            RoleBadge(role = room.host.role)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Host ID: ${room.host.id}",
                                color = TextMuted,
                                fontSize = 10.5.sp
                            )
                            Text(text = "•", color = TextMuted, fontSize = 9.sp)
                            Text(
                                text = "🎙️ ${room.speakersCount} speaking",
                                color = BrightGold,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                BdGradientButton(
                    text = "Enter Stage",
                    onClick = onEnterStage,
                    brush = Brush.horizontalGradient(listOf(NeonMagenta, NeonViolet)),
                    testTag = "enter_featured_stage_button"
                )
            }
        }
    }
}

/**
 * Creator Avatar with live ring and badge.
 */
@Composable
private fun CreatorAvatarItem(
    creator: User,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag("creator_card_${creator.id}")
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(2.dp)
    ) {
        BdAvatar(
            seed = creator.avatarSeed,
            name = creator.displayName,
            size = 56.dp,
            hasFrame = true,
            isLive = creator.isLive,
            frameColor = Brush.sweepGradient(
                listOf(BrightGold, NeonMagenta, NeonViolet, ElectricCyan, BrightGold)
            )
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = creator.displayName,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 66.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        RoleBadge(role = creator.role)
    }
}

/**
 * Refined 4-Action Entertainment Grid.
 */
@Composable
private fun EntertainmentHubGrid(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Fruit Game Button
        QuickActivityCard(
            icon = "🎰",
            title = "Fruit Game",
            badge = "45x Win",
            brush = GameGreenGradient,
            onClick = { onNavigate(Screen.FruitGame) },
            testTag = "home_fruit_game_banner",
            modifier = Modifier.weight(1f)
        )

        // Mic Battle PK Button
        QuickActivityCard(
            icon = "⚡",
            title = "Mic Battle",
            badge = "Live PK",
            brush = PKBattleGradient,
            onClick = { onNavigate(Screen.MicBattle) },
            testTag = "home_mic_battle_banner",
            modifier = Modifier.weight(1f)
        )

        // Daily Rewards Button
        QuickActivityCard(
            icon = "🎁",
            title = "7-Day Bonus",
            badge = "Claim 2k",
            brush = RewardsGoldGradient,
            onClick = { onNavigate(Screen.DailyRewards) },
            testTag = "home_daily_rewards_banner",
            modifier = Modifier.weight(1f)
        )

        // Leaderboard Ranking Button
        QuickActivityCard(
            icon = "🏆",
            title = "Rankings",
            badge = "Top Stars",
            brush = RankingsPurpleGradient,
            onClick = { onNavigate(Screen.Ranking) },
            testTag = "home_ranking_banner",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActivityCard(
    icon: String,
    title: String,
    badge: String,
    brush: Brush,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(0.7.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .padding(vertical = 10.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(text = icon, fontSize = 20.sp)
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33000000))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * High-Density, Luxury Live Room Card.
 */
@Composable
fun RoomCard(
    room: VoiceRoom,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("room_card_${room.id}")
            .clip(RoundedCornerShape(18.dp))
            .clickable { onJoin() }
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(Color(0x33A855F7), Color(0x15EC4899), Color(0x0A06B6D4))
                ),
                RoundedCornerShape(18.dp)
            ),
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Thumbnail: Rounded Room Cover Image + Host Avatar
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x33A855F7), RoundedCornerShape(16.dp))
            ) {
                // Real room cover image
                Image(
                    painter = painterResource(id = BdAssets.getRoomCoverDrawable(room.category, room.id, room.title)),
                    contentDescription = room.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x1A000000), Color(0xB3000000))
                            )
                        )
                )

                // Host circular avatar overlay with live ring
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                ) {
                    BdAvatar(
                        seed = room.host.avatarSeed,
                        name = room.host.displayName,
                        size = 28.dp,
                        hasFrame = false,
                        isLive = room.isLive
                    )
                }
            }

            // Middle Room Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Top Category & Listener Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x2EEC4899))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = room.category.displayName,
                            color = NeonMagenta,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(text = "🎧", fontSize = 10.sp)
                        Text(
                            text = String.format("%,d", room.listenersCount),
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(text = "•", color = TextMuted, fontSize = 8.sp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(text = "🎙️", fontSize = 10.sp)
                        Text(
                            text = "${room.speakersCount}",
                            color = BrightGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Room Title
                Text(
                    text = room.title,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Host row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = room.host.displayName,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    RoleBadge(role = room.host.role)
                }
            }

            // Right "Join" Action Pill
            Surface(
                color = Color(0x33A855F7),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Join",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Join",
                        tint = NeonViolet,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
