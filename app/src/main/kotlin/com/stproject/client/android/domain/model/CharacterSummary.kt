package com.stproject.client.android.domain.model

data class CharacterSummary(
    val id: String,
    val name: String,
    val description: String,
    val avatarUrl: String?,
    val tags: List<String> = emptyList(),
    val isNsfw: Boolean?,
    val moderationAgeRating: AgeRating? = null,
    val totalFollowers: Int,
    val isFollowed: Boolean,
)
