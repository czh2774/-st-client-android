package com.stproject.client.android.core.network

data class CharacterDetailDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val avatar: String? = null,
    val tags: List<String>? = null,
    val creatorId: String? = null,
    val creatorName: String? = null,
    val isNsfw: Boolean? = null,
    val totalFollowers: Int? = null,
    val isFollowed: Boolean? = null,
)

data class ResolveShareCodeResponseDto(
    val characterId: String? = null,
)

data class ShareCodeResponseDto(
    val shareCode: String? = null,
    val shareUrl: String? = null,
)
