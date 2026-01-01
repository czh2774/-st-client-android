package com.stproject.client.android.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.session.ChatSessionStore
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
        private val input = MutableStateFlow("")
        private val isSending = MutableStateFlow(false)
        private val isActionRunning = MutableStateFlow(false)
        private val shareInfo = MutableStateFlow<ShareCodeInfo?>(null)
        private val error = MutableStateFlow<String?>(null)

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
                    error = null,
                )
            }.combine(error) { state, errorText ->
                state.copy(error = errorText)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

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

        fun startNewChat(
            memberId: String,
            shareCode: String? = null,
            onSuccess: (() -> Unit)? = null,
        ) {
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId, null)
                    if (access is ContentAccessDecision.Blocked) {
                        handleAccessBlocked(access)
                        return@launch
                    }
                    chatRepository.startNewSession(memberId, shareCode)
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
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(primaryMemberId, null)
                    if (access is ContentAccessDecision.Blocked) {
                        handleAccessBlocked(access)
                        return@launch
                    }
                    chatRepository.openSession(sessionId, primaryMemberId)
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
            error.value =
                when (access.reason) {
                    ContentBlockReason.NSFW_DISABLED -> "mature content disabled"
                    ContentBlockReason.AGE_REQUIRED -> "age verification required"
                    ContentBlockReason.CONSENT_REQUIRED -> "terms acceptance required"
                    ContentBlockReason.CONSENT_PENDING -> "compliance not loaded"
                }
        }
    }
