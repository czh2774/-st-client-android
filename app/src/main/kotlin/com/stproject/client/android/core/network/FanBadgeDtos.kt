package com.stproject.client.android.core.network

data class FanBadgeDto(
    val id: String? = null,
    val userBadgeId: String? = null,
    val creatorId: String? = null,
    val creatorName: String? = null,
    val name: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val priceDiamonds: Int? = null,
    val level: Int? = null,
    val maxLevel: Int? = null,
    val owned: Boolean? = null,
    val equipped: Boolean? = null,
    val experience: Int? = null,
    val experienceToNextLevel: Int? = null,
    val isMaxLevel: Boolean? = null,
)

data class FanBadgeListResponseDto(
    val items: List<FanBadgeDto>? = null,
    val total: Int? = null,
    val hasMore: Boolean? = null,
    val pageNum: Int? = null,
    val pageSize: Int? = null,
)

data class FanBadgePurchaseRequestDto(
    val badgeId: String,
)

data class FanBadgePurchaseResponseDto(
    val ok: Boolean? = null,
    val userBadgeId: String? = null,
)

data class FanBadgeEquipRequestDto(
    val badgeId: String,
    val equip: Boolean,
)
