package com.example.ui.screens.room

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gift.GiftRepository
import com.example.model.FirestoreGift
import com.example.model.FirestoreGiftTransaction
import com.example.model.RoomParticipant
import com.example.model.User
import com.example.ui.components.BdAvatar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Professional Gift Center bottom sheet for BD PARTY Voice Rooms.
 * Displays:
 * - Real wallet coin balance
 * - Real participant receiver selection with username & BDP ID (strictly rejects self and invalid receivers)
 * - Gift catalog with category filter tabs (All, Popular, Luxury, Romantic)
 * - Safe quantity selector with preset chips and custom quantity limits (1..9999)
 * - Real-time gift send request flow with backend-ready PENDING settlement notice
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftCenterSheet(
    roomId: String,
    currentUser: User,
    coinBalance: Long,
    participants: List<RoomParticipant>,
    giftRepository: GiftRepository,
    onDismiss: () -> Unit,
    onOpenHistory: (() -> Unit)? = null,
    onGiftSent: ((FirestoreGiftTransaction) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Filter eligible receivers: strictly anyone in room except current authenticated user
    val eligibleReceivers = remember(participants, currentUser.uid) {
        participants.filter { it.uid.isNotBlank() && it.uid != currentUser.uid }
    }

    var selectedReceiver by remember(eligibleReceivers) {
        mutableStateOf(eligibleReceivers.firstOrNull())
    }

    val catalogGifts by giftRepository.observeGiftCatalog().collectAsState(initial = giftRepository.defaultGifts())

    // Category Tabs: All, Popular, Luxury, Romantic
    val categories = listOf("All", "Popular", "Luxury", "Romantic")
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredGifts = remember(catalogGifts, selectedCategory) {
        when (selectedCategory) {
            "Popular" -> catalogGifts.filter { it.coinPrice in 10..500 }
            "Luxury" -> catalogGifts.filter { it.coinPrice >= 1000 }
            "Romantic" -> catalogGifts.filter {
                it.giftId in listOf("g_rose", "g_heart", "g_diamond") ||
                        it.name.contains("Rose", ignoreCase = true) ||
                        it.name.contains("Heart", ignoreCase = true)
            }
            else -> catalogGifts
        }
    }

    var selectedGift by remember(filteredGifts) {
        mutableStateOf(filteredGifts.firstOrNull() ?: catalogGifts.firstOrNull())
    }

    var selectedQuantity by remember { mutableStateOf(1) }
    var showCustomQuantityDialog by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    val presetQuantities = listOf(1, 10, 66, 99, 520, 1314)
    val totalCoins = (selectedGift?.coinPrice ?: 0L) * selectedQuantity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x66FFFFFF))
            )
        },
        modifier = modifier.testTag("gift_center_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Header: Balance, Title, and Gift History link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎁 Gift Center",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (onOpenHistory != null) {
                        Surface(
                            color = Color(0x22FFFFFF),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clickable {
                                    onDismiss()
                                    onOpenHistory()
                                }
                                .testTag("gift_sheet_history_btn")
                        ) {
                            Text(
                                text = "📜 History",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33A855F7))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "🪙", fontSize = 12.sp)
                    Text(
                        text = String.format("%,d", coinBalance),
                        color = BrightGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Receiver Selector Header & List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Send To Stage Member:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (eligibleReceivers.isNotEmpty()) {
                    Text(
                        text = "${eligibleReceivers.size} Available",
                        color = BrightGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            if (eligibleReceivers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceCard)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No other participants in room yet. Invite friends to stage!",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(eligibleReceivers) { target ->
                        val isSelected = target.uid == selectedReceiver?.uid
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .testTag("gift_receiver_${target.uid}")
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) NeonViolet else DarkSurfaceCard)
                                .border(
                                    1.dp,
                                    if (isSelected) BrightGold else Color(0x22FFFFFF),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedReceiver = target }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            BdAvatar(
                                seed = (target.uid.hashCode() % 6).let { if (it <= 0) 1 else it },
                                name = target.username,
                                photoUrl = target.photoUrl,
                                size = 24.dp
                            )
                            Column {
                                Text(
                                    text = target.username,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ID: ${target.bdpId.ifBlank { "N/A" }}",
                                    color = if (isSelected) BrightGold else TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // Prominent Selected Receiver Banner
                selectedReceiver?.let { rx ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22A855F7))
                            .border(0.5.dp, BrightGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target: ${rx.username} (BDP ID: ${rx.bdpId.ifBlank { "N/A" }})",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = rx.roomRole,
                            color = BrightGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isCatSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .testTag("gift_cat_$category")
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCatSelected) NeonMagenta else DarkSurfaceCard)
                            .border(
                                1.dp,
                                if (isCatSelected) BrightGold else Color(0x22FFFFFF),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedCategory = category
                                selectedGift = filteredGifts.firstOrNull() ?: catalogGifts.firstOrNull()
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isCatSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gift Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                items(filteredGifts) { gift ->
                    val isSelected = gift.giftId == selectedGift?.giftId
                    Box(
                        modifier = Modifier
                            .testTag("gift_item_${gift.giftId}")
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) Color(0x40EC4899) else DarkSurfaceCard
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) BrightGold else Color(0x22FFFFFF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedGift = gift }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = gift.iconUrl.ifBlank { "🎁" }, fontSize = 26.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = gift.name,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🪙 ${gift.coinPrice}",
                                color = BrightGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity Selection: Presets and Stepper / Custom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(presetQuantities) { qty ->
                        val isSelected = selectedQuantity == qty
                        Box(
                            modifier = Modifier
                                .testTag("gift_qty_$qty")
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BrightGold else DarkSurfaceCard)
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White else Color(0x33A855F7),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedQuantity = qty }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "x$qty",
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Stepper / Custom Button
                    item {
                        Surface(
                            color = DarkSurfaceCard,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7)),
                            modifier = Modifier
                                .clickable { showCustomQuantityDialog = true }
                                .testTag("gift_custom_qty_btn")
                        ) {
                            Text(
                                text = "Custom",
                                color = NeonViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Total: 🪙 $totalCoins",
                    color = BrightGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Backend Notice: Gifting settlement notice
            Text(
                text = "ℹ️ Gift orders are recorded securely in Firestore (PENDING backend settlement).",
                color = TextMuted,
                fontSize = 9.5.sp,
                lineHeight = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Send Button
            Button(
                onClick = {
                    val receiver = selectedReceiver
                    val gift = selectedGift
                    if (receiver == null) {
                        Toast.makeText(context, "Please select a stage member to send gift to.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (receiver.uid == currentUser.uid) {
                        Toast.makeText(context, "Cannot send a gift to yourself.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (gift == null) {
                        Toast.makeText(context, "Please select a gift.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Validate safe limits
                    val validationError = giftRepository.validateGiftRequest(
                        receiverUid = receiver.uid,
                        currentUserUid = currentUser.uid,
                        quantity = selectedQuantity
                    )
                    if (validationError != null) {
                        Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (coinBalance < totalCoins) {
                        Toast.makeText(context, "Insufficient coin balance. Please recharge your wallet.", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    isSending = true
                    coroutineScope.launch {
                        val result = giftRepository.requestSendGift(
                            roomId = roomId,
                            receiverUid = receiver.uid,
                            receiverBdpId = receiver.bdpId,
                            receiverUsername = receiver.username,
                            gift = gift,
                            quantity = selectedQuantity,
                            senderBdpId = currentUser.bdpId,
                            senderUsername = currentUser.username
                        )
                        isSending = false
                        if (result.isSuccess) {
                            val txn = result.getOrNull()
                            Toast.makeText(
                                context,
                                "Gift submitted (PENDING backend verification)! 🎁",
                                Toast.LENGTH_LONG
                            ).show()
                            if (txn != null && onGiftSent != null) {
                                onGiftSent(txn)
                            }
                            onDismiss()
                        } else {
                            Toast.makeText(
                                context,
                                result.exceptionOrNull()?.message ?: "Failed to process gift request",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                enabled = !isSending && selectedReceiver != null && selectedGift != null,
                colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("send_gift_button")
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Send Gift to ${selectedReceiver?.username ?: "Member"} 🎁",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }

    // Custom Quantity Safe Input Dialog
    if (showCustomQuantityDialog) {
        var customInput by remember { mutableStateOf(selectedQuantity.toString()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCustomQuantityDialog = false },
            containerColor = DarkSurfaceCard,
            title = {
                Text(
                    text = "Select Gift Quantity",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter quantity between 1 and 9,999:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            customInput = filtered
                            val num = filtered.toIntOrNull()
                            errorMessage = when {
                                num == null || num <= 0 -> "Quantity must be at least 1"
                                num > 9999 -> "Maximum allowed quantity is 9,999"
                                else -> null
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = errorMessage != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrightGold,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_quantity_input")
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = customInput.toIntOrNull()
                        if (num != null && num in 1..9999) {
                            selectedQuantity = num
                            showCustomQuantityDialog = false
                        } else {
                            errorMessage = "Please enter a valid quantity (1..9,999)"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightGold),
                    modifier = Modifier.testTag("confirm_custom_quantity_btn")
                ) {
                    Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomQuantityDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}
