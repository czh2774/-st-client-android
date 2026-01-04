package com.stproject.client.android.domain.model

enum class CommentSort(val apiValue: String) {
    Hot("heat"),
    New("latest"),
}

data class CommentUser(
    val id: String,
    val username: String,
    val avatarUrl: String?,
)

data class Comment(
    val id: String,
    val characterId: String,
    val userId: String,
    val content: String,
    val likesCount: Int,
    val isLiked: Boolean,
    val createdAt: String,
    val parentId: String?,
    val user: CommentUser?,
    val replies: List<Comment>,
)

data class CommentListResult(
    val items: List<Comment>,
    val total: Int,
    val hasMore: Boolean,
    val character: ContentSummary? = null,
)

data class CommentLikeResult(
    val likesCount: Int,
    val isLiked: Boolean,
)
