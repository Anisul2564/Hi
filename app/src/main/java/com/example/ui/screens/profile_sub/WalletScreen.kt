package com.example.ui.screens.profile_sub

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payment
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.wallet.WalletRepository
import com.example.model.FirestoreCoinPackage
import com.example.model.FirestorePaymentRequest
import com.example.model.FirestoreWallet
import com.example.model.User
import com.example.ui.components.BdTopBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Production-ready BD PARTY Virtual Wallet Screen.
 *
 * Displays:
 * - Coin Balance (from real Firestore /wallets/{uid})
 * - Purchased Coins, Coins Gifted, Coins Received
 * - Virtual Coin Recharge Packages (from Firestore /coinPackages)
 * - Professional Buy Coins Payment Flow with manual verification for bKash, Nagad, Rocket
 * - Transaction / Payment Request History
 *
 * Does NOT auto-credit or fake payments; submits real PENDING payment requests.
 */
@Composable
fun WalletScreen(
    currentUser: User,
    walletRepository: WalletRepository = remember { WalletRepository() },
    onBack: () -> Unit,
    onNavigate: ((Screen) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize/observe real wallet
    LaunchedEffect(currentUser.uid) {
        if (currentUser.uid.isNotBlank()) {
            walletRepository.getOrCreateWallet(currentUser.uid, currentUser.bdpId)
        }
    }

    val wallet by walletRepository.observeWallet(currentUser.uid).collectAsState(initial = null)
    val packages by walletRepository.observeCoinPackages().collectAsState(initial = walletRepository.defaultCoinPackages())
    val paymentRequests by walletRepository.observeUserPaymentRequests(currentUser.uid).collectAsState(initial = emptyList())

    val displayCoinBalance = wallet?.coinBalance ?: currentUser.coins
    val totalPurchased = wallet?.totalPurchasedCoins ?: 0L
    val totalGifted = wallet?.totalGiftedCoins ?: 0L
    val totalReceived = wallet?.totalReceivedCoins ?: 0L

    var selectedPackageForRecharge by remember { mutableStateOf<FirestoreCoinPackage?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BdTopBar(
                title = "Virtual Wallet",
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
            // Main Balance Card
            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(BrightGold, NeonMagenta))
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("wallet_balance_card")
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF3B185F), Color(0xFF1E0E3E), DarkBackground)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BD PARTY Virtual Balance",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "ID: ${currentUser.bdpId}",
                                color = BrightGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "🪙", fontSize = 28.sp)
                                    Text(
                                        text = String.format("%,d", displayCoinBalance),
                                        color = BrightGold,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Text(
                                    text = "Current Coin Balance",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = {
                                    selectedPackageForRecharge = packages.firstOrNull()
                                    showPaymentDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrightGold),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("buy_coins_button")
                            ) {
                                Text(text = "Recharge ⚡", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Stats Grid (Purchased, Gifted, Received)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x33000000))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = String.format("%,d", totalPurchased), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Purchased", color = TextMuted, fontSize = 10.sp)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0x33FFFFFF)))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onNavigate?.invoke(Screen.GiftHistory) }
                                    .testTag("wallet_coins_gifted_btn")
                            ) {
                                Text(text = String.format("%,d", totalGifted), color = NeonMagenta, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Coins Gifted ↗", color = TextMuted, fontSize = 10.sp)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0x33FFFFFF)))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onNavigate?.invoke(Screen.GiftHistory) }
                                    .testTag("wallet_coins_received_btn")
                            ) {
                                Text(text = String.format("%,d", totalReceived), color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Coins Received ↗", color = TextMuted, fontSize = 10.sp)
                            }
                        }

                        if (onNavigate != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = Color(0x33A855F7),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrightGold.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigate(Screen.GiftHistory) }
                                    .testTag("wallet_view_gift_history_btn")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = "🎁", fontSize = 14.sp)
                                        Text(
                                            text = "Gift Transaction History",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "View Records →",
                                        color = BrightGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Coin Packages Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🪙 Coin Recharge Packages",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "bKash • Nagad • Rocket",
                        color = BrightGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(packages) { pkg ->
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coin_package_${pkg.packageId}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🪙", fontSize = 28.sp)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${String.format("%,d", pkg.coinAmount)} Coins",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (pkg.bonusCoins > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x33EC4899))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "+${String.format("%,d", pkg.bonusCoins)} Bonus",
                                                color = NeonMagenta,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = pkg.name,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                selectedPackageForRecharge = pkg
                                showPaymentDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightGold),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(text = pkg.price, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Transaction / Payment Requests History
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📋 Recent Recharge Requests",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (paymentRequests.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceCard)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recharge requests yet. Choose a package above to buy coins.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(paymentRequests) { req ->
                    PaymentRequestItem(req)
                }
            }

            // Disclaimer
            item {
                Text(
                    text = "Note: Recharge verification is processed securely via manual agent review. Coins are credited directly to your authenticated wallet once transaction ID is verified. BD PARTY coins are virtual items for social gifting and games.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }

    // Professional Manual Payment Method Dialog (bKash, Nagad, Rocket)
    if (showPaymentDialog && selectedPackageForRecharge != null) {
        val pkg = selectedPackageForRecharge!!
        ManualRechargeDialog(
            pkg = pkg,
            currentUser = currentUser,
            onDismiss = { showPaymentDialog = false },
            onSubmit = { method, phone, trxId ->
                showPaymentDialog = false
                coroutineScope.launch {
                    val result = walletRepository.submitPaymentRequest(
                        paymentMethod = method,
                        packageId = pkg.packageId,
                        coinAmount = pkg.coinAmount,
                        amountLabel = pkg.price,
                        senderNumber = phone,
                        transactionId = trxId,
                        userBdpId = currentUser.bdpId,
                        username = currentUser.username
                    )
                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            "Recharge request submitted (Status: PENDING verification)! 📋",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            result.exceptionOrNull()?.message ?: "Failed to submit payment request",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun PaymentRequestItem(req: FirestorePaymentRequest) {
    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth().testTag("payment_request_${req.requestId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${req.paymentMethod} • ${req.amount}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "+${String.format("%,d", req.coinAmount)} 🪙",
                        color = BrightGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TrxID: ${req.transactionId} • From: ${req.senderNumber}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                if (!req.reviewNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Note: ${req.reviewNote}",
                        color = if (req.status == "REJECTED") Color(0xFFFF8A80) else BrightGold,
                        fontSize = 10.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (req.status) {
                            "APPROVED" -> Color(0x3310B981)
                            "REJECTED" -> Color(0x33EF4444)
                            else -> Color(0x33F59E0B)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = req.status,
                    color = when (req.status) {
                        "APPROVED" -> Color(0xFF10B981)
                        "REJECTED" -> Color(0xFFEF4444)
                        else -> BrightGold
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ManualRechargeDialog(
    pkg: FirestoreCoinPackage,
    currentUser: User,
    onDismiss: () -> Unit,
    onSubmit: (method: String, phone: String, trxId: String) -> Unit
) {
    val methods = listOf("bKash", "Nagad", "Rocket")
    var selectedMethod by remember { mutableStateOf(methods[0]) }
    var senderPhone by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }

    val merchantNumbers = mapOf(
        "bKash" to "01700-112233 (Merchant)",
        "Nagad" to "01800-445566 (Merchant)",
        "Rocket" to "01900-778899 (Merchant)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceCard,
        title = {
            Text(text = "Recharge Coins 🪙", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Package: ${pkg.name} (${String.format("%,d", pkg.coinAmount)} coins for ${pkg.price})",
                    color = BrightGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "1. Choose Payment Method:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    methods.forEach { method ->
                        val isSelected = method == selectedMethod
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonViolet else DarkSurface)
                                .border(1.dp, if (isSelected) BrightGold else Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                .clickable { selectedMethod = method }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Official Merchant instructions
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Send Money / Payment to Official Number:",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = merchantNumbers[selectedMethod] ?: "",
                            color = BrightGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "2. Enter Verification Details:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = senderPhone,
                    onValueChange = { senderPhone = it },
                    placeholder = { Text("Sender Mobile Number (e.g. 017xxxxxxxx)", fontSize = 11.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("payment_sender_phone_input")
                )

                OutlinedTextField(
                    value = transactionId,
                    onValueChange = { transactionId = it },
                    placeholder = { Text("Transaction ID / TrxID (e.g. 9J4K2L8)", fontSize = 11.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("payment_trxid_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(selectedMethod, senderPhone, transactionId)
                },
                enabled = senderPhone.isNotBlank() && transactionId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BrightGold)
            ) {
                Text("Submit for Verification", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
