package com.stproject.client.android.features.compliance

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stproject.client.android.R
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.session.ChatSessionStore
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatSessionSummary
import com.stproject.client.android.domain.model.ReportReasonMeta
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.repository.ReportRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import com.stproject.client.android.features.characters.CharacterDetailScreen
import com.stproject.client.android.features.characters.CharacterDetailViewModel
import com.stproject.client.android.features.chat.ModerationViewModel
import com.stproject.client.android.features.chats.ChatsListScreen
import com.stproject.client.android.features.chats.ChatsListViewModel
import com.stproject.client.android.features.explore.ExploreScreen
import com.stproject.client.android.features.explore.ExploreViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class NsfwGatingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun exploreBlocksNsfwStartChat() {
        val startChatLabel = composeRule.activity.getString(R.string.common_start_chat)
        val matureDisabledTitle = composeRule.activity.getString(R.string.content_mature_disabled_title)
        val contentGate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = false,
            )
        val repo =
            FakeCharacterRepository(
                items =
                    listOf(
                        CharacterSummary(
                            id = "char-1",
                            name = "NSFW",
                            description = "desc",
                            avatarUrl = null,
                            isNsfw = true,
                            totalFollowers = 0,
                            isFollowed = false,
                        ),
                    ),
                details =
                    mapOf(
                        "char-1" to nsfwDetail("char-1"),
                    ),
            )
        val exploreViewModel =
            ExploreViewModel(
                characterRepository = repo,
                resolveContentAccess =
                    ResolveContentAccessUseCase(
                        accessManager = AllowAllAccessManager(),
                        characterRepository = repo,
                    ),
            ).apply {
                setNsfwAllowed(false)
            }
        val startChatCount = AtomicInteger(0)
        val moderationViewModel =
            ModerationViewModel(
                reportRepository = FakeReportRepository(),
                characterRepository = repo,
                chatSessionStore = FakeChatSessionStore(),
            )

        composeRule.setContent {
            ExploreScreen(
                viewModel = exploreViewModel,
                onStartChat = { _, _ -> startChatCount.incrementAndGet() },
                onOpenDetail = {},
                moderationViewModel = moderationViewModel,
                contentGate = contentGate,
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(startChatLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(startChatLabel).performClick()
        composeRule.onNodeWithText(matureDisabledTitle).assertIsDisplayed()
        composeRule.runOnIdle {
            if (startChatCount.get() != 0) {
                throw AssertionError("Expected start chat to be blocked.")
            }
        }
    }

    @Test
    fun exploreShareCodeBlocksNsfw() {
        val matureDisabledTitle = composeRule.activity.getString(R.string.content_mature_disabled_title)
        val contentGate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = false,
            )
        val repo =
            FakeCharacterRepository(
                items = emptyList(),
                details =
                    mapOf(
                        "char-1" to nsfwDetail("char-1"),
                    ),
                shareCodes = mapOf("code" to "char-1"),
            )
        val exploreViewModel =
            ExploreViewModel(
                characterRepository = repo,
                resolveContentAccess =
                    ResolveContentAccessUseCase(
                        accessManager = AllowAllAccessManager(),
                        characterRepository = repo,
                    ),
            ).apply {
                setNsfwAllowed(false)
            }
        val startChatCount = AtomicInteger(0)
        val moderationViewModel =
            ModerationViewModel(
                reportRepository = FakeReportRepository(),
                characterRepository = repo,
                chatSessionStore = FakeChatSessionStore(),
            )

        composeRule.setContent {
            ExploreScreen(
                viewModel = exploreViewModel,
                onStartChat = { _, _ -> startChatCount.incrementAndGet() },
                onOpenDetail = {},
                moderationViewModel = moderationViewModel,
                contentGate = contentGate,
            )
        }

        composeRule.runOnIdle {
            exploreViewModel.onShareCodeChanged("code")
            exploreViewModel.resolveShareCode()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(matureDisabledTitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(matureDisabledTitle).assertIsDisplayed()
        composeRule.runOnIdle {
            if (startChatCount.get() != 0) {
                throw AssertionError("Expected share code to be blocked.")
            }
        }
    }

    @Test
    fun characterDetailDisablesStartChatWhenNsfwBlocked() {
        val startChatLabel = composeRule.activity.getString(R.string.common_start_chat)
        val matureDisabledInline = composeRule.activity.getString(R.string.content_mature_disabled_inline)
        val contentGate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = false,
            )
        val repo =
            FakeCharacterRepository(
                items = emptyList(),
                details =
                    mapOf(
                        "char-1" to nsfwDetail("char-1"),
                    ),
            )
        val viewModel =
            CharacterDetailViewModel(
                characterRepository = repo,
                resolveContentAccess =
                    ResolveContentAccessUseCase(
                        accessManager = AllowAllAccessManager(),
                        characterRepository = repo,
                    ),
            )
        val moderationViewModel =
            ModerationViewModel(
                reportRepository = FakeReportRepository(),
                characterRepository = repo,
                chatSessionStore = FakeChatSessionStore(),
            )

        composeRule.setContent {
            CharacterDetailScreen(
                characterId = "char-1",
                viewModel = viewModel,
                moderationViewModel = moderationViewModel,
                onBack = {},
                onStartChat = { _, _ -> },
                contentGate = contentGate,
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(startChatLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(matureDisabledInline)
            .assertIsDisplayed()
        composeRule.onNodeWithText(startChatLabel).assertIsNotEnabled()
    }

    @Test
    fun chatListBlocksNsfwOpen() {
        val openLabel = composeRule.activity.getString(R.string.common_open)
        val matureDisabledTitle = composeRule.activity.getString(R.string.content_mature_disabled_title)
        val contentGate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = false,
            )
        val chatRepo =
            FakeChatRepository(
                sessions =
                    listOf(
                        ChatSessionSummary(
                            sessionId = "s1",
                            primaryMemberId = "char-1",
                            displayName = "Test",
                            updatedAt = null,
                        ),
                    ),
            )
        val characterRepo =
            FakeCharacterRepository(
                items = emptyList(),
                details =
                    mapOf(
                        "char-1" to nsfwDetail("char-1"),
                    ),
            )
        val viewModel = ChatsListViewModel(chatRepo, characterRepo)
        val openCount = AtomicInteger(0)

        composeRule.setContent {
            ChatsListScreen(
                viewModel = viewModel,
                onOpenSession = { openCount.incrementAndGet() },
                contentGate = contentGate,
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(openLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(openLabel).performClick()
        composeRule.onNodeWithText(matureDisabledTitle).assertIsDisplayed()
        composeRule.runOnIdle {
            if (openCount.get() != 0) {
                throw AssertionError("Expected open session to be blocked.")
            }
        }
    }
}

private class FakeChatRepository(
    private val sessions: List<ChatSessionSummary>,
) : ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun sendUserMessage(content: String) = Unit

    override suspend fun startNewSession(
        memberId: String,
        shareCode: String?,
    ) = Unit

    override suspend fun openSession(
        sessionId: String,
        primaryMemberId: String?,
    ) = Unit

    override suspend fun listSessions(
        limit: Int,
        offset: Int,
    ): List<ChatSessionSummary> = sessions

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

    override suspend fun clearLocalSession() {
        _messages.value = emptyList()
    }
}

private class FakeCharacterRepository(
    private val items: List<CharacterSummary>,
    private val details: Map<String, CharacterDetail>,
    private val shareCodes: Map<String, String> = emptyMap(),
) : CharacterRepository {
    override suspend fun queryCharacters(
        cursor: String?,
        limit: Int?,
        sortBy: String?,
        isNsfw: Boolean?,
    ): List<CharacterSummary> = items

    override suspend fun getCharacterDetail(characterId: String): CharacterDetail {
        return details[characterId] ?: CharacterDetail(
            id = characterId,
            name = "Unknown",
            description = "",
            tags = emptyList(),
            creatorName = null,
            isNsfw = false,
            totalFollowers = 0,
            isFollowed = false,
        )
    }

    override suspend fun resolveShareCode(shareCode: String): String? = shareCodes[shareCode]

    override suspend fun generateShareCode(characterId: String): ShareCodeInfo? = null

    override suspend fun blockCharacter(
        characterId: String,
        value: Boolean,
    ) = Unit

    override suspend fun followCharacter(
        characterId: String,
        value: Boolean,
    ): CharacterFollowResult {
        return CharacterFollowResult(totalFollowers = 0, isFollowed = false)
    }
}

private class FakeReportRepository : ReportRepository {
    override suspend fun getReasonMeta(): ReportReasonMeta {
        return ReportReasonMeta(reasons = emptyList(), requiresDetailReasons = emptyList(), maxDetailLength = 0)
    }

    override suspend fun submitReport(
        targetId: String,
        reasons: List<String>,
        detail: String?,
        sessionId: String?,
    ) = Unit
}

private class FakeChatSessionStore : ChatSessionStore {
    private var sessionId: String? = null
    private var clientSessionId: String? = null
    private var primaryMemberId: String? = null
    private var shareCode: String? = null
    private var updatedAtMs: Long? = null

    override fun getSessionId(): String? = sessionId

    override fun setSessionId(sessionId: String?) {
        this.sessionId = sessionId
    }

    override fun getClientSessionId(): String? = clientSessionId

    override fun setClientSessionId(clientSessionId: String?) {
        this.clientSessionId = clientSessionId
    }

    override fun getPrimaryMemberId(): String? = primaryMemberId

    override fun setPrimaryMemberId(memberId: String?) {
        primaryMemberId = memberId
    }

    override fun getShareCode(): String? = shareCode

    override fun setShareCode(shareCode: String?) {
        this.shareCode = shareCode
    }

    override fun getSessionUpdatedAtMs(): Long? = updatedAtMs

    override fun setSessionUpdatedAtMs(updatedAtMs: Long?) {
        this.updatedAtMs = updatedAtMs
    }

    override fun clear() {
        sessionId = null
        clientSessionId = null
        primaryMemberId = null
        shareCode = null
        updatedAtMs = null
    }
}

private class AllowAllAccessManager : ContentAccessManager {
    override val gate: StateFlow<ContentGate> =
        MutableStateFlow(
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = true,
            ),
        )

    override fun updateGate(gate: ContentGate) = Unit

    override fun decideAccess(isNsfw: Boolean?): ContentAccessDecision {
        return ContentAccessDecision.Allowed
    }
}

private fun nsfwDetail(id: String): CharacterDetail {
    return CharacterDetail(
        id = id,
        name = "NSFW",
        description = "desc",
        tags = emptyList(),
        creatorName = null,
        isNsfw = true,
        totalFollowers = 0,
        isFollowed = false,
    )
}
