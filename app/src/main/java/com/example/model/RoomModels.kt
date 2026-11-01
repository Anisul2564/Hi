package com.example.model

enum class RoomRole {
    HOST,
    CO_HOST,
    MODERATOR,
    SPEAKER,
    LISTENER
}

enum class MicState {
    MUTED,
    UNMUTED
}

enum class RoomVisibility {
    PUBLIC,
    PRIVATE
}

enum class RoomStatus {
    LIVE,
    ENDED
}

data class FirestoreRoom(
    val roomId: String = "",
    val hostUid: String = "",
    val hostBdpId: String = "",
    val hostUsername: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "PARTY",
    val visibility: String = "PUBLIC",
    val status: String = "LIVE",
    val maxSpeakers: Int = 12,
    val createdAt: String = "",
    val updatedAt: String = "",
    val participantCount: Int = 1,
    val listenerCount: Int = 0,
    val backgroundUrl: String = "",
    val coverUrl: String = ""
)

data class RoomParticipant(
    val uid: String = "",
    val bdpId: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val role: String = "USER", // System role e.g. USER, ADMIN
    val roomRole: String = "LISTENER", // HOST, CO_HOST, MODERATOR, SPEAKER, LISTENER
    val micState: String = "MUTED", // MUTED, UNMUTED
    val seatIndex: Int = -1, // -1 means in audience/listener, 0..11 means active mic seat
    val joinedAt: String = "",
    val updatedAt: String = ""
)

data class LiveKitTokenResponse(
    val token: String,
    val roomName: String,
    val identity: String,
    val expiresAt: Long,
    val serverUrl: String? = null,
    val roomRole: String? = null,
    val canPublish: Boolean = false
)

enum class LiveKitConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}
