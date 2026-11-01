package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.BrightGold
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Data model representing a live gift animation event triggered exclusively
 * by genuine gift transactions.
 */
data class GiftAnimationEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val giftName: String,
    val giftIcon: String = "🎁",
    val senderName: String,
    val senderBdpId: String = "",
    val receiverName: String,
    val receiverBdpId: String = "",
    val quantity: Int = 1,
    val totalCoins: Long = 0L
)

/**
 * Production-ready, reusable Gift Animation Overlay for BD PARTY voice rooms.
 *
 * Features:
 * - Smooth spring-based entrance and fluid slide/fade exit.
 * - Displays gift icon/image, sender info, receiver info, gift name, quantity multiplier, and coin value.
 * - Non-freezing, lightweight Compose animations with automatic lifecycle resource cleanup.
 * - Graceful fallback for missing images/URLs to standard emojis.
 * - Supports sequential/repeated gifts through lifecycle queue dispatch.
 */
@Composable
fun GiftAnimationOverlay(
    activeEvent: GiftAnimationEvent?,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(activeEvent?.eventId) {
        if (activeEvent != null) {
            isVisible = true
            // Display duration for the gift celebration
            delay(2800L)
            isVisible = false
            // Wait for exit transition to finish before releasing event
            delay(350L)
            onAnimationEnd()
        } else {
            isVisible = false
        }
    }

    // Infinite pulsing effect for the gift badge
    val infiniteTransition = rememberInfiniteTransition(label = "gift_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AnimatedVisibility(
        visible = isVisible && activeEvent != null,
        enter = fadeIn(animationSpec = tween(250)) +
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) +
                scaleIn(initialScale = 0.85f),
        exit = fadeOut(animationSpec = tween(300)) +
                slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = tween(300, easing = FastOutLinearInEasing)
                ) +
                scaleOut(targetScale = 1.05f),
        modifier = modifier
    ) {
        if (activeEvent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("gift_animation_overlay_${activeEvent.eventId}")
                    .clickable {
                        // Allow user to tap and dismiss if desired
                        isVisible = false
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing card
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .shadow(16.dp, shape = RoundedCornerShape(22.dp), spotColor = NeonMagenta)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF2E0854),
                                    Color(0xFF4A0E4E),
                                    Color(0xFF1E0638)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                listOf(BrightGold, NeonMagenta, BrightGold)
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Gift Icon / Image with animated pulse
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0x66EC4899), Color(0x33A855F7), Color.Transparent)
                                    )
                                )
                                .border(1.5.dp, BrightGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeEvent.giftIcon.startsWith("http://") || activeEvent.giftIcon.startsWith("https://")) {
                                AsyncImage(
                                    model = activeEvent.giftIcon,
                                    contentDescription = activeEvent.giftName,
                                    modifier = Modifier.size(38.dp)
                                )
                            } else {
                                Text(
                                    text = activeEvent.giftIcon.ifBlank { "🎁" },
                                    fontSize = 32.sp
                                )
                            }
                        }

                        // Middle details: Sender, Receiver, Gift Name & Quantity
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Gift Name + Quantity Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = activeEvent.giftName,
                                    color = BrightGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonMagenta)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "x${activeEvent.quantity}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Sender & Receiver lines
                            Text(
                                text = "From: ${activeEvent.senderName}${if (activeEvent.senderBdpId.isNotBlank()) " (${activeEvent.senderBdpId})" else ""}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "To: ${activeEvent.receiverName}${if (activeEvent.receiverBdpId.isNotBlank()) " (${activeEvent.receiverBdpId})" else ""}",
                                color = NeonViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Total Coins Badge
                        if (activeEvent.totalCoins > 0) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x44000000))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(text = "🪙", fontSize = 10.sp)
                                    Text(
                                        text = String.format("%,d", activeEvent.totalCoins),
                                        color = BrightGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
