package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StCreatorAssistantApi {
    @POST("creator-assistant/start")
    suspend fun startSession(
        @Body request: CreatorAssistantStartRequestDto,
    ): ApiEnvelope<CreatorAssistantStartResponseDto>

    @POST("creator-assistant/chat")
    suspend fun chat(
        @Body request: CreatorAssistantChatRequestDto,
    ): ApiEnvelope<CreatorAssistantChatResponseDto>

    @POST("creator-assistant/generate-draft")
    suspend fun generateDraft(
        @Body request: CreatorAssistantGenerateDraftRequestDto,
    ): ApiEnvelope<CreatorAssistantDraftResponseDto>

    @POST("creator-assistant/update-draft")
    suspend fun updateDraft(
        @Body request: CreatorAssistantUpdateDraftRequestDto,
    ): ApiEnvelope<CreatorAssistantDraftResponseDto>

    @POST("creator-assistant/publish")
    suspend fun publish(
        @Body request: CreatorAssistantPublishRequestDto,
    ): ApiEnvelope<CreatorAssistantPublishResponseDto>

    @GET("creator-assistant/sessions")
    suspend fun listSessions(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int,
        @Query("status") status: String? = null,
    ): ApiEnvelope<CreatorAssistantSessionsResponseDto>

    @GET("creator-assistant/sessions/{sessionId}")
    suspend fun getSessionHistory(
        @Path("sessionId") sessionId: String,
    ): ApiEnvelope<CreatorAssistantSessionHistoryDto>

    @POST("creator-assistant/sessions/{sessionId}/abandon")
    suspend fun abandonSession(
        @Path("sessionId") sessionId: String,
    ): ApiEnvelope<Map<String, Any>>
}
