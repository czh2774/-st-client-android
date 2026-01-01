package com.stproject.client.android.domain.model

data class CharacterDetail(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val creatorName: String?,
    val isNsfw: Boolean,
    val totalFollowers: Int,
    val isFollowed: Boolean,
)
