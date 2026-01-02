package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StCommentApi {
    @GET("comments")
    suspend fun listComments(
        @Query("characterId") characterId: String,
        @Query("sortBy") sortBy: String? = null,
        @Query("pageNum") pageNum: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
    ): ApiEnvelope<CommentListResponseDto>

    @POST("comments")
    suspend fun createComment(
        @Body request: CreateCommentRequestDto,
    ): ApiEnvelope<CommentItemDto>

    @DELETE("comments/{id}")
    suspend fun deleteComment(
        @Path("id") commentId: String,
    ): ApiEnvelope<Map<String, Any>>

    @POST("comments/{id}/like")
    suspend fun likeComment(
        @Path("id") commentId: String,
        @Body request: LikeCommentRequestDto,
    ): ApiEnvelope<LikeCommentResponseDto>
}
