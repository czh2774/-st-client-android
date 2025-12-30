package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * v1 API interface aligned with st-server-go and st-client-react pact tests.
 */
interface StApi {
    @GET("health")
    suspend fun health(): ApiEnvelope<HealthDto>

    @POST("chats")
    suspend fun createChatSession(
        @Body request: CreateChatSessionRequestDto
    ): ApiEnvelope<CreateChatSessionResponseDto>

    @POST("chats/{sessionId}/completion")
    suspend fun createChatCompletion(
        @Path("sessionId") sessionId: String,
        @Body request: ChatCompletionRequestDto
    ): ApiEnvelope<ChatCompletionResponseDto>
}

