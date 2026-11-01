package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.RoomCategory

/**
 * Centralized Asset Management for BD PARTY.
 *
 * Provides realistic portrait image mappings and attractive room cover graphics.
 * Guarantees that real, high-resolution visual assets are displayed across all screens,
 * eliminating initial letters / placeholder initials.
 */
object BdAssets {

    /**
     * 10 Realistic portrait avatars (5 male, 5 female).
     */
    val avatarDrawables = listOf(
        R.drawable.img_avatar_1,  // Prince Rayan (Male)
        R.drawable.img_avatar_2,  // Samira Voice (Female)
        R.drawable.img_avatar_3,  // DJ Shanto Live (Male)
        R.drawable.img_avatar_4,  // Anika Queen (Female)
        R.drawable.img_avatar_5,  // Tanvir Beats (Male)
        R.drawable.img_avatar_6,  // Nusrat Melody (Female)
        R.drawable.img_avatar_7,  // Arman Rocker (Male)
        R.drawable.img_avatar_8,  // Farhan Boss (Male)
        R.drawable.img_avatar_9,  // Zara Stream (Female)
        R.drawable.img_avatar_10  // Kabir Host (Male)
    )

    /**
     * 8 Attractive live voice-room cover graphics.
     */
    val roomCovers = listOf(
        R.drawable.img_room_cover_party,    // 1: Party Nightclub
        R.drawable.img_room_cover_music,    // 2: Live Music & Acoustic
        R.drawable.img_room_cover_gaming,   // 3: Esports Gaming Arena
        R.drawable.img_room_cover_chat,     // 4: Late Night Tea & Lounge
        R.drawable.img_room_cover_singing,  // 5: Idol Vocal & Concert
        R.drawable.img_room_cover_lounge,   // 6: Rooftop City Chill
        R.drawable.img_room_cover_battle,   // 7: Mic Battle & PK Stage
        R.drawable.img_room_cover_club      // 8: DJ Club & EDM Party
    )

    /**
     * Maps user seed or name to a consistent, realistic portrait drawable.
     */
    fun getAvatarDrawable(seed: Int, name: String = ""): Int {
        val lower = name.lowercase().trim()
        return when {
            lower.contains("rayan") || lower.contains("prince") -> R.drawable.img_avatar_1
            lower.contains("samira") -> R.drawable.img_avatar_2
            lower.contains("shanto") || lower.contains("dj") -> R.drawable.img_avatar_3
            lower.contains("anika") -> R.drawable.img_avatar_4
            lower.contains("tanvir") -> R.drawable.img_avatar_5
            lower.contains("nusrat") -> R.drawable.img_avatar_6
            lower.contains("arman") -> R.drawable.img_avatar_7
            lower.contains("farhan") -> R.drawable.img_avatar_8
            lower.contains("zara") -> R.drawable.img_avatar_9
            lower.contains("kabir") -> R.drawable.img_avatar_10
            else -> {
                if (seed > 0) {
                    avatarDrawables[(seed - 1).mod(avatarDrawables.size)]
                } else if (name.isNotBlank()) {
                    val hash = kotlin.math.abs(name.hashCode())
                    avatarDrawables[hash.mod(avatarDrawables.size)]
                } else {
                    R.drawable.img_avatar_1
                }
            }
        }
    }

    /**
     * Maps room category, title, or ID to one of 8 attractive room covers.
     */
    fun getRoomCoverDrawable(
        category: RoomCategory = RoomCategory.PARTY,
        roomId: String = "",
        title: String = ""
    ): Int {
        val lower = title.lowercase()
        return when {
            lower.contains("jamming") || lower.contains("dhaka") || lower.contains("midnight") -> R.drawable.img_room_cover_party
            lower.contains("singing") || lower.contains("idol") -> R.drawable.img_room_cover_singing
            lower.contains("pubg") || lower.contains("gaming") || lower.contains("squad") -> R.drawable.img_room_cover_gaming
            lower.contains("tea") || lower.contains("heart") || lower.contains("talk") -> R.drawable.img_room_cover_chat
            lower.contains("music") || lower.contains("acoustic") -> R.drawable.img_room_cover_music
            lower.contains("lounge") || lower.contains("rooftop") -> R.drawable.img_room_cover_lounge
            lower.contains("battle") || lower.contains("pk") || lower.contains("mic") -> R.drawable.img_room_cover_battle
            lower.contains("club") || lower.contains("dj") || lower.contains("edm") -> R.drawable.img_room_cover_club
            category == RoomCategory.PARTY -> R.drawable.img_room_cover_party
            category == RoomCategory.SINGING -> R.drawable.img_room_cover_singing
            category == RoomCategory.GAMING -> R.drawable.img_room_cover_gaming
            category == RoomCategory.CHAT -> R.drawable.img_room_cover_chat
            else -> {
                val combinedHash = kotlin.math.abs(roomId.hashCode() + category.hashCode())
                roomCovers[combinedHash.mod(roomCovers.size)]
            }
        }
    }
}

/**
 * Reusable rounded room cover component with dark gradient scrim for readability.
 */
@Composable
fun BdRoomCover(
    category: RoomCategory,
    roomId: String = "",
    title: String = "",
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    showScrim: Boolean = true
) {
    val coverRes = BdAssets.getRoomCoverDrawable(category, roomId, title)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        Image(
            painter = painterResource(id = coverRes),
            contentDescription = title.ifBlank { "Room Cover" },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (showScrim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x33000000),
                                Color(0x66000000),
                                Color(0xD9080B18)
                            )
                        )
                    )
            )
        }
    }
}
