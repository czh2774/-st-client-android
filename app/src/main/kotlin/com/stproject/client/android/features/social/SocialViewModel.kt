package com.stproject.client.android.features.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.repository.SocialRepository
import com.stproject.client.android.domain.usecase.BlockUserUseCase
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
class SocialViewModel
    @Inject
    constructor(
        private val socialRepository: SocialRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
        private val followUserUseCase: FollowUserUseCase,
        private val blockUserUseCase: BlockUserUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SocialUiState())
        val uiState: StateFlow<SocialUiState> = _uiState

        fun load() {
            val state = _uiState.value
            if (state.isLoading) return
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
                                pageNum = 1,
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val result =
                        when (state.activeTab) {
                            SocialTab.Followers ->
                                socialRepository.listFollowers(
                                    pageNum = 1,
                                    pageSize = 20,
                                    userId = state.targetUserId.trim().takeIf { it.isNotEmpty() },
                                )
                            SocialTab.Following ->
                                socialRepository.listFollowing(
                                    pageNum = 1,
                                    pageSize = 20,
                                    userId = state.targetUserId.trim().takeIf { it.isNotEmpty() },
                                )
                            SocialTab.Blocked ->
                                socialRepository.listBlocked(
                                    pageNum = 1,
                                    pageSize = 20,
                                )
                        }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = result.items,
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
                    val result =
                        when (state.activeTab) {
                            SocialTab.Followers ->
                                socialRepository.listFollowers(
                                    pageNum = nextPage,
                                    pageSize = 20,
                                    userId = state.targetUserId.trim().takeIf { it.isNotEmpty() },
                                )
                            SocialTab.Following ->
                                socialRepository.listFollowing(
                                    pageNum = nextPage,
                                    pageSize = 20,
                                    userId = state.targetUserId.trim().takeIf { it.isNotEmpty() },
                                )
                            SocialTab.Blocked ->
                                socialRepository.listBlocked(
                                    pageNum = nextPage,
                                    pageSize = 20,
                                )
                        }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = it.items + result.items,
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

        fun setTab(tab: SocialTab) {
            if (_uiState.value.activeTab == tab) return
            _uiState.update { it.copy(activeTab = tab, items = emptyList(), pageNum = 1, hasMore = false) }
            load()
        }

        fun onActionUserIdChanged(value: String) {
            _uiState.update { it.copy(actionUserId = value) }
        }

        fun onTargetUserIdChanged(value: String) {
            _uiState.update { it.copy(targetUserId = value) }
        }

        fun followUser(value: Boolean) {
            val userId = _uiState.value.actionUserId.trim()
            if (userId.isEmpty()) {
                _uiState.update { it.copy(error = "user id required") }
                return
            }
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val result = followUserUseCase.execute(userId, value)
                    if (result is GuardedActionResult.Blocked) {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.decision.userMessage())
                        }
                        return@launch
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    load()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun blockUser(value: Boolean) {
            val userId = _uiState.value.actionUserId.trim()
            if (userId.isEmpty()) {
                _uiState.update { it.copy(error = "user id required") }
                return
            }
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val result = blockUserUseCase.execute(userId, value)
                    if (result is GuardedActionResult.Blocked) {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.decision.userMessage())
                        }
                        return@launch
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    load()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }
    }
