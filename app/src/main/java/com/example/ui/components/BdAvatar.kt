package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*

/**
 * Premium Circular BD PARTY Avatar Component.
 *
 * Guaranteed real portrait photo rendering using 10 realistic local drawables,
 * with smooth breathing live-status rings, speaking indicator animations,
 * and high-end VIP frame gradients.
 */
@Composable
fun BdAvatar(
    seed: Int,
    name: String,
    size: Dp = 52.dp,
    photoUrl: String? = null,
    hasFrame: Boolean = false,
    isOnline: Boolean = false,
    isLive: Boolean = false,
    frameColor: Brush? = null,
    modifier: Modifier = Modifier
) {
    // Smooth live animation transitions
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_live_transition")

    val liveRingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_ring_scale"
    )

    val liveRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_ring_alpha"
    )

    val liveBadgePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_badge_pulse"
    )

    val defaultFrameBrush = Brush.sweepGradient(
        listOf(LuxuryGold, NeonMagenta, NeonViolet, BrightGold, ElectricCyan, LuxuryGold)
    )

    val liveFrameBrush = Brush.sweepGradient(
        listOf(NeonMagenta, NeonViolet, ElectricCyan, BrightGold, NeonMagenta)
    )

    val outerSize = if (hasFrame || isLive) size + 8.dp else size
    val localDrawableRes = BdAssets.getAvatarDrawable(seed, name)
    val hasValidRemoteUrl = !photoUrl.isNullOrBlank() && (
        photoUrl.startsWith("http://") ||
        photoUrl.startsWith("https://") ||
        photoUrl.startsWith("content://") ||
        photoUrl.startsWith("file://") ||
        photoUrl.startsWith("android.resource://") ||
        photoUrl.startsWith("/")
    )

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing / Animated Live or VIP Frame
        if (hasFrame || isLive) {
            Box(
                modifier = Modifier
                    .size(outerSize)
                    .scale(if (isLive) liveRingScale else 1f)
                    .alpha(if (isLive) liveRingAlpha else 1f)
                    .clip(CircleShape)
                    .background(frameColor ?: if (isLive) liveFrameBrush else defaultFrameBrush)
                    .padding(2.5.dp)
            )
        }

        // Inner Avatar Circle - Real Portrait Image
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(DarkSurfaceCard)
                .border(
                    1.5.dp,
                    if (hasFrame || isLive) DarkBackground else Color(0x33FFFFFF),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (hasValidRemoteUrl) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .placeholder(localDrawableRes)
                        .error(localDrawableRes)
                        .build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(id = localDrawableRes),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }

        // Live badge or Online status pip
        if (isLive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .scale(liveBadgePulse)
                    .clip(RoundedCornerShape(6.dp))
                    .background(LiveRed)
                    .border(1.dp, Color.White, RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(1.5.dp, DarkBackground, CircleShape)
            )
        }
    }
}
