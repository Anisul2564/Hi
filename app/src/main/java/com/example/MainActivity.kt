package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.MockRepository
import com.example.data.firebase.AuthRepository
import com.example.data.firebase.AuthState
import com.example.model.User
import com.example.ui.components.BdBottomNav
import com.example.ui.navigation.Screen
import com.example.ui.screens.admin.AdminHubScreen
import com.example.ui.screens.admin.AdminSectionScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.battle.MicBattleScreen
import com.example.ui.screens.create.CreateFlowScreen
import com.example.ui.screens.create.CreateScreen
import com.example.ui.screens.event.EventScreen
import com.example.ui.screens.game.FruitGameScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.items.MyItemsScreen
import com.example.ui.screens.level.LevelScreen
import com.example.ui.screens.messages.ChatDetailScreen
import com.example.ui.screens.messages.MessagesScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile_sub.GiftHistoryScreen
import com.example.ui.screens.profile_sub.MedalScreen
import com.example.ui.screens.profile_sub.NobleScreen
import com.example.ui.screens.profile_sub.RoleApplicationScreen
import com.example.ui.screens.profile_sub.WalletScreen
import com.example.ui.screens.ranking.RankingScreen
import com.example.ui.screens.rewards.DailyRewardsScreen
import com.example.ui.screens.room.VoiceRoomScreen
import com.example.ui.screens.rooms.RoomsScreen
import com.example.ui.screens.support.RoomSupportScreen
import com.example.ui.screens.svip.SvipScreen
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val authRepository = remember { AuthRepository(context) }
                val authState by authRepository.authState.collectAsState()
                val currentDomainUser by authRepository.currentDomainUser.collectAsState()

                val isFirebaseUserSignedIn = remember(authState) {
                    Firebase.auth.currentUser != null
                }

                if (!isFirebaseUserSignedIn) {
                    AuthScreen(authRepository = authRepository)
                } else if (currentDomainUser == null && authState is AuthState.Loading) {
                    // Loading user profile from Firestore
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BrightGold,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    BdPartyApp(
                        currentUser = currentDomainUser ?: MockRepository.currentUser,
                        authRepository = authRepository
                    )
                }
            }
        }
    }
}

@Composable
fun BdPartyApp(
    currentUser: User,
    authRepository: AuthRepository
) {
    val coroutineScope = rememberCoroutineScope()
    // Navigation backstack state
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Home

    fun navigateTo(screen: Screen) {
        when (screen) {
            Screen.Home, Screen.Rooms, Screen.Create, Screen.Messages, Screen.Profile -> {
                backStack.clear()
                backStack.add(screen)
            }
            else -> {
                backStack.add(screen)
            }
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        } else if (currentScreen !is Screen.Home) {
            backStack.clear()
            backStack.add(Screen.Home)
        }
    }

    val isMainTab = currentScreen in listOf(
        Screen.Home, Screen.Rooms, Screen.Create, Screen.Messages, Screen.Profile
    )

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            if (isMainTab) {
                BdBottomNav(
                    currentScreen = currentScreen,
                    onNavigate = { navigateTo(it) }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isMainTab) innerPadding else androidx.compose.foundation.layout.PaddingValues())
        ) {
            Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                when (screen) {
                    is Screen.Home -> HomeScreen(
                        currentUser = currentUser,
                        onNavigate = { navigateTo(it) }
                    )
                    is Screen.Rooms -> RoomsScreen(
                        currentUser = currentUser,
                        onNavigate = { navigateTo(it) }
                    )
                    is Screen.Create -> CreateScreen(onNavigate = { navigateTo(it) })
                    is Screen.Messages -> MessagesScreen(onNavigate = { navigateTo(it) })
                    is Screen.Profile -> ProfileScreen(
                        user = currentUser,
                        authRepository = authRepository,
                        onNavigate = { navigateTo(it) },
                        onUpdateProfile = { newUsername, newBio, newPhoto ->
                            coroutineScope.launch {
                                authRepository.updateSafeProfile(newUsername, newBio, newPhoto)
                            }
                        },
                        onSignOut = {
                            authRepository.signOut()
                        }
                    )

                    // Sub-screens & Experiences
                    is Screen.VoiceRoom -> VoiceRoomScreen(
                        roomId = screen.roomId,
                        currentUser = currentUser,
                        onBack = { navigateBack() },
                        onNavigate = { navigateTo(it) }
                    )
                    is Screen.FruitGame -> FruitGameScreen(onBack = { navigateBack() })
                    is Screen.MicBattle -> MicBattleScreen(onBack = { navigateBack() })
                    is Screen.DailyRewards -> DailyRewardsScreen(onBack = { navigateBack() })
                    is Screen.CommunityEvent -> EventScreen(onBack = { navigateBack() })
                    is Screen.RoomSupport -> RoomSupportScreen(onBack = { navigateBack() })
                    is Screen.MyItems -> MyItemsScreen(onBack = { navigateBack() })
                    is Screen.Level -> LevelScreen(onBack = { navigateBack() })
                    is Screen.Svip -> SvipScreen(onBack = { navigateBack() })
                    is Screen.Ranking -> RankingScreen(onBack = { navigateBack() })
                    is Screen.Wallet -> WalletScreen(
                        currentUser = currentUser,
                        onBack = { navigateBack() },
                        onNavigate = { navigateTo(it) }
                    )
                    is Screen.GiftHistory -> GiftHistoryScreen(
                        currentUser = currentUser,
                        onBack = { navigateBack() }
                    )
                    is Screen.Medal -> MedalScreen(onBack = { navigateBack() })
                    is Screen.Noble -> NobleScreen(onBack = { navigateBack() })
                    is Screen.RoleApplication -> RoleApplicationScreen(
                        user = currentUser,
                        authRepository = authRepository,
                        onBack = { navigateBack() }
                    )
                    is Screen.AdminHub -> AdminHubScreen(
                        currentUser = currentUser,
                        authRepository = authRepository,
                        onBack = { navigateBack() },
                        onNavigate = { navigateTo(it) }
                    )
                    is Screen.AdminSection -> AdminSectionScreen(
                        sectionTitle = screen.sectionTitle,
                        currentUser = currentUser,
                        authRepository = authRepository,
                        onBack = { navigateBack() }
                    )
                    is Screen.CreateFlow -> CreateFlowScreen(
                        flowTitle = screen.flowTitle,
                        currentUser = currentUser,
                        onBack = { navigateBack() },
                        onNavigate = { navigateTo(it) }
                    )
                    is Screen.ChatDetail -> ChatDetailScreen(
                        conversationId = screen.conversationId,
                        onBack = { navigateBack() }
                    )
                }
            }
        }
    }
}
