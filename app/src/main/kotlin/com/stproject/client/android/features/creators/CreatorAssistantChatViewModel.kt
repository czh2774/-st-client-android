package com.stproject.client.android.features.creators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.CreatorAssistantMessage
import com.stproject.client.android.domain.repository.CreatorAssistantRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreatorAssistantChatViewModel
    @Inject
    constructor(
        private val repository: CreatorAssistantRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CreatorAssistantChatUiState())
        val uiState: StateFlow<CreatorAssistantChatUiState> = _uiState

        fun loadSession(sessionId: String) {
            val cleanId = sessionId.trim()
            if (cleanId.isEmpty() || _uiState.value.isLoading) return
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    sessionId = cleanId,
                    publishResult = null,
                    draftResult = null,
                    currentDraft = null,
                    messages = emptyList(),
                )
            }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(isLoading = false, error = accessErrorMessage(access))
                        }
                        return@launch
                    }
                    val history = repository.getSessionHistory(cleanId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = history.messages,
                            currentDraft = history.currentDraft,
                            draftReady = false,
                            error = null,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun onInputChanged(value: String) {
            _uiState.update { it.copy(input = value) }
        }

        fun sendMessage() {
            val state = _uiState.value
            val sessionId = state.sessionId?.trim().orEmpty()
            val content = state.input.trim()
            if (sessionId.isEmpty() || content.isEmpty() || state.isSending) return
            _uiState.update { it.copy(isSending = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(isSending = false, error = accessErrorMessage(access)) }
                        return@launch
                    }
                    val userMessage =
                        CreatorAssistantMessage(
                            messageId = "local-${UUID.randomUUID()}",
                            role = "user",
                            content = content,
                            createdAt = null,
                        )
                    val resp = repository.chat(sessionId, content)
                    val assistantMessage =
                        CreatorAssistantMessage(
                            messageId = resp.messageId,
                            role = "assistant",
                            content = resp.content,
                            createdAt = null,
                        )
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            input = "",
                            messages = it.messages + userMessage + assistantMessage,
                            draftReady = resp.draftReady,
                            error = null,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSending = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSending = false, error = "unexpected error") }
                }
            }
        }

        fun generateDraft() {
            val state = _uiState.value
            val sessionId = state.sessionId?.trim().orEmpty()
            if (sessionId.isEmpty() || state.isDrafting) return
            _uiState.update { it.copy(isDrafting = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(isDrafting = false, error = accessErrorMessage(access)) }
                        return@launch
                    }
                    val result = repository.generateDraft(sessionId)
                    _uiState.update {
                        it.copy(
                            isDrafting = false,
                            draftResult = result,
                            currentDraft = result.draft,
                            error = null,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isDrafting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isDrafting = false, error = "unexpected error") }
                }
            }
        }

        fun publishDraft() {
            val state = _uiState.value
            val sessionId = state.sessionId?.trim().orEmpty()
            val draftId = state.draftResult?.draftId?.trim().orEmpty()
            if (sessionId.isEmpty() || state.isPublishing) return
            if (draftId.isEmpty()) {
                _uiState.update { it.copy(error = "generate a draft before publishing") }
                return
            }
            _uiState.update { it.copy(isPublishing = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(isPublishing = false, error = accessErrorMessage(access)) }
                        return@launch
                    }
                    val result = repository.publish(sessionId, draftId, isPublic = true)
                    _uiState.update { it.copy(isPublishing = false, publishResult = result) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isPublishing = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isPublishing = false, error = "unexpected error") }
                }
            }
        }

        private fun accessErrorMessage(access: ContentAccessDecision.Blocked): String {
            return when (access.reason) {
                ContentBlockReason.NSFW_DISABLED -> "mature content disabled"
                ContentBlockReason.AGE_REQUIRED -> "age verification required"
                ContentBlockReason.CONSENT_REQUIRED -> "terms acceptance required"
                ContentBlockReason.CONSENT_PENDING -> "compliance not loaded"
            }
        }
    }
