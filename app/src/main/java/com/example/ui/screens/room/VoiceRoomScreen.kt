package com.example.ui.screens.room

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.MockRepository
import com.example.data.gift.GiftRepository
import com.example.data.livekit.LiveKitManager
import com.example.data.room.RoomRepository
import com.example.data.wallet.WalletRepository
import com.example.data.firebase.FirestoreUserProfile
import com.example.model.*
import com.example.ui.components.BdAvatar
import com.example.ui.components.GiftAnimationEvent
import com.example.ui.components.GiftAnimationOverlay
import com.example.ui.components.RoleBadge
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceRoomScreen(
    roomId: String,
    currentUser: User,
    onBack: () -> Unit,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val roomRepository = remember { RoomRepository(context) }
    val liveKitManager = remember { LiveKitManager(context) }
    val walletRepository = remember { WalletRepository() }
    val giftRepository = remember { GiftRepository() }

    // Firebase Auth & Firestore live profile detection
    val auth = remember { Firebase.auth }
    val firestore = remember { Firebase.firestore }
    val currentAuthUid = auth.currentUser?.uid?.ifBlank { currentUser.uid } ?: currentUser.uid

    // Real-time listener for current logged-in user's profile from Firestore users/{uid}
    var myLiveProfile by remember { mutableStateOf<FirestoreUserProfile?>(null) }
    DisposableEffect(currentAuthUid) {
        if (currentAuthUid.isNotBlank()) {
            val listener = firestore.collection("users").document(currentAuthUid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        myLiveProfile = snapshot.toObject(FirestoreUserProfile::class.java)
                    }
                }
            onDispose { listener.remove() }
        } else {
            onDispose {}
        }
    }

    // Real-time Firestore streams
    val roomState by roomRepository.getRoomFlow(roomId).collectAsState(initial = null)
    val participants by roomRepository.getParticipantsFlow(roomId).collectAsState(initial = emptyList())
    val liveKitStatus by liveKitManager.connectionStatus.collectAsState()
    val userWallet by walletRepository.observeWallet(currentUser.uid).collectAsState(initial = null)
    val realCoinBalance = userWallet?.coinBalance ?: currentUser.coins

    // Real-time listener for all seated participants' profiles from Firestore users/{uid}
    val seatedUids = remember(participants) {
        participants.filter { it.seatIndex in 0..11 && it.uid.isNotBlank() }.map { it.uid }.toSet()
    }
    val seatedProfiles = remember { mutableStateMapOf<String, FirestoreUserProfile>() }
    DisposableEffect(seatedUids) {
        val listeners = seatedUids.map { uid ->
            firestore.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val prof = snapshot.toObject(FirestoreUserProfile::class.java)
                    if (prof != null) {
                        seatedProfiles[uid] = prof
                    }
                }
            }
        }
        onDispose {
            listeners.forEach { it.remove() }
        }
    }

    // Current user in this room
    val myParticipant = remember(participants, currentAuthUid) {
        participants.firstOrNull { it.uid == currentAuthUid }
    }
    val isHost = remember(roomState, currentAuthUid) {
        roomState?.hostUid == currentAuthUid
    }
    val isModerator = remember(myParticipant) {
        myParticipant?.roomRole == RoomRole.MODERATOR.name || myParticipant?.roomRole == RoomRole.CO_HOST.name
    }
    val hasSpeakerSeat = remember(myParticipant) {
        myParticipant != null && myParticipant.seatIndex in 0..11
    }

    // Microphone permission launcher
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Microphone access granted 🎙️", Toast.LENGTH_SHORT).show()
        } else {
            showPermissionRationaleDialog = true
        }
    }

    // Auto-join room participant record when entering
    LaunchedEffect(roomId) {
        roomRepository.joinRoom(roomId, currentUser)
    }

    // Clean up when leaving screen
    DisposableEffect(roomId) {
        onDispose {
            liveKitManager.disconnect()
            coroutineScope.launch {
                roomRepository.leaveRoom(roomId)
            }
        }
    }

    // Chat messages state
    var chatInputText by remember { mutableStateOf("") }
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("c1", MockRepository.onlineCreators[0], "Welcome to BD PARTY Live Stage! 🎙️✨", "Just now"),
            ChatMessage("c2", MockRepository.onlineCreators[1], "Drop the beat! Ready for the contest! 🔥", "Just now")
        )
    }

    var showGiftSheet by remember { mutableStateOf(false) }
    var showRoomInfoDialog by remember { mutableStateOf(false) }
    var showRaiseHandDialog by remember { mutableStateOf(false) }
    var selectedSeatParticipant by remember { mutableStateOf<RoomParticipant?>(null) }

    // Reusable Gift Animation Queue (handles repeated gifts without freezing room UI)
    val giftAnimationQueue = remember { mutableStateListOf<GiftAnimationEvent>() }
    val activeGiftAnimation = giftAnimationQueue.firstOrNull()
    val seenTransactionIds = remember { mutableSetOf<String>() }

    // Observe real incoming gifts for the current user in this room
    LaunchedEffect(currentUser.uid, roomId) {
        if (currentUser.uid.isNotBlank()) {
            giftRepository.observeReceivedGifts(currentUser.uid).collect { txns ->
                val roomTxns = txns.filter { it.roomId == roomId }
                roomTxns.forEach { txn ->
                    if (txn.transactionId.isNotBlank() && !seenTransactionIds.contains(txn.transactionId)) {
                        seenTransactionIds.add(txn.transactionId)
                        val icon = giftRepository.defaultGifts().find { it.giftId == txn.giftId }?.iconUrl ?: "🎁"
                        giftAnimationQueue.add(
                            GiftAnimationEvent(
                                giftName = txn.giftName,
                                giftIcon = icon,
                                senderName = txn.senderUsername,
                                senderBdpId = txn.senderBdpId,
                                receiverName = txn.receiverUsername,
                                receiverBdpId = txn.receiverBdpId,
                                quantity = txn.quantity,
                                totalCoins = txn.totalCoins
                            )
                        )
                    }
                }
            }
        }
    }

    // If room is ended by host, inform and navigate back
    LaunchedEffect(roomState?.status) {
        if (roomState?.status == RoomStatus.ENDED.name) {
            Toast.makeText(context, "The host has ended this live stage.", Toast.LENGTH_LONG).show()
            onBack()
        }
    }

    // Render 12 seats mapped from Firestore participants & real-time profile streams
    val seats = remember(participants, seatedProfiles, myLiveProfile, currentUser, currentAuthUid) {
        (0 until 12).map { index ->
            val occupant = participants.firstOrNull { it.seatIndex == index }
            val seatUser = occupant?.let { occ ->
                val isMe = occ.uid == currentAuthUid
                val liveProf = if (isMe) (myLiveProfile ?: seatedProfiles[occ.uid]) else seatedProfiles[occ.uid]

                // Identify the real profile photo URL
                val realPhotoUrl = when {
                    !liveProf?.photoUrl.isNullOrBlank() -> liveProf!!.photoUrl
                    isMe && currentUser.photoUrl.isNotBlank() -> currentUser.photoUrl
                    occ.photoUrl.isNotBlank() -> occ.photoUrl
                    else -> ""
                }

                // Identify the real username / display name
                val realDisplayName = when {
                    !liveProf?.username.isNullOrBlank() -> liveProf!!.username
                    isMe && currentUser.displayName.isNotBlank() -> currentUser.displayName
                    occ.username.isNotBlank() -> occ.username
                    else -> "BD Member"
                }

                val realBdpId = liveProf?.bdpId?.ifBlank { occ.bdpId } ?: occ.bdpId
                val seed = (occ.uid.hashCode() % 10).let { s -> kotlin.math.abs(s) + 1 }

                User(
                    id = occ.uid,
                    uid = occ.uid,
                    bdpId = realBdpId,
                    username = realDisplayName,
                    displayName = realDisplayName,
                    photoUrl = realPhotoUrl,
                    avatarSeed = seed,
                    role = try { UserRole.valueOf(occ.role) } catch (_: Exception) { UserRole.USER }
                )
            }
            Seat(
                seatIndex = index,
                user = seatUser,
                isMuted = occupant?.micState == MicState.MUTED.name,
                isSpeaking = occupant != null && occupant.micState == MicState.UNMUTED.name
            )
        }
    }

    val displayTitle = roomState?.title ?: "BD PARTY Live Stage"
    val displayHostName = roomState?.hostUsername ?: "Host"
    val participantTotal = participants.size.coerceAtLeast(1)
    val listenersTotal = participants.count { it.seatIndex == -1 }

    Scaffold(
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2E0854), Color(0xFF140728), DarkBackground)
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Room Header
                RoomHeader(
                    title = displayTitle,
                    roomId = roomId,
                    hostName = displayHostName,
                    isHost = isHost,
                    onExit = {
                        coroutineScope.launch {
                            roomRepository.leaveRoom(roomId)
                            onBack()
                        }
                    },
                    onInfoClick = { showRoomInfoDialog = true },
                    onRankingClick = { onNavigate(Screen.Ranking) },
                    onEndRoomClick = {
                        coroutineScope.launch {
                            roomRepository.endRoom(roomId)
                            Toast.makeText(context, "Room ended.", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                )

                // LiveKit Backend & Connection Status Bar
                LiveKitStatusBar(
                    status = liveKitStatus,
                    isBackendReady = liveKitManager.isBackendTokenServiceReady
                )

                // Audience Row
                AudienceRow(
                    listenersCount = listenersTotal,
                    participantCount = participantTotal,
                    participants = participants
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 12 Microphone Seats Grid
                SpeakerSeatsGrid12(
                    seats = seats,
                    onSeatClick = { index ->
                        val occupant = participants.firstOrNull { it.seatIndex == index }
                        if (occupant != null) {
                            selectedSeatParticipant = occupant
                        } else {
                            // Seat is empty: attempt to take seat
                            if (!hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                coroutineScope.launch {
                                    val res = roomRepository.occupySeat(roomId, index)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "You took Speaker Seat #${index + 1} 🎙️", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Could not take seat", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // In-Room Live Chat Stream
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(chatMessages.reversed()) { msg ->
                            InRoomChatRow(message = msg)
                        }
                    }

                    // Reusable Gift Animation Overlay
                    GiftAnimationOverlay(
                        activeEvent = activeGiftAnimation,
                        onAnimationEnd = {
                            if (giftAnimationQueue.isNotEmpty()) {
                                giftAnimationQueue.removeAt(0)
                            }
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                // In-Room Bottom Action Bar with Mute/Unmute, Raise Hand, Leave Room, Gift, PK
                RoomBottomActionBar(
                    hasSeat = hasSpeakerSeat,
                    isMuted = myParticipant?.micState == MicState.MUTED.name,
                    onToggleMic = {
                        if (!hasMicPermission) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@RoomBottomActionBar
                        }
                        if (!hasSpeakerSeat) {
                            Toast.makeText(context, "Take a microphone seat to speak!", Toast.LENGTH_SHORT).show()
                            return@RoomBottomActionBar
                        }
                        val newMic = if (myParticipant?.micState == MicState.MUTED.name) MicState.UNMUTED else MicState.MUTED
                        coroutineScope.launch {
                            roomRepository.setMicState(roomId, newMic)
                            liveKitManager.setMicrophoneEnabled(newMic == MicState.UNMUTED)
                        }
                    },
                    onRaiseHand = {
                        showRaiseHandDialog = true
                    },
                    onLeaveSeat = {
                        coroutineScope.launch {
                            roomRepository.releaseSeat(roomId)
                            liveKitManager.setMicrophoneEnabled(false)
                            Toast.makeText(context, "You stepped down from the microphone seat", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenGift = { showGiftSheet = true },
                    onOpenGame = { onNavigate(Screen.FruitGame) },
                    onOpenPk = { onNavigate(Screen.MicBattle) },
                    onSendMessage = {
                        if (chatInputText.isNotBlank()) {
                            chatMessages.add(
                                ChatMessage(
                                    id = "c_${System.currentTimeMillis()}",
                                    sender = currentUser,
                                    text = chatInputText,
                                    timestamp = "Now"
                                )
                            )
                            chatInputText = ""
                        }
                    },
                    chatText = chatInputText,
                    onChatTextChange = { chatInputText = it }
                )
            }
        }
    }

    // Permission Rationale Dialog
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            containerColor = DarkSurfaceCard,
            title = {
                Text(text = "Microphone Permission Required", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "BD PARTY needs microphone permission so other users can hear you on stage. Microphone access is only enabled when you occupy a speaker seat and unmute.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationaleDialog = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                ) {
                    Text("Grant Permission", color = BrightGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Raise Hand Dialog (Listener Requesting Mic Seat)
    if (showRaiseHandDialog) {
        AlertDialog(
            onDismissRequest = { showRaiseHandDialog = false },
            containerColor = DarkSurfaceCard,
            title = {
                Text(text = "Request to Speak ✋", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Would you like to take an available microphone seat? Once on stage, listeners will be able to hear you speak.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRaiseHandDialog = false
                        // Find first open seat
                        val openSeatIndex = (0 until 12).firstOrNull { idx ->
                            participants.none { it.seatIndex == idx }
                        }
                        if (openSeatIndex != null) {
                            if (!hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                coroutineScope.launch {
                                    val res = roomRepository.occupySeat(roomId, openSeatIndex)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Granted Seat #${openSeatIndex + 1}! 🎙️", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Could not take seat", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "All 12 microphone seats are currently occupied.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Take Available Seat", color = BrightGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRaiseHandDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Moderation Action Dialog on Seat Participant
    if (selectedSeatParticipant != null) {
        val target = selectedSeatParticipant!!
        val isSelf = target.uid == currentUser.uid
        val canModerate = (isHost || isModerator) && !isSelf

        AlertDialog(
            onDismissRequest = { selectedSeatParticipant = null },
            containerColor = DarkSurfaceCard,
            title = {
                val isTargetMe = target.uid == currentAuthUid
                val targetProf = if (isTargetMe) (myLiveProfile ?: seatedProfiles[target.uid]) else seatedProfiles[target.uid]
                val targetPhoto = targetProf?.photoUrl?.takeIf { it.isNotBlank() } ?: target.photoUrl
                val targetName = targetProf?.username?.takeIf { it.isNotBlank() } ?: target.username
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BdAvatar(
                        seed = (target.uid.hashCode() % 6).let { if (it <= 0) 1 else it },
                        name = targetName,
                        photoUrl = targetPhoto,
                        size = 32.dp
                    )
                    Column {
                        Text(text = targetName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Seat #${target.seatIndex + 1} • ${target.roomRole}", color = BrightGold, fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Participant ID: ${target.bdpId} • Role: ${target.role}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (canModerate) {
                        Text(
                            text = "Moderation Options:",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val newMic = if (target.micState == MicState.MUTED.name) MicState.UNMUTED else MicState.MUTED
                                        roomRepository.moderatorMuteParticipant(roomId, target.uid, newMic)
                                        selectedSeatParticipant = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (target.micState == MicState.MUTED.name) "Unmute" else "Mute Mic", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        roomRepository.moderatorKickFromSeat(roomId, target.uid)
                                        selectedSeatParticipant = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Remove Seat", fontSize = 11.sp)
                            }
                        }

                        // Host-only Role Transitions: Co-Host & Moderator delegation
                        if (isHost && target.roomRole != RoomRole.HOST.name) {
                            Text(
                                text = "Host Privileges:",
                                color = BrightGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (target.roomRole == RoomRole.CO_HOST.name) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                roomRepository.changeParticipantRoomRole(roomId, target.uid, RoomRole.SPEAKER)
                                                selectedSeatParticipant = null
                                                Toast.makeText(context, "Demoted to Speaker", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Demote to Speaker", fontSize = 10.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                roomRepository.changeParticipantRoomRole(roomId, target.uid, RoomRole.CO_HOST)
                                                selectedSeatParticipant = null
                                                Toast.makeText(context, "Promoted to Co-Host", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Make Co-Host", fontSize = 10.sp)
                                    }
                                }

                                if (target.roomRole == RoomRole.MODERATOR.name) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                roomRepository.changeParticipantRoomRole(roomId, target.uid, RoomRole.SPEAKER)
                                                selectedSeatParticipant = null
                                                Toast.makeText(context, "Removed Moderator role", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Dismiss Mod", fontSize = 10.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                roomRepository.changeParticipantRoomRole(roomId, target.uid, RoomRole.MODERATOR)
                                                selectedSeatParticipant = null
                                                Toast.makeText(context, "Appointed as Moderator", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Make Moderator", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    } else if (isSelf) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    roomRepository.releaseSeat(roomId)
                                    selectedSeatParticipant = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Leave Seat", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSeatParticipant = null }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // Gift Center Sheet
    if (showGiftSheet) {
        GiftCenterSheet(
            roomId = roomId,
            currentUser = currentUser,
            coinBalance = realCoinBalance,
            participants = participants,
            giftRepository = giftRepository,
            onDismiss = { showGiftSheet = false },
            onOpenHistory = { onNavigate(Screen.GiftHistory) },
            onGiftSent = { txn ->
                seenTransactionIds.add(txn.transactionId)
                val icon = giftRepository.defaultGifts().find { it.giftId == txn.giftId }?.iconUrl ?: "🎁"
                giftAnimationQueue.add(
                    GiftAnimationEvent(
                        giftName = txn.giftName,
                        giftIcon = icon,
                        senderName = txn.senderUsername,
                        senderBdpId = txn.senderBdpId,
                        receiverName = txn.receiverUsername,
                        receiverBdpId = txn.receiverBdpId,
                        quantity = txn.quantity,
                        totalCoins = txn.totalCoins
                    )
                )
            }
        )
    }

    // Stage Info Dialog
    if (showRoomInfoDialog) {
        AlertDialog(
            onDismissRequest = { showRoomInfoDialog = false },
            containerColor = DarkSurfaceCard,
            title = {
                Text(text = "Stage Information", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Title: $displayTitle", color = BrightGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Room ID: #$roomId", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "Host: $displayHostName", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "Category: ${roomState?.category ?: "PARTY"}", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "Speakers Capacity: 12 Microphone Seats", color = Color.White, fontSize = 12.sp)
                    Text(text = "Description: ${roomState?.description ?: "Welcome to BD PARTY Live Stage!"}", color = TextMuted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoomInfoDialog = false }) {
                    Text("Close", color = BrightGold)
                }
            }
        )
    }
}

@Composable
private fun RoomHeader(
    title: String,
    roomId: String,
    hostName: String,
    isHost: Boolean,
    onExit: () -> Unit,
    onInfoClick: () -> Unit,
    onRankingClick: () -> Unit,
    onEndRoomClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33000000))
                    .testTag("room_exit_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Exit Room",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            BdAvatar(seed = 1, name = hostName, size = 38.dp, hasFrame = true)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "ID: $roomId", color = TextSecondary, fontSize = 10.sp)
                    Text(text = "• Host: $hostName", color = BrightGold, fontSize = 10.sp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isHost) {
                Button(
                    onClick = onEndRoomClick,
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("end_room_button")
                ) {
                    Text("End Stage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(
                onClick = onRankingClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFB703))
                    .testTag("room_ranking_button")
            ) {
                Text(text = "🏆", fontSize = 16.sp)
            }

            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33000000))
                    .testTag("room_info_button")
            ) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun LiveKitStatusBar(
    status: LiveKitConnectionStatus,
    isBackendReady: Boolean
) {
    Surface(
        color = DarkSurfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isBackendReady) Color(0xFF10B981) else BrightGold)
                )
                Text(
                    text = if (isBackendReady) "LiveKit Voice: Ready" else "LiveKit: Secure Token Backend Pending",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "WebRTC: ${status.name}",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun AudienceRow(
    listenersCount: Int,
    participantCount: Int,
    participants: List<RoomParticipant>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((-6).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            participants.take(6).forEach { p ->
                val seed = (p.uid.hashCode() % 6).let { if (it <= 0) 1 else it }
                BdAvatar(seed = seed, name = p.username, photoUrl = p.photoUrl, size = 28.dp)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x40000000))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(text = "👥 $participantCount total", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = "• 🎧 $listenersCount listeners", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SpeakerSeatsGrid12(
    seats: List<Seat>,
    onSeatClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 3 rows of 4 seats = exactly 12 seats
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until 4) {
                    val seatIndex = row * 4 + col
                    val seat = seats.getOrNull(seatIndex) ?: Seat(seatIndex)
                    SpeakerSeatItem(
                        seat = seat,
                        index = seatIndex,
                        onClick = { onSeatClick(seatIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeakerSeatItem(
    seat: Seat,
    index: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag("speaker_seat_$index")
            .clickable { onClick() }
    ) {
        if (seat.user != null) {
            Box(contentAlignment = Alignment.Center) {
                if (seat.isSpeaking) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NeonMagenta.copy(alpha = 0.5f), Color.Transparent)))
                    )
                }

                BdAvatar(
                    seed = seat.user.avatarSeed,
                    name = seat.user.displayName,
                    photoUrl = seat.user.photoUrl,
                    size = 44.dp,
                    hasFrame = index == 0,
                    frameColor = Brush.sweepGradient(listOf(BrightGold, NeonMagenta, BrightGold))
                )

                if (seat.isMuted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(LiveRed)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = seat.user.displayName,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 68.dp),
                textAlign = TextAlign.Center
            )
        } else {
            // Empty Seat
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceCard)
                    .border(1.dp, Color(0x33A855F7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Empty Mic Seat",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${index + 1}",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (index == 0) "Host" else "Seat ${index + 1}",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun InRoomChatRow(message: ChatMessage) {
    Surface(
        color = Color(0x661A0B2E),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BdAvatar(
                seed = message.sender.avatarSeed,
                name = message.sender.displayName,
                photoUrl = message.sender.photoUrl,
                size = 24.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = message.sender.displayName,
                        color = BrightGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    RoleBadge(role = message.sender.role)
                    Text(
                        text = message.timestamp,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun RoomBottomActionBar(
    hasSeat: Boolean,
    isMuted: Boolean,
    onToggleMic: () -> Unit,
    onRaiseHand: () -> Unit,
    onLeaveSeat: () -> Unit,
    onOpenGift: () -> Unit,
    onOpenGame: () -> Unit,
    onOpenPk: () -> Unit,
    onSendMessage: () -> Unit,
    chatText: String,
    onChatTextChange: (String) -> Unit
) {
    Surface(
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Chat Input
            OutlinedTextField(
                value = chatText,
                onValueChange = onChatTextChange,
                placeholder = { Text("Chat with stage...", color = TextMuted, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceCard,
                    unfocusedContainerColor = DarkSurfaceCard,
                    focusedBorderColor = NeonViolet,
                    unfocusedBorderColor = Color(0x33A855F7),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                trailingIcon = {
                    if (chatText.isNotBlank()) {
                        IconButton(onClick = onSendMessage) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = NeonMagenta)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("room_chat_input")
            )

            // Mic Toggle / Raise Hand Button
            if (hasSeat) {
                IconButton(
                    onClick = onToggleMic,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) LiveRed else NeonViolet)
                        .testTag("room_mic_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Step down from mic seat
                IconButton(
                    onClick = onLeaveSeat,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceCard)
                        .testTag("room_leave_seat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AirlineSeatReclineNormal,
                        contentDescription = "Leave Seat",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Listener Raise Hand Button
                IconButton(
                    onClick = onRaiseHand,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NeonViolet)
                        .testTag("room_raise_hand_button")
                ) {
                    Text(text = "✋", fontSize = 18.sp)
                }
            }

            // Fruit Game Quick Shortcut
            IconButton(
                onClick = onOpenGame,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF065F46))
                    .testTag("room_game_shortcut_button")
            ) {
                Text(text = "🎰", fontSize = 18.sp)
            }

            // Mic Battle Quick Shortcut
            IconButton(
                onClick = onOpenPk,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF831843))
                    .testTag("room_pk_shortcut_button")
            ) {
                Text(text = "⚡", fontSize = 18.sp)
            }

            // Gift Button
            IconButton(
                onClick = onOpenGift,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(BrightGold, Color(0xFFD97706)))
                    )
                    .testTag("room_gift_button")
            ) {
                Text(text = "🎁", fontSize = 20.sp)
            }
        }
    }
}
