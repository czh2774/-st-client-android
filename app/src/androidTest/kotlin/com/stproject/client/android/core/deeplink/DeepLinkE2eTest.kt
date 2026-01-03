package com.stproject.client.android.core.deeplink

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.ViewModelProvider
import com.stproject.client.android.MainActivity
import com.stproject.client.android.core.a2ui.A2UIRuntimeState
import com.stproject.client.android.core.auth.AuthService
import com.stproject.client.android.core.auth.AuthTokenStore
import com.stproject.client.android.core.auth.AuthTokens
import com.stproject.client.android.core.di.AuthModule
import com.stproject.client.android.data.di.RepositoryModule
import com.stproject.client.android.domain.model.A2UIAction
import com.stproject.client.android.domain.model.A2UIActionResult
import com.stproject.client.android.domain.model.BackgroundList
import com.stproject.client.android.domain.model.CardCreateInput
import com.stproject.client.android.domain.model.CardCreateResult
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatSessionSummary
import com.stproject.client.android.domain.model.Comment
import com.stproject.client.android.domain.model.CommentLikeResult
import com.stproject.client.android.domain.model.CommentListResult
import com.stproject.client.android.domain.model.CommentSort
import com.stproject.client.android.domain.model.CommentUser
import com.stproject.client.android.domain.model.CreatorAssistantChatResult
import com.stproject.client.android.domain.model.CreatorAssistantDraft
import com.stproject.client.android.domain.model.CreatorAssistantDraftResult
import com.stproject.client.android.domain.model.CreatorAssistantPublishResult
import com.stproject.client.android.domain.model.CreatorAssistantSessionHistory
import com.stproject.client.android.domain.model.ReportReasonMeta
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.model.UserConfig
import com.stproject.client.android.domain.model.UserConfigUpdate
import com.stproject.client.android.domain.model.UserProfile
import com.stproject.client.android.domain.model.WalletBalance
import com.stproject.client.android.domain.model.WorldInfoEntry
import com.stproject.client.android.domain.model.WorldInfoEntryInput
import com.stproject.client.android.domain.repository.BackgroundRepository
import com.stproject.client.android.domain.repository.CardRepository
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.repository.CommentRepository
import com.stproject.client.android.domain.repository.CreatorAssistantRepository
import com.stproject.client.android.domain.repository.CreatorAssistantSessionsResult
import com.stproject.client.android.domain.repository.CreatorAssistantStartResult
import com.stproject.client.android.domain.repository.CreatorCharactersResult
import com.stproject.client.android.domain.repository.CreatorListResult
import com.stproject.client.android.domain.repository.CreatorRepository
import com.stproject.client.android.domain.repository.DecorationRepository
import com.stproject.client.android.domain.repository.IapCatalog
import com.stproject.client.android.domain.repository.IapRepository
import com.stproject.client.android.domain.repository.IapRestoreRequest
import com.stproject.client.android.domain.repository.IapRestoreResult
import com.stproject.client.android.domain.repository.IapTransactionRequest
import com.stproject.client.android.domain.repository.IapTransactionResult
import com.stproject.client.android.domain.repository.NotificationListResult
import com.stproject.client.android.domain.repository.NotificationRepository
import com.stproject.client.android.domain.repository.PresetRepository
import com.stproject.client.android.domain.repository.ReportRepository
import com.stproject.client.android.domain.repository.SocialListResult
import com.stproject.client.android.domain.repository.SocialRepository
import com.stproject.client.android.domain.repository.UnreadCounts
import com.stproject.client.android.domain.repository.UserRepository
import com.stproject.client.android.domain.repository.WalletRepository
import com.stproject.client.android.domain.repository.WalletTransactionsResult
import com.stproject.client.android.domain.repository.WorldInfoRepository
import com.stproject.client.android.features.auth.AuthViewModel
import com.stproject.client.android.features.settings.ComplianceViewModel
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(RepositoryModule::class, AuthModule::class)
class DeepLinkE2eTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val chatRepository: ChatRepository = FakeChatRepository()

    private val fakeAuthTokenStore = FakeAuthTokenStore()

    @BindValue
    @JvmField
    val authTokenStore: AuthTokenStore = fakeAuthTokenStore

    @BindValue
    @JvmField
    val authService: AuthService = FakeAuthService(fakeAuthTokenStore)

    @BindValue
    @JvmField
    val userRepository: UserRepository = FakeUserRepository()

    @BindValue
    @JvmField
    val reportRepository: ReportRepository = FakeReportRepository()

    @BindValue
    @JvmField
    val commentRepository: CommentRepository = FakeCommentRepository()

    @BindValue
    @JvmField
    val characterRepository: CharacterRepository = FakeCharacterRepository()

    @BindValue
    @JvmField
    val creatorRepository: CreatorRepository = FakeCreatorRepository()

    @BindValue
    @JvmField
    val creatorAssistantRepository: CreatorAssistantRepository = FakeCreatorAssistantRepository()

    @BindValue
    @JvmField
    val notificationRepository: NotificationRepository = FakeNotificationRepository()

    @BindValue
    @JvmField
    val socialRepository: SocialRepository = FakeSocialRepository()

    @BindValue
    @JvmField
    val iapRepository: IapRepository = FakeIapRepository()

    @BindValue
    @JvmField
    val walletRepository: WalletRepository = FakeWalletRepository()

    @BindValue
    @JvmField
    val cardRepository: CardRepository = FakeCardRepository()

    @BindValue
    @JvmField
    val worldInfoRepository: WorldInfoRepository = FakeWorldInfoRepository()

    @BindValue
    @JvmField
    val presetRepository: PresetRepository = FakePresetRepository()

    @BindValue
    @JvmField
    val backgroundRepository: BackgroundRepository = FakeBackgroundRepository()

    @BindValue
    @JvmField
    val decorationRepository: DecorationRepository = FakeDecorationRepository()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        fakeAuthTokenStore.clear()
    }

    @Test
    fun deepLinkShareCodeOpensChat() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("stproject://share/c/code-123"))
        val authViewModel = ViewModelProvider(composeRule.activity).get(AuthViewModel::class.java)
        val complianceViewModel = ViewModelProvider(composeRule.activity).get(ComplianceViewModel::class.java)
        val fakeChatRepository = chatRepository as FakeChatRepository
        val fakeCharacterRepository = characterRepository as FakeCharacterRepository
        fakeCharacterRepository.nsfwIds = emptySet()
        fakeChatRepository.holdStartSession()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            authViewModel.uiState.value.isAuthenticated
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            val state = complianceViewModel.uiState.value
            state.consentLoaded && !state.consentRequired && state.ageVerified
        }

        composeRule.activityRule.scenario.onActivity { activity ->
            deliverDeepLink(activity, intent)
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("chat.share.loading").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            fakeChatRepository.lastStartShareCode == "code-123"
        }
        fakeChatRepository.releaseStartSession()
        assertEquals("char-1", fakeChatRepository.lastStartMemberId)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("chat.send").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("chat.send").assertIsDisplayed()
    }

    @Test
    fun deepLinkShareCodeBlocksNsfwWhenDisabled() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("stproject://share/c/code-123"))
        val authViewModel = ViewModelProvider(composeRule.activity).get(AuthViewModel::class.java)
        val complianceViewModel = ViewModelProvider(composeRule.activity).get(ComplianceViewModel::class.java)
        val fakeChatRepository = chatRepository as FakeChatRepository
        val fakeCharacterRepository = characterRepository as FakeCharacterRepository
        fakeCharacterRepository.nsfwIds = setOf("char-1")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            authViewModel.uiState.value.isAuthenticated
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            val state = complianceViewModel.uiState.value
            state.consentLoaded && !state.consentRequired && state.ageVerified
        }

        composeRule.activityRule.scenario.onActivity { activity ->
            deliverDeepLink(activity, intent)
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("chat.share.error").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("chat.share.error").assertIsDisplayed()
        composeRule.runOnIdle {
            if (fakeChatRepository.lastStartMemberId != null) {
                throw AssertionError("Expected deep link to be blocked for NSFW content.")
            }
        }
    }
}

private fun deliverDeepLink(
    activity: MainActivity,
    intent: Intent,
) {
    val shareCode = ShareCodeParser.extractShareCode(intent) ?: return
    val field = activity.javaClass.getDeclaredField("pendingShareCode")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val state = field.get(activity) as MutableState<String?>
    state.value = shareCode
}

private class FakeAuthTokenStore : AuthTokenStore {
    @Volatile
    private var tokens: AuthTokens? =
        AuthTokens(
            accessToken = "test-access",
            refreshToken = "test-refresh",
            expiresAtEpochSeconds = (System.currentTimeMillis() / 1000) + 3600,
        )

    override fun getAccessToken(): String? = tokens?.accessToken

    override fun getRefreshToken(): String? = tokens?.refreshToken

    override fun getExpiresAtEpochSeconds(): Long? = tokens?.expiresAtEpochSeconds

    override fun getTokens(): AuthTokens? = tokens

    override fun updateTokens(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long?,
    ) {
        val expiresAt =
            if (expiresInSeconds != null && expiresInSeconds > 0) {
                (System.currentTimeMillis() / 1000) + expiresInSeconds
            } else {
                null
            }
        tokens = AuthTokens(accessToken = accessToken, refreshToken = refreshToken, expiresAtEpochSeconds = expiresAt)
    }

    override fun clear() {
        tokens = null
    }
}

private class FakeAuthService(
    private val tokenStore: AuthTokenStore,
) : AuthService {
    override suspend fun login(
        email: String,
        password: String,
    ): AuthTokens {
        val tokens = tokenStore.getTokens()
        if (tokens != null) return tokens
        val refreshed =
            AuthTokens(
                accessToken = "test-access",
                refreshToken = "test-refresh",
                expiresAtEpochSeconds = (System.currentTimeMillis() / 1000) + 3600,
            )
        tokenStore.updateTokens(refreshed.accessToken, refreshed.refreshToken, 3600)
        return refreshed
    }

    override suspend fun refreshTokens(): AuthTokens? = tokenStore.getTokens()

    override suspend fun logout() {
        tokenStore.clear()
    }
}

private class FakeUserRepository : UserRepository {
    private var config =
        UserConfig(
            ageVerified = true,
            birthDate = null,
            blockedTags = emptyList(),
        )
    private var profile =
        UserProfile(
            id = "user-1",
            email = "test@example.com",
            tosVersion = "v1",
            tosAcceptedAt = "2024-01-01T00:00:00Z",
        )

    override suspend fun getMe(): UserProfile = profile

    override suspend fun acceptTos(version: String?): UserProfile {
        profile = profile.copy(tosAcceptedAt = "2024-01-01T00:00:00Z", tosVersion = version ?: profile.tosVersion)
        return profile
    }

    override suspend fun getUserConfig(): UserConfig = config

    override suspend fun updateUserConfig(update: UserConfigUpdate): UserConfig {
        config =
            config.copy(
                ageVerified = update.ageVerified ?: config.ageVerified,
                birthDate = update.birthDate ?: config.birthDate,
                blockedTags = update.blockedTags ?: config.blockedTags,
            )
        return config
    }

    override suspend fun deleteMe() = Unit
}

private class FakeCharacterRepository : CharacterRepository {
    var nsfwIds: Set<String> = emptySet()

    override suspend fun queryCharacters(
        cursor: String?,
        limit: Int?,
        sortBy: String?,
        isNsfw: Boolean?,
    ): List<CharacterSummary> = emptyList()

    override suspend fun getCharacterDetail(characterId: String): CharacterDetail {
        val isNsfw = nsfwIds.contains(characterId)
        return CharacterDetail(
            id = characterId,
            name = "Test",
            description = "",
            tags = emptyList(),
            creatorName = null,
            isNsfw = isNsfw,
            totalFollowers = 0,
            isFollowed = false,
        )
    }

    override suspend fun resolveShareCode(shareCode: String): String? {
        return if (shareCode == "code-123") "char-1" else null
    }

    override suspend fun generateShareCode(characterId: String): ShareCodeInfo? = null

    override suspend fun blockCharacter(
        characterId: String,
        value: Boolean,
    ) = Unit

    override suspend fun followCharacter(
        characterId: String,
        value: Boolean,
    ): CharacterFollowResult {
        return CharacterFollowResult(totalFollowers = 0, isFollowed = value)
    }
}

private class FakeChatRepository : ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: Flow<List<ChatMessage>> = _messages
    override val a2uiState: Flow<A2UIRuntimeState?> =
        MutableStateFlow(null)

    @Volatile
    private var startSessionGate: CompletableDeferred<Unit>? = null

    @Volatile
    var lastStartMemberId: String? = null
        private set

    @Volatile
    var lastStartShareCode: String? = null
        private set

    override suspend fun sendUserMessage(content: String) = Unit

    override suspend fun sendA2UIAction(action: A2UIAction): A2UIActionResult {
        return A2UIActionResult(accepted = false, reason = "not_supported")
    }

    override suspend fun startNewSession(
        memberId: String,
        shareCode: String?,
    ) {
        lastStartMemberId = memberId
        lastStartShareCode = shareCode
        startSessionGate?.await()
    }

    override suspend fun openSession(
        sessionId: String,
        primaryMemberId: String?,
    ) = Unit

    override suspend fun listSessions(
        limit: Int,
        offset: Int,
    ): List<ChatSessionSummary> = emptyList()

    override suspend fun getLastSessionSummary(): ChatSessionSummary? = null

    override suspend fun regenerateMessage(messageId: String) = Unit

    override suspend fun continueMessage(messageId: String) = Unit

    override suspend fun deleteMessage(
        messageId: String,
        deleteAfter: Boolean,
    ) = Unit

    override suspend fun setActiveSwipe(
        messageId: String,
        swipeId: Int,
    ) = Unit

    override suspend fun deleteSwipe(
        messageId: String,
        swipeId: Int?,
    ) = Unit

    override suspend fun loadSessionVariables(): Map<String, Any> = emptyMap()

    override suspend fun updateSessionVariables(variables: Map<String, Any>) = Unit

    override suspend fun updateMessageVariables(
        messageId: String,
        swipesData: List<Map<String, Any>>,
    ) = Unit

    override suspend fun clearLocalSession() = Unit

    fun holdStartSession() {
        startSessionGate = CompletableDeferred()
    }

    fun releaseStartSession() {
        startSessionGate?.complete(Unit)
        startSessionGate = null
    }
}

private class FakeReportRepository : ReportRepository {
    override suspend fun getReasonMeta(): ReportReasonMeta {
        return ReportReasonMeta(reasons = emptyList(), requiresDetailReasons = emptyList(), maxDetailLength = 0)
    }

    override suspend fun submitReport(
        targetType: String,
        targetId: String,
        reasons: List<String>,
        detail: String?,
        sessionId: String?,
    ) = Unit
}

private class FakeCommentRepository : CommentRepository {
    override suspend fun listComments(
        characterId: String,
        sort: CommentSort,
        pageNum: Int,
        pageSize: Int,
    ): CommentListResult {
        return CommentListResult(items = emptyList(), total = 0, hasMore = false)
    }

    override suspend fun createComment(
        characterId: String,
        content: String,
        parentId: String?,
    ): Comment {
        return Comment(
            id = "comment-1",
            characterId = characterId,
            userId = "user-1",
            content = content,
            likesCount = 0,
            isLiked = false,
            createdAt = "2025-01-01T00:00:00Z",
            parentId = parentId,
            user = CommentUser(id = "user-1", username = "User", avatarUrl = null),
            replies = emptyList(),
        )
    }

    override suspend fun deleteComment(commentId: String) = Unit

    override suspend fun likeComment(
        commentId: String,
        value: Boolean,
    ): CommentLikeResult {
        return CommentLikeResult(likesCount = 0, isLiked = value)
    }
}

private class FakeCreatorRepository : CreatorRepository {
    override suspend fun listCreators(
        limit: Int,
        cursor: String?,
        sortBy: String?,
        searchKeyword: String?,
    ): CreatorListResult {
        return CreatorListResult(items = emptyList(), hasMore = false, nextCursor = null)
    }

    override suspend fun listCreatorCharacters(
        creatorId: String,
        pageNum: Int,
        pageSize: Int,
    ): CreatorCharactersResult {
        return CreatorCharactersResult(items = emptyList(), total = 0, hasMore = false)
    }
}

private class FakeCreatorAssistantRepository : CreatorAssistantRepository {
    override suspend fun listSessions(
        pageNum: Int,
        pageSize: Int,
        status: String?,
    ): CreatorAssistantSessionsResult {
        return CreatorAssistantSessionsResult(items = emptyList(), total = 0, hasMore = false)
    }

    override suspend fun startSession(
        characterType: String?,
        parentCharacterId: String?,
        initialPrompt: String?,
    ): CreatorAssistantStartResult {
        return CreatorAssistantStartResult(
            sessionId = "session-1",
            characterType = null,
            greeting = null,
            suggestions = emptyList(),
        )
    }

    override suspend fun getSessionHistory(sessionId: String): CreatorAssistantSessionHistory {
        return CreatorAssistantSessionHistory(
            sessionId = sessionId,
            characterType = null,
            status = null,
            messages = emptyList(),
            currentDraft = emptyDraft(),
            createdAt = null,
            updatedAt = null,
        )
    }

    override suspend fun chat(
        sessionId: String,
        content: String,
    ): CreatorAssistantChatResult {
        return CreatorAssistantChatResult(
            messageId = "msg-1",
            content = "",
            suggestions = emptyList(),
            draftReady = false,
        )
    }

    override suspend fun generateDraft(sessionId: String): CreatorAssistantDraftResult {
        return CreatorAssistantDraftResult(
            draftId = "draft-1",
            draft = emptyDraft(),
            confidence = 0.0,
            missingFields = emptyList(),
        )
    }

    override suspend fun updateDraft(
        sessionId: String,
        draftId: String,
        updates: Map<String, Any>,
    ): CreatorAssistantDraftResult {
        return CreatorAssistantDraftResult(
            draftId = draftId,
            draft = emptyDraft(),
            confidence = 0.0,
            missingFields = emptyList(),
        )
    }

    override suspend fun publish(
        sessionId: String,
        draftId: String,
        isPublic: Boolean,
    ): CreatorAssistantPublishResult {
        return CreatorAssistantPublishResult(
            characterId = "char-1",
            name = null,
            avatarUrl = null,
            backgroundUrl = null,
            shareCode = null,
        )
    }

    override suspend fun abandon(sessionId: String): Boolean = true
}

private fun emptyDraft(): CreatorAssistantDraft {
    return CreatorAssistantDraft(
        name = null,
        description = null,
        greeting = null,
        personality = null,
        scenario = null,
        exampleDialogs = null,
        tags = emptyList(),
        gender = 0,
        isNsfw = false,
        characterType = null,
        parentCharacterId = null,
        subCharacters = emptyList(),
    )
}

private class FakeNotificationRepository : NotificationRepository {
    override suspend fun listNotifications(
        pageNum: Int,
        pageSize: Int,
    ): NotificationListResult {
        return NotificationListResult(items = emptyList(), total = 0, hasMore = false)
    }

    override suspend fun markAsRead(
        ids: List<String>,
        markAll: Boolean,
    ) = Unit

    override suspend fun getUnreadCounts(): UnreadCounts {
        return UnreadCounts(system = 0, follow = 0, like = 0, comment = 0, total = 0)
    }
}

private class FakeSocialRepository : SocialRepository {
    override suspend fun followUser(
        userId: String,
        value: Boolean,
    ) = Unit

    override suspend fun blockUser(
        userId: String,
        value: Boolean,
    ) = Unit

    override suspend fun listFollowers(
        pageNum: Int,
        pageSize: Int,
        userId: String?,
    ): SocialListResult {
        return SocialListResult(items = emptyList(), total = 0, hasMore = false)
    }

    override suspend fun listFollowing(
        pageNum: Int,
        pageSize: Int,
        userId: String?,
    ): SocialListResult {
        return SocialListResult(items = emptyList(), total = 0, hasMore = false)
    }

    override suspend fun listBlocked(
        pageNum: Int,
        pageSize: Int,
    ): SocialListResult {
        return SocialListResult(items = emptyList(), total = 0, hasMore = false)
    }
}

private class FakeIapRepository : IapRepository {
    override suspend fun getCatalog(): IapCatalog {
        return IapCatalog(environment = "Test", products = emptyList())
    }

    override suspend fun submitTransaction(request: IapTransactionRequest): IapTransactionResult {
        return IapTransactionResult(ok = true, status = "ok", serverTimeMs = 0)
    }

    override suspend fun restore(request: IapRestoreRequest): IapRestoreResult {
        return IapRestoreResult(serverTimeMs = 0)
    }
}

private class FakeWalletRepository : WalletRepository {
    override suspend fun getBalance(): WalletBalance {
        return WalletBalance(
            balanceCredits = 0,
            currency = "USD",
            diamonds = 0,
        )
    }

    override suspend fun listTransactions(
        pageNum: Int,
        pageSize: Int,
    ): WalletTransactionsResult {
        return WalletTransactionsResult(
            items = emptyList(),
            total = 0,
            hasMore = false,
            pageNum = pageNum,
            pageSize = pageSize,
        )
    }
}

private class FakeCardRepository : CardRepository {
    override suspend fun createCard(input: CardCreateInput): CardCreateResult {
        return CardCreateResult(
            characterId = "char-1",
            name = input.name,
        )
    }

    override suspend fun createCardFromWrapper(wrapper: Map<String, Any>): CardCreateResult {
        return CardCreateResult(
            characterId = "char-1",
            name = null,
        )
    }

    override suspend fun updateCardFromWrapper(
        id: String,
        wrapper: Map<String, Any>,
    ): CardCreateResult {
        return CardCreateResult(
            characterId = id,
            name = null,
        )
    }

    override suspend fun fetchCardWrapper(id: String): Map<String, Any> {
        return emptyMap()
    }

    override suspend fun fetchExportPng(id: String): ByteArray {
        return ByteArray(0)
    }

    override suspend fun parseCardFile(
        fileName: String,
        bytes: ByteArray,
    ): Map<String, Any> {
        return emptyMap()
    }

    override suspend fun parseCardText(
        content: String,
        fileName: String?,
    ): Map<String, Any> {
        return emptyMap()
    }

    override suspend fun fetchTemplate(): Map<String, Any> {
        return emptyMap()
    }
}

private class FakeWorldInfoRepository : WorldInfoRepository {
    override suspend fun listEntries(
        characterId: String?,
        includeGlobal: Boolean,
    ): List<WorldInfoEntry> = emptyList()

    override suspend fun createEntry(input: WorldInfoEntryInput): WorldInfoEntry {
        return WorldInfoEntry(
            id = "wi-1",
            characterId = input.characterId,
            keys = input.keys,
            secondaryKeys = emptyList(),
            content = input.content,
            comment = input.comment,
            enabled = input.enabled,
        )
    }

    override suspend fun updateEntry(
        id: String,
        input: WorldInfoEntryInput,
    ): WorldInfoEntry {
        return WorldInfoEntry(
            id = id,
            characterId = input.characterId,
            keys = input.keys,
            secondaryKeys = emptyList(),
            content = input.content,
            comment = input.comment,
            enabled = input.enabled,
        )
    }

    override suspend fun deleteEntry(id: String): Boolean = true
}

private class FakePresetRepository : PresetRepository {
    override suspend fun listPresets(seriesId: String?): List<com.stproject.client.android.domain.model.ModelPreset> {
        return emptyList()
    }
}

private class FakeBackgroundRepository : BackgroundRepository {
    override suspend fun listBackgrounds(): BackgroundList {
        return BackgroundList(items = emptyList(), config = null)
    }

    override suspend fun uploadBackground(
        fileName: String,
        bytes: ByteArray,
    ): String = fileName

    override suspend fun renameBackground(
        oldName: String,
        newName: String,
    ) = Unit

    override suspend fun deleteBackground(name: String) = Unit
}

private class FakeDecorationRepository : DecorationRepository {
    override suspend fun listDecorations(): List<com.stproject.client.android.domain.model.DecorationItem> {
        return emptyList()
    }

    override suspend fun setDecorationEquipped(
        decorationId: String,
        equip: Boolean,
    ) = Unit
}
