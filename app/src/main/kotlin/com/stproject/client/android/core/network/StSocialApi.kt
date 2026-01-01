package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface StSocialApi {
    @POST("users/follow")
    suspend fun followUser(
        @Body request: FollowRequestDto,
    ): ApiEnvelope<FollowResponseDto>

    @POST("users/block")
    suspend fun blockUser(
        @Body request: FollowRequestDto,
    ): ApiEnvelope<BlockResponseDto>

    @GET("users/followers")
    suspend fun listFollowers(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int,
        @Query("userId") userId: String? = null,
    ): ApiEnvelope<FollowListResponseDto>

    @GET("users/following")
    suspend fun listFollowing(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int,
        @Query("userId") userId: String? = null,
    ): ApiEnvelope<FollowListResponseDto>

    @GET("users/blocked")
    suspend fun listBlocked(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int,
    ): ApiEnvelope<FollowListResponseDto>
}
