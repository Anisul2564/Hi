package com.example.ui.screens.admin

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
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
import com.example.data.firebase.AuthRepository
import com.example.data.firebase.FirestoreUserProfile
import com.example.data.wallet.WalletRepository
import com.example.model.FirestoreCoinPackage
import com.example.model.FirestorePaymentRequest
import com.example.model.RoleRequest
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.BdAvatar
import com.example.ui.components.BdTopBar
import com.example.ui.components.RoleBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminSectionScreen(
    sectionTitle: String,
    currentUser: User,
    authRepository: AuthRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isAuthorized = currentUser.role in listOf(
        UserRole.SUPER_ADMIN,
        UserRole.ADMIN,
        UserRole.ASSISTANT_ADMIN
    )

    if (!isAuthorized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Access Denied: Administrative role required.", color = LiveRed, fontSize = 16.sp)
        }
        return
    }

    Scaffold(
        topBar = {
            BdTopBar(
                title = sectionTitle,
                onBackClick = onBack,
                showActions = false
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when (sectionTitle) {
                "Users", "Hosts", "Moderators", "Official Accounts", "Assistant Admins", "Admins" -> {
                    val roleFilter = when (sectionTitle) {
                        "Hosts" -> "HOST"
                        "Moderators" -> "MODERATOR"
                        "Official Accounts" -> "OFFICIAL"
                        "Assistant Admins" -> "ASSISTANT_ADMIN"
                        "Admins" -> "ADMIN"
                        else -> null
                    }
                    AdminUsersView(
                        roleFilter = roleFilter,
                        authRepository = authRepository
                    )
                }
                "Role Requests" -> {
                    AdminRoleRequestsView(
                        authRepository = authRepository,
                        currentUser = currentUser
                    )
                }
                "Payments" -> {
                    AdminPaymentRequestsView(
                        currentUser = currentUser
                    )
                }
                "Coin Packages" -> {
                    AdminCoinPackagesView(
                        currentUser = currentUser
                    )
                }
                else -> {
                    AdminGenericModuleView(sectionTitle = sectionTitle)
                }
            }
        }
    }
}

@Composable
private fun AdminUsersView(
    roleFilter: String?,
    authRepository: AuthRepository
) {
    var searchQuery by remember { mutableStateOf("") }
    var usersList by remember { mutableStateOf<List<FirestoreUserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedUserForDetails by remember { mutableStateOf<FirestoreUserProfile?>(null) }

    LaunchedEffect(roleFilter) {
        isLoading = true
        errorMessage = null
        val result = authRepository.fetchUsers(roleFilter = roleFilter, limit = 50)
        isLoading = false
        if (result.isSuccess) {
            usersList = result.getOrNull() ?: emptyList()
        } else {
            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to fetch users"
        }
    }

    val filteredUsers = remember(searchQuery, usersList) {
        if (searchQuery.isBlank()) usersList
        else {
            val q = searchQuery.trim().lowercase()
            usersList.filter {
                it.username.lowercase().contains(q) || it.bdpId.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by username or BDP ID...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonViolet) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceCard,
                unfocusedContainerColor = DarkSurfaceCard,
                focusedBorderColor = NeonViolet,
                unfocusedBorderColor = Color(0x33A855F7),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("admin_user_search_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Registered Directory (${filteredUsers.size})",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Real Firestore Collection",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrightGold)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = errorMessage ?: "", color = LiveRed, fontSize = 13.sp)
            }
        } else if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "👥", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No users matching query" else "No users registered in this role yet",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real records will display here as accounts register.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredUsers, key = { it.uid }) { profile ->
                    Surface(
                        color = DarkSurfaceCard,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUserForDetails = profile }
                            .testTag("admin_user_card_${profile.bdpId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BdAvatar(
                                    seed = profile.uid.hashCode(),
                                    name = profile.username,
                                    photoUrl = profile.photoUrl,
                                    size = 44.dp
                                )
                                Column {
                                    Text(
                                        text = profile.username.ifEmpty { "Member" },
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "BDP ID: ${profile.bdpId.ifEmpty { "N/A" }}",
                                        color = BrightGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Joined: ${profile.createdAt.take(10)}",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            val roleEnum = try {
                                UserRole.valueOf(profile.role.uppercase())
                            } catch (_: Exception) {
                                UserRole.USER
                            }
                            RoleBadge(role = roleEnum)
                        }
                    }
                }
            }
        }
    }

    // User Details Dialog (Strictly Read-Only System Values)
    if (selectedUserForDetails != null) {
        val user = selectedUserForDetails!!
        AlertDialog(
            onDismissRequest = { selectedUserForDetails = null },
            containerColor = DarkSurfaceCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "User Profile Inspection",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ReadOnlyDetailRow("Username / Handle:", user.username)
                    ReadOnlyDetailRow("Permanent BD PARTY ID:", user.bdpId)
                    ReadOnlyDetailRow("Firebase UID (System):", user.uid)
                    ReadOnlyDetailRow("Role Tier:", user.role)
                    ReadOnlyDetailRow("Account Coins Balance:", String.format("%,d Coins", user.coins))
                    ReadOnlyDetailRow("Personal Bio:", user.bio)
                    ReadOnlyDetailRow("Registered At:", user.createdAt)
                    ReadOnlyDetailRow("Last Profile Update:", user.updatedAt)

                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0x33F59E0B),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔒 Protected Invariant: Role, Coins, and BDP ID cannot be modified client-side.",
                            color = BrightGold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedUserForDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun AdminRoleRequestsView(
    authRepository: AuthRepository,
    currentUser: User
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf("PENDING") }
    val allRequests by authRepository.getAllRoleRequestsFlow().collectAsState(initial = emptyList())

    val filtered = remember(selectedFilter, allRequests) {
        if (selectedFilter == "ALL") allRequests
        else allRequests.filter { it.status == selectedFilter }
    }

    var selectedRequestForAction by remember { mutableStateOf<RoleRequest?>(null) }
    var actionType by remember { mutableStateOf<String>("APPROVE") } // APPROVE or REJECT
    var reviewNoteInput by remember { mutableStateOf("") }
    var isContractExecuting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("PENDING", "APPROVED", "REJECTED", "CANCELLED", "ALL")) { filter ->
                val isSelected = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NeonViolet else DarkSurfaceCard)
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "Active Submissions (${filtered.size})",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📋", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No applications under '$selectedFilter'",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Live submissions in Firestore will appear here.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.requestId }) { req ->
                    AdminRoleRequestCard(
                        request = req,
                        onApprove = {
                            selectedRequestForAction = req
                            actionType = "APPROVE"
                            reviewNoteInput = ""
                        },
                        onReject = {
                            selectedRequestForAction = req
                            actionType = "REJECT"
                            reviewNoteInput = ""
                        }
                    )
                }
            }
        }
    }

    // Backend Contract Dialog for Review Actions
    if (selectedRequestForAction != null) {
        val req = selectedRequestForAction!!
        AlertDialog(
            onDismissRequest = { if (!isContractExecuting) selectedRequestForAction = null },
            containerColor = DarkSurfaceCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (actionType == "APPROVE") "Approve Role Application Contract" else "Reject Role Application",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Applicant: ${req.requesterUsername} (${req.requesterBdpId})\nTarget Role: ${req.requestedRole}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = reviewNoteInput,
                        onValueChange = { reviewNoteInput = it },
                        label = { Text("Reviewer Note", color = TextSecondary) },
                        placeholder = { Text("Enter formal review rationale...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = Color(0x33A855F7),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        color = Color(0x3310B981),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ Backend Security Contract: Final server-side user promotion is executed by trusted Firebase Cloud Functions / Admin SDK.",
                            color = MintEmerald,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isContractExecuting = true
                        coroutineScope.launch {
                            val res = if (actionType == "APPROVE") {
                                authRepository.executeRoleApprovalContract(req.requestId, reviewNoteInput)
                            } else {
                                authRepository.executeRoleRejectionContract(req.requestId, reviewNoteInput)
                            }
                            isContractExecuting = false
                            selectedRequestForAction = null
                            if (res.isSuccess) {
                                Toast.makeText(context, res.getOrNull() ?: "Contract verified", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "Failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == "APPROVE") MintEmerald else LiveRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isContractExecuting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text(if (actionType == "APPROVE") "Execute Approval" else "Execute Rejection", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isContractExecuting) selectedRequestForAction = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun AdminRoleRequestCard(
    request: RoleRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = request.requesterUsername,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "BDP ID: ${request.requesterBdpId} • Current: ${request.currentRole}",
                        color = BrightGold,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33A855F7))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Target: ${request.requestedRole}",
                        color = NeonViolet,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Reason: ${request.reason}", color = TextSecondary, fontSize = 12.sp)

            if (request.experience.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Experience: ${request.experience}", color = TextMuted, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Submitted: ${request.createdAt.take(16)}",
                    color = TextMuted,
                    fontSize = 10.sp
                )

                if (request.status == "PENDING") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onReject,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Reject", color = LiveRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Approve", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "Status: ${request.status}",
                        color = if (request.status == "APPROVED") MintEmerald else LiveRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminGenericModuleView(sectionTitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x33A855F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonViolet,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "$sectionTitle Module",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "No records found in '$sectionTitle'.\nThis administrative collection is awaiting live event ingestion.",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ReadOnlyDetailRow(label: String, value: String) {
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
private fun AdminPaymentRequestsView(
    currentUser: User,
    walletRepository: WalletRepository = remember { WalletRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("PENDING") } // ALL, PENDING, APPROVED, REJECTED
    val filters = listOf("PENDING", "APPROVED", "REJECTED", "ALL")

    val allRequests by walletRepository.observeAllPaymentRequests().collectAsState(initial = emptyList())

    val filtered = remember(allRequests, selectedFilter) {
        if (selectedFilter == "ALL") allRequests
        else allRequests.filter { it.status.equals(selectedFilter, ignoreCase = true) }
    }

    var selectedRequestForAction by remember { mutableStateOf<FirestorePaymentRequest?>(null) }
    var actionType by remember { mutableStateOf("APPROVE") } // APPROVE or REJECT
    var reviewNoteInput by remember { mutableStateOf("") }
    var isContractExecuting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BrightGold else DarkSurfaceCard)
                        .border(1.dp, if (isSelected) BrightGold else Color(0x33A855F7), RoundedCornerShape(12.dp))
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "Recharge Submissions (${filtered.size})",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "💳", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No payments under '$selectedFilter'",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "User bKash, Nagad & Rocket payment requests will appear here.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.requestId }) { req ->
                    Surface(
                        color = DarkSurfaceCard,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_payment_card_${req.requestId}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (req.paymentMethod) {
                                                    "bKash" -> Color(0x33E2136E)
                                                    "Nagad" -> Color(0x33F7941D)
                                                    else -> Color(0x338C3494)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = req.paymentMethod,
                                            color = when (req.paymentMethod) {
                                                "bKash" -> Color(0xFFFF4081)
                                                "Nagad" -> Color(0xFFFFB74D)
                                                else -> Color(0xFFBA68C8)
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = req.amount,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• +${String.format("%,d", req.coinAmount)} 🪙",
                                        color = BrightGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
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

                            Divider(color = Color(0x1AFFFFFF))

                            ReadOnlyDetailRow("Requester", "${req.username} (ID: ${req.bdpId})")
                            ReadOnlyDetailRow("TrxID", req.transactionId)
                            ReadOnlyDetailRow("Sender Mobile", req.senderNumber)
                            ReadOnlyDetailRow("Created At", req.createdAt)

                            if (!req.reviewNote.isNullOrBlank()) {
                                ReadOnlyDetailRow("Review Note", req.reviewNote)
                            }
                            if (!req.reviewedBy.isNullOrBlank()) {
                                ReadOnlyDetailRow("Reviewed By", req.reviewedBy)
                            }

                            if (req.status == "PENDING") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedRequestForAction = req
                                            actionType = "REJECT"
                                            reviewNoteInput = ""
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            selectedRequestForAction = req
                                            actionType = "APPROVE"
                                            reviewNoteInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Approve", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedRequestForAction != null) {
        val req = selectedRequestForAction!!
        AlertDialog(
            onDismissRequest = { if (!isContractExecuting) selectedRequestForAction = null },
            containerColor = DarkSurfaceCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (actionType == "APPROVE") "Approve Payment Request" else "Reject Payment Request",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Applicant: ${req.username} (${req.bdpId})\nAmount: ${req.amount} (${req.coinAmount} Coins)\nMethod: ${req.paymentMethod} • TrxID: ${req.transactionId}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = reviewNoteInput,
                        onValueChange = { reviewNoteInput = it },
                        label = { Text("Reviewer Note", color = TextSecondary) },
                        placeholder = { Text("Enter payment verification note...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = Color(0x33A855F7),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isContractExecuting = true
                        coroutineScope.launch {
                            val newStatus = if (actionType == "APPROVE") "APPROVED" else "REJECTED"
                            val res = walletRepository.reviewPaymentRequest(
                                requestId = req.requestId,
                                newStatus = newStatus,
                                reviewerBdpId = currentUser.bdpId,
                                reviewerRole = currentUser.role.name,
                                reviewNote = reviewNoteInput
                            )
                            isContractExecuting = false
                            selectedRequestForAction = null
                            if (res.isSuccess) {
                                Toast.makeText(context, "Payment marked as $newStatus", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Operation failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isContractExecuting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == "APPROVE") MintEmerald else LiveRed
                    )
                ) {
                    Text(if (actionType == "APPROVE") "Confirm Approval" else "Confirm Rejection", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedRequestForAction = null },
                    enabled = !isContractExecuting
                ) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun AdminCoinPackagesView(
    currentUser: User,
    walletRepository: WalletRepository = remember { WalletRepository() }
) {
    val packages by walletRepository.observeCoinPackages().collectAsState(initial = walletRepository.defaultCoinPackages())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "🪙 Active Coin Packages (${packages.size})",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Virtual coin recharge bundles displayed in the BD PARTY Virtual Wallet.",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        items(packages, key = { it.packageId }) { pkg ->
            Surface(
                color = DarkSurfaceCard,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "🪙", fontSize = 28.sp)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = pkg.name,
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
                                text = "${String.format("%,d", pkg.coinAmount)} Coins • ${pkg.price}",
                                color = BrightGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3310B981))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Active", color = MintEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
