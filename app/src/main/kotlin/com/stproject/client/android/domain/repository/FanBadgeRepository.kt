package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.FanBadge

data class FanBadgeListResult(
    val items: List<FanBadge>,
    val total: Int,
    val hasMore: Boolean,
    val pageNum: Int,
    val pageSize: Int,
)

data class FanBadgePurchaseResult(
    val ok: Boolean,
    val userBadgeId: String?,
)

interface FanBadgeRepository {
    suspend fun listCreatorBadges(
        creatorId: String,
        pageNum: Int,
        pageSize: Int,
    ): FanBadgeListResult

    suspend fun listPurchasedBadges(): FanBadgeListResult

    suspend fun purchaseBadge(badgeId: String): FanBadgePurchaseResult

    suspend fun equipBadge(
        badgeId: String,
        equip: Boolean,
    )
}
