package com.stproject.client.android.features.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.ChatSessionSummary
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsListViewModel
    @Inject
    constructor(
        private val chatRepository: ChatRepository,
        private val characterRepository: CharacterRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChatsListUiState())
        val uiState: StateFlow<ChatsListUiState> = _uiState

        fun load(allowNsfw: Boolean) {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(isLoading = false, error = accessErrorMessage(access)) }
                        return@launch
                    }
                    val items = chatRepository.listSessions(limit = 20, offset = 0)
                    val resolved =
                        if (allowNsfw) {
                            items
                        } else {
                            resolveNsfw(items)
                        }
                    _uiState.update { it.copy(isLoading = false, items = resolved) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        private suspend fun resolveNsfw(items: List<ChatSessionSummary>): List<ChatSessionSummary> {
            return items.map { item ->
                val memberId = item.primaryMemberId?.trim().orEmpty()
                if (memberId.isEmpty()) return@map item
                val isNsfw =
                    runCatching {
                        characterRepository.getCharacterDetail(memberId).isNsfw
                    }.getOrNull()
                item.copy(primaryMemberIsNsfw = isNsfw)
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
