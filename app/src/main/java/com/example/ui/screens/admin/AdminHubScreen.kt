package com.example.ui.screens.admin

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.AuthRepository
import com.example.model.RolePermissions
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.BdAvatar
import com.example.ui.components.BdTopBar
import com.example.ui.components.RoleBadge
import com.example.ui.navigation.Screen
import com.example.ui.theme.*

data class AdminModule(
    val title: String,
    val description: String,
    val icon: String,
    val superAdminOnly: Boolean = false,
    val testTag: String
)

@Composable
fun AdminHubScreen(
    currentUser: User,
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val isAuthorized = currentUser.role in listOf(
        UserRole.SUPER_ADMIN,
        UserRole.ADMIN,
        UserRole.ASSISTANT_ADMIN
    )

    // Access Gate: If not authorized, show zero-trust access denied screen
    if (!isAuthorized) {
        UnauthorizedAdminGate(currentUser = currentUser, onBack = onBack)
        return
    }

    var totalUsers by remember { mutableStateOf<String>("Loading...") }
    var pendingRoleRequests by remember { mutableStateOf<String>("Loading...") }
    var totalHosts by remember { mutableStateOf<String>("Loading...") }
    var totalMods by remember { mutableStateOf<String>("Loading...") }
    var totalOfficials by remember { mutableStateOf<String>("Loading...") }

    LaunchedEffect(currentUser.uid) {
        val usersRes = authRepository.fetchTotalUsersCount()
        totalUsers = usersRes.getOrNull()?.let { String.format("%,d", it) } ?: "Not available yet"

        val reqsRes = authRepository.fetchPendingRoleRequestsCount()
        pendingRoleRequests = reqsRes.getOrNull()?.let { "$it Pending" } ?: "Not available yet"

        val hostsRes = authRepository.fetchRoleCount("HOST")
        totalHosts = hostsRes.getOrNull()?.let { "$it Active" } ?: "Not available yet"

        val modsRes = authRepository.fetchRoleCount("MODERATOR")
        totalMods = modsRes.getOrNull()?.let { "$it Active" } ?: "Not available yet"

        val officialsRes = authRepository.fetchRoleCount("OFFICIAL")
        totalOfficials = officialsRes.getOrNull()?.let { "$it Accounts" } ?: "Not available yet"
    }

    val adminModules = remember(currentUser.role) {
        listOf(
            AdminModule("Users", "Registered member directory & lookup", "👥", false, "admin_users"),
            AdminModule("Role Requests", "Review applications for Host, Mod & Admin", "📋", false, "admin_role_requests"),
            AdminModule("Hosts", "Stage hosts verification & quotas", "🎙️", false, "admin_hosts"),
            AdminModule("Moderators", "Community patrol officers & safety logs", "🛡️", false, "admin_mods"),
            AdminModule("Official Accounts", "Partner, artist & celebrity accounts", "✨", false, "admin_officials"),
            AdminModule("Assistant Admins", "Operations staff & support members", "👑", currentUser.role != UserRole.SUPER_ADMIN, "admin_assistant_admins"),
            AdminModule("Admins", "Platform administrators management", "⚡", true, "admin_admins"),
            AdminModule("Rooms", "Active stages, categories & mic slots", "🏛️", false, "admin_rooms"),
            AdminModule("Reports", "Harassment flags, user bans & moderation audit", "🚨", false, "admin_reports"),
            AdminModule("Payments", "Transaction reconciliations & receipts", "💳", false, "admin_payments"),
            AdminModule("Wallet", "Virtual wallets, balance adjustments", "💰", false, "admin_wallet"),
            AdminModule("Coin Packages", "Virtual coin bundle definitions", "🪙", false, "admin_coin_packages"),
            AdminModule("Gifts", "Virtual gifts catalog, animations & prices", "🎁", false, "admin_gifts"),
            AdminModule("VIP", "VIP & SVIP tier privilege definitions", "💎", false, "admin_vip"),
            AdminModule("Announcements", "Broadcast notices to BD PARTY members", "📢", false, "admin_announcements"),
            AdminModule("Security", "Custom claims audit, threat monitoring", "🔒", true, "admin_security"),
            AdminModule("Audit Logs", "Immutable administrative trace records", "📜", true, "admin_audit_logs"),
            AdminModule("Settings", "System configuration & platform thresholds", "⚙️", true, "admin_settings")
        )
    }

    val permissions = remember(currentUser.role) {
        RolePermissions.getPermissionsForRole(currentUser.role)
    }

    Scaffold(
        topBar = {
            BdTopBar(
                title = "Admin Center",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Admin Profile & Authority Card
            item {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Brush.horizontalGradient(listOf(BrightGold, NeonViolet))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BdAvatar(
                                seed = currentUser.avatarSeed,
                                name = currentUser.displayName,
                                size = 52.dp,
                                photoUrl = currentUser.photoUrl,
                                hasFrame = true,
                                frameColor = Brush.sweepGradient(listOf(BrightGold, NeonMagenta, LuxuryGold, BrightGold))
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentUser.displayName.ifEmpty { currentUser.username },
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    RoleBadge(role = currentUser.role)
                                }
                                Text(
                                    text = "Permanent ID: ${currentUser.bdpId.ifEmpty { currentUser.id }}",
                                    color = BrightGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Active Permissions: ${permissions.size} authorized modules",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Real Dashboard Metrics Overview
            item {
                Text(
                    text = "📊 Real-Time Operations Overview",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RealMetricCard("Total Users", totalUsers, "👥", Modifier.weight(1f))
                        RealMetricCard("Pending Requests", pendingRoleRequests, "📋", Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RealMetricCard("Total Hosts", totalHosts, "🎙️", Modifier.weight(1f))
                        RealMetricCard("Total Moderators", totalMods, "⚡", Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RealMetricCard("Official Accounts", totalOfficials, "✨", Modifier.weight(1f))
                        RealMetricCard("Online Users", "Not available yet", "🟢", Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RealMetricCard("Active Rooms", "Not available yet", "🏛️", Modifier.weight(1f))
                        RealMetricCard("Pending Reports", "Not available yet", "🚨", Modifier.weight(1f))
                    }
                }
            }

            // Management Navigation Modules
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🛠️ Administrative Modules",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(adminModules, key = { it.title }) { module ->
                val isLocked = module.superAdminOnly && currentUser.role != UserRole.SUPER_ADMIN
                Surface(
                    color = if (isLocked) Color(0x221E1535) else DarkSurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (isLocked) Color(0x22FFFFFF) else Color(0x33A855F7)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLocked) {
                            onNavigate(Screen.AdminSection(module.title))
                        }
                        .testTag(module.testTag)
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
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x33A855F7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = module.icon, fontSize = 18.sp)
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = module.title,
                                        color = if (isLocked) TextMuted else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (module.superAdminOnly) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x33F59E0B))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "SUPER ADMIN",
                                                color = BrightGold,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = module.description,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Restricted",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open",
                                tint = NeonViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RealMetricCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = TextMuted, fontSize = 11.sp)
                Text(text = icon, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = if (value == "Not available yet") TextMuted else Color.White,
                fontSize = if (value == "Not available yet") 11.sp else 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun UnauthorizedAdminGate(currentUser: User, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            BdTopBar(title = "Access Restricted", onBackClick = onBack, showActions = false)
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0x33EF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Access Denied",
                        tint = LiveRed,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    text = "Admin Center Access Denied",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "This area is restricted to authorized personnel holding ASSISTANT_ADMIN, ADMIN, or SUPER_ADMIN roles.\n\nYour current account role is: ${currentUser.role.displayName}.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Return to BD PARTY", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
