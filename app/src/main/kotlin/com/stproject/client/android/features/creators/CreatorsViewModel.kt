package com.stproject.client.android.features.creators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.CreatorSummary
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

        fun load(force: Boolean = false) {
            if (_uiState.value.isLoading && !force) return
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    items = if (force) emptyList() else it.items,
                    nextCursor = if (force) null else it.nextCursor,
                    hasMore = if (force) false else it.hasMore,
                )
            }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = emptyList(),
                                hasMore = false,
                                nextCursor = null,
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val state = _uiState.value
                    val result =
                        creatorRepository.listCreators(
                            limit = 20,
                            cursor = null,
                            sortBy = state.sortBy,
                            searchKeyword = state.searchKeyword.trim().takeIf { it.isNotEmpty() },
                        )
                    val filtered = filterByPreviewAccess(result.items)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = filtered,
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
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = emptyList(),
                                hasMore = false,
                                nextCursor = null,
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val result =
                        creatorRepository.listCreators(
                            limit = 20,
                            cursor = state.nextCursor,
                            sortBy = state.sortBy,
                            searchKeyword = state.searchKeyword.trim().takeIf { it.isNotEmpty() },
                        )
                    val filtered = filterByPreviewAccess(result.items)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = it.items + filtered,
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
            load(force = true)
        }

        fun setSortBy(sortBy: String) {
            val normalized = sortBy.trim()
            if (normalized.isEmpty()) return
            if (_uiState.value.sortBy == normalized) return
            _uiState.update { it.copy(sortBy = normalized) }
            load(force = true)
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
                        _uiState.update { it.copy(error = result.decision.userMessage()) }
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

        private suspend fun filterByPreviewAccess(
            items: List<CreatorSummary>,
        ): List<CreatorSummary> {
            if (items.isEmpty()) return items
            return items.filter { creator ->
                val previews = creator.previewCharacters
                if (previews.isEmpty()) return@filter true
                previews.any { preview ->
                    resolveContentAccess.execute(
                        memberId = preview.id,
                        isNsfwHint = preview.isNsfw,
                        ageRatingHint = preview.moderationAgeRating,
                        tags = preview.tags,
                        requireMetadata = true,
                    ) is ContentAccessDecision.Allowed
                }
            }
        }
    }
