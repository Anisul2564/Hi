package com.example.ui.screens.create

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.room.RoomRepository
import com.example.model.RoomCategory
import com.example.model.User
import com.example.ui.components.BdGradientButton
import com.example.ui.components.BdTopBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CreateFlowScreen(
    flowTitle: String,
    currentUser: User,
    onBack: () -> Unit,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val roomRepository = remember { RoomRepository(context) }

    var roomTitle by remember { mutableStateOf("${currentUser.displayName.ifBlank { "BD" }}'s Live Voice Stage 🎙️") }
    var roomDescription by remember { mutableStateOf("Welcome to my voice room! Take a mic, enjoy music, and chat with friends.") }
    var selectedCategory by remember { mutableStateOf(RoomCategory.PARTY) }
    var isPrivate by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            BdTopBar(
                title = flowTitle,
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
            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Stage Configuration",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Room Name / Title",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = roomTitle,
                            onValueChange = {
                                roomTitle = it
                                errorMessage = null
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonViolet,
                                unfocusedBorderColor = Color(0x33A855F7),
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_flow_title_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Description / Announcement",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = roomDescription,
                            onValueChange = { roomDescription = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonViolet,
                                unfocusedBorderColor = Color(0x33A855F7),
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_flow_desc_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Category",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RoomCategory.values().filter { it != RoomCategory.ALL }.forEach { cat ->
                                val isSelected = cat == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) NeonViolet else DarkSurface)
                                        .border(1.dp, if (isSelected) NeonViolet else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat.displayName,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Microphone Seats",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrightGold.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "12 Active Microphone Seats",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "🎙️ Full Stage",
                                    color = BrightGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Host Profile Verification Card (read-only, authenticated Firebase user)
            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Authenticated Host Information",
                            color = BrightGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Host: ${currentUser.displayName} (${currentUser.bdpId})",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Role: ${currentUser.role.name} • UID: ${currentUser.uid.take(12)}...",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Host identity is securely derived from your session and cannot be spoofed.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Private Voice Stage",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isPrivate) "Invite-only room" else "Discoverable in BD PARTY lobby",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isPrivate,
                            onCheckedChange = { isPrivate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonViolet
                            ),
                            modifier = Modifier.testTag("create_flow_private_switch")
                        )
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage ?: "",
                        color = LiveRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                BdGradientButton(
                    text = if (isCreating) "Creating Stage..." else "Launch Voice Stage 🚀",
                    onClick = {
                        if (roomTitle.isBlank()) {
                            errorMessage = "Please enter a valid stage title."
                            return@BdGradientButton
                        }
                        isCreating = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = roomRepository.createRoom(
                                title = roomTitle,
                                description = roomDescription,
                                category = selectedCategory.name,
                                visibility = if (isPrivate) "PRIVATE" else "PUBLIC",
                                currentUser = currentUser
                            )
                            isCreating = false
                            result.onSuccess { newRoomId ->
                                Toast.makeText(context, "Voice Room Created! Room ID: $newRoomId", Toast.LENGTH_LONG).show()
                                onNavigate(Screen.VoiceRoom(newRoomId))
                            }.onFailure { ex ->
                                errorMessage = "Failed to create room: ${ex.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "launch_stage_button"
                )
            }
        }
    }
}
