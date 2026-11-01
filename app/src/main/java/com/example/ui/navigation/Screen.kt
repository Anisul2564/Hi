package com.example.ui.navigation

sealed class Screen {
    // 5 Main Tabs
    object Home : Screen()
    object Rooms : Screen()
    object Create : Screen()
    object Messages : Screen()
    object Profile : Screen()

    // Sub-screens & Experiences
    data class VoiceRoom(val roomId: String) : Screen()
    object FruitGame : Screen()
    object MicBattle : Screen()
    object DailyRewards : Screen()
    object CommunityEvent : Screen()
    object RoomSupport : Screen()
    object MyItems : Screen()
    object Level : Screen()
    object Svip : Screen()
    object Ranking : Screen()
    object Wallet : Screen()
    object Medal : Screen()
    object Noble : Screen()
    object RoleApplication : Screen()
    object AdminHub : Screen()
    object GiftHistory : Screen()
    data class AdminSection(val sectionTitle: String) : Screen()
    data class CreateFlow(val flowTitle: String) : Screen()
    data class ChatDetail(val conversationId: String) : Screen()
}
