package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface StFanBadgeApi {
    @GET("users/fan-badges")
    suspend fun listCreatorBadges(
        @Query("creatorId") creatorId: String? = null,
        @Query("pageNum") pageNum: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
    ): ApiEnvelope<FanBadgeListResponseDto>

    @GET("users/fan-badges/purchased")
    suspend fun listPurchasedBadges(): ApiEnvelope<FanBadgeListResponseDto>

    @POST("users/fan-badges/purchase")
    suspend fun purchaseBadge(
        @Body request: FanBadgePurchaseRequestDto,
    ): ApiEnvelope<FanBadgePurchaseResponseDto>

    @POST("users/fan-badges/equip")
    suspend fun equipBadge(
        @Body request: FanBadgeEquipRequestDto,
    ): ApiEnvelope<OkResponseDto>
}
