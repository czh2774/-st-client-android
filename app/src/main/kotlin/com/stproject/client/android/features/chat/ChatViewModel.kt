package com.stproject.client.android.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.stproject.client.android.core.a2ui.A2UIRuntimeState
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.preferences.UserPreferencesStore
import com.stproject.client.android.core.session.ChatSessionStore
import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.domain.model.A2UIAction
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatRole
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CardRepository
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import com.stproject.client.android.domain.usecase.SendUserMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel
    @Inject
    constructor(
        private val chatRepository: ChatRepository,
        private val sendUserMessage: SendUserMessageUseCase,
        private val characterRepository: CharacterRepository,
        private val cardRepository: CardRepository,
        private val chatSessionStore: ChatSessionStore,
        private val userPreferencesStore: UserPreferencesStore,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        private val variablesType = object : TypeToken<Map<String, Any>>() {}.type
        private val input = MutableStateFlow("")
        private val isSending = MutableStateFlow(false)
        private val isActionRunning = MutableStateFlow(false)
        private val shareInfo = MutableStateFlow<ShareCodeInfo?>(null)
        private val activeCharacterIsNsfw = MutableStateFlow<Boolean?>(null)
        private val activeCharacterAgeRating = MutableStateFlow<AgeRating?>(null)
        private val accessError = MutableStateFlow<String?>(null)
        private val error = MutableStateFlow<String?>(null)
        private val variablesState = MutableStateFlow(ChatVariablesUiState())

        val uiState: StateFlow<ChatUiState> =
            combine(
                baseStateFlow(),
                activeCharacterIsNsfw,
                activeCharacterAgeRating,
                accessError,
                error,
            ) { state, isNsfw, ageRating, accessErrorText, errorText ->
                state.copy(
                    activeCharacterIsNsfw = isNsfw,
                    activeCharacterAgeRating = ageRating,
                    accessError = accessErrorText,
                    error = errorText,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

        private fun baseStateFlow(): Flow<ChatUiState> =
            combine(
                chatRepository.messages,
                input,
                isSending,
                isActionRunning,
                shareInfo,
            ) { messages, inputText, sending, actionRunning, share ->
                ChatUiState(
                    messages = messages,
                    input = inputText,
                    isSending = sending,
                    isActionRunning = actionRunning,
                    shareInfo = share,
                    activeCharacterIsNsfw = null,
                    accessError = null,
                    error = null,
                )
            }

        val variablesUiState: StateFlow<ChatVariablesUiState> =
            variablesState.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ChatVariablesUiState(),
            )

        val a2uiState: StateFlow<A2UIRuntimeState?> =
            chatRepository.a2uiState.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null,
            )

        fun onInputChanged(value: String) {
            input.value = value
        }

        fun onSendClicked() {
            val content = input.value.trim()
            if (content.isEmpty()) return
            if (isSending.value) return

            // Update UI state synchronously for immediate feedback and stable tests.
            isSending.value = true
            error.value = null

            viewModelScope.launch {
                try {
                    if (!ensureSessionAccess()) return@launch
                    sendUserMessage(content)
                    input.value = ""
                } catch (e: ApiException) {
                    error.value = e.userMessage ?: e.message
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    error.value = "unexpected error"
                } finally {
                    isSending.value = false
                }
            }
        }

        fun onA2UIAction(action: A2UIAction) {
            if (action.name.isBlank()) return
            when (action.normalizedName) {
                "sendmessage", "send" -> handleA2UISend(action)
                "continue", "continuegeneration" -> handleA2UIFallback(action, chatRepository::continueMessage)
                "regenerate" -> handleA2UIFallback(action, chatRepository::regenerateMessage)
                else -> handleA2UIEvent(action)
            }
        }

        fun refreshAccessForActiveSession() {
            viewModelScope.launch {
                val memberId = chatSessionStore.getPrimaryMemberId()?.trim().orEmpty()
                if (memberId.isEmpty()) {
                    accessError.value = null
                    return@launch
                }
                val access =
                    resolveContentAccess.execute(
                        memberId,
                        activeCharacterIsNsfw.value,
                        ageRatingHint = activeCharacterAgeRating.value,
                    )
                if (access is ContentAccessDecision.Blocked) {
                    handleAccessBlocked(access)
                    return@launch
                }
                accessError.value = null
                if (activeCharacterIsNsfw.value == null || activeCharacterAgeRating.value == null) {
                    resolveCharacterNsfw(memberId)
                }
            }
        }

        fun startNewChat(
            memberId: String,
            shareCode: String? = null,
            onSuccess: (() -> Unit)? = null,
        ) {
            activeCharacterIsNsfw.value = null
            activeCharacterAgeRating.value = null
            accessError.value = null
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId, null)
                    if (access is ContentAccessDecision.Blocked) {
                        handleAccessBlocked(access)
                        return@launch
                    }
                    chatRepository.startNewSession(memberId, shareCode)
                    resolveCharacterNsfw(memberId)
                    error.value = null
                    onSuccess?.invoke()
                } catch (e: ApiException) {
                    error.value = e.userMessage ?: e.message
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    error.value = "unexpected error"
                }
            }
        }

        fun openSession(
            sessionId: String,
            primaryMemberId: String?,
            onSuccess: (() -> Unit)? = null,
        ) {
            activeCharacterIsNsfw.value = null
            activeCharacterAgeRating.value = null
            accessError.value = null
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(primaryMemberId, null)
                    if (access is ContentAccessDecision.Blocked) {
                        handleAccessBlocked(access)
                        return@launch
                    }
                    chatRepository.openSession(sessionId, primaryMemberId)
                    resolveCharacterNsfw(primaryMemberId)
                    error.value = null
                    onSuccess?.invoke()
                } catch (e: ApiException) {
                    error.value = e.userMessage ?: e.message
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    error.value = "unexpected error"
                }
            }
        }

        fun regenerateMessage(message: com.stproject.client.android.domain.model.ChatMessage) {
            runAction {
                chatRepository.regenerateMessage(message.serverId ?: message.id)
            }
        }

        fun continueMessage(message: com.stproject.client.android.domain.model.ChatMessage) {
            runAction {
                chatRepository.continueMessage(message.serverId ?: message.id)
            }
        }

        fun deleteMessage(
            message: com.stproject.client.android.domain.model.ChatMessage,
            deleteAfter: Boolean,
        ) {
            runAction {
                chatRepository.deleteMessage(message.serverId ?: message.id, deleteAfter)
            }
        }

        fun setActiveSwipe(
            message: com.stproject.client.android.domain.model.ChatMessage,
            swipeId: Int,
        ) {
            runAction {
                chatRepository.setActiveSwipe(message.serverId ?: message.id, swipeId)
            }
        }

        fun deleteSwipe(
            message: com.stproject.client.android.domain.model.ChatMessage,
            swipeId: Int?,
        ) {
            runAction {
                chatRepository.deleteSwipe(message.serverId ?: message.id, swipeId)
            }
        }

        fun requestShareCode() {
            runAction {
                val memberId =
                    chatSessionStore.getPrimaryMemberId()?.trim()?.takeIf { it.isNotEmpty() }
                        ?: throw ApiException(message = "missing character", userMessage = "missing character")
                val access = resolveContentAccess.execute(memberId, null)
                if (access is ContentAccessDecision.Blocked) {
                    handleAccessBlocked(access)
                    return@runAction
                }
                val info =
                    characterRepository.generateShareCode(memberId)
                        ?: throw ApiException(message = "share failed", userMessage = "share failed")
                shareInfo.value = info
            }
        }

        fun clearShareInfo() {
            shareInfo.value = null
        }

        fun setVariablesScope(
            scope: VariablesScope,
            messages: List<ChatMessage>,
        ) {
            if (variablesState.value.activeScope == scope) return
            variablesState.update { it.copy(activeScope = scope) }
            loadVariables(scope, messages)
        }

        fun loadVariables(
            scope: VariablesScope,
            messages: List<ChatMessage>,
        ) {
            val editor = getEditorState(variablesState.value, scope)
            if (editor.isLoading || editor.isSaving) return
            updateEditorState(scope) { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    when (scope) {
                        VariablesScope.Session -> {
                            val variables = chatRepository.loadSessionVariables()
                            val text = gson.toJson(variables)
                            variablesState.update {
                                it.copy(
                                    session =
                                        it.session.copy(
                                            text = text,
                                            isLoading = false,
                                            isDirty = false,
                                            error = null,
                                        ),
                                )
                            }
                        }
                        VariablesScope.Global -> {
                            val variables = userPreferencesStore.getGlobalVariables()
                            val text = gson.toJson(variables)
                            variablesState.update {
                                it.copy(
                                    global =
                                        it.global.copy(
                                            text = text,
                                            isLoading = false,
                                            isDirty = false,
                                            error = null,
                                        ),
                                )
                            }
                        }
                        VariablesScope.Preset -> {
                            val presetId =
                                userPreferencesStore.getModelPresetId()?.trim()?.takeIf { it.isNotEmpty() }
                            val variables =
                                if (presetId == null) {
                                    emptyMap()
                                } else {
                                    userPreferencesStore.getPresetVariables(presetId)
                                }
                            val text = gson.toJson(variables)
                            variablesState.update {
                                it.copy(
                                    presetId = presetId,
                                    preset =
                                        it.preset.copy(
                                            text = text,
                                            isLoading = false,
                                            isDirty = false,
                                            error = null,
                                        ),
                                )
                            }
                        }
                        VariablesScope.Character -> {
                            val characterId =
                                chatSessionStore.getPrimaryMemberId()?.trim()?.takeIf { it.isNotEmpty() }
                            if (characterId == null) {
                                variablesState.update {
                                    it.copy(
                                        characterId = null,
                                        character =
                                            it.character.copy(
                                                text = gson.toJson(emptyMap<String, Any>()),
                                                isLoading = false,
                                                isDirty = false,
                                                error = null,
                                            ),
                                    )
                                }
                            } else {
                                val wrapper = cardRepository.fetchCardWrapper(characterId)
                                val variables = extractTavernHelperVariables(wrapper)
                                val text = gson.toJson(variables)
                                variablesState.update {
                                    it.copy(
                                        characterId = characterId,
                                        character =
                                            it.character.copy(
                                                text = text,
                                                isLoading = false,
                                                isDirty = false,
                                                error = null,
                                            ),
                                    )
                                }
                            }
                        }
                        VariablesScope.Message -> {
                            val options = messages.filter { it.serverId != null }
                            val selectedMessage =
                                resolveSelectedMessage(
                                    messages = options,
                                    selectedId = variablesState.value.selectedMessageId,
                                )
                            val variables =
                                selectedMessage?.let { readMessageVariables(it) } ?: emptyMap()
                            val text = gson.toJson(variables)
                            variablesState.update {
                                it.copy(
                                    selectedMessageId = selectedMessage?.id,
                                    message =
                                        it.message.copy(
                                            text = text,
                                            isLoading = false,
                                            isDirty = false,
                                            error = null,
                                        ),
                                )
                            }
                        }
                    }
                } catch (e: ApiException) {
                    updateEditorState(scope) {
                        it.copy(isLoading = false, error = e.userMessage ?: e.message)
                    }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    updateEditorState(scope) { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun selectMessageVariables(
            messageId: String,
            messages: List<ChatMessage>,
        ) {
            val options = messages.filter { it.serverId != null }
            val selected = options.firstOrNull { it.id == messageId }
            val variables = selected?.let { readMessageVariables(it) } ?: emptyMap()
            val text = gson.toJson(variables)
            variablesState.update {
                it.copy(
                    selectedMessageId = selected?.id,
                    message =
                        it.message.copy(
                            text = text,
                            isLoading = false,
                            isDirty = false,
                            error = null,
                        ),
                )
            }
        }

        fun updateVariablesText(
            scope: VariablesScope,
            value: String,
        ) {
            updateEditorState(scope) { it.copy(text = value, isDirty = true) }
        }

        fun saveVariables(
            scope: VariablesScope,
            messages: List<ChatMessage>,
        ) {
            val editor = getEditorState(variablesState.value, scope)
            if (editor.isSaving) return
            val raw = editor.text.trim()
            val parsed =
                if (raw.isEmpty()) {
                    emptyMap()
                } else {
                    runCatching { gson.fromJson<Map<String, Any>>(raw, variablesType) }.getOrNull()
                }
            if (parsed == null) {
                updateEditorState(scope) { it.copy(error = "invalid json", isSaving = false) }
                return
            }
            updateEditorState(scope) { it.copy(isSaving = true, error = null) }
            viewModelScope.launch {
                try {
                    when (scope) {
                        VariablesScope.Session -> {
                            chatRepository.updateSessionVariables(parsed)
                            variablesState.update {
                                it.copy(
                                    session =
                                        it.session.copy(
                                            text = gson.toJson(parsed),
                                            isSaving = false,
                                            isDirty = false,
                                            error = null,
                                        ),
                                )
                            }
                        }
                        VariablesScope.Global -> {
                            userPreferencesStore.setGlobalVariables(parsed)
                            variablesState.update {
                                it.copy(
                                    global =
                                        it.global.copy(
                                            text = gson.toJson(parsed),
                                            isSaving = false,
                                            isDirty = false,
                                            error = null,
                                        ),
                                )
                            }
                        }
                        VariablesScope.Preset -> {
                            val presetId =
                                variablesState.value.presetId
                                    ?: userPreferencesStore.getModelPresetId()?.trim()?.takeIf { it.isNotEmpty() }
                            if (presetId == null) {
                                updateEditorState(scope) {
                                    it.copy(isSaving = false, error = "missing preset")
                                }
                                return@launch
                            }
                            userPreferencesStore.setPresetVariables(presetId, parsed)
                            variablesState.update {
                                it.copy(
                                    presetId = presetId,
                                    preset =
                                        it.preset.copy(
                                            text = gson.toJson(parsed),
                                            isSaving = false,
                                            isDirty = false,
                                            error = null,
                                        ),
                                )
                            }
                        }
                        VariablesScope.Character -> {
                            val characterId =
                                variablesState.value.characterId
                                    ?: chatSessionStore.getPrimaryMemberId()?.trim()?.takeIf { it.isNotEmpty() }
                            if (characterId == null) {
                                updateEditorState(scope) {
                                    it.copy(isSaving = false, error = "missing character")
                                }
                                return@launch
                            }
                            val wrapper = cardRepository.fetchCardWrapper(characterId)
                            val updated = updateTavernHelperWrapper(wrapper, parsed)
                            cardRepository.updateCardFromWrapper(characterId, updated)
                            variablesState.update {
                                it.copy(
                                    characterId = characterId,
                                    character =
                                        it.character.copy(
                                            text = gson.toJson(parsed),
                                            isSaving = false,
                                            isDirty = false,
                                            error = null,
                                        ),
                                )
                            }
                        }
                        VariablesScope.Message -> {
                            val selectedId = variablesState.value.selectedMessageId
                            val selected =
                                messages.firstOrNull { it.id == selectedId && it.serverId != null }
                            if (selected == null) {
                                updateEditorState(scope) {
                                    it.copy(isSaving = false, error = "message not ready")
                                }
                                return@launch
                            }
                            val swipesData = buildSwipesData(selected, parsed)
                            chatRepository.updateMessageVariables(selected.id, swipesData)
                            variablesState.update {
                                it.copy(
                                    message =
                                        it.message.copy(
                                            text = gson.toJson(parsed),
                                            isSaving = false,
                                            isDirty = false,
                                            error = null,
                                        ),
                                )
                            }
                        }
                    }
                } catch (e: ApiException) {
                    updateEditorState(scope) {
                        it.copy(isSaving = false, error = e.userMessage ?: e.message)
                    }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    updateEditorState(scope) { it.copy(isSaving = false, error = "unexpected error") }
                }
            }
        }

        private fun getEditorState(
            state: ChatVariablesUiState,
            scope: VariablesScope,
        ): VariablesEditorState =
            when (scope) {
                VariablesScope.Session -> state.session
                VariablesScope.Global -> state.global
                VariablesScope.Preset -> state.preset
                VariablesScope.Character -> state.character
                VariablesScope.Message -> state.message
            }

        private fun updateEditorState(
            scope: VariablesScope,
            updater: (VariablesEditorState) -> VariablesEditorState,
        ) {
            variablesState.update { state ->
                when (scope) {
                    VariablesScope.Session -> state.copy(session = updater(state.session))
                    VariablesScope.Global -> state.copy(global = updater(state.global))
                    VariablesScope.Preset -> state.copy(preset = updater(state.preset))
                    VariablesScope.Character -> state.copy(character = updater(state.character))
                    VariablesScope.Message -> state.copy(message = updater(state.message))
                }
            }
        }

        private fun resolveSelectedMessage(
            messages: List<ChatMessage>,
            selectedId: String?,
        ): ChatMessage? {
            if (messages.isEmpty()) return null
            val current = selectedId?.let { id -> messages.firstOrNull { it.id == id } }
            if (current != null) return current
            val lastAssistant = messages.lastOrNull { it.role == ChatRole.Assistant }
            return lastAssistant ?: messages.last()
        }

        private fun readMessageVariables(message: ChatMessage): Map<String, Any> {
            val metadata = message.metadata ?: emptyMap()
            val swipesData = metadata["swipes_data"] as? List<*>
            if (!swipesData.isNullOrEmpty()) {
                val idx =
                    (message.swipeId ?: 0).coerceIn(0, swipesData.lastIndex.coerceAtLeast(0))
                val entry = swipesData.getOrNull(idx)
                return asStringKeyMap(entry)
            }
            return asStringKeyMap(metadata)
        }

        private fun buildSwipesData(
            message: ChatMessage,
            nextVars: Map<String, Any>,
        ): List<Map<String, Any>> {
            val swipeCount = message.swipes.size.takeIf { it > 0 } ?: 1
            val swipeId = (message.swipeId ?: 0).coerceIn(0, swipeCount - 1)
            val existing =
                (message.metadata?.get("swipes_data") as? List<*>)?.map { asStringKeyMap(it) }
                    ?: emptyList()
            val swipes = existing.toMutableList()
            while (swipes.size < swipeCount) {
                swipes.add(emptyMap())
            }
            if (swipes.isEmpty()) {
                swipes.add(emptyMap())
            }
            swipes[swipeId] = nextVars
            return swipes
        }

        private fun extractTavernHelperVariables(wrapper: Map<String, Any>): Map<String, Any> {
            val data = asStringKeyMap(wrapper["data"])
            val extensions = asStringKeyMap(data["extensions"])
            val tavernHelper = parseTavernHelperSettings(extensions["tavern_helper"]) ?: return emptyMap()
            val vars =
                tavernHelper["variables"]
                    ?: tavernHelper["character_variables"]
                    ?: tavernHelper["characterScriptVariables"]
            return asStringKeyMap(vars)
        }

        private fun updateTavernHelperWrapper(
            wrapper: Map<String, Any>,
            nextVars: Map<String, Any>,
        ): Map<String, Any> {
            val data = asMutableStringKeyMap(wrapper["data"])
            val extensions = asMutableStringKeyMap(data["extensions"])
            val next = updateTavernHelperVariables(extensions["tavern_helper"], nextVars)
            extensions["tavern_helper"] = next
            data["extensions"] = extensions
            return wrapper.toMutableMap().apply { this["data"] = data }
        }

        private fun updateTavernHelperVariables(
            raw: Any?,
            nextVars: Map<String, Any>,
        ): Any {
            if (raw is List<*>) {
                var updated = false
                val next =
                    raw.map { entry ->
                        if (entry is List<*> && entry.size >= 2) {
                            val key = entry[0] as? String
                            val trimmed = key?.trim().orEmpty()
                            if (!updated &&
                                (trimmed == "variables" ||
                                    trimmed == "character_variables" ||
                                    trimmed == "characterScriptVariables")
                            ) {
                                updated = true
                                listOf(entry[0], nextVars)
                            } else {
                                entry
                            }
                        } else {
                            entry
                        }
                    }.toMutableList()
                if (!updated) {
                    next.add(listOf("variables", nextVars))
                }
                return next
            }
            val base = asMutableStringKeyMap(raw)
            when {
                base.containsKey("variables") -> base["variables"] = nextVars
                base.containsKey("character_variables") -> base["character_variables"] = nextVars
                base.containsKey("characterScriptVariables") -> base["characterScriptVariables"] = nextVars
                else -> base["variables"] = nextVars
            }
            return base
        }

        private fun parseTavernHelperSettings(value: Any?): Map<String, Any>? {
            if (value is Map<*, *>) return asStringKeyMap(value)
            if (value !is List<*>) return null
            val out = mutableMapOf<String, Any>()
            for (entry in value) {
                if (entry !is List<*> || entry.size < 2) continue
                val key = entry[0] as? String ?: continue
                val trimmed = key.trim()
                if (trimmed.isEmpty()) continue
                val v = entry[1] ?: continue
                out[trimmed] = v
            }
            return out.takeIf { it.isNotEmpty() }
        }

        private fun asStringKeyMap(value: Any?): Map<String, Any> {
            if (value !is Map<*, *>) return emptyMap()
            val out = mutableMapOf<String, Any>()
            for ((key, v) in value) {
                if (key is String && v != null) {
                    out[key] = v
                }
            }
            return out
        }

        private fun asMutableStringKeyMap(value: Any?): MutableMap<String, Any> {
            val out = mutableMapOf<String, Any>()
            if (value !is Map<*, *>) return out
            for ((key, v) in value) {
                if (key is String && v != null) {
                    out[key] = v
                }
            }
            return out
        }

        private fun handleA2UISend(action: A2UIAction) {
            val content = action.contextString("text", "message", "input")?.trim().orEmpty()
            if (content.isEmpty()) return
            if (isSending.value) return

            isSending.value = true
            error.value = null

            viewModelScope.launch {
                try {
                    val enriched = withSessionContext(normalizeActionForServer(action))
                    val result = chatRepository.sendA2UIAction(enriched)
                    if (!result.accepted && result.reason == "client_action_required") {
                        sendUserMessage(content)
                        input.value = ""
                    }
                } catch (e: ApiException) {
                    error.value = e.userMessage ?: e.message
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    error.value = "unexpected error"
                } finally {
                    isSending.value = false
                }
            }
        }

        private fun handleA2UIEvent(action: A2UIAction) {
            runAction {
                chatRepository.sendA2UIAction(withSessionContext(normalizeActionForServer(action)))
            }
        }

        private fun handleA2UIFallback(
            action: A2UIAction,
            fallback: suspend (String) -> Unit,
        ) {
            runAction {
                val enriched = withSessionContext(normalizeActionForServer(action))
                val result = chatRepository.sendA2UIAction(enriched)
                if (result.accepted || result.reason != "client_action_required") return@runAction
                val messageId = resolveActionMessageId(action) ?: return@runAction
                fallback(messageId)
            }
        }

        private fun resolveActionMessageId(action: A2UIAction): String? {
            val fromContext =
                action.contextString(
                    "messageId",
                    "messageID",
                    "message_id",
                )
            if (!fromContext.isNullOrBlank()) return fromContext
            val lastAssistant =
                uiState.value.messages.lastOrNull { it.role == ChatRole.Assistant }
            return lastAssistant?.serverId ?: lastAssistant?.id
        }

        private fun withSessionContext(action: A2UIAction): A2UIAction {
            val sessionId = chatSessionStore.getSessionId()?.trim().orEmpty()
            if (sessionId.isEmpty()) return action
            if (action.context.containsKey("sessionId") ||
                action.context.containsKey("sessionID") ||
                action.context.containsKey("session_id")
            ) {
                return action
            }
            return action.copy(context = action.context + ("sessionId" to sessionId))
        }

        private fun normalizeActionForServer(action: A2UIAction): A2UIAction {
            val mapped =
                when (action.normalizedName) {
                    "send" -> "sendMessage"
                    "cancelgeneration" -> "cancel"
                    "continuegeneration" -> "continue"
                    "regeneratemessage" -> "regenerate"
                    else -> action.name
                }
            return if (mapped == action.name) action else action.copy(name = mapped)
        }

        private fun runAction(block: suspend () -> Unit) {
            if (isActionRunning.value) return
            isActionRunning.value = true
            error.value = null
            viewModelScope.launch {
                try {
                    if (!ensureSessionAccess()) return@launch
                    block()
                } catch (e: ApiException) {
                    error.value = e.userMessage ?: e.message
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    error.value = "unexpected error"
                } finally {
                    isActionRunning.value = false
                }
            }
        }

        private suspend fun ensureSessionAccess(): Boolean {
            val memberId = chatSessionStore.getPrimaryMemberId()?.trim().orEmpty()
            val access =
                resolveContentAccess.execute(
                    memberId.takeIf { it.isNotEmpty() },
                    activeCharacterIsNsfw.value,
                    ageRatingHint = activeCharacterAgeRating.value,
                )
            if (access is ContentAccessDecision.Blocked) {
                handleAccessBlocked(access)
                return false
            }
            accessError.value = null
            if (memberId.isNotEmpty() &&
                (activeCharacterIsNsfw.value == null || activeCharacterAgeRating.value == null)
            ) {
                resolveCharacterNsfw(memberId)
            }
            return true
        }

        private fun handleAccessBlocked(access: ContentAccessDecision.Blocked) {
            val message = access.userMessage()
            accessError.value = message
            error.value = message
        }

        private suspend fun resolveCharacterNsfw(memberId: String?) {
            val clean = memberId?.trim()?.takeIf { it.isNotEmpty() }
            if (clean == null) {
                activeCharacterIsNsfw.value = null
                activeCharacterAgeRating.value = null
                return
            }
            try {
                val detail = characterRepository.getCharacterDetail(clean)
                activeCharacterIsNsfw.value = detail.isNsfw
                activeCharacterAgeRating.value = detail.moderationAgeRating
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                activeCharacterIsNsfw.value = null
                activeCharacterAgeRating.value = null
            }
        }
    }
