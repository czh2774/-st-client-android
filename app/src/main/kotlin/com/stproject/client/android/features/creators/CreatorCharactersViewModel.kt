package com.stproject.client.android.features.creators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.repository.CreatorRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatorCharactersViewModel
    @Inject
    constructor(
        private val creatorRepository: CreatorRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CreatorCharactersUiState())
        val uiState: StateFlow<CreatorCharactersUiState> = _uiState

        fun load(creatorId: String) {
            if (_uiState.value.isLoading && _uiState.value.creatorId == creatorId) return
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    items = emptyList(),
                    creatorId = creatorId,
                    pageNum = 1,
                )
            }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val result =
                        creatorRepository.listCreatorCharacters(
                            creatorId = creatorId,
                            pageNum = 1,
                            pageSize = 20,
                        )
                    val filtered =
                        result.items.filter { item ->
                            resolveContentAccess.execute(item.id, item.isNsfw) is ContentAccessDecision.Allowed
                        }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = filtered,
                            hasMore = result.hasMore,
                            pageNum = 1,
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
            val creatorId = state.creatorId ?: return
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
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val result =
                        creatorRepository.listCreatorCharacters(
                            creatorId = creatorId,
                            pageNum = nextPage,
                            pageSize = 20,
                        )
                    val filtered =
                        result.items.filter { item ->
                            resolveContentAccess.execute(item.id, item.isNsfw) is ContentAccessDecision.Allowed
                        }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = it.items + filtered,
                            hasMore = result.hasMore,
                            pageNum = nextPage,
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
    }
