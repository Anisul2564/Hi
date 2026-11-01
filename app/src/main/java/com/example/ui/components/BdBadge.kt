package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun RoleBadge(
    role: UserRole,
    modifier: Modifier = Modifier
) {
    val (gradient, label, icon) = when (role) {
        UserRole.OFFICIAL -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))),
            "OFFICIAL",
            "🛡️"
        )
        UserRole.HOST -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
            "HOST",
            "🎙️"
        )
        UserRole.MODERATOR -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF0284C7))),
            "MOD",
            "⚡"
        )
        UserRole.ASSISTANT_ADMIN -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFFBE185D))),
            "ASST ADMIN",
            "💠"
        )
        UserRole.ADMIN -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFF9F1239))),
            "ADMIN",
            "👑"
        )
        UserRole.SUPER_ADMIN -> Triple(
            Brush.horizontalGradient(listOf(LuxuryGold, BrightGold)),
            "SUPER ADMIN",
            "🌟"
        )
        UserRole.USER -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF4B5563), Color(0xFF374151))),
            "USER",
            "👤"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(gradient)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$icon $label",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LevelBadge(
    level: Int,
    modifier: Modifier = Modifier
) {
    val (bgColors, borderCol) = when {
        level >= 80 -> listOf(Color(0xFFFF007A), Color(0xFF7928CA)) to BrightGold
        level >= 60 -> listOf(Color(0xFF38BDF8), Color(0xFF0284C7)) to Color.White
        level >= 40 -> listOf(Color(0xFFF59E0B), Color(0xFFD97706)) to BrightGold
        level >= 20 -> listOf(Color(0xFF94A3B8), Color(0xFF64748B)) to Color.LightGray
        else -> listOf(Color(0xFFB45309), Color(0xFF78350F)) to Color(0xFFFDE68A)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(bgColors))
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Lv.$level",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun SvipBadge(
    svipLevel: Int,
    modifier: Modifier = Modifier
) {
    if (svipLevel <= 0) return

    val gradient = when (svipLevel) {
        5 -> Brush.horizontalGradient(listOf(Color(0xFFFF0080), Color(0xFF7928CA), BrightGold))
        4 -> Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF38BDF8)))
        3 -> Brush.horizontalGradient(listOf(BrightGold, LuxuryGold, Color(0xFFB45309)))
        2 -> Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF64748B)))
        else -> Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFF92400E)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(gradient)
            .border(1.dp, Color(0x80FFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SVIP $svipLevel",
            color = if (svipLevel == 3 || svipLevel == 4) Color.Black else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun RankMedalBadge(
    rank: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, label) = when (rank) {
        1 -> LuxuryGold to "🥇"
        2 -> Color(0xFFCBD5E1) to "🥈"
        3 -> Color(0xFFCD7F32) to "🥉"
        else -> Color(0xFF374151) to "#$rank"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (rank <= 2) Color.Black else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
