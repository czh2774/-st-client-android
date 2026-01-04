package com.stproject.client.android.core.network

data class CommentItemDto(
    val id: String? = null,
    val characterId: String? = null,
    val userId: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
    val content: String? = null,
    val parentId: String? = null,
    val likesCount: Int? = null,
    val isLiked: Boolean? = null,
    val createdAt: String? = null,
    val replies: List<CommentItemDto>? = null,
)

data class CommentListResponseDto(
    val items: List<CommentItemDto>? = null,
    val total: Int? = null,
    val hasMore: Boolean? = null,
    val character: CommentCharacterDto? = null,
)

data class CommentCharacterDto(
    val id: String? = null,
    val isNsfw: Boolean? = null,
    val moderationAgeRating: String? = null,
    val tags: List<String>? = null,
    val visibility: String? = null,
)

data class CreateCommentRequestDto(
    val characterId: String,
    val content: String,
    val parentId: String? = null,
)

data class LikeCommentRequestDto(
    val value: Boolean,
)

data class LikeCommentResponseDto(
    val success: Boolean? = null,
    val likesCount: Int? = null,
    val isLiked: Boolean? = null,
)
