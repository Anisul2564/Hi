package com.example.ui.screens.items

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MockRepository
import com.example.model.InventoryItem
import com.example.model.ItemCategory
import com.example.ui.components.BdTopBar
import com.example.ui.theme.*

@Composable
fun MyItemsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedCategory by remember { mutableStateOf(ItemCategory.FRAME) }
    val itemsList = remember { mutableStateListOf(*MockRepository.inventoryItems.toTypedArray()) }

    val filteredItems = itemsList.filter { it.category == selectedCategory }

    Scaffold(
        topBar = {
            BdTopBar(
                title = "My Items Inventory",
                onBackClick = onBack,
                showActions = false
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            // Category Tabs (Horizontal Scrollable)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ItemCategory.values().toList(), key = { it.name }) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .testTag("item_category_${category.name.lowercase()}")
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) NeonViolet else DarkSurfaceCard)
                            .border(1.dp, if (isSelected) BrightGold else Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category.displayName,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Items Grid
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎁", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No ${selectedCategory.displayName} items currently equipped",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Acquire through SVIP rewards, events, or level chests",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        InventoryItemCard(
                            item = item,
                            onToggleUse = {
                                val idx = itemsList.indexOfFirst { it.id == item.id }
                                if (idx != -1) {
                                    itemsList[idx] = item.copy(isEquipped = !item.isEquipped)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onToggleUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inventory_item_${item.id}")
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                if (item.isEquipped) BrightGold else Color(0x33A855F7),
                RoundedCornerShape(18.dp)
            ),
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Image Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (item.isEquipped) Color(0x33FFB703) else Color(0x331F1438)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.iconEmoji,
                    fontSize = 42.sp
                )

                // Remaining time pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x80000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.remainingDays,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stars
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(item.stars) {
                    Text(text = "⭐", fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Name
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Description
            Text(
                text = item.description,
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Use / Using Button
            Button(
                onClick = onToggleUse,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (item.isEquipped) BrightGold else NeonViolet
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("item_use_button_${item.id}")
            ) {
                Text(
                    text = if (item.isEquipped) "✓ In Use" else "Use",
                    color = if (item.isEquipped) Color.Black else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
