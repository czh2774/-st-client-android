package com.stproject.client.android.features.chats

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.a2ui.A2UIRuntimeState
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.domain.model.A2UIAction
import com.stproject.client.android.domain.model.A2UIActionResult
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatSessionSummary
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatsListViewModelTest : BaseUnitTest() {
    private class FakeChatRepository(
        private val sessions: List<ChatSessionSummary>,
        private val lastSession: ChatSessionSummary? = null,
    ) : ChatRepository {
        private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()
        override val a2uiState: Flow<A2UIRuntimeState?> =
            MutableStateFlow(null)
        var listCalls = 0

        override suspend fun sendUserMessage(content: String) = Unit

        override suspend fun sendA2UIAction(action: A2UIAction): A2UIActionResult {
            return A2UIActionResult(accepted = false, reason = "not_supported")
        }

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
        ): List<ChatSessionSummary> {
            listCalls += 1
            return sessions
        }

        override suspend fun getLastSessionSummary(): ChatSessionSummary? = lastSession

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

        override suspend fun loadSessionVariables(): Map<String, Any?> = emptyMap()

        override suspend fun updateSessionVariables(variables: Map<String, Any?>) = Unit

        override suspend fun updateMessageVariables(
            messageId: String,
            swipesData: List<Map<String, Any?>>,
        ) = Unit

        override suspend fun clearLocalSession() = Unit
    }

    private class FakeCharacterRepository : CharacterRepository {
        var detailCalls = 0
        var detailMap: Map<String, Boolean> = emptyMap()

        override suspend fun queryCharacters(
            cursor: String?,
            limit: Int?,
            sortBy: String?,
            isNsfw: Boolean?,
        ): List<CharacterSummary> = emptyList()

        override suspend fun getCharacterDetail(characterId: String): CharacterDetail {
            detailCalls += 1
            val isNsfw = detailMap[characterId] ?: false
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

        override suspend fun resolveShareCode(shareCode: String): String? = null

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

    private class AllowAllAccessManager : ContentAccessManager {
        private val _gate =
            MutableStateFlow(
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = true,
                ),
            )

        override val gate: StateFlow<ContentGate> = _gate

        override fun updateGate(gate: ContentGate) {
            _gate.value = gate
        }

        override fun decideAccess(isNsfw: Boolean?): ContentAccessDecision {
            return ContentAccessDecision.Allowed
        }
    }

    private class DenyAccessUseCase :
        ResolveContentAccessUseCase(
            accessManager = AllowAllAccessManager(),
            characterRepository = FakeCharacterRepository(),
        ) {
        override suspend fun execute(
            memberId: String?,
            isNsfwHint: Boolean?,
            ageRatingHint: com.stproject.client.android.domain.model.AgeRating?,
            tags: List<String>?,
            requireMetadata: Boolean,
        ): ContentAccessDecision {
            return ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED)
        }
    }

    @Test
    fun `load resolves nsfw for sessions when not allowed`() =
        runTest(mainDispatcherRule.dispatcher) {
            val sessions =
                listOf(
                    ChatSessionSummary(
                        sessionId = "s1",
                        primaryMemberId = "char-1",
                        displayName = "One",
                        updatedAt = null,
                    ),
                    ChatSessionSummary(
                        sessionId = "s2",
                        primaryMemberId = null,
                        displayName = "Two",
                        updatedAt = null,
                    ),
                )
            val chatRepo = FakeChatRepository(sessions)
            val characterRepo =
                FakeCharacterRepository().apply {
                    detailMap = mapOf("char-1" to true)
                }

            val viewModel =
                ChatsListViewModel(
                    chatRepository = chatRepo,
                    characterRepository = characterRepo,
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = characterRepo,
                        ),
                )
            viewModel.load(allowNsfw = false, blockedTags = emptyList())
            advanceUntilIdle()

            val resolved = viewModel.uiState.value.items
            assertEquals(true, resolved.first().primaryMemberIsNsfw)
            assertEquals(1, characterRepo.detailCalls)
        }

    @Test
    fun `load skips nsfw resolve when allowed`() =
        runTest(mainDispatcherRule.dispatcher) {
            val sessions =
                listOf(
                    ChatSessionSummary(
                        sessionId = "s1",
                        primaryMemberId = "char-1",
                        displayName = "One",
                        updatedAt = null,
                    ),
                )
            val chatRepo = FakeChatRepository(sessions)
            val characterRepo = FakeCharacterRepository()

            val viewModel =
                ChatsListViewModel(
                    chatRepository = chatRepo,
                    characterRepository = characterRepo,
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = characterRepo,
                        ),
                )
            viewModel.load(allowNsfw = true, blockedTags = emptyList())
            advanceUntilIdle()

            assertEquals(0, characterRepo.detailCalls)
        }

    @Test
    fun `load sets error when access denied`() =
        runTest(mainDispatcherRule.dispatcher) {
            val chatRepo = FakeChatRepository(emptyList())
            val characterRepo = FakeCharacterRepository()
            val viewModel =
                ChatsListViewModel(
                    chatRepository = chatRepo,
                    characterRepository = characterRepo,
                    resolveContentAccess = DenyAccessUseCase(),
                )

            viewModel.load(allowNsfw = false, blockedTags = emptyList())
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.error?.contains("mature") == true)
            assertEquals(0, chatRepo.listCalls)
        }

    @Test
    fun `load exposes last session summary`() =
        runTest(mainDispatcherRule.dispatcher) {
            val last =
                ChatSessionSummary(
                    sessionId = "s-last",
                    primaryMemberId = "char-1",
                    displayName = "Last",
                    updatedAt = null,
                )
            val chatRepo = FakeChatRepository(emptyList(), lastSession = last)
            val characterRepo =
                FakeCharacterRepository().apply {
                    detailMap = mapOf("char-1" to true)
                }
            val viewModel =
                ChatsListViewModel(
                    chatRepository = chatRepo,
                    characterRepository = characterRepo,
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = characterRepo,
                        ),
                )

            viewModel.load(allowNsfw = false, blockedTags = emptyList())
            advanceUntilIdle()

            val resolved = viewModel.uiState.value.lastSession
            assertEquals("s-last", resolved?.sessionId)
            assertEquals(true, resolved?.primaryMemberIsNsfw)
            assertEquals(1, characterRepo.detailCalls)
        }
}
