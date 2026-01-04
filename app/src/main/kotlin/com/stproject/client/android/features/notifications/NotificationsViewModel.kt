package com.stproject.client.android.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.NotificationItem
import com.stproject.client.android.domain.repository.NotificationRepository
import com.stproject.client.android.domain.repository.UnreadCounts
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NotificationsUiState())
        val uiState: StateFlow<NotificationsUiState> = _uiState

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
                                hasMore = false,
                                pageNum = 1,
                                unreadCounts = UnreadCounts(0, 0, 0, 0, 0),
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val unread = notificationRepository.getUnreadCounts()
                    val result = notificationRepository.listNotifications(pageNum = 1, pageSize = 50)
                    val filtered = filterByContentAccess(result.items)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = filtered,
                            hasMore = result.hasMore,
                            pageNum = 1,
                            unreadCounts = unread,
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
                    val result = notificationRepository.listNotifications(pageNum = nextPage, pageSize = 50)
                    val filtered = filterByContentAccess(result.items)
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

        fun markAllRead() {
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
                                hasMore = false,
                                unreadCounts = UnreadCounts(0, 0, 0, 0, 0),
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    notificationRepository.markAsRead(ids = emptyList(), markAll = true)
                    val unread = notificationRepository.getUnreadCounts()
                    _uiState.update { it.copy(isLoading = false, unreadCounts = unread) }
                    load()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun markRead(id: String) {
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
                                hasMore = false,
                                unreadCounts = UnreadCounts(0, 0, 0, 0, 0),
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    notificationRepository.markAsRead(ids = listOf(id), markAll = false)
                    val unread = notificationRepository.getUnreadCounts()
                    _uiState.update { it.copy(isLoading = false, unreadCounts = unread) }
                    load()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        private suspend fun filterByContentAccess(
            items: List<NotificationItem>,
        ): List<NotificationItem> {
            if (items.isEmpty()) return items
            return items.filter { item ->
                if (item.type.equals("system", ignoreCase = true)) {
                    return@filter true
                }
                val content = item.contentMeta
                val memberId = content?.characterId?.trim()?.takeIf { it.isNotEmpty() }
                val decision =
                    resolveContentAccess.execute(
                        memberId = memberId,
                        isNsfwHint = content?.isNsfw,
                        ageRatingHint = content?.moderationAgeRating,
                        tags = content?.tags,
                        requireMetadata = true,
                    )
                decision is ContentAccessDecision.Allowed
            }
        }
    }
