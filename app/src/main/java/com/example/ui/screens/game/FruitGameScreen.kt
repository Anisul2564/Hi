package com.example.ui.screens.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MockRepository
import com.example.model.FruitGameHistory
import com.example.model.FruitGameOption
import com.example.ui.components.BdTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun FruitGameScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    // 8 Options strictly as specified by prompt
    val options = remember { MockRepository.fruitGameOptions }
    // Chips
    val chipDenominations = remember {
        listOf(
            100L to "100",
            500L to "500",
            1000L to "1K",
            10000L to "10K",
            100000L to "100K",
            1000000L to "1M",
            5000000L to "5M",
            10000000L to "10M"
        )
    }

    var selectedChipAmount by remember { mutableStateOf(100L) }
    var userCoinBalance by remember { mutableStateOf(MockRepository.currentUser.coins) }
    // Map of optionId -> total selected coins
    val selectedBets = remember { mutableStateMapOf<Int, Long>() }

    var roundNumber by remember { mutableStateOf(2026100742L) }
    var countdownSeconds by remember { mutableStateOf(30) }
    var isSpinning by remember { mutableStateOf(false) }
    var highlightedIndex by remember { mutableStateOf<Int?>(null) }
    var winningOption by remember { mutableStateOf<FruitGameOption?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var lastWinAmount by remember { mutableStateOf(0L) }

    val recentHistory = remember {
        mutableStateListOf(
            FruitGameHistory("#41", options[5], "1m ago"), // Meat 45x
            FruitGameHistory("#40", options[1], "2m ago"), // Orange 5x
            FruitGameHistory("#39", options[0], "3m ago"), // Kebab 15x
            FruitGameHistory("#38", options[7], "4m ago")  // Fish 10x
        )
    }

    // Auto countdown timer effect
    LaunchedEffect(countdownSeconds, isSpinning) {
        if (!isSpinning && countdownSeconds > 0) {
            delay(1000)
            countdownSeconds -= 1
        } else if (!isSpinning && countdownSeconds == 0) {
            // Trigger round roll
            isSpinning = true
            // Spin animation
            val winnerIdx = Random.nextInt(options.size)
            for (step in 0..16) {
                highlightedIndex = (step % options.size)
                delay(80L + (step * 15L))
            }
            highlightedIndex = winnerIdx
            val winner = options[winnerIdx]
            winningOption = winner

            val betOnWinner = selectedBets[winner.id] ?: 0L
            val winAmount = betOnWinner * winner.multiplier
            lastWinAmount = winAmount
            if (winAmount > 0) {
                userCoinBalance += winAmount
            }
            recentHistory.add(0, FruitGameHistory("#${roundNumber % 100}", winner, "Just now"))
            showResultDialog = true
            isSpinning = false
        }
    }

    val totalSelectedCoins = selectedBets.values.sum()

    Scaffold(
        topBar = {
            BdTopBar(
                title = "Treasure Hunter (Virtual Game)",
                onBackClick = onBack,
                coins = userCoinBalance,
                showActions = false
            )
        },
        containerColor = Color(0xFF031A14), // Premium emerald/gold game theme
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF042F24), Color(0xFF021B14), Color(0xFF010E0A))
                    )
                ),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Disclaimer pill
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x3310B981))
                        .border(0.5.dp, Color(0x6610B981), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎮 Non-cash virtual entertainment • In-app virtual coins only",
                        color = BrightGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Top Status Bar: Round Number, Coin Balance, Total Selected
            item {
                Surface(
                    color = Color(0x66064E3B),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66F59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Round #$roundNumber", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Balance: ${String.format("%,d", userCoinBalance)}", color = BrightGold, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Total Selected", color = TextMuted, fontSize = 10.sp)
                            Text(
                                text = "${String.format("%,d", totalSelectedCoins)} Coins",
                                color = if (totalSelectedCoins > 0) BrightGold else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // 3 x 3 Grid with 8 Surrounding Options and Center Countdown/Result
            item {
                GameGrid3x3(
                    options = options,
                    selectedBets = selectedBets,
                    highlightedIndex = highlightedIndex,
                    countdownSeconds = countdownSeconds,
                    isSpinning = isSpinning,
                    winningOption = winningOption,
                    onOptionClick = { option ->
                        if (!isSpinning) {
                            val current = selectedBets[option.id] ?: 0L
                            selectedBets[option.id] = current + selectedChipAmount
                        }
                    }
                )
            }

            // Control Actions: Clear / Undo & Immediate Roll Test
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { selectedBets.clear() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fruit_game_clear_button")
                    ) {
                        Text(text = "↺ Clear Selections", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (!isSpinning) {
                                countdownSeconds = 0 // Trigger immediate roll
                            }
                        },
                        enabled = !isSpinning,
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fruit_game_spin_button")
                    ) {
                        Text(
                            text = if (isSpinning) "Spinning..." else "⚡ Spin Now",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Chip Selector Row (100, 500, 1K, 10K, 100K, 1M, 5M, 10M)
            item {
                Text(
                    text = "🪙 Select Coin Chip:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(chipDenominations) { (amount, label) ->
                        val isSelected = selectedChipAmount == amount
                        Box(
                            modifier = Modifier
                                .testTag("chip_button_$label")
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Brush.radialGradient(listOf(BrightGold, Color(0xFFD97706)))
                                    else Brush.radialGradient(listOf(Color(0xFF065F46), Color(0xFF042F24)))
                                )
                                .border(
                                    2.dp,
                                    if (isSelected) Color.White else Color(0x66FFD166),
                                    CircleShape
                                )
                                .clickable { selectedChipAmount = amount },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else BrightGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Recent Round History
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📜 Recent Round History",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentHistory) { hist ->
                        Surface(
                            color = Color(0x40064E3B),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFD166))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = hist.winningOption.emoji, fontSize = 16.sp)
                                Column {
                                    Text(
                                        text = "${hist.winningOption.name} ${hist.winningOption.multiplier}x",
                                        color = BrightGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = hist.timeAgo, color = TextMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Round Result Celebration Dialog
    if (showResultDialog && winningOption != null) {
        val winner = winningOption!!
        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
                selectedBets.clear()
                countdownSeconds = 30
                roundNumber += 1
            },
            containerColor = Color(0xFF064E3B),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "🎉 Round Finished! 🎉", color = BrightGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = winner.emoji, fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${winner.name} (${winner.multiplier}x)",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (lastWinAmount > 0) {
                        Text(
                            text = "YOU WON +${String.format("%,d", lastWinAmount)} VIRTUAL COINS! 🪙",
                            color = BrightGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "Better luck next round! Keep hunting treasure!",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResultDialog = false
                        selectedBets.clear()
                        countdownSeconds = 30
                        roundNumber += 1
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fruit_game_next_round_button")
                ) {
                    Text("Next Round (30s)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun GameGrid3x3(
    options: List<FruitGameOption>,
    selectedBets: Map<Int, Long>,
    highlightedIndex: Int?,
    countdownSeconds: Int,
    isSpinning: Boolean,
    winningOption: FruitGameOption?,
    onOptionClick: (FruitGameOption) -> Unit
) {
    // 3x3 layout representation:
    // Row 0: Option 0 (Kebab 15x), Option 1 (Orange 5x), Option 2 (Chicken 25x)
    // Row 1: Option 7 (Fish 10x), Center Area, Option 3 (Cherry 5x)
    // Row 2: Option 6 (Apple 5x), Option 5 (Meat 45x), Option 4 (Lemon 5x)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF03241C))
            .border(2.dp, Color(0x66FFB703), RoundedCornerShape(20.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OptionCell(options[0], selectedBets[options[0].id] ?: 0L, highlightedIndex == 0, onOptionClick, Modifier.weight(1f))
            OptionCell(options[1], selectedBets[options[1].id] ?: 0L, highlightedIndex == 1, onOptionClick, Modifier.weight(1f))
            OptionCell(options[2], selectedBets[options[2].id] ?: 0L, highlightedIndex == 2, onOptionClick, Modifier.weight(1f))
        }

        // Row 2 (Fish, Center, Cherry)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OptionCell(options[7], selectedBets[options[7].id] ?: 0L, highlightedIndex == 7, onOptionClick, Modifier.weight(1f))

            // Center Countdown / Result Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF064E3B), Color(0xFF022C22))
                        )
                    )
                    .border(1.5.dp, BrightGold, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isSpinning) {
                        Text(text = "🎲", fontSize = 26.sp)
                        Text(text = "ROLLING", color = BrightGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    } else {
                        Text(text = "⏳", fontSize = 18.sp)
                        Text(
                            text = "${countdownSeconds}s",
                            color = if (countdownSeconds <= 5) LiveRed else BrightGold,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(text = "COUNTDOWN", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            OptionCell(options[3], selectedBets[options[3].id] ?: 0L, highlightedIndex == 3, onOptionClick, Modifier.weight(1f))
        }

        // Row 3 (Apple, Meat, Lemon)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OptionCell(options[6], selectedBets[options[6].id] ?: 0L, highlightedIndex == 6, onOptionClick, Modifier.weight(1f))
            OptionCell(options[5], selectedBets[options[5].id] ?: 0L, highlightedIndex == 5, onOptionClick, Modifier.weight(1f))
            OptionCell(options[4], selectedBets[options[4].id] ?: 0L, highlightedIndex == 4, onOptionClick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun OptionCell(
    option: FruitGameOption,
    selectedAmount: Long,
    isHighlighted: Boolean,
    onClick: (FruitGameOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selectedAmount > 0

    Box(
        modifier = modifier
            .testTag("game_option_${option.name.lowercase()}")
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    isHighlighted -> Brush.radialGradient(listOf(Color(0xFFFBBF24), Color(0xFFB45309)))
                    isSelected -> Brush.verticalGradient(listOf(Color(0xFF047857), Color(0xFF064E3B)))
                    else -> Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF022C22)))
                }
            )
            .border(
                when {
                    isHighlighted -> 3.dp
                    isSelected -> 2.dp
                    else -> 1.dp
                },
                when {
                    isHighlighted -> Color.White
                    isSelected -> BrightGold
                    else -> Color(0x33F59E0B)
                },
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick(option) }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = option.emoji, fontSize = 26.sp)
            Text(
                text = "${option.name} ${option.multiplier}x",
                color = if (isHighlighted) Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            // Current selected amount pill
            if (selectedAmount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrightGold)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${String.format("%,d", selectedAmount)}",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
