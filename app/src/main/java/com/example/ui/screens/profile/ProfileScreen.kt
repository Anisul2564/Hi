package com.example.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.MockRepository
import com.example.data.firebase.AuthRepository
import com.example.data.wallet.WalletRepository
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class ProfileMenuItem(
    val title: String,
    val subtitle: String,
    val icon: String,
    val badge: String? = null,
    val screen: Screen? = null,
    val onClick: (() -> Unit)? = null,
    val testTag: String
)

@Composable
fun ProfileScreen(
    user: User = MockRepository.currentUser,
    authRepository: AuthRepository? = null,
    walletRepository: WalletRepository = remember { WalletRepository() },
    onNavigate: (Screen) -> Unit,
    onUpdateProfile: ((String, String, String) -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedProfileTab by remember { mutableStateOf(0) } // 0: Profile, 1: Honor, 2: Moments
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val wallet by walletRepository.observeWallet(user.uid).collectAsState(initial = null)
    val realCoinBalance = wallet?.coinBalance ?: user.coins

    val displayId = user.bdpId.ifEmpty { user.id }

    val menuItems = listOf(
        ProfileMenuItem("Edit Profile", "Change username, bio & avatar", "✏️", null, null, onClick = { showEditProfileDialog = true }, "profile_menu_edit"),
        ProfileMenuItem("Role & Verification", "Apply for Host, Moderator & Official", "🛡️", "Apply", Screen.RoleApplication, null, "profile_menu_role_app"),
        ProfileMenuItem("Wallet", "Coins & Diamond Balance", "💰", String.format("%,d", realCoinBalance), Screen.Wallet, null, "profile_menu_wallet"),
        ProfileMenuItem("Gift History", "Sent & received gift records", "🎁", "Records", Screen.GiftHistory, null, "profile_menu_gift_history"),
        ProfileMenuItem("SVIP System", "Prestige Privileges & Tiers", "👑", "SVIP ${user.svipLevel}", Screen.Svip, null, "profile_menu_svip"),
        ProfileMenuItem("Noble", "Knight to King Aristocracy", "🛡️", "Baron", Screen.Noble, null, "profile_menu_noble"),
        ProfileMenuItem("Level", "XP Progress & Unlocks", "⭐", "Lv.${user.level}", Screen.Level, null, "profile_menu_level"),
        ProfileMenuItem("My Items", "Frames, Vehicles & Rings", "🎒", "13 Items", Screen.MyItems, null, "profile_menu_my_items"),
        ProfileMenuItem("Medals", "Honorary Achievements", "🏅", "7 Won", Screen.Medal, null, "profile_menu_medal"),
        ProfileMenuItem("Leaderboard", "Honor, Charm & Room", "🏆", "Top 3", Screen.Ranking, null, "profile_menu_ranking"),
        ProfileMenuItem("Room Support", "Weekly Agency Metrics", "📊", "Lv.6 Palace", Screen.RoomSupport, null, "profile_menu_room_support"),
        ProfileMenuItem("CP Space", "Couple Love Tree & Rings", "💖", "Active", Screen.Noble, null, "profile_menu_cp"),
        ProfileMenuItem("Relationship", "Besties, Brothers & Confidants", "🤝", null, Screen.Noble, null, "profile_menu_relationship"),
        ProfileMenuItem("Family & Guild", "Dhaka All-Stars Team", "🏰", "Lv.12", Screen.Noble, null, "profile_menu_family"),
        ProfileMenuItem("Guardians", "Top Shielding Supporters", "👼", "3 Active", Screen.Noble, null, "profile_menu_guardians"),
        ProfileMenuItem("Invite Friends", "Earn 5,000 Coins per Referral", "🎁", "Invite", Screen.DailyRewards, null, "profile_menu_invite"),
        if (user.role in listOf(UserRole.ADMIN, UserRole.SUPER_ADMIN, UserRole.ASSISTANT_ADMIN)) {
            ProfileMenuItem("Admin Center", "Governance & Operations Hub", "⚙️", user.role.displayName, Screen.AdminHub, null, "profile_menu_admin")
        } else null,
        ProfileMenuItem("Sign Out", "Log out of BD PARTY account", "🚪", null, null, onClick = onSignOut, "profile_menu_sign_out")
    ).filterNotNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Profile Header with rich cover and glowing avatar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF4A148C),
                                Color(0xFF2E0854),
                                DarkBackground
                            )
                        )
                    )
            ) {
                // Top controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Profile",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoinBalanceChip(coins = realCoinBalance, onClick = { onNavigate(Screen.Wallet) })
                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .testTag("profile_edit_icon")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Avatar and User Info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BdAvatar(
                        seed = user.avatarSeed,
                        name = user.displayName,
                        size = 80.dp,
                        photoUrl = user.photoUrl,
                        hasFrame = true,
                        frameColor = Brush.sweepGradient(listOf(BrightGold, NeonMagenta, LuxuryGold, NeonViolet, BrightGold)),
                        modifier = Modifier
                            .testTag("profile_avatar_header")
                            .clickable { showEditProfileDialog = true }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user.displayName,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (user.isVerified) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Unique Permanent BD PARTY ID with Copy Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x40000000))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(displayId))
                                Toast.makeText(context, "Copied BD PARTY ID: $displayId", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("copy_user_id_button")
                    ) {
                        Text(
                            text = "BDP ID: $displayId",
                            color = BrightGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy ID",
                            tint = NeonViolet,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Badges row (Role, Level, SVIP)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LevelBadge(level = user.level)
                        if (user.svipLevel > 0) {
                            SvipBadge(svipLevel = user.svipLevel)
                        }
                        RoleBadge(role = user.role)
                    }
                }
            }
        }

        // Bio
        item {
            Text(
                text = user.bio,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Stats Row (Friends, Following, Followers)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = DarkSurfaceCard,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill("Friends", user.friendsCount.toString(), "👥")
                    Divider(
                        color = Color(0x22FFFFFF),
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                    )
                    StatPill("Following", user.followingCount.toString(), "⭐")
                    Divider(
                        color = Color(0x22FFFFFF),
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                    )
                    StatPill("Followers", String.format("%,d", user.followersCount), "❤️")
                }
            }
        }

        // Profile Tabs (Profile, Honor, Moments)
        item {
            TabRow(
                selectedTabIndex = selectedProfileTab,
                containerColor = DarkBackground,
                contentColor = NeonViolet,
                indicator = {},
                divider = {},
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                listOf("Profile", "Honor Wall", "Moments").forEachIndexed { index, title ->
                    val isSelected = selectedProfileTab == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) NeonViolet else DarkSurfaceCard)
                            .clickable { selectedProfileTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Menu Items List
        items(menuItems.size) { index ->
            val item = menuItems[index]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag(item.testTag)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (item.onClick != null) {
                            item.onClick.invoke()
                        } else if (item.screen != null) {
                            onNavigate(item.screen)
                        }
                    }
                    .border(0.5.dp, Color(0x22A855F7), RoundedCornerShape(16.dp)),
                color = DarkSurfaceCard,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33A855F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = item.icon, fontSize = 20.sp)
                        }

                        Column {
                            Text(
                                text = item.title,
                                color = if (item.title == "Sign Out") LiveRed else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = item.subtitle,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (item.badge != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33EC4899))
                                    .border(0.5.dp, NeonMagenta, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = item.badge,
                                    color = NeonMagenta,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Upgraded Edit Profile Dialog with Real Photo Picker & Firebase Storage Upload
    if (showEditProfileDialog) {
        var editedUsername by remember(user) { mutableStateOf(user.username.ifEmpty { user.displayName }) }
        var editedBio by remember(user) { mutableStateOf(user.bio) }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var isSaving by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                errorMessage = null
            }
        }

        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    showEditProfileDialog = false
                    selectedImageUri = null
                    errorMessage = null
                }
            },
            containerColor = DarkSurfaceCard,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Edit Profile",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33A855F7))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BD PARTY",
                            color = NeonViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar Image Preview & Picker Trigger
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .testTag("edit_profile_avatar_preview"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Brush.sweepGradient(listOf(BrightGold, NeonMagenta, LuxuryGold, NeonViolet, BrightGold)))
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "New Selected Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                )
                            } else if (user.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = "Current Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    BdAvatar(
                                        seed = user.avatarSeed,
                                        name = user.displayName,
                                        size = 90.dp,
                                        hasFrame = false
                                    )
                                }
                            }
                        }

                        // Camera overlay badge button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BrightGold)
                                .border(2.dp, DarkBackground, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("edit_profile_camera_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Choose Photo",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Photo Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonViolet),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("choose_photo_from_gallery_button")
                        ) {
                            Text(
                                text = if (selectedImageUri != null) "Choose Another Photo 📷" else "Select Photo from Gallery 📷",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedImageUri != null) {
                            TextButton(
                                onClick = { selectedImageUri = null },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Discard", color = LiveRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (selectedImageUri != null) {
                        Text(
                            text = "✓ New photo selected. Tap Save Changes to upload to cloud storage.",
                            color = MintEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Error Message Banner
                    if (errorMessage != null) {
                        Surface(
                            color = Color(0x33EF4444),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LiveRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("⚠️", fontSize = 14.sp)
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Read-Only Identity Card (Strictly Protected Fields)
                    Surface(
                        color = Color(0x331E1535),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x44A855F7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "System Identity (Read-Only 🔒)",
                                    color = BrightGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Protected",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            ReadOnlyFieldRow("Permanent BD PARTY ID:", displayId)
                            ReadOnlyFieldRow("Role & Authorization:", "${user.role.displayName} (${user.role.name})")
                            ReadOnlyFieldRow("Account Coins Balance:", String.format("%,d Coins", user.coins))
                            ReadOnlyFieldRow("Firebase Account UID:", if (user.uid.isNotEmpty()) "${user.uid.take(12)}..." else "N/A")
                            ReadOnlyFieldRow("Verification Badge:", if (user.isVerified) "Verified Official ✓" else "Standard Member")
                        }
                    }

                    // Editable Username Field
                    OutlinedTextField(
                        value = editedUsername,
                        onValueChange = {
                            editedUsername = it
                            if (errorMessage != null) errorMessage = null
                        },
                        label = { Text("Username / Handle (Required)", color = TextSecondary) },
                        placeholder = { Text("Enter your BD handle", color = TextMuted) },
                        isError = editedUsername.trim().isEmpty(),
                        supportingText = {
                            if (editedUsername.trim().isEmpty()) {
                                Text("Username cannot be empty", color = LiveRed, fontSize = 10.sp)
                            } else {
                                Text("Unique handle displayed across voice stages", color = TextMuted, fontSize = 10.sp)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = Color(0x33A855F7),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_username_input")
                    )

                    // Editable Bio Field
                    OutlinedTextField(
                        value = editedBio,
                        onValueChange = {
                            if (it.length <= 200) {
                                editedBio = it
                            }
                        },
                        label = { Text("Personal Bio", color = TextSecondary) },
                        placeholder = { Text("Introduce yourself to the community...", color = TextMuted) },
                        supportingText = {
                            Text("${editedBio.length}/200 characters", color = TextMuted, fontSize = 10.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = Color(0x33A855F7),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_bio_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUsername = editedUsername.trim()
                        if (cleanUsername.isBlank()) {
                            errorMessage = "Username cannot be empty."
                            return@Button
                        }
                        val cleanBio = if (editedBio.length > 200) editedBio.take(200).trim() else editedBio.trim()

                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            if (authRepository != null) {
                                val result = authRepository.saveProfileChanges(
                                    context = context,
                                    username = cleanUsername,
                                    bio = cleanBio,
                                    imageUri = selectedImageUri
                                )
                                isSaving = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Profile updated successfully! 🎉", Toast.LENGTH_SHORT).show()
                                    showEditProfileDialog = false
                                    selectedImageUri = null
                                } else {
                                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to update profile."
                                }
                            } else {
                                onUpdateProfile?.invoke(cleanUsername, cleanBio, "")
                                isSaving = false
                                showEditProfileDialog = false
                                Toast.makeText(context, "Profile saved locally", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isSaving && editedUsername.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrightGold),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("edit_profile_save_button")
                ) {
                    if (isSaving) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Saving...", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isSaving) {
                            showEditProfileDialog = false
                            selectedImageUri = null
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.testTag("edit_profile_cancel_button")
                ) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun ReadOnlyFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 11.sp)
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatPill(label: String, value: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$emoji $value",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
