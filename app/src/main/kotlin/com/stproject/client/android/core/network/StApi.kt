package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * v1 API interface aligned with st-server-go and st-client-react pact tests.
 */
interface StApi {
    @GET("health")
    suspend fun health(): ApiEnvelope<HealthDto>

    @POST("chats")
    suspend fun createChatSession(
        @Body request: CreateChatSessionRequestDto,
    ): ApiEnvelope<CreateChatSessionResponseDto>

    @GET("chats")
    suspend fun listChatSessions(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("characterId") characterId: String? = null,
    ): ApiEnvelope<ChatSessionsResponseDto>

    @GET("chats/{sessionId}")
    suspend fun getChatSession(
        @Path("sessionId") sessionId: String,
    ): ApiEnvelope<ChatSessionDetailDto>

    @GET("chats/{sessionId}/messages")
    suspend fun listChatMessages(
        @Path("sessionId") sessionId: String,
        @Query("limit") limit: Int? = null,
        @Query("beforeMessageId") beforeMessageId: String? = null,
    ): ApiEnvelope<ChatMessagesResponseDto>

    @POST("chats/{sessionId}/completion")
    suspend fun createChatCompletion(
        @Path("sessionId") sessionId: String,
        @Body request: ChatCompletionRequestDto,
    ): ApiEnvelope<ChatCompletionResponseDto>

    @PUT("chats/{sessionId}")
    suspend fun updateChatSession(
        @Path("sessionId") sessionId: String,
        @Body request: UpdateChatSessionRequestDto,
    ): ApiEnvelope<ChatSessionDetailDto>

    @POST("dialogs/delete")
    suspend fun deleteDialog(
        @Body request: DialogDeleteRequestDto,
    ): ApiEnvelope<DialogDeleteResponseDto>

    @POST("dialogs/swipe")
    suspend fun setActiveSwipe(
        @Body request: DialogSwipeRequestDto,
    ): ApiEnvelope<DialogSwipeResponseDto>

    @POST("dialogs/swipe/delete")
    suspend fun deleteSwipe(
        @Body request: DialogSwipeDeleteRequestDto,
    ): ApiEnvelope<DialogSwipeResponseDto>

    @POST("a2ui/event")
    suspend fun sendA2UIEvent(
        @Body request: A2UIEventRequestDto,
    ): ApiEnvelope<A2UIEventResponseDto>
}
