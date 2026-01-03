package com.stproject.client.android.features.chat

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.a2ui.A2UIRuntimeState
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.preferences.UserPreferencesStore
import com.stproject.client.android.core.session.ChatSessionStore
import com.stproject.client.android.domain.model.A2UIAction
import com.stproject.client.android.domain.model.A2UIActionResult
import com.stproject.client.android.domain.model.CardCreateInput
import com.stproject.client.android.domain.model.CardCreateResult
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatRole
import com.stproject.client.android.domain.model.ChatSessionSummary
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CardRepository
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

    private class FakeUserPreferencesStore : UserPreferencesStore {
        private var nsfwAllowed = false
        private var themeMode = com.stproject.client.android.core.theme.ThemeMode.System
        private var languageTag: String? = null
        private var presetId: String? = null
        private var globalVariables: Map<String, Any> = emptyMap()
        private val presetVariables = mutableMapOf<String, Map<String, Any>>()

        override fun isNsfwAllowed(): Boolean = nsfwAllowed

        override fun setNsfwAllowed(value: Boolean) {
            nsfwAllowed = value
        }

        override fun getThemeMode(): com.stproject.client.android.core.theme.ThemeMode = themeMode

        override fun setThemeMode(mode: com.stproject.client.android.core.theme.ThemeMode) {
            themeMode = mode
        }

        override fun getLanguageTag(): String? = languageTag

        override fun setLanguageTag(tag: String?) {
            languageTag = tag
        }

        override fun getModelPresetId(): String? = presetId

        override fun setModelPresetId(presetId: String?) {
            this.presetId = presetId
        }

        override fun getGlobalVariables(): Map<String, Any> = globalVariables

        override fun setGlobalVariables(variables: Map<String, Any>) {
            globalVariables = variables
        }

        override fun getPresetVariables(presetId: String): Map<String, Any> {
            return presetVariables[presetId] ?: emptyMap()
        }

        override fun setPresetVariables(
            presetId: String,
            variables: Map<String, Any>,
        ) {
            presetVariables[presetId] = variables
        }
    }

    private class FakeCardRepository(
        private val wrapper: Map<String, Any> = emptyMap(),
    ) : CardRepository {
        var updatedWrapper: Map<String, Any>? = null

        override suspend fun createCard(input: CardCreateInput): CardCreateResult {
            return CardCreateResult(characterId = "char-1", name = input.name)
        }

        override suspend fun createCardFromWrapper(wrapper: Map<String, Any>): CardCreateResult {
            return CardCreateResult(characterId = "char-1", name = null)
        }

        override suspend fun updateCardFromWrapper(
            id: String,
            wrapper: Map<String, Any>,
        ): CardCreateResult {
            updatedWrapper = wrapper
            return CardCreateResult(characterId = id, name = null)
        }

        override suspend fun fetchCardWrapper(id: String): Map<String, Any> {
            return wrapper
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
            ageRatingHint: com.stproject.client.android.domain.model.AgeRating?,
        ): ContentAccessDecision {
            return ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED)
        }
    }

    private class FakeChatRepository : ChatRepository {
        private val _messages = MutableStateFlow(emptyList<ChatMessage>())
        override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()
        override val a2uiState: Flow<A2UIRuntimeState?> =
            MutableStateFlow(null)
        var startCalls = 0
        var storedVariables: Map<String, Any> = emptyMap()
        val updateCalls = mutableListOf<Map<String, Any>>()
        val messageVariablesCalls =
            mutableListOf<Pair<String, List<Map<String, Any>>>>()

        override suspend fun sendUserMessage(content: String) {
            // no-op: we only test that ViewModel clears input and toggles sending.
        }

        override suspend fun sendA2UIAction(action: A2UIAction): A2UIActionResult {
            return A2UIActionResult(accepted = false, reason = "not_supported")
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

        override suspend fun loadSessionVariables(): Map<String, Any> = storedVariables

        override suspend fun updateSessionVariables(variables: Map<String, Any>) {
            updateCalls.add(variables)
            storedVariables = variables
        }

        override suspend fun updateMessageVariables(
            messageId: String,
            swipesData: List<Map<String, Any>>,
        ) {
            messageVariablesCalls.add(messageId to swipesData)
        }

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
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
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
    fun `start new chat blocked by access gate`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val accessManager = AllowAllAccessManager()
            val denyUseCase = DenyAccessUseCase(accessManager, FakeCharacterRepository())
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
                    resolveContentAccess = denyUseCase,
                )
            val collectJob = backgroundScope.launch { vm.uiState.collect() }

            vm.startNewChat("char-1")
            advanceUntilIdle()

            assertEquals(0, repo.startCalls)
            assertEquals("mature content disabled", vm.uiState.value.error)
            collectJob.cancel()
        }

    @Test
    fun `send toggles isSending while running`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                object : ChatRepository {
                    private val _messages = MutableStateFlow(emptyList<ChatMessage>())
                    override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()
                    override val a2uiState: Flow<A2UIRuntimeState?> =
                        MutableStateFlow(null)

                    override suspend fun sendUserMessage(content: String) {
                        // Suspend until the test advances the dispatcher.
                        kotlinx.coroutines.delay(1000)
                    }

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
                    ) = emptyList<ChatSessionSummary>()

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
                }

            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
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
                    override val a2uiState: Flow<A2UIRuntimeState?> =
                        MutableStateFlow(null)

                    override suspend fun sendUserMessage(content: String) {
                        throw ApiException(message = "boom")
                    }

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
                    ) = emptyList<ChatSessionSummary>()

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
                }

            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
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
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
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
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
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
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
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

    @Test
    fun `load variables populates ui state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                FakeChatRepository().apply {
                    storedVariables = mapOf("foo" to "bar", "count" to 2)
                }
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.variablesUiState.collect() }

            vm.loadVariables(VariablesScope.Session, emptyList())
            advanceUntilIdle()

            val state = vm.variablesUiState.value.session
            assertFalse(state.isLoading)
            assertNull(state.error)
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val parsed = Gson().fromJson<Map<String, Any>>(state.text, type)
            assertEquals("bar", parsed["foo"])
            assertEquals(2.0, parsed["count"])
            collectJob.cancel()
        }

    @Test
    fun `save variables persists valid json`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.variablesUiState.collect() }

            vm.updateVariablesText(VariablesScope.Session, """{"foo":"bar","count":2}""")
            vm.saveVariables(VariablesScope.Session, emptyList())
            advanceUntilIdle()

            assertEquals(1, repo.updateCalls.size)
            val saved = repo.updateCalls.first()
            assertEquals("bar", saved["foo"])
            assertEquals(2.0, saved["count"])
            assertFalse(vm.variablesUiState.value.session.isDirty)
            assertNull(vm.variablesUiState.value.session.error)
            collectJob.cancel()
        }

    @Test
    fun `save variables rejects invalid json`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.variablesUiState.collect() }

            vm.updateVariablesText(VariablesScope.Session, "{")
            vm.saveVariables(VariablesScope.Session, emptyList())
            advanceUntilIdle()

            assertEquals("invalid json", vm.variablesUiState.value.session.error)
            assertTrue(repo.updateCalls.isEmpty())
            collectJob.cancel()
        }

    @Test
    fun `load global variables populates ui state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val prefs =
                FakeUserPreferencesStore().apply {
                    setGlobalVariables(mapOf("foo" to "bar", "count" to 2))
                }
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = prefs,
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.variablesUiState.collect() }

            vm.loadVariables(VariablesScope.Global, emptyList())
            advanceUntilIdle()

            val state = vm.variablesUiState.value.global
            assertFalse(state.isLoading)
            assertNull(state.error)
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val parsed = Gson().fromJson<Map<String, Any>>(state.text, type)
            assertEquals("bar", parsed["foo"])
            assertEquals(2.0, parsed["count"])
            collectJob.cancel()
        }

    @Test
    fun `save global variables persists to preferences`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val prefs = FakeUserPreferencesStore()
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = prefs,
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.variablesUiState.collect() }

            vm.updateVariablesText(VariablesScope.Global, """{"score":3}""")
            vm.saveVariables(VariablesScope.Global, emptyList())
            advanceUntilIdle()

            assertEquals(3.0, prefs.getGlobalVariables()["score"])
            assertNull(vm.variablesUiState.value.global.error)
            collectJob.cancel()
        }

    @Test
    fun `load preset variables populates ui state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val prefs =
                FakeUserPreferencesStore().apply {
                    setModelPresetId("preset-1")
                    setPresetVariables("preset-1", mapOf("foo" to "bar", "count" to 2))
                }
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = prefs,
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.variablesUiState.collect() }

            vm.loadVariables(VariablesScope.Preset, emptyList())
            advanceUntilIdle()

            val state = vm.variablesUiState.value.preset
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals("preset-1", vm.variablesUiState.value.presetId)
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val parsed = Gson().fromJson<Map<String, Any>>(state.text, type)
            assertEquals("bar", parsed["foo"])
            assertEquals(2.0, parsed["count"])
            collectJob.cancel()
        }

    @Test
    fun `save preset variables persists to preferences`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val prefs =
                FakeUserPreferencesStore().apply {
                    setModelPresetId("preset-1")
                }
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = prefs,
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.variablesUiState.collect() }

            vm.updateVariablesText(VariablesScope.Preset, """{"score":3}""")
            vm.saveVariables(VariablesScope.Preset, emptyList())
            advanceUntilIdle()

            assertEquals(3.0, prefs.getPresetVariables("preset-1")["score"])
            assertNull(vm.variablesUiState.value.preset.error)
            collectJob.cancel()
        }

    @Test
    fun `load character variables populates ui state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val wrapper =
                mapOf(
                    "data" to
                        mapOf(
                            "extensions" to
                                mapOf(
                                    "tavern_helper" to
                                        mapOf(
                                            "variables" to
                                                mapOf(
                                                    "foo" to "bar",
                                                    "count" to 2,
                                                ),
                                        ),
                                ),
                        ),
                )
            val cardRepo = FakeCardRepository(wrapper)
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = cardRepo,
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.variablesUiState.collect() }

            vm.loadVariables(VariablesScope.Character, emptyList())
            advanceUntilIdle()

            val state = vm.variablesUiState.value.character
            assertFalse(state.isLoading)
            assertNull(state.error)
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val parsed = Gson().fromJson<Map<String, Any>>(state.text, type)
            assertEquals("bar", parsed["foo"])
            assertEquals(2.0, parsed["count"])
            collectJob.cancel()
        }

    @Test
    fun `save character variables updates card wrapper`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val wrapper = mapOf("data" to mapOf("extensions" to emptyMap<String, Any>()))
            val cardRepo = FakeCardRepository(wrapper)
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = cardRepo,
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.variablesUiState.collect() }

            vm.updateVariablesText(VariablesScope.Character, """{"mood":"ok"}""")
            vm.saveVariables(VariablesScope.Character, emptyList())
            advanceUntilIdle()

            val updated = cardRepo.updatedWrapper ?: emptyMap()
            val data = updated["data"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
            val extensions = data["extensions"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
            val tavernHelper = extensions["tavern_helper"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
            val vars = tavernHelper["variables"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
            assertEquals("ok", vars["mood"])
            collectJob.cancel()
        }

    @Test
    fun `save message variables persists swipes data`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeChatRepository()
            val vm =
                ChatViewModel(
                    chatRepository = repo,
                    sendUserMessage = SendUserMessageUseCase(repo),
                    characterRepository = FakeCharacterRepository(),
                    cardRepository = FakeCardRepository(),
                    chatSessionStore = FakeChatSessionStore(),
                    userPreferencesStore = FakeUserPreferencesStore(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.variablesUiState.collect() }
            val messages =
                listOf(
                    ChatMessage(
                        id = "m1",
                        role = ChatRole.Assistant,
                        content = "hello",
                        serverId = "srv1",
                        swipes = listOf("hello", "alt"),
                        swipeId = 1,
                        metadata =
                            mapOf(
                                "swipes_data" to
                                    listOf(
                                        mapOf("foo" to "bar"),
                                        mapOf("foo" to "old"),
                                    ),
                            ),
                    ),
                )

            vm.loadVariables(VariablesScope.Message, messages)
            advanceUntilIdle()
            vm.updateVariablesText(VariablesScope.Message, """{"score":3}""")
            vm.saveVariables(VariablesScope.Message, messages)
            advanceUntilIdle()

            assertEquals(1, repo.messageVariablesCalls.size)
            val call = repo.messageVariablesCalls.first()
            assertEquals("m1", call.first)
            assertEquals(2, call.second.size)
            assertEquals("bar", call.second[0]["foo"])
            assertEquals(3.0, call.second[1]["score"])
            collectJob.cancel()
        }
}
