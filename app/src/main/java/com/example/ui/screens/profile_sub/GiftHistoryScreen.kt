package com.example.ui.screens.profile_sub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import coil.compose.AsyncImage
import com.example.data.gift.GiftRepository
import com.example.model.FirestoreGiftTransaction
import com.example.model.GiftTransactionStatus
import com.example.model.User
import com.example.ui.components.BdAvatar
import com.example.ui.theme.*

/**
 * Production-ready Gift History Screen for BD PARTY.
 *
 * Features:
 * - Two tabs: "Sent Gifts" and "Received Gifts".
 * - Shows only real transaction records streamed directly from Firestore.
 * - Displays gift name, icon, sender & receiver info, quantity, coin cost, room ID, timestamp, and status.
 * - Strictly labels transactions with their true status (PENDING, COMPLETED, FAILED, CANCELLED) — never labels COMPLETED unless verified in Firestore.
 * - Clean empty states and backend processing security disclaimers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftHistoryScreen(
    currentUser: User,
    giftRepository: GiftRepository = remember { GiftRepository() },
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableStateOf(0) } // 0: Sent, 1: Received

    val sentGifts by giftRepository.observeSentGifts(currentUser.uid).collectAsState(initial = emptyList())
    val receivedGifts by giftRepository.observeReceivedGifts(currentUser.uid).collectAsState(initial = emptyList())

    val totalSentCoins = remember(sentGifts) { sentGifts.sumOf { it.totalCoins } }
    val totalReceivedCoins = remember(receivedGifts) { receivedGifts.sumOf { it.totalCoins } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🎁 Gift History",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Firestore Virtual Gift Records",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("gift_history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.testTag("gift_history_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats Summary Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(BrightGold, NeonMagenta))
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("gift_history_summary_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Sent Gifts",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${sentGifts.size} items",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "🪙 ${String.format("%,d", totalSentCoins)}",
                                color = BrightGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(Color(0x33FFFFFF))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Received Gifts",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${receivedGifts.size} items",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "🪙 ${String.format("%,d", totalReceivedCoins)}",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Tabs: Sent Gifts / Received Gifts
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceCard)
                        .padding(4.dp)
                ) {
                    val tabTitles = listOf("Sent Gifts (${sentGifts.size})", "Received Gifts (${receivedGifts.size})")
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonViolet else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp)
                                .testTag("gift_history_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Notice about backend settlement
            item {
                Text(
                    text = "ℹ️ Note: Gift transfer requests submitted from voice rooms are recorded in Firestore. Real coin balance adjustments require trusted backend processing. Client applications submit requests in PENDING status.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }

            // Transactions list
            val currentList = if (selectedTab == 0) sentGifts else receivedGifts
            if (currentList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceCard)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🎁", fontSize = 40.sp)
                            Text(
                                text = if (selectedTab == 0) "No sent gifts recorded yet." else "No received gifts recorded yet.",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Send gifts to stage members in live voice rooms to see records here.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                items(currentList, key = { it.transactionId }) { txn ->
                    GiftTransactionItem(
                        transaction = txn,
                        isSentTab = selectedTab == 0,
                        defaultGifts = giftRepository.defaultGifts()
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Individual Gift Transaction Card displaying real Firestore data.
 */
@Composable
fun GiftTransactionItem(
    transaction: FirestoreGiftTransaction,
    isSentTab: Boolean,
    defaultGifts: List<com.example.model.FirestoreGift>
) {
    val giftIcon = remember(transaction.giftId) {
        defaultGifts.find { it.giftId == transaction.giftId }?.iconUrl ?: "🎁"
    }

    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gift_transaction_${transaction.transactionId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Gift Icon, Name, and Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x33A855F7))
                            .border(1.dp, BrightGold.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (giftIcon.startsWith("http://") || giftIcon.startsWith("https://")) {
                            AsyncImage(
                                model = giftIcon,
                                contentDescription = transaction.giftName,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Text(text = giftIcon.ifBlank { "🎁" }, fontSize = 22.sp)
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = transaction.giftName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x33EC4899))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "x${transaction.quantity}",
                                    color = NeonMagenta,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "🪙 ${transaction.coinPrice} each • Total: 🪙 ${String.format("%,d", transaction.totalCoins)}",
                            color = BrightGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Transaction Status Badge
                GiftStatusBadge(status = transaction.status)
            }

            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)

            // Middle: Sender & Receiver Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSentTab) {
                    Column {
                        Text(
                            text = "Receiver:",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${transaction.receiverUsername} (ID: ${transaction.receiverBdpId.ifBlank { "N/A" }})",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Column {
                        Text(
                            text = "Sender:",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${transaction.senderUsername} (ID: ${transaction.senderBdpId.ifBlank { "N/A" }})",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (transaction.roomId.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Room ID:",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "#${transaction.roomId.take(8)}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom: Date and Transaction ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Txn: ${transaction.transactionId.take(12)}...",
                    color = TextMuted,
                    fontSize = 9.sp
                )
                Text(
                    text = transaction.createdAt.replace("T", " ").replace("Z", ""),
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Renders the accurate status badge for a gift transaction without false COMPLETED states.
 */
@Composable
fun GiftStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        GiftTransactionStatus.COMPLETED.name -> Triple(
            Color(0x3310B981),
            Color(0xFF10B981),
            "COMPLETED"
        )
        GiftTransactionStatus.PENDING.name -> Triple(
            Color(0x33F59E0B),
            BrightGold,
            "PENDING VERIFICATION"
        )
        GiftTransactionStatus.FAILED.name -> Triple(
            Color(0x33EF4444),
            Color(0xFFEF4444),
            "FAILED"
        )
        GiftTransactionStatus.CANCELLED.name -> Triple(
            Color(0x339CA3AF),
            Color(0xFF9CA3AF),
            "CANCELLED"
        )
        else -> Triple(
            Color(0x33F59E0B),
            BrightGold,
            status
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.5.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
