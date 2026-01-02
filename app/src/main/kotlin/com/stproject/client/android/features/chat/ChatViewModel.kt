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
import com.stproject.client.android.core.session.ChatSessionStore
import com.stproject.client.android.domain.model.A2UIAction
import com.stproject.client.android.domain.model.ChatRole
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import com.stproject.client.android.domain.usecase.SendUserMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
        private val chatSessionStore: ChatSessionStore,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        private val variablesType = object : TypeToken<Map<String, Any>>() {}.type
        private val input = MutableStateFlow("")
        private val isSending = MutableStateFlow(false)
        private val isActionRunning = MutableStateFlow(false)
        private val shareInfo = MutableStateFlow<ShareCodeInfo?>(null)
        private val activeCharacterIsNsfw = MutableStateFlow<Boolean?>(null)
        private val error = MutableStateFlow<String?>(null)
        private val variablesState = MutableStateFlow(ChatVariablesUiState())

        val uiState: StateFlow<ChatUiState> =
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
                    error = null,
                )
            }.combine(activeCharacterIsNsfw) { state, isNsfw ->
                state.copy(activeCharacterIsNsfw = isNsfw)
            }.combine(error) { state, errorText ->
                state.copy(error = errorText)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

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

        fun startNewChat(
            memberId: String,
            shareCode: String? = null,
            onSuccess: (() -> Unit)? = null,
        ) {
            activeCharacterIsNsfw.value = null
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

        fun loadVariables() {
            if (variablesState.value.isLoading || variablesState.value.isSaving) return
            variablesState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val variables = chatRepository.loadSessionVariables()
                    val text = gson.toJson(variables)
                    variablesState.update {
                        it.copy(
                            text = text,
                            isLoading = false,
                            isDirty = false,
                            error = null,
                        )
                    }
                } catch (e: ApiException) {
                    variablesState.update {
                        it.copy(
                            isLoading = false,
                            error = e.userMessage ?: e.message,
                        )
                    }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    variablesState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun updateVariablesText(value: String) {
            variablesState.update { it.copy(text = value, isDirty = true) }
        }

        fun saveVariables() {
            if (variablesState.value.isSaving) return
            val raw = variablesState.value.text.trim()
            val parsed =
                if (raw.isEmpty()) {
                    emptyMap()
                } else {
                    runCatching { gson.fromJson<Map<String, Any>>(raw, variablesType) }.getOrNull()
                }
            if (parsed == null) {
                variablesState.update {
                    it.copy(error = "invalid json", isSaving = false)
                }
                return
            }
            variablesState.update { it.copy(isSaving = true, error = null) }
            viewModelScope.launch {
                try {
                    chatRepository.updateSessionVariables(parsed)
                    val text = gson.toJson(parsed)
                    variablesState.update {
                        it.copy(
                            text = text,
                            isSaving = false,
                            isDirty = false,
                            error = null,
                        )
                    }
                } catch (e: ApiException) {
                    variablesState.update {
                        it.copy(
                            isSaving = false,
                            error = e.userMessage ?: e.message,
                        )
                    }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    variablesState.update { it.copy(isSaving = false, error = "unexpected error") }
                }
            }
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

        private fun handleAccessBlocked(access: ContentAccessDecision.Blocked) {
            error.value = access.userMessage()
        }

        private suspend fun resolveCharacterNsfw(memberId: String?) {
            val clean = memberId?.trim()?.takeIf { it.isNotEmpty() }
            if (clean == null) {
                activeCharacterIsNsfw.value = null
                return
            }
            try {
                activeCharacterIsNsfw.value = characterRepository.getCharacterDetail(clean).isNsfw
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                activeCharacterIsNsfw.value = null
            }
        }
    }
