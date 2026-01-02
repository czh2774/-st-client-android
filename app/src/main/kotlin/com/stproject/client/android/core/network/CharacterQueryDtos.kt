package com.stproject.client.android.core.network

data class QueryCharactersRequestDto(
    val cursor: String? = null,
    val limit: Int? = null,
    val sortBy: String? = null,
    val gender: String? = null,
    val tags: List<String>? = null,
    val isNsfw: Boolean? = null,
    val searchKeyword: String? = null,
    val creatorId: String? = null,
)

data class QueryCharacterItemDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val backgroundUrl: String? = null,
    val avatar: String? = null,
    val tags: List<String>? = null,
    val gender: Int? = null,
    val totalLikes: Int? = null,
    val totalFollowers: Int? = null,
    val totalChats: Int? = null,
    val totalShares: Int? = null,
    val isNsfw: Boolean? = null,
    val moderationAgeRating: String? = null,
    val isLiked: Boolean? = null,
    val isFollowed: Boolean? = null,
    val creatorId: String? = null,
    val creatorName: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val hasRedPacket: Boolean? = null,
)

data class QueryCharactersResponseDto(
    val items: List<QueryCharacterItemDto>? = null,
    val hasMore: Boolean? = null,
    val nextCursor: String? = null,
)
