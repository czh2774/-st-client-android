package com.stproject.client.android.domain.model

data class CreatorCharacter(
    val id: String,
    val name: String,
    val description: String,
    val avatarUrl: String?,
    val backgroundUrl: String?,
    val tags: List<String>,
    val isNsfw: Boolean,
    val creatorId: String?,
    val creatorName: String?,
    val updatedAt: String?,
)
