package com.example.model

enum class UserRole(val displayName: String) {
    USER("User"),
    HOST("Host"),
    OFFICIAL("Official"),
    MODERATOR("Moderator"),
    ASSISTANT_ADMIN("Assistant Admin"),
    ADMIN("Admin"),
    SUPER_ADMIN("Super Admin")
}

data class User(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarSeed: Int = 1,
    val level: Int = 1,
    val svipLevel: Int = 0,
    val role: UserRole = UserRole.USER,
    val isLive: Boolean = false,
    val coins: Long = 0,
    val diamonds: Long = 0,
    val honorScore: Long = 0,
    val charmScore: Long = 0,
    val bio: String = "Welcome to BD PARTY! ✨🎙️",
    val friendsCount: Int = 0,
    val followingCount: Int = 0,
    val followersCount: Int = 0,
    val equippedFrame: String = "",
    val equippedBadge: String = "",
    val isVerified: Boolean = false,
    val uid: String = "",
    val bdpId: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

enum class RoomCategory(val displayName: String) {
    ALL("All"),
    PARTY("Party"),
    SINGING("Singing"),
    GAMING("Gaming"),
    CHAT("Chat")
}

data class Seat(
    val seatIndex: Int,
    val user: User? = null,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val isSpeaking: Boolean = false
)

data class VoiceRoom(
    val id: String,
    val title: String,
    val host: User,
    val category: RoomCategory,
    val listenersCount: Int,
    val speakersCount: Int,
    val seats: List<Seat>,
    val tags: List<String> = emptyList(),
    val isLive: Boolean = true,
    val isFeatured: Boolean = false,
    val announcement: String = "Welcome to BD PARTY! Follow the host and respect room rules 🎧"
)

enum class ItemCategory(val displayName: String) {
    FRAME("Frame"),
    VEHICLE("Vehicle"),
    THEME("Theme"),
    CUSTOM_ID("Custom ID"),
    CHAT_BUBBLE("Chat Bubble"),
    MIC_WAVE("Mic Wave"),
    ROOM_BACKGROUND("Room Background"),
    GIFTING_BANNER("Gifting Banner"),
    PROFILE_EFFECT("Profile Effect"),
    ENTRANCE("Entrance"),
    RING("Ring")
}

data class InventoryItem(
    val id: String,
    val name: String,
    val category: ItemCategory,
    val stars: Int,
    val remainingDays: String,
    val isEquipped: Boolean,
    val description: String,
    val iconEmoji: String
)

data class SvipTier(
    val level: Int,
    val title: String,
    val requiredXp: Long,
    val currentXp: Long,
    val perks: List<String>,
    val seasonInfo: String = "Season 4 • 23 Days Left",
    val badgeLabel: String = "SVIP $level"
)

data class LevelTier(
    val level: Int,
    val tierName: String,
    val xpRequired: Long,
    val rewards: List<String>,
    val isUnlocked: Boolean
)

enum class RankingCategory(val displayName: String) {
    HONOR("Honor"),
    CHARM("Charm"),
    ROOM("Room")
}

enum class RankingTimeframe(val displayName: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month")
}

data class RankingEntry(
    val rank: Int,
    val user: User,
    val score: Long,
    val category: RankingCategory,
    val timeframe: RankingTimeframe,
    val rewardTitle: String
)

data class PkBattleState(
    val challenger: User,
    val defender: User,
    val countdownSeconds: Int,
    val challengerScore: Long,
    val defenderScore: Long,
    val challengerSupporters: List<User>,
    val defenderSupporters: List<User>,
    val isLive: Boolean = true
)

data class DailyReward(
    val day: Int,
    val coinAmount: Long,
    val bonusItem: String,
    val isClaimed: Boolean,
    val isCurrentDay: Boolean
)

enum class EventTaskType {
    RECHARGE, MIC_TIME, GIFTING, INVITE
}

data class EventTask(
    val id: String,
    val title: String,
    val requirement: String,
    val currentProgress: Int,
    val totalProgress: Int,
    val rewardCoins: Long,
    val isClaimed: Boolean,
    val type: EventTaskType
)

data class CommunityEvent(
    val id: String,
    val title: String,
    val subtitle: String,
    val countdownText: String,
    val tasks: List<EventTask>
)

data class RoomPartner(
    val name: String,
    val avatarSeed: Int,
    val sharePercent: Int,
    val contributionCoins: Long
)

data class SupportTarget(
    val targetLabel: String,
    val targetCoins: Long,
    val ownerReward: Long,
    val partnerReward: Long,
    val isReached: Boolean
)

data class WeeklyRecord(
    val weekName: String,
    val coinsGenerated: Long,
    val visitors: Int,
    val rewardClaimed: Long
)

data class RoomSupportInfo(
    val roomId: String,
    val currentWeek: String,
    val visitors: Int,
    val coinsGenerated: Long,
    val roomLevel: String,
    val totalRewards: Long,
    val partners: List<RoomPartner>,
    val targets: List<SupportTarget>,
    val historicalRecords: List<WeeklyRecord>
)

data class FruitGameOption(
    val id: Int,
    val name: String,
    val multiplier: Int,
    val emoji: String
)

data class FruitGameHistory(
    val roundId: String,
    val winningOption: FruitGameOption,
    val timeAgo: String
)

enum class GiftCategory(val displayName: String) {
    POPULAR("Popular"),
    LUXURY("Luxury"),
    EFFECTS("Effects"),
    EVENT("Event"),
    ROMANCE("Romance")
}

data class GiftItem(
    val id: String,
    val name: String,
    val price: Long,
    val category: GiftCategory,
    val iconEmoji: String
)

data class ChatMessage(
    val id: String,
    val sender: User,
    val text: String,
    val timestamp: String,
    val isSystem: Boolean = false,
    val giftSent: GiftItem? = null
)

data class MessageConversation(
    val id: String,
    val peer: User,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = true
)

enum class RoleRequestStatus(val displayName: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled")
}

data class RoleRequest(
    val requestId: String = "",
    val requesterUid: String = "",
    val requesterBdpId: String = "",
    val requesterUsername: String = "",
    val currentRole: String = "USER",
    val requestedRole: String = "HOST",
    val reason: String = "",
    val experience: String = "",
    val additionalInfo: String = "",
    val status: String = "PENDING",
    val createdAt: String = "",
    val reviewedAt: String? = null,
    val reviewerUid: String? = null,
    val reviewerRole: String? = null,
    val reviewNote: String? = null
)

data class AuditLog(
    val logId: String = "",
    val actorUid: String = "",
    val actorBdpId: String = "",
    val actorRole: String = "",
    val action: String = "",
    val targetUid: String = "",
    val targetBdpId: String = "",
    val oldValue: String = "",
    val newValue: String = "",
    val reason: String = "",
    val timestamp: String = ""
)

enum class AdminPermission(val key: String, val description: String) {
    USERS_READ("users.read", "View registered user profiles"),
    USERS_MANAGE("users.manage", "Manage user account states"),
    ROLE_REQUESTS_READ("roleRequests.read", "View submitted role requests"),
    ROLE_REQUESTS_REVIEW("roleRequests.review", "Review and action role requests"),
    HOSTS_MANAGE("hosts.manage", "Manage host statuses and quotas"),
    MODERATORS_MANAGE("moderators.manage", "Manage community moderators"),
    OFFICIALS_MANAGE("officials.manage", "Manage official verified accounts"),
    ROOMS_MANAGE("rooms.manage", "Moderate active voice stages"),
    REPORTS_MANAGE("reports.manage", "Review user and room safety reports"),
    PAYMENTS_MANAGE("payments.manage", "Audit transaction reconciliations"),
    WALLET_MANAGE("wallet.manage", "Manage virtual wallets and balances"),
    GIFTS_MANAGE("gifts.manage", "Configure gift catalog"),
    VIP_MANAGE("vip.manage", "Manage VIP and SVIP tiers"),
    ANNOUNCEMENTS_MANAGE("announcements.manage", "Publish system announcements"),
    SECURITY_VIEW("security.view", "Inspect security metrics and alerts"),
    AUDIT_LOGS_READ("auditLogs.read", "Inspect immutable audit trail"),
    SETTINGS_MANAGE("settings.manage", "Configure administrative parameters")
}

object RolePermissions {
    val SUPER_ADMIN_PERMISSIONS: Set<AdminPermission> = AdminPermission.values().toSet()

    val ADMIN_PERMISSIONS: Set<AdminPermission> = setOf(
        AdminPermission.USERS_READ,
        AdminPermission.USERS_MANAGE,
        AdminPermission.ROLE_REQUESTS_READ,
        AdminPermission.ROLE_REQUESTS_REVIEW,
        AdminPermission.HOSTS_MANAGE,
        AdminPermission.MODERATORS_MANAGE,
        AdminPermission.OFFICIALS_MANAGE,
        AdminPermission.ROOMS_MANAGE,
        AdminPermission.REPORTS_MANAGE,
        AdminPermission.PAYMENTS_MANAGE,
        AdminPermission.WALLET_MANAGE,
        AdminPermission.GIFTS_MANAGE,
        AdminPermission.VIP_MANAGE,
        AdminPermission.ANNOUNCEMENTS_MANAGE
    )

    val ASSISTANT_ADMIN_PERMISSIONS: Set<AdminPermission> = setOf(
        AdminPermission.USERS_READ,
        AdminPermission.ROLE_REQUESTS_READ,
        AdminPermission.ROOMS_MANAGE,
        AdminPermission.REPORTS_MANAGE
    )

    fun getPermissionsForRole(role: UserRole): Set<AdminPermission> = when (role) {
        UserRole.SUPER_ADMIN -> SUPER_ADMIN_PERMISSIONS
        UserRole.ADMIN -> ADMIN_PERMISSIONS
        UserRole.ASSISTANT_ADMIN -> ASSISTANT_ADMIN_PERMISSIONS
        else -> emptySet()
    }
}
