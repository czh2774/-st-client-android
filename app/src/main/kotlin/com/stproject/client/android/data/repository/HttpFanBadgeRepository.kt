package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.FanBadgeDto
import com.stproject.client.android.core.network.FanBadgeEquipRequestDto
import com.stproject.client.android.core.network.FanBadgePurchaseRequestDto
import com.stproject.client.android.core.network.FanBadgePurchaseResponseDto
import com.stproject.client.android.core.network.StFanBadgeApi
import com.stproject.client.android.domain.model.FanBadge
import com.stproject.client.android.domain.repository.FanBadgeListResult
import com.stproject.client.android.domain.repository.FanBadgePurchaseResult
import com.stproject.client.android.domain.repository.FanBadgeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpFanBadgeRepository
    @Inject
    constructor(
        private val api: StFanBadgeApi,
        private val apiClient: ApiClient,
    ) : FanBadgeRepository {
        override suspend fun listCreatorBadges(
            creatorId: String,
            pageNum: Int,
            pageSize: Int,
        ): FanBadgeListResult {
            val resp =
                apiClient.call {
                    api.listCreatorBadges(
                        creatorId = creatorId,
                        pageNum = pageNum,
                        pageSize = pageSize,
                    )
                }
            return resp.toResult(pageNum, pageSize)
        }

        override suspend fun listPurchasedBadges(): FanBadgeListResult {
            val resp = apiClient.call { api.listPurchasedBadges() }
            return resp.toResult(pageNum = 1, pageSize = resp.pageSize ?: resp.items?.size ?: 0)
        }

        override suspend fun purchaseBadge(badgeId: String): FanBadgePurchaseResult {
            val resp =
                apiClient.call {
                    api.purchaseBadge(
                        FanBadgePurchaseRequestDto(badgeId = badgeId),
                    )
                }
            return resp.toResult()
        }

        override suspend fun equipBadge(
            badgeId: String,
            equip: Boolean,
        ) {
            apiClient.call {
                api.equipBadge(
                    FanBadgeEquipRequestDto(
                        badgeId = badgeId,
                        equip = equip,
                    ),
                )
            }
        }

        private fun com.stproject.client.android.core.network.FanBadgeListResponseDto.toResult(
            pageNum: Int,
            pageSize: Int,
        ): FanBadgeListResult {
            val items = items?.mapNotNull { it.toDomain() } ?: emptyList()
            return FanBadgeListResult(
                items = items,
                total = total ?: items.size,
                hasMore = hasMore ?: false,
                pageNum = this.pageNum ?: pageNum,
                pageSize = this.pageSize ?: pageSize,
            )
        }

        private fun FanBadgePurchaseResponseDto.toResult(): FanBadgePurchaseResult {
            return FanBadgePurchaseResult(
                ok = ok ?: true,
                userBadgeId = userBadgeId?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

        private fun FanBadgeDto.toDomain(): FanBadge? {
            val idValue = id?.trim().orEmpty()
            val creatorIdValue = creatorId?.trim().orEmpty()
            if (idValue.isEmpty() || creatorIdValue.isEmpty()) return null
            val nameValue = name?.trim().orEmpty()
            if (nameValue.isEmpty()) return null
            return FanBadge(
                id = idValue,
                userBadgeId = userBadgeId?.trim()?.takeIf { it.isNotEmpty() },
                creatorId = creatorIdValue,
                creatorName = creatorName?.trim()?.takeIf { it.isNotEmpty() },
                name = nameValue,
                description = description?.trim().orEmpty(),
                imageUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() },
                priceDiamonds = priceDiamonds ?: 0,
                level = level ?: 1,
                maxLevel = maxLevel,
                owned = owned ?: false,
                equipped = equipped ?: false,
                experience = experience,
                experienceToNextLevel = experienceToNextLevel,
                isMaxLevel = isMaxLevel,
            )
        }
    }
