package com.stproject.client.android.core.network

data class CreatorPreviewDto(
    val id: String? = null,
    val characterId: String? = null,
    val name: String? = null,
    val backgroundUrl: String? = null,
)

data class CreatorCardDto(
    val id: String? = null,
    val userId: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val followerCount: Int? = null,
    val interactionCount: Int? = null,
    val bio: String? = null,
    val followStatus: Int? = null,
    val isBlocked: Boolean? = null,
    val characters: List<CreatorPreviewDto>? = null,
)

data class QueryCreatorsResponseDto(
    val items: List<CreatorCardDto>? = null,
    val hasMore: Boolean? = null,
    val nextCursor: String? = null,
)

data class CreatorCharacterDto(
    val characterId: String? = null,
    val name: String? = null,
    val description: String? = null,
    val backgroundUrl: String? = null,
    val avatarUrl: String? = null,
    val tags: List<String>? = null,
    val isNsfw: Boolean? = null,
    val moderationAgeRating: String? = null,
    val isPublic: Boolean? = null,
    val creatorId: String? = null,
    val creatorName: String? = null,
    val updatedAt: String? = null,
)

data class CreatorCharactersResponseDto(
    val items: List<CreatorCharacterDto>? = null,
    val total: Int? = null,
    val hasMore: Boolean? = null,
)
