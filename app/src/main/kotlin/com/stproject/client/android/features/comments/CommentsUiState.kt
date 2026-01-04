package com.stproject.client.android.features.comments

import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.domain.model.Comment
import com.stproject.client.android.domain.model.CommentSort

data class ReplyTarget(
    val commentId: String,
    val username: String,
)

data class CommentsUiState(
    val characterId: String? = null,
    val characterName: String? = null,
    val characterIsNsfw: Boolean? = null,
    val characterAgeRating: AgeRating? = null,
    val characterTags: List<String> = emptyList(),
    val sort: CommentSort = CommentSort.Hot,
    val items: List<Comment> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val pageNum: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val accessError: String? = null,
    val input: String = "",
    val replyTarget: ReplyTarget? = null,
    val currentUserId: String? = null,
)
