package com.stproject.client.android.features.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.Comment
import com.stproject.client.android.domain.model.CommentLikeResult
import com.stproject.client.android.domain.model.CommentSort
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.CommentRepository
import com.stproject.client.android.domain.repository.UserRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentsViewModel
    @Inject
    constructor(
        private val commentRepository: CommentRepository,
        private val characterRepository: CharacterRepository,
        private val userRepository: UserRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CommentsUiState())
        val uiState: StateFlow<CommentsUiState> = _uiState

        private val pageSize = 20

        fun load(
            characterId: String,
            force: Boolean = false,
        ) {
            val cleanId = characterId.trim()
            if (cleanId.isEmpty()) return
            if (_uiState.value.isLoading && !force && _uiState.value.characterId == cleanId) return
            _uiState.update {
                it.copy(
                    characterId = cleanId,
                    isLoading = true,
                    error = null,
                    accessError = null,
                )
            }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(cleanId, _uiState.value.characterIsNsfw)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                accessError = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val detail = characterRepository.getCharacterDetail(cleanId)
                    val currentUserId = runCatching { userRepository.getMe().id }.getOrNull()
                    val list = commentRepository.listComments(cleanId, _uiState.value.sort, 1, pageSize)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = list.items,
                            total = list.total,
                            hasMore = list.hasMore,
                            pageNum = 1,
                            characterName = detail.name,
                            characterIsNsfw = detail.isNsfw,
                            currentUserId = currentUserId ?: it.currentUserId,
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

        fun refresh() {
            val id = _uiState.value.characterId ?: return
            load(id, force = true)
        }

        fun setSort(sort: CommentSort) {
            if (sort == _uiState.value.sort) return
            _uiState.update { it.copy(sort = sort) }
            val id = _uiState.value.characterId ?: return
            load(id, force = true)
        }

        fun loadMore() {
            val state = _uiState.value
            val characterId = state.characterId ?: return
            if (state.isLoading || state.isLoadingMore || !state.hasMore) return
            _uiState.update { it.copy(isLoadingMore = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(characterId, state.characterIsNsfw)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoadingMore = false,
                                accessError = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val nextPage = state.pageNum + 1
                    val list = commentRepository.listComments(characterId, state.sort, nextPage, pageSize)
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + list.items,
                            total = list.total,
                            hasMore = list.hasMore,
                            pageNum = nextPage,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoadingMore = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoadingMore = false, error = "unexpected error") }
                }
            }
        }

        fun onInputChanged(value: String) {
            _uiState.update { it.copy(input = value, error = null) }
        }

        fun setReplyTarget(comment: Comment?) {
            _uiState.update {
                it.copy(
                    replyTarget =
                        comment?.let {
                            ReplyTarget(commentId = it.id, username = it.user?.username ?: "User")
                        },
                )
            }
        }

        fun clearReplyTarget() {
            _uiState.update { it.copy(replyTarget = null) }
        }

        fun submitComment() {
            val state = _uiState.value
            val characterId = state.characterId ?: return
            val content = state.input.trim()
            if (content.isEmpty()) {
                _uiState.update { it.copy(error = "comment is required") }
                return
            }
            if (state.isSubmitting) return
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(characterId, state.characterIsNsfw)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                accessError = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    commentRepository.createComment(characterId, content, state.replyTarget?.commentId)
                    _uiState.update { it.copy(input = "", replyTarget = null) }
                    val list = commentRepository.listComments(characterId, state.sort, 1, pageSize)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            items = list.items,
                            total = list.total,
                            hasMore = list.hasMore,
                            pageNum = 1,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSubmitting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSubmitting = false, error = "unexpected error") }
                }
            }
        }

        fun deleteComment(commentId: String) {
            val state = _uiState.value
            val characterId = state.characterId ?: return
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(characterId, state.characterIsNsfw)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(accessError = access.userMessage()) }
                        return@launch
                    }
                    commentRepository.deleteComment(commentId)
                    _uiState.update { it.copy(items = removeComment(it.items, commentId)) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(error = "unexpected error") }
                }
            }
        }

        fun toggleLike(comment: Comment) {
            val state = _uiState.value
            val characterId = state.characterId ?: return
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(characterId, state.characterIsNsfw)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(accessError = access.userMessage()) }
                        return@launch
                    }
                    val result = commentRepository.likeComment(comment.id, !comment.isLiked)
                    _uiState.update { it.copy(items = updateLike(it.items, comment.id, result)) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(error = "unexpected error") }
                }
            }
        }

        private fun updateLike(
            items: List<Comment>,
            commentId: String,
            result: CommentLikeResult,
        ): List<Comment> {
            return items.map { item ->
                if (item.id == commentId) {
                    item.copy(likesCount = result.likesCount, isLiked = result.isLiked)
                } else {
                    item.copy(replies = updateLike(item.replies, commentId, result))
                }
            }
        }

        private fun removeComment(
            items: List<Comment>,
            commentId: String,
        ): List<Comment> {
            return items.mapNotNull { item ->
                if (item.id == commentId) {
                    null
                } else {
                    item.copy(replies = removeComment(item.replies, commentId))
                }
            }
        }
    }
