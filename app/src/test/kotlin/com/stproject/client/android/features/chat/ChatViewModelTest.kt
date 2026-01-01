package com.stproject.client.android.features.chat

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.session.ChatSessionStore
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatSessionSummary
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import com.stproject.client.android.domain.usecase.SendUserMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest : BaseUnitTest() {
    private class FakeCharacterRepository : CharacterRepository {
        override suspend fun queryCharacters(
            cursor: String?,
            limit: Int?,
            sortBy: String?,
            isNsfw: Boolean?,
        ) = emptyList<CharacterSummary>()

        override suspend fun getCharacterDetail(characterId: String): CharacterDetail {
            return CharacterDetail(
                id = characterId,
                name = "Test",
                description = "",
                tags = emptyList(),
                creatorName = null,
                isNsfw = false,
                totalFollowers = 0,
                isFollowed = false,
            )
        }

        override suspend fun resolveShareCode(shareCode: String): String? = null

        override suspend fun generateShareCode(characterId: String) = ShareCodeInfo("code", "url")

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

    private class FakeChatSessionStore : ChatSessionStore {
        override fun getSessionId(): String? = "s1"

        override fun setSessionId(sessionId: String?) = Unit

        override fun getClientSessionId(): String? = null

        override fun setClientSessionId(clientSessionId: String?) = Unit

        override fun getPrimaryMemberId(): String? = "char-1"

        override fun setPrimaryMemberId(memberId: String?) = Unit

        override fun getShareCode(): String? = null

        override fun setShareCode(shareCode: String?) = Unit

        override fun getSessionUpdatedAtMs(): Long? = null

        override fun setSessionUpdatedAtMs(updatedAtMs: Long?) = Unit

        override fun clear() = Unit
    }

    internal class AllowAllAccessManager : ContentAccessManager {
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

    private class DenyAccessUseCase(
        accessManager: ContentAccessManager,
        characterRepository: CharacterRepository,
    ) : ResolveContentAccessUseCase(
            accessManager = accessManager,
            characterRepository = characterRepository,
        ) {
        override suspend fun execute(
            memberId: String?,
            isNsfwHint: Boolean?,
        ): ContentAccessDecision {
            return ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED)
        }
    }

    private class FakeChatRepository : ChatRepository {
        private val _messages = MutableStateFlow(emptyList<ChatMessage>())
        override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()
        var startCalls = 0

        override suspend fun sendUserMessage(content: String) {
            // no-op: we only test that ViewModel clears input and toggles sending.
        }

        override suspend fun startNewSession(
            memberId: String,
            shareCode: String?,
        ) {
            startCalls += 1
        }

        override suspend fun openSession(
            sessionId: String,
            primaryMemberId: String?,
        ) = Unit

        override suspend fun listSessions(
            limit: Int,
            offset: Int,
        ) = emptyList<ChatSessionSummary>()

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

        override suspend fun clearLocalSession() = Unit
    }

    @Test
    fun `send clears input`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.uiState.collect() }

            vm.onInputChanged(" hello ")
            vm.onSendClicked()
            advanceUntilIdle()

            assertEquals("", vm.uiState.value.input)
            collectJob.cancel()
        }

    @Test
    fun `send toggles isSending while running`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                object : ChatRepository {
                    private val _messages = MutableStateFlow(emptyList<ChatMessage>())
                    override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

                    override suspend fun sendUserMessage(content: String) {
                        // Suspend until the test advances the dispatcher.
                        kotlinx.coroutines.delay(1000)
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
                    ) = emptyList<ChatSessionSummary>()

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

                    override suspend fun clearLocalSession() = Unit
                }

            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.uiState.collect() }

            vm.onInputChanged("hi")
            vm.onSendClicked()
            runCurrent()

            // Job launched but not completed yet.
            assertTrue(vm.uiState.value.isSending)
            assertNull(vm.uiState.value.error)

            advanceTimeBy(1000)
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isSending)
            collectJob.cancel()
        }

    @Test
    fun `api exception sets error and does not clear input`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                object : ChatRepository {
                    private val _messages = MutableStateFlow(emptyList<ChatMessage>())
                    override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

                    override suspend fun sendUserMessage(content: String) {
                        throw ApiException(message = "boom")
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
                    ) = emptyList<ChatSessionSummary>()

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

                    override suspend fun clearLocalSession() = Unit
                }

            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.uiState.collect() }

            vm.onInputChanged("hello")
            vm.onSendClicked()
            advanceUntilIdle()

            assertEquals("hello", vm.uiState.value.input)
            assertEquals("boom", vm.uiState.value.error)
            assertFalse(vm.uiState.value.isSending)
            collectJob.cancel()
        }

    @Test
    fun `start new chat invokes onSuccess when allowed`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.uiState.collect() }
            var navigated = false

            vm.startNewChat("char-1", onSuccess = { navigated = true })
            advanceUntilIdle()

            assertTrue(navigated)
            assertEquals(1, repo.startCalls)
            collectJob.cancel()
        }

    @Test
    fun `start new chat blocks navigation when access denied`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    resolveContentAccess =
                        DenyAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.uiState.collect() }
            var navigated = false

            vm.startNewChat("char-1", onSuccess = { navigated = true })
            advanceUntilIdle()

            assertFalse(navigated)
            assertEquals(0, repo.startCalls)
            assertTrue(vm.uiState.value.error?.contains("mature") == true)
            collectJob.cancel()
        }

    @Test
    fun `request share code blocks when access denied`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    resolveContentAccess =
                        DenyAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.uiState.collect() }

            vm.requestShareCode()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.error?.contains("mature") == true)
            assertNull(vm.uiState.value.shareInfo)
            collectJob.cancel()
        }
}
