package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface StNotificationApi {
    @GET("notifications")
    suspend fun listNotifications(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int,
        @Query("types") types: String? = null,
        @Query("excludeTypes") excludeTypes: String? = null,
        @Query("unreadOnly") unreadOnly: Boolean? = null,
    ): ApiEnvelope<NotificationsResponseDto>

    @GET("notifications/unread")
    suspend fun getUnreadCounts(): ApiEnvelope<UnreadCountsDto>

    @POST("notifications/read")
    suspend fun markAsRead(
        @Body request: MarkReadRequestDto,
    ): ApiEnvelope<MarkReadResponseDto>
}
