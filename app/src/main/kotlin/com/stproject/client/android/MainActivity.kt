package com.stproject.client.android

import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.deeplink.ShareCodeParser
import com.stproject.client.android.core.theme.StTheme
import com.stproject.client.android.features.auth.AuthScreen
import com.stproject.client.android.features.auth.AuthViewModel
import com.stproject.client.android.features.characters.CharacterDetailScreen
import com.stproject.client.android.features.characters.CharacterDetailViewModel
import com.stproject.client.android.features.chat.ChatScreen
import com.stproject.client.android.features.chat.ChatShareScreen
import com.stproject.client.android.features.chat.ChatShareViewModel
import com.stproject.client.android.features.chat.ChatViewModel
import com.stproject.client.android.features.chat.ModerationViewModel
import com.stproject.client.android.features.chats.ChatsListScreen
import com.stproject.client.android.features.chats.ChatsListViewModel
import com.stproject.client.android.features.creators.CreatorAssistantChatScreen
import com.stproject.client.android.features.creators.CreatorAssistantChatViewModel
import com.stproject.client.android.features.creators.CreatorAssistantListScreen
import com.stproject.client.android.features.creators.CreatorAssistantListViewModel
import com.stproject.client.android.features.creators.CreatorCharactersScreen
import com.stproject.client.android.features.creators.CreatorCharactersViewModel
import com.stproject.client.android.features.creators.CreatorsScreen
import com.stproject.client.android.features.creators.CreatorsViewModel
import com.stproject.client.android.features.explore.ExploreScreen
import com.stproject.client.android.features.explore.ExploreViewModel
import com.stproject.client.android.features.notifications.NotificationsScreen
import com.stproject.client.android.features.notifications.NotificationsViewModel
import com.stproject.client.android.features.profile.ProfileScreen
import com.stproject.client.android.features.profile.ProfileViewModel
import com.stproject.client.android.features.settings.AgeVerificationDialog
import com.stproject.client.android.features.settings.ComplianceViewModel
import com.stproject.client.android.features.settings.PrivacyConsentDialog
import com.stproject.client.android.features.settings.SettingsScreen
import com.stproject.client.android.features.shop.ShopScreen
import com.stproject.client.android.features.shop.ShopViewModel
import com.stproject.client.android.features.social.SocialScreen
import com.stproject.client.android.features.social.SocialViewModel
import com.stproject.client.android.features.wallet.WalletScreen
import com.stproject.client.android.features.wallet.WalletViewModel
import dagger.hilt.android.AndroidEntryPoint

private const val CHAT_SESSION_ROUTE = "chat/session"
private const val CHAT_SHARE_ROUTE = "chat-share"
private const val CHAT_SHARE_ROUTE_PATTERN = "chat-share?shareCode={shareCode}"
private const val CHARACTER_DETAIL_ROUTE = "explore/detail"
private const val CHARACTER_DETAIL_ROUTE_PATTERN = "explore/detail/{characterId}"
private const val CREATOR_DETAIL_ROUTE = "creators/detail"
private const val CREATOR_DETAIL_ROUTE_PATTERN = "creators/detail/{creatorId}"
private const val CREATOR_ASSISTANT_LIST_ROUTE = "creators/assistant"
private const val CREATOR_ASSISTANT_CHAT_ROUTE_PATTERN = "creators/assistant/{sessionId}"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val chatShareViewModel: ChatShareViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val complianceViewModel: ComplianceViewModel by viewModels()
    private val moderationViewModel: ModerationViewModel by viewModels()
    private val exploreViewModel: ExploreViewModel by viewModels()
    private val chatsListViewModel: ChatsListViewModel by viewModels()
    private val characterDetailViewModel: CharacterDetailViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val creatorsViewModel: CreatorsViewModel by viewModels()
    private val creatorCharactersViewModel: CreatorCharactersViewModel by viewModels()
    private val creatorAssistantListViewModel: CreatorAssistantListViewModel by viewModels()
    private val creatorAssistantChatViewModel: CreatorAssistantChatViewModel by viewModels()
    private val notificationsViewModel: NotificationsViewModel by viewModels()
    private val socialViewModel: SocialViewModel by viewModels()
    private val shopViewModel: ShopViewModel by viewModels()
    private val walletViewModel: WalletViewModel by viewModels()
    private val pendingShareCode = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingShareCode.value = ShareCodeParser.extractShareCode(intent)
        setContent {
            val authState by authViewModel.uiState.collectAsState()
            val complianceState by complianceViewModel.uiState.collectAsState()
            val context = LocalContext.current
            val incomingShareCode by pendingShareCode

            LaunchedEffect(authState.isAuthenticated) {
                if (authState.isAuthenticated) {
                    complianceViewModel.load()
                } else {
                    complianceViewModel.reset()
                }
            }

            LaunchedEffect(complianceState.accountDeleted) {
                if (complianceState.accountDeleted) {
                    authViewModel.onLogout()
                    complianceViewModel.reset()
                }
            }
            LaunchedEffect(complianceState.languageTag) {
                applyLanguageTag(context, complianceState.languageTag)
            }
            StTheme(themeMode = complianceState.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (authState.isAuthenticated) {
                        AuthenticatedContent(
                            chatViewModel = chatViewModel,
                            chatShareViewModel = chatShareViewModel,
                            moderationViewModel = moderationViewModel,
                            complianceViewModel = complianceViewModel,
                            exploreViewModel = exploreViewModel,
                            chatsListViewModel = chatsListViewModel,
                            characterDetailViewModel = characterDetailViewModel,
                            profileViewModel = profileViewModel,
                            creatorsViewModel = creatorsViewModel,
                            creatorCharactersViewModel = creatorCharactersViewModel,
                            creatorAssistantListViewModel = creatorAssistantListViewModel,
                            creatorAssistantChatViewModel = creatorAssistantChatViewModel,
                            notificationsViewModel = notificationsViewModel,
                            socialViewModel = socialViewModel,
                            shopViewModel = shopViewModel,
                            walletViewModel = walletViewModel,
                            complianceState = complianceState,
                            onLogout = authViewModel::onLogout,
                            incomingShareCode = incomingShareCode,
                            onShareCodeConsumed = { pendingShareCode.value = null },
                        )
                    } else if (authState.isRestoring) {
                        AuthLoadingScreen()
                    } else {
                        AuthScreen(viewModel = authViewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingShareCode.value = ShareCodeParser.extractShareCode(intent)
    }
}

@Composable
private fun AuthenticatedContent(
    chatViewModel: ChatViewModel,
    chatShareViewModel: ChatShareViewModel,
    moderationViewModel: ModerationViewModel,
    complianceViewModel: ComplianceViewModel,
    exploreViewModel: ExploreViewModel,
    chatsListViewModel: ChatsListViewModel,
    characterDetailViewModel: CharacterDetailViewModel,
    profileViewModel: ProfileViewModel,
    creatorsViewModel: CreatorsViewModel,
    creatorCharactersViewModel: CreatorCharactersViewModel,
    creatorAssistantListViewModel: CreatorAssistantListViewModel,
    creatorAssistantChatViewModel: CreatorAssistantChatViewModel,
    notificationsViewModel: NotificationsViewModel,
    socialViewModel: SocialViewModel,
    shopViewModel: ShopViewModel,
    walletViewModel: WalletViewModel,
    complianceState: com.stproject.client.android.features.settings.ComplianceUiState,
    onLogout: () -> Unit,
    incomingShareCode: String?,
    onShareCodeConsumed: () -> Unit,
) {
    val contentGate = ContentGate.from(complianceState)
    val contentAllowed = contentGate.contentAllowed
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val activeRoute = backStackEntry?.destination?.route ?: MainTab.Explore.route

    fun navigateTo(tab: MainTab) {
        if (activeRoute == tab.route) return
        navController.navigate(tab.route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(MainTab.Explore.route) {
                saveState = true
            }
        }
    }

    fun characterDetailRoute(characterId: String): String = "$CHARACTER_DETAIL_ROUTE/${characterId.trim()}"

    fun creatorDetailRoute(creatorId: String): String = "$CREATOR_DETAIL_ROUTE/${creatorId.trim()}"

    fun chatShareRoute(shareCode: String): String = "$CHAT_SHARE_ROUTE?shareCode=${Uri.encode(shareCode)}"

    fun creatorAssistantChatRoute(sessionId: String): String = "creators/assistant/${sessionId.trim()}"

    LaunchedEffect(contentGate.nsfwAllowed, contentAllowed) {
        exploreViewModel.setNsfwAllowed(contentGate.nsfwAllowed)
        if (contentAllowed) {
            exploreViewModel.load(force = true)
        }
    }

    LaunchedEffect(incomingShareCode, contentAllowed) {
        val code = incomingShareCode?.trim()?.takeIf { it.isNotEmpty() }
        if (code != null && contentAllowed) {
            navController.navigate(chatShareRoute(code)) {
                launchSingleTop = true
            }
            onShareCodeConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { navigateTo(MainTab.Explore) }) {
                Text(stringResource(R.string.main_tab_explore))
            }
            TextButton(onClick = { navigateTo(MainTab.Chat) }) {
                Text(stringResource(R.string.main_tab_chat))
            }
            TextButton(onClick = { navigateTo(MainTab.Creators) }) {
                Text(stringResource(R.string.main_tab_creators))
            }
            TextButton(onClick = { navigateTo(MainTab.Notifications) }) {
                Text(stringResource(R.string.main_tab_notifications))
            }
            TextButton(onClick = { navigateTo(MainTab.Social) }) {
                Text(stringResource(R.string.main_tab_social))
            }
            TextButton(onClick = { navigateTo(MainTab.Profile) }) {
                Text(stringResource(R.string.main_tab_profile))
            }
            TextButton(onClick = { navigateTo(MainTab.Shop) }) {
                Text(stringResource(R.string.main_tab_shop))
            }
            TextButton(onClick = { navigateTo(MainTab.Wallet) }) {
                Text(stringResource(R.string.main_tab_wallet))
            }
            TextButton(onClick = { navigateTo(MainTab.Settings) }) {
                Text(stringResource(R.string.main_tab_settings))
            }
            TextButton(onClick = onLogout) {
                Text(stringResource(R.string.main_tab_logout))
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            if (contentAllowed) {
                NavHost(
                    navController = navController,
                    startDestination = MainTab.Explore.route,
                ) {
                    composable(MainTab.Explore.route) {
                        ExploreScreen(
                            viewModel = exploreViewModel,
                            onStartChat = { memberId, shareCode ->
                                chatViewModel.startNewChat(
                                    memberId,
                                    shareCode,
                                    onSuccess = {
                                        navController.navigate(CHAT_SESSION_ROUTE)
                                        if (!shareCode.isNullOrBlank()) {
                                            onShareCodeConsumed()
                                        }
                                    },
                                )
                            },
                            onOpenDetail = { characterId ->
                                navController.navigate(characterDetailRoute(characterId))
                            },
                            moderationViewModel = moderationViewModel,
                            contentGate = contentGate,
                        )
                    }
                    composable(
                        route = CHARACTER_DETAIL_ROUTE_PATTERN,
                        arguments =
                            listOf(
                                navArgument("characterId") { type = NavType.StringType },
                            ),
                    ) { entry ->
                        val characterId = entry.arguments?.getString("characterId").orEmpty()
                        CharacterDetailScreen(
                            characterId = characterId,
                            viewModel = characterDetailViewModel,
                            moderationViewModel = moderationViewModel,
                            onBack = { navController.popBackStack() },
                            onStartChat = { memberId, shareCode ->
                                chatViewModel.startNewChat(
                                    memberId,
                                    shareCode,
                                    onSuccess = { navController.navigate(CHAT_SESSION_ROUTE) },
                                )
                            },
                            contentGate = contentGate,
                        )
                    }
                    composable(MainTab.Chat.route) {
                        ChatsListScreen(
                            viewModel = chatsListViewModel,
                            onOpenSession = { summary ->
                                chatViewModel.openSession(
                                    summary.sessionId,
                                    summary.primaryMemberId,
                                    onSuccess = { navController.navigate(CHAT_SESSION_ROUTE) },
                                )
                            },
                            contentGate = contentGate,
                        )
                    }
                    composable(CHAT_SESSION_ROUTE) {
                        ChatScreen(
                            viewModel = chatViewModel,
                            moderationViewModel = moderationViewModel,
                            onBackToList = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = CHAT_SHARE_ROUTE_PATTERN,
                        arguments =
                            listOf(
                                navArgument("shareCode") {
                                    type = NavType.StringType
                                    nullable = true
                                },
                            ),
                    ) { entry ->
                        val shareCode = entry.arguments?.getString("shareCode")
                        ChatShareScreen(
                            shareCode = shareCode,
                            viewModel = chatShareViewModel,
                            chatViewModel = chatViewModel,
                            onBackToExplore = { navigateTo(MainTab.Explore) },
                            onOpenChat = { navController.navigate(CHAT_SESSION_ROUTE) },
                            onShareCodeConsumed = onShareCodeConsumed,
                        )
                    }
                    composable(MainTab.Profile.route) {
                        ProfileScreen(viewModel = profileViewModel)
                    }
                    composable(MainTab.Shop.route) {
                        ShopScreen(viewModel = shopViewModel)
                    }
                    composable(MainTab.Wallet.route) {
                        WalletScreen(viewModel = walletViewModel)
                    }
                    composable(MainTab.Creators.route) {
                        CreatorsScreen(
                            viewModel = creatorsViewModel,
                            onOpenCreator = { creatorId ->
                                navController.navigate(creatorDetailRoute(creatorId))
                            },
                            onOpenAssistant = {
                                navController.navigate(CREATOR_ASSISTANT_LIST_ROUTE)
                            },
                        )
                    }
                    composable(
                        route = CREATOR_DETAIL_ROUTE_PATTERN,
                        arguments =
                            listOf(
                                navArgument("creatorId") { type = NavType.StringType },
                            ),
                    ) { entry ->
                        val creatorId = entry.arguments?.getString("creatorId").orEmpty()
                        CreatorCharactersScreen(
                            creatorId = creatorId,
                            viewModel = creatorCharactersViewModel,
                            onBack = { navController.popBackStack() },
                            onStartChat = { memberId ->
                                chatViewModel.startNewChat(
                                    memberId,
                                    onSuccess = { navController.navigate(CHAT_SESSION_ROUTE) },
                                )
                            },
                            onOpenDetail = { characterId ->
                                navController.navigate(characterDetailRoute(characterId))
                            },
                            contentGate = contentGate,
                        )
                    }
                    composable(CREATOR_ASSISTANT_LIST_ROUTE) {
                        CreatorAssistantListScreen(
                            viewModel = creatorAssistantListViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenSession = { sessionId ->
                                navController.navigate(creatorAssistantChatRoute(sessionId))
                            },
                        )
                    }
                    composable(
                        route = CREATOR_ASSISTANT_CHAT_ROUTE_PATTERN,
                        arguments =
                            listOf(
                                navArgument("sessionId") { type = NavType.StringType },
                            ),
                    ) { entry ->
                        val sessionId = entry.arguments?.getString("sessionId").orEmpty()
                        CreatorAssistantChatScreen(
                            sessionId = sessionId,
                            viewModel = creatorAssistantChatViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(MainTab.Notifications.route) {
                        NotificationsScreen(viewModel = notificationsViewModel)
                    }
                    composable(MainTab.Social.route) {
                        SocialScreen(viewModel = socialViewModel)
                    }
                    composable(MainTab.Settings.route) {
                        SettingsScreen(
                            uiState = complianceState,
                            onBack = { navigateTo(MainTab.Chat) },
                            onVerifyAge = complianceViewModel::verifyAge,
                            onDeleteAccount = complianceViewModel::deleteAccount,
                            onAllowNsfwChanged = complianceViewModel::setAllowNsfw,
                            onThemeModeChanged = complianceViewModel::setThemeMode,
                            onLanguageTagChanged = complianceViewModel::setLanguageTag,
                        )
                    }
                }
            } else {
                ComplianceBlockingScreen()
            }
        }
    }

    if (complianceState.consentLoaded && complianceState.consentRequired) {
        PrivacyConsentDialog(
            onAccept = complianceViewModel::acceptTos,
            onDecline = onLogout,
            isSubmitting = complianceState.isSubmitting,
        )
    }

    AgeVerificationDialog(
        open =
            complianceState.consentLoaded &&
                !complianceState.consentRequired &&
                !complianceState.ageVerified,
        isSubmitting = complianceState.isSubmitting,
        allowDismiss = false,
        onDismiss = {},
        onVerified = complianceViewModel::verifyAge,
        onUnderage = onLogout,
    )
}

@Composable
private fun AuthLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private enum class MainTab(val route: String) {
    Explore("explore"),
    Chat("chat"),
    Creators("creators"),
    Notifications("notifications"),
    Social("social"),
    Profile("profile"),
    Shop("shop"),
    Wallet("wallet"),
    Settings("settings"),
}

@Composable
private fun ComplianceBlockingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(R.string.compliance_loading),
            )
        }
    }
}

private fun applyLanguageTag(
    context: android.content.Context,
    tag: String?,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val manager = context.getSystemService(LocaleManager::class.java) ?: return
    val localeList =
        if (tag.isNullOrBlank()) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
    manager.applicationLocales = localeList
}
