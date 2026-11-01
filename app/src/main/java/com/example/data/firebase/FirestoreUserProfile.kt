package com.example.data.firebase

import com.example.model.User
import com.example.model.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class FirestoreUserProfile(
    val uid: String = "",
    val bdpId: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bio: String = "Welcome to BD PARTY! ✨🎙️",
    val role: String = "USER",
    val coins: Long = 0L,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toDomainUser(): User {
        val userRole = try {
            UserRole.valueOf(role.uppercase())
        } catch (_: Exception) {
            UserRole.USER
        }
        val seed = if (uid.isNotEmpty()) abs(uid.hashCode() % 8) else 1

        return User(
            id = bdpId.ifEmpty { uid },
            uid = uid,
            bdpId = bdpId,
            username = username,
            displayName = username.ifEmpty { if (bdpId.isNotEmpty()) "User ${bdpId.takeLast(4)}" else "BD Member" },
            email = email,
            photoUrl = photoUrl,
            avatarSeed = seed,
            level = 1,
            svipLevel = 0,
            role = userRole,
            isLive = false,
            coins = coins,
            diamonds = 0L,
            bio = bio.ifEmpty { "Welcome to BD PARTY! ✨🎙️" },
            friendsCount = 0,
            followingCount = 0,
            followersCount = 0,
            equippedFrame = "",
            equippedBadge = "",
            isVerified = false,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun toMap(): Map<String, Any> {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        return mapOf(
            "uid" to uid,
            "bdpId" to bdpId,
            "username" to username,
            "email" to email,
            "photoUrl" to photoUrl,
            "bio" to bio,
            "role" to role,
            "coins" to coins,
            "createdAt" to createdAt.ifEmpty { now },
            "updatedAt" to now
        )
    }
}
