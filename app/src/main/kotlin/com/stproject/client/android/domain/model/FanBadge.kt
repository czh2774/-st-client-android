package com.stproject.client.android.domain.model

data class FanBadge(
    val id: String,
    val userBadgeId: String?,
    val creatorId: String,
    val creatorName: String?,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val priceDiamonds: Int,
    val level: Int,
    val maxLevel: Int?,
    val owned: Boolean,
    val equipped: Boolean,
    val experience: Int?,
    val experienceToNextLevel: Int?,
    val isMaxLevel: Boolean?,
)
