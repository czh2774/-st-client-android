package com.stproject.client.android.features.creators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.CreatorAssistantSessionSummary
import com.stproject.client.android.domain.repository.CreatorAssistantRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatorAssistantListViewModel
    @Inject
    constructor(
        private val repository: CreatorAssistantRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CreatorAssistantListUiState())
        val uiState: StateFlow<CreatorAssistantListUiState> = _uiState

        fun load() {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = emptyList(),
                                pageNum = 1,
                                hasMore = false,
                                newSessionId = null,
                                openSessionId = null,
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val result = repository.listSessions(pageNum = 1, pageSize = 20)
                    val filtered = filterByDraftAccess(result.items)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = filtered,
                            pageNum = 1,
                            hasMore = result.hasMore,
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

        fun loadMore() {
            val state = _uiState.value
            if (state.isLoading || !state.hasMore) return
            val nextPage = state.pageNum + 1
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = emptyList(),
                                hasMore = false,
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val result = repository.listSessions(pageNum = nextPage, pageSize = 20)
                    val filtered = filterByDraftAccess(result.items)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = it.items + filtered,
                            pageNum = nextPage,
                            hasMore = result.hasMore,
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

        fun startSession() {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null, newSessionId = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                newSessionId = null,
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val result = repository.startSession()
                    _uiState.update { it.copy(isLoading = false, newSessionId = result.sessionId) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun requestOpenSession(sessionId: String) {
            val cleanId = sessionId.trim()
            if (cleanId.isEmpty() || _uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null, openSessionId = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                openSessionId = null,
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    _uiState.update { it.copy(isLoading = false, openSessionId = cleanId) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun consumeNewSession() {
            _uiState.update { it.copy(newSessionId = null) }
        }

        fun consumeOpenSession() {
            _uiState.update { it.copy(openSessionId = null) }
        }

        private suspend fun filterByDraftAccess(
            items: List<CreatorAssistantSessionSummary>,
        ): List<CreatorAssistantSessionSummary> {
            if (items.isEmpty()) return items
            return items.filter { item ->
                val hasDraftMeta =
                    !item.draftName.isNullOrBlank() ||
                        item.draftIsNsfw != null ||
                        item.draftTags.isNotEmpty() ||
                        item.draftModerationAgeRating != null
                if (!hasDraftMeta) return@filter true
                val decision =
                    resolveContentAccess.execute(
                        memberId = null,
                        isNsfwHint = item.draftIsNsfw,
                        ageRatingHint = item.draftModerationAgeRating,
                        tags = item.draftTags,
                    )
                decision is ContentAccessDecision.Allowed
            }
        }
    }
