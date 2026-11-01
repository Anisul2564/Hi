package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// BD PARTY Dark Luxury Palette - Deep Navy / Near-Black Backgrounds
val DarkBackground = Color(0xFF080612)
val DarkBackgroundElevated = Color(0xFF0E0A1E)
val DarkSurface = Color(0xFF120C24)
val DarkSurfaceVariant = Color(0xFF191130)
val DarkSurfaceCard = Color(0xFF161028)
val DarkSurfaceCardElevated = Color(0xFF20173A)
val DarkSurfaceGlass = Color(0xD9161028)
val DarkSurfaceGlassLight = Color(0x6622173E)

// Primary & Accent Palette
val NeonViolet = Color(0xFFA855F7)
val DeepViolet = Color(0xFF7E22CE)
val UltraViolet = Color(0xFF6366F1)
val NeonMagenta = Color(0xFFEC4899)
val HotPink = Color(0xFFF43F5E)
val ElectricCyan = Color(0xFF06B6D4)
val CyberCyan = Color(0xFF06B6D4)
val CyberTeal = Color(0xFF14B8A6)
val ElectricBlue = Color(0xFF3B82F6)

// Gold Luxury Accents
val LuxuryGold = Color(0xFFFFB703)
val BrightGold = Color(0xFFFFD166)
val GoldDark = Color(0xFFD48B00)
val GoldLight = Color(0xFFFFF3B0)

// Status & Indicators
val MintEmerald = Color(0xFF10B981)
val LiveRed = Color(0xFFEF4444)
val LiveCrimson = Color(0xFFDC2626)
val OnlineGreen = Color(0xFF22C55E)

// High-Legibility Typography Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFD4C8EB)
val TextMuted = Color(0xFF9687B3)
val TextSubtle = Color(0xFF6B5C85)
val TextGold = Color(0xFFFFE082)

// Premium Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(NeonViolet, NeonMagenta)
)

val BrandGradientHorizontal = Brush.horizontalGradient(
    colors = listOf(Color(0xFF8B5CF6), Color(0xFFD946EF), Color(0xFF06B6D4))
)

val AccentGradient = Brush.linearGradient(
    colors = listOf(NeonMagenta, NeonViolet, ElectricCyan)
)

val GoldGradient = Brush.horizontalGradient(
    colors = listOf(BrightGold, LuxuryGold, GoldDark)
)

val LivePulseGradient = Brush.horizontalGradient(
    colors = listOf(LiveRed, HotPink)
)

val CardGlowGradient = Brush.linearGradient(
    colors = listOf(Color(0x33A855F7), Color(0x1AEC4899), Color(0x00000000))
)

val CardSubtleBorder = Brush.linearGradient(
    colors = listOf(Color(0x40A855F7), Color(0x20EC4899), Color(0x0A06B6D4))
)

val FeaturedStageGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF24103E), Color(0xFF130922), DarkBackground)
)

val RoomStageGradient = FeaturedStageGradient

val RoomCardCoverGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF261845), Color(0xFF150D27))
)

val GameGreenGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF064E3B), Color(0xFF047857))
)

val PKBattleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF881337), Color(0xFFBE185D))
)

val RewardsGoldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF78350F), Color(0xFFB45309))
)

val RankingsPurpleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4C1D95), Color(0xFF6D28D9))
)
