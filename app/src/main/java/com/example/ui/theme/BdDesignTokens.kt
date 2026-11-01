package com.example.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * App-wide design tokens for BD PARTY.
 * Provides unified spacing, corner radiuses, and animation specs.
 */
object BdSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val huge = 32.dp
}

object BdShapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(14.dp)
    val large = RoundedCornerShape(18.dp)
    val card = RoundedCornerShape(22.dp)
    val pill = RoundedCornerShape(999.dp)
}

object BdMotion {
    val fastDurationMs = 200
    val normalDurationMs = 300
    val slowDurationMs = 500

    fun <T> responsiveSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> standardTween(durationMillis: Int = normalDurationMs) = tween<T>(durationMillis)
}
