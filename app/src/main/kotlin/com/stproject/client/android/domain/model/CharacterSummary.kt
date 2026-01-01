package com.stproject.client.android.domain.model

data class CharacterSummary(
    val id: String,
    val name: String,
    val description: String,
    val avatarUrl: String?,
    val isNsfw: Boolean,
    val totalFollowers: Int,
    val isFollowed: Boolean,
)
