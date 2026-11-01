package com.example.data

import com.example.model.*

object MockRepository {

    val currentUser = User(
        id = "BD889201",
        username = "rayan_king",
        displayName = "Prince Rayan",
        avatarSeed = 7,
        level = 48,
        svipLevel = 3,
        role = UserRole.HOST,
        isLive = true,
        coins = 128500,
        diamonds = 45200,
        honorScore = 1250000,
        charmScore = 980000,
        bio = "Official BD PARTY Host 🎙️ Music & Good Vibes only! ✨",
        friendsCount = 246,
        followingCount = 412,
        followersCount = 38900,
        equippedFrame = "Royal Crown Frame",
        equippedBadge = "Official Host",
        isVerified = true
    )

    val onlineCreators = listOf(
        User("u1", "samira_vocal", "Samira Voice", 1, 55, 4, UserRole.OFFICIAL, true, 420000, 180000),
        User("u2", "dj_shanto", "DJ Shanto Live", 2, 62, 5, UserRole.HOST, true, 890000, 450000),
        User("u3", "anika_stars", "Anika Queen", 3, 38, 2, UserRole.MODERATOR, true, 150000, 95000),
        User("u4", "tanvir_beats", "Tanvir Beats", 4, 71, 5, UserRole.ASSISTANT_ADMIN, true, 1400000, 890000),
        User("u5", "nusrat_melody", "Nusrat Melody", 5, 44, 3, UserRole.HOST, true, 310000, 120000),
        User("u6", "arman_rock", "Arman Rocker", 6, 29, 1, UserRole.USER, true, 85000, 32000),
        User("u7", "farhan_pro", "Farhan Boss", 8, 80, 5, UserRole.OFFICIAL, true, 2100000, 1300000),
    )

    fun getVoiceRooms(): List<VoiceRoom> {
        val sampleSeats1 = List(8) { index ->
            when (index) {
                0 -> Seat(0, currentUser, isSpeaking = true)
                1 -> Seat(1, onlineCreators[0], isSpeaking = true)
                2 -> Seat(2, onlineCreators[1], isSpeaking = false)
                3 -> Seat(3, onlineCreators[2], isSpeaking = false)
                4 -> Seat(4, null, isLocked = false)
                5 -> Seat(5, null, isLocked = false)
                6 -> Seat(6, null, isLocked = true)
                else -> Seat(7, null, isLocked = true)
            }
        }

        return listOf(
            VoiceRoom(
                id = "889922",
                title = "🔥 Dhaka Midnight Vibes & Jamming Session",
                host = currentUser,
                category = RoomCategory.PARTY,
                listenersCount = 1840,
                speakersCount = 4,
                seats = sampleSeats1,
                tags = listOf("Acoustic", "BD Chill", "Night Owls"),
                isLive = true,
                isFeatured = true
            ),
            VoiceRoom(
                id = "771103",
                title = "🎙️ BD Idol Singing Competition [Round 3]",
                host = onlineCreators[0],
                category = RoomCategory.SINGING,
                listenersCount = 2450,
                speakersCount = 6,
                seats = sampleSeats1,
                tags = listOf("Singing", "Judges", "Gifts"),
                isLive = true,
                isFeatured = false
            ),
            VoiceRoom(
                id = "662244",
                title = "🎮 PUBG Mobile Squad & Banter",
                host = onlineCreators[1],
                category = RoomCategory.GAMING,
                listenersCount = 980,
                speakersCount = 4,
                seats = sampleSeats1,
                tags = listOf("Gaming", "Clips", "Fun"),
                isLive = true,
                isFeatured = false
            ),
            VoiceRoom(
                id = "553311",
                title = "☕ Late Night Tea & Heart-to-Heart Chat",
                host = onlineCreators[2],
                category = RoomCategory.CHAT,
                listenersCount = 1290,
                speakersCount = 3,
                seats = sampleSeats1,
                tags = listOf("Deep Talks", "Friendship"),
                isLive = true,
                isFeatured = false
            ),
            VoiceRoom(
                id = "449988",
                title = "⚡ PK Mic Battle Arena • Finals Tonight",
                host = onlineCreators[3],
                category = RoomCategory.PARTY,
                listenersCount = 4120,
                speakersCount = 2,
                seats = sampleSeats1,
                tags = listOf("PK Battle", "Super Gifting"),
                isLive = true,
                isFeatured = false
            ),
            VoiceRoom(
                id = "338877",
                title = "🎵 Acoustic Bangla Classics & Requests",
                host = onlineCreators[4],
                category = RoomCategory.SINGING,
                listenersCount = 760,
                speakersCount = 3,
                seats = sampleSeats1,
                tags = listOf("Unplugged", "Requests"),
                isLive = true,
                isFeatured = false
            )
        )
    }

    val inventoryItems = listOf(
        InventoryItem("i1", "Royal Crown Frame", ItemCategory.FRAME, 5, "Permanent", true, "Gleaming golden crown with animated sparkles", "👑"),
        InventoryItem("i2", "Cyber Neon Halo", ItemCategory.FRAME, 4, "14 Days", false, "Futuristic pulsing violet and cyan aura", "🪐"),
        InventoryItem("i3", "Golden Dragon Supercar", ItemCategory.VEHICLE, 5, "30 Days", true, "Epic entrance car roaring into the room with fireworks", "🏎️"),
        InventoryItem("i4", "Amethyst Pegasus", ItemCategory.VEHICLE, 4, "7 Days", false, "Majestic flying pegasus vehicle with starlight trail", "🦄"),
        InventoryItem("i5", "Galaxy Nebula Theme", ItemCategory.THEME, 5, "Permanent", true, "Deep violet cosmic background with swirling stars", "🌌"),
        InventoryItem("i6", "Golden ID 8888", ItemCategory.CUSTOM_ID, 5, "Permanent", true, "Rare gold badge 4-digit premium number ID", "✨"),
        InventoryItem("i7", "Love Pink Bubble", ItemCategory.CHAT_BUBBLE, 3, "21 Days", true, "Cute pastel bubble with floating heart accents", "💬"),
        InventoryItem("i8", "Electric Pulse Mic", ItemCategory.MIC_WAVE, 4, "Permanent", true, "Soundwaves ripple with neon lightning when speaking", "🎙️"),
        InventoryItem("i9", "Luxury Palace Hall", ItemCategory.ROOM_BACKGROUND, 5, "30 Days", true, "Aristocratic royal ballroom room stage background", "🏛️"),
        InventoryItem("i10", "Diamond Storm Banner", ItemCategory.GIFTING_BANNER, 5, "15 Days", false, "Broadcasts your gifts across all BD PARTY rooms", "🚩"),
        InventoryItem("i11", "Phoenix Rise Effect", ItemCategory.PROFILE_EFFECT, 5, "12 Days", true, "Flaming phoenix wings spreading over your profile", "🦅"),
        InventoryItem("i12", "Imperial Royal Chariot", ItemCategory.ENTRANCE, 5, "25 Days", true, "Grand entrance fanfare whenever you enter any room", "⚡"),
        InventoryItem("i13", "Eternal Diamond Ring", ItemCategory.RING, 5, "Permanent", true, "Exclusive CP loyalty pledge luxury ring", "💍")
    )

    val svipTiers = listOf(
        SvipTier(
            level = 1,
            title = "SVIP 1 • Bronze Aristocrat",
            requiredXp = 100000,
            currentXp = 100000,
            perks = listOf("Exclusive Bronze SVIP Badge", "Custom Chat Bubble", "Room Entrance Sound", "+5% Daily Coin Bonus")
        ),
        SvipTier(
            level = 2,
            title = "SVIP 2 • Silver Vanguard",
            requiredXp = 300000,
            currentXp = 300000,
            perks = listOf("Silver SVIP Frame", "Anti-Kick Protection in Casual Rooms", "Flying Bullet Chat Banner", "+10% Daily Coin Bonus")
        ),
        SvipTier(
            level = 3,
            title = "SVIP 3 • Golden Sovereign",
            requiredXp = 800000,
            currentXp = 680000,
            perks = listOf("Golden Sovereign Avatar Frame", "Custom Entrance Vehicle (Sports Car)", "Exclusive Animated Gifts Access", "+20% Daily Coin Bonus", "Priority Support Line")
        ),
        SvipTier(
            level = 4,
            title = "SVIP 4 • Platinum Monarch",
            requiredXp = 2000000,
            currentXp = 680000,
            perks = listOf("Platinum Dragon Frame", "Room Wide Flying Announcement Banner", "Stealth Visitor Mode", "Immune to Room Bans", "Personal VIP Host Concierge")
        ),
        SvipTier(
            level = 5,
            title = "SVIP 5 • Diamond Emperor",
            requiredXp = 5000000,
            currentXp = 680000,
            perks = listOf("Emperor's Diamond Crown & Wing Frame", "Full Server Broadcast on Room Entrance", "Custom Animated Gift Creation", "Exclusive Supercar Vehicle", "Infinite Mic Priority")
        )
    )

    val levelTiers = listOf(
        LevelTier(1, "Bronze Explorer (Lv 1 - 20)", 50000, listOf("Basic Chat Colors", "Unlock Voice Rooms"), true),
        LevelTier(21, "Silver Rising Star (Lv 21 - 40)", 200000, listOf("Silver Badge", "Special Emoji Pack", "Daily +500 Coins"), true),
        LevelTier(41, "Gold Master (Lv 41 - 60)", 500000, listOf("Gold Mic Wave", "Sparkling Name Color", "Daily +2,000 Coins"), true),
        LevelTier(61, "Diamond Legend (Lv 61 - 80)", 1500000, listOf("Diamond Frame", "Custom Room Theme", "Daily +5,000 Coins"), false),
        LevelTier(81, "Imperial Supreme (Lv 81 - 100)", 5000000, listOf("Full Server Broadcast", "Supreme Crown", "Daily +15,000 Coins"), false)
    )

    val rankingEntries = listOf(
        RankingEntry(1, User("r1", "sultana_bd", "Queen Sultana", 1, 74, 5, UserRole.OFFICIAL), 4250000, RankingCategory.HONOR, RankingTimeframe.DAY, "Gold Honor Trophy + 100k Coins"),
        RankingEntry(2, User("r2", "king_fahim", "Fahim Royal", 4, 68, 4, UserRole.HOST), 3120000, RankingCategory.HONOR, RankingTimeframe.DAY, "Silver Honor Trophy + 50k Coins"),
        RankingEntry(3, currentUser, 2480000, RankingCategory.HONOR, RankingTimeframe.DAY, "Bronze Honor Trophy + 25k Coins"),
        RankingEntry(4, onlineCreators[0], 1940000, RankingCategory.HONOR, RankingTimeframe.DAY, "Top 10 Special Frame"),
        RankingEntry(5, onlineCreators[1], 1520000, RankingCategory.HONOR, RankingTimeframe.DAY, "Top 10 Special Frame"),
        RankingEntry(6, onlineCreators[2], 1230000, RankingCategory.HONOR, RankingTimeframe.DAY, "Top 10 Special Frame"),
        RankingEntry(7, onlineCreators[3], 980000, RankingCategory.HONOR, RankingTimeframe.DAY, "Top 10 Special Frame"),
        RankingEntry(8, onlineCreators[4], 840000, RankingCategory.HONOR, RankingTimeframe.DAY, "Top 10 Special Frame"),
        RankingEntry(9, onlineCreators[5], 720000, RankingCategory.HONOR, RankingTimeframe.DAY, "Top 10 Special Frame"),
        RankingEntry(10, onlineCreators[6], 610000, RankingCategory.HONOR, RankingTimeframe.DAY, "Top 10 Special Frame")
    )

    val pkBattleState = PkBattleState(
        challenger = currentUser,
        defender = onlineCreators[0],
        countdownSeconds = 165,
        challengerScore = 84200,
        defenderScore = 69400,
        challengerSupporters = listOf(onlineCreators[1], onlineCreators[3], onlineCreators[4]),
        defenderSupporters = listOf(onlineCreators[2], onlineCreators[5], onlineCreators[6]),
        isLive = true
    )

    val dailyRewards = listOf(
        DailyReward(1, 500, "Bronze Mic (1d)", true, false),
        DailyReward(2, 1000, "Silver Bubble (1d)", true, false),
        DailyReward(3, 2000, "Flame Frame (3d)", false, true), // Today
        DailyReward(4, 3000, "Rose Bouquet x10", false, false),
        DailyReward(5, 5000, "Neon Mic Wave (3d)", false, false),
        DailyReward(6, 8000, "VIP Pass (1d)", false, false),
        DailyReward(7, 15000, "Golden Crown Entrance (7d)", false, false)
    )

    val communityEvent = CommunityEvent(
        id = "evt_carnival",
        title = "🎪 Galaxy Star Carnival 2026",
        subtitle = "Compete with rooms, collect star gems, and win exclusive badges!",
        countdownText = "03d 14h 28m remaining",
        tasks = listOf(
            EventTask("t1", "Voice Stage Mic-Time", "Stay on microphone for 30 minutes", 25, 30, 2500, false, EventTaskType.MIC_TIME),
            EventTask("t2", "Send Gifts to Hosts", "Send any 5 gifts in live rooms", 5, 5, 5000, false, EventTaskType.GIFTING),
            EventTask("t3", "Recharge Coins Quest", "Recharge any virtual coin package", 1000, 1000, 10000, true, EventTaskType.RECHARGE),
            EventTask("t4", "Invite Friends to Party", "Invite 3 new friends to join a room", 2, 3, 3000, false, EventTaskType.INVITE)
        )
    )

    val roomSupportInfo = RoomSupportInfo(
        roomId = "#889922",
        currentWeek = "Week 41 (Oct 2026)",
        visitors = 14280,
        coinsGenerated = 4850000,
        roomLevel = "Level 6 • Royal Palace",
        totalRewards = 245000,
        partners = listOf(
            RoomPartner("Prince Rayan (Owner)", 7, 60, 2910000),
            RoomPartner("Samira Voice", 1, 25, 1212500),
            RoomPartner("DJ Shanto", 2, 15, 727500)
        ),
        targets = listOf(
            SupportTarget("Bronze Goal (500k Coins)", 500000, 25000, 10000, true),
            SupportTarget("Silver Goal (2M Coins)", 2000000, 100000, 40000, true),
            SupportTarget("Gold Goal (5M Coins)", 5000000, 250000, 100000, false),
            SupportTarget("Diamond Goal (10M Coins)", 10000000, 600000, 250000, false)
        ),
        historicalRecords = listOf(
            WeeklyRecord("Week 40", 5200000, 15400, 260000),
            WeeklyRecord("Week 39", 4100000, 12800, 205000),
            WeeklyRecord("Week 38", 3800000, 11200, 190000)
        )
    )

    // The 8 options surrounding center countdown area for Fruit Game (Treasure Hunter)
    val fruitGameOptions = listOf(
        FruitGameOption(1, "Kebab", 15, "🍢"),
        FruitGameOption(2, "Orange", 5, "🍊"),
        FruitGameOption(3, "Chicken", 25, "🍗"),
        FruitGameOption(4, "Cherry", 5, "🍒"),
        FruitGameOption(5, "Lemon", 5, "🍋"),
        FruitGameOption(6, "Meat", 45, "🥩"),
        FruitGameOption(7, "Apple", 5, "🍎"),
        FruitGameOption(8, "Fish", 10, "🐟")
    )

    val giftCatalog = listOf(
        GiftItem("g1", "Red Rose", 10, GiftCategory.POPULAR, "🌹"),
        GiftItem("g2", "Golden Mic", 100, GiftCategory.POPULAR, "🎙️"),
        GiftItem("g3", "Love Heart", 200, GiftCategory.ROMANCE, "💖"),
        GiftItem("g4", "Luxury Perfume", 520, GiftCategory.ROMANCE, "💎"),
        GiftItem("g5", "Sports Car", 2000, GiftCategory.LUXURY, "🏎️"),
        GiftItem("g6", "Crown of Glory", 5000, GiftCategory.LUXURY, "👑"),
        GiftItem("g7", "Royal Castle", 20000, GiftCategory.EFFECTS, "🏰"),
        GiftItem("g8", "Super Yacht", 50000, GiftCategory.EFFECTS, "🛥️"),
        GiftItem("g9", "Galaxy Dragon", 100000, GiftCategory.EVENT, "🐉"),
        GiftItem("g10", "Phoenix Rebirth", 250000, GiftCategory.EVENT, "🔥")
    )

    val conversations = listOf(
        MessageConversation("c1", onlineCreators[0], "Hey Rayan! Ready for the duet tonight on stage? 🎙️", "10:42 AM", 2, true),
        MessageConversation("c2", onlineCreators[1], "Bro that PK battle yesterday was insane! 40k points! 🔥", "Yesterday", 0, true),
        MessageConversation("c3", onlineCreators[2], "Thanks so much for the support in my room! ✨", "Oct 05", 0, false),
        MessageConversation("c4", User("sys_bd", "BD PARTY Official", "BD Party Official", 99, 100, 5, UserRole.OFFICIAL), "🎉 Congratulations! You received the Week 41 Host Bonus.", "Oct 04", 1, true)
    )
}
