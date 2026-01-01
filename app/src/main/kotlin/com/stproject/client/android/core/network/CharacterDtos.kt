package com.stproject.client.android.core.network

data class CharacterBlockRequestDto(
    val characterId: String,
    val value: Boolean,
)

data class CharacterFollowRequestDto(
    val characterId: String,
    val value: Boolean,
)

data class CharacterFollowResponseDto(
    val success: Boolean? = null,
    val totalFollowers: Int? = null,
    val isFollowed: Boolean? = null,
)
