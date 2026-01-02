package com.stproject.client.android.features.creators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.repository.CreatorRepository
import com.stproject.client.android.domain.repository.SocialRepository
import com.stproject.client.android.domain.usecase.FollowUserUseCase
import com.stproject.client.android.domain.usecase.GuardedActionResult
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatorsViewModel
    @Inject
    constructor(
        private val creatorRepository: CreatorRepository,
        private val socialRepository: SocialRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
        private val followUserUseCase: FollowUserUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CreatorsUiState())
        val uiState: StateFlow<CreatorsUiState> = _uiState

        fun load() {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = null)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(isLoading = false, error = accessErrorMessage(access)) }
                        return@launch
                    }
                    val result =
                        creatorRepository.listCreators(
                            limit = 20,
                            cursor = null,
                            sortBy = "recommend",
                            searchKeyword = _uiState.value.searchKeyword.trim().takeIf { it.isNotEmpty() },
                        )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = result.items,
                            hasMore = result.hasMore,
                            nextCursor = result.nextCursor,
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
            if (state.isLoading || !state.hasMore || state.nextCursor.isNullOrBlank()) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = null)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(isLoading = false, error = accessErrorMessage(access)) }
                        return@launch
                    }
                    val result =
                        creatorRepository.listCreators(
                            limit = 20,
                            cursor = state.nextCursor,
                            sortBy = "recommend",
                            searchKeyword = state.searchKeyword.trim().takeIf { it.isNotEmpty() },
                        )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = it.items + result.items,
                            hasMore = result.hasMore,
                            nextCursor = result.nextCursor,
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

        fun onSearchChanged(value: String) {
            _uiState.update { it.copy(searchKeyword = value, error = null) }
        }

        fun submitSearch() {
            load()
        }

        fun followCreator(
            creatorId: String,
            value: Boolean,
        ) {
            val cleanId = creatorId.trim()
            if (cleanId.isEmpty()) return
            viewModelScope.launch {
                try {
                    val result = followUserUseCase.execute(cleanId, value)
                    if (result is GuardedActionResult.Blocked) {
                        _uiState.update { it.copy(error = accessErrorMessage(result.decision)) }
                        return@launch
                    }
                    _uiState.update { state ->
                        state.copy(
                            items =
                                state.items.map { item ->
                                    if (item.id == cleanId) {
                                        val followerDelta = if (value) 1 else -1
                                        val updatedFollowers = (item.followerCount + followerDelta).coerceAtLeast(0)
                                        item.copy(
                                            followerCount = updatedFollowers,
                                            followStatus = if (value) 1 else 0,
                                        )
                                    } else {
                                        item
                                    }
                                },
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(error = "unexpected error") }
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
