package com.example.ui.screens.profile_sub

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.firebase.AuthRepository
import com.example.model.RoleRequest
import com.example.model.User
import com.example.ui.components.BdTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RoleApplicationScreen(
    user: User,
    authRepository: AuthRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: Apply, 1: My Applications, 2: Requirements
    val availableRoles = remember { listOf("HOST", "MODERATOR", "OFFICIAL", "ASSISTANT_ADMIN", "ADMIN") }
    var selectedRole by remember { mutableStateOf("HOST") }

    var reason by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var additionalInfo by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val myRequests by authRepository.getUserRoleRequestsFlow().collectAsState(initial = emptyList())
    val hasPendingForSelectedRole = myRequests.any { it.requestedRole == selectedRole && it.status == "PENDING" }

    Scaffold(
        topBar = {
            BdTopBar(
                title = "Role & Verification",
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
            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBackground,
                contentColor = NeonViolet,
                indicator = {},
                divider = {},
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                listOf("Apply for Role", "My Applications (${myRequests.size})", "Requirements").forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) NeonViolet else DarkSurfaceCard)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // Application Form
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
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
                                        text = "Applicant Credentials",
                                        color = BrightGold,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ReadOnlyCredentialRow("Applicant:", user.displayName.ifEmpty { user.username })
                                    ReadOnlyCredentialRow("Permanent BDP ID:", user.bdpId.ifEmpty { user.id })
                                    ReadOnlyCredentialRow("Current Role:", user.role.displayName)
                                }
                            }
                        }

                        // Target Role Selection
                        item {
                            Text(
                                text = "Select Target Role:",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableRoles.forEach { role ->
                                    val isSelected = role == selectedRole
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) NeonMagenta else DarkSurfaceCard)
                                            .border(1.dp, if (isSelected) BrightGold else Color(0x33A855F7), RoundedCornerShape(12.dp))
                                        .clickable { selectedRole = role }
                                        .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = role,
                                            color = if (isSelected) Color.White else TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Duplicate Application Warning Banner
                        item {
                            AnimatedVisibility(visible = hasPendingForSelectedRole) {
                                Surface(
                                    color = Color(0x33F59E0B),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BrightGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = "⏳", fontSize = 18.sp)
                                        Text(
                                            text = "You already have a Pending application for $selectedRole. Duplicate submissions are protected.",
                                            color = BrightGold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Input fields
                        item {
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { if (it.length <= 500) reason = it },
                                label = { Text("Reason for Application (Required)", color = TextSecondary) },
                                placeholder = { Text("Why do you want to become a $selectedRole on BD PARTY?", color = TextMuted) },
                                supportingText = { Text("${reason.length}/500 characters", color = TextMuted, fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceCard,
                                    unfocusedContainerColor = DarkSurfaceCard,
                                    focusedBorderColor = NeonViolet,
                                    unfocusedBorderColor = Color(0x33A855F7),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("role_app_reason_input")
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = experience,
                                onValueChange = { if (it.length <= 500) experience = it },
                                label = { Text("Prior Experience & Stage Hours", color = TextSecondary) },
                                placeholder = { Text("E.g. Audio hosting, community moderation, DJ experience...", color = TextMuted) },
                                supportingText = { Text("${experience.length}/500 characters", color = TextMuted, fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceCard,
                                    unfocusedContainerColor = DarkSurfaceCard,
                                    focusedBorderColor = NeonViolet,
                                    unfocusedBorderColor = Color(0x33A855F7),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("role_app_experience_input")
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = additionalInfo,
                                onValueChange = { if (it.length <= 500) additionalInfo = it },
                                label = { Text("Additional Information (Optional)", color = TextSecondary) },
                                placeholder = { Text("Social links, portfolio, or references...", color = TextMuted) },
                                supportingText = { Text("${additionalInfo.length}/500 characters", color = TextMuted, fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceCard,
                                    unfocusedContainerColor = DarkSurfaceCard,
                                    focusedBorderColor = NeonViolet,
                                    unfocusedBorderColor = Color(0x33A855F7),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Submit Button
                        item {
                            Button(
                                onClick = {
                                    if (reason.isBlank()) {
                                        Toast.makeText(context, "Please provide a reason for application", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (hasPendingForSelectedRole) {
                                        Toast.makeText(context, "A pending request for $selectedRole is already active", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    isSubmitting = true
                                    coroutineScope.launch {
                                        val result = authRepository.submitRoleRequest(
                                            requestedRole = selectedRole,
                                            reason = reason,
                                            experience = experience,
                                            additionalInfo = additionalInfo
                                        )
                                        isSubmitting = false
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "Application submitted successfully", Toast.LENGTH_LONG).show()
                                            reason = ""
                                            experience = ""
                                            additionalInfo = ""
                                            selectedTab = 1 // Switch to My Applications view
                                        } else {
                                            Toast.makeText(
                                                context,
                                                result.exceptionOrNull()?.localizedMessage ?: "Submission failed",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                enabled = !isSubmitting && !hasPendingForSelectedRole,
                                colors = ButtonDefaults.buttonColors(containerColor = BrightGold),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("submit_role_application_button")
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = if (hasPendingForSelectedRole) "Application Already Pending" else "Submit Application 🛡️",
                                        color = Color.Black,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // My Applications List
                    if (myRequests.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📋", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No applications submitted yet",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Submit a request to become a Host, Moderator, or Official",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(myRequests, key = { it.requestId }) { req ->
                                ApplicationStatusCard(
                                    request = req,
                                    onCancel = { reqId ->
                                        coroutineScope.launch {
                                            val res = authRepository.cancelRoleRequest(reqId)
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Application cancelled successfully.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    res.exceptionOrNull()?.localizedMessage ?: "Failed to cancel application",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Requirements & Information
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            RequirementCard(
                                roleName = "🎙️ HOST",
                                requirement = "Requirements: Active voice room presence (min. 10 hours/week), good sound setup, adherence to community safety standards.\nPerks: 10-Mic stage capacity, host ranking rewards, discovery boost."
                            )
                        }
                        item {
                            RequirementCard(
                                roleName = "⚡ MODERATOR",
                                requirement = "Requirements: High trust score, zero violation history, active community patrol capability.\nPerks: Room mute/kick permissions, dedicated security channel, moderator badge."
                            )
                        }
                        item {
                            RequirementCard(
                                roleName = "🛡️ OFFICIAL",
                                requirement = "Requirements: Identity verification, recognized artist, partner or content creator.\nPerks: Verified checkmark, server entrance banner, featured stage priority."
                            )
                        }
                        item {
                            RequirementCard(
                                roleName = "👑 ASSISTANT_ADMIN / ADMIN",
                                requirement = "Requirements: Community leadership experience, direct operations training, zero self-promotion.\nPerks: Platform governance, event coordination, administrative channel access."
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyCredentialRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ApplicationStatusCard(
    request: RoleRequest,
    onCancel: ((String) -> Unit)? = null
) {
    val (statusColor, statusIcon) = when (request.status) {
        "APPROVED" -> MintEmerald to "✓ APPROVED"
        "REJECTED" -> LiveRed to "✕ REJECTED"
        "CANCELLED" -> TextMuted to "⊘ CANCELLED"
        else -> BrightGold to "⏳ PENDING"
    }

    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Role: ${request.requestedRole}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusIcon,
                        color = statusColor,
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

            if (!request.additionalInfo.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Additional Info: ${request.additionalInfo}", color = TextMuted, fontSize = 11.sp)
            }

            if (!request.reviewedAt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reviewed on: ${request.reviewedAt} ${if (!request.reviewerRole.isNullOrBlank()) "by ${request.reviewerRole}" else ""}",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!request.reviewNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33000000))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Reviewer Note: ${request.reviewNote}",
                        color = BrightGold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Submitted: ${request.createdAt}",
                    color = TextMuted,
                    fontSize = 10.sp
                )

                if (request.status == "PENDING" && onCancel != null) {
                    OutlinedButton(
                        onClick = { onCancel(request.requestId) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LiveRed.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(text = "Cancel", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RequirementCard(roleName: String, requirement: String) {
    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33A855F7)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = roleName, color = BrightGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = requirement, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
