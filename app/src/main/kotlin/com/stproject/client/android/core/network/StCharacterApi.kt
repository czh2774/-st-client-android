package com.stproject.client.android.core.network

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StCharacterApi {
    @POST("characters/query")
    suspend fun queryCharacters(
        @Body request: QueryCharactersRequestDto,
    ): ApiEnvelope<QueryCharactersResponseDto>

    @GET("characters/{id}")
    suspend fun getCharacter(
        @Path("id") id: String,
    ): ApiEnvelope<CharacterDetailDto>

    @GET("characters/share-code")
    suspend fun resolveShareCode(
        @Query("shareCode") shareCode: String,
    ): ApiEnvelope<ResolveShareCodeResponseDto>

    @GET("characters/{id}/share-code")
    suspend fun generateShareCode(
        @Path("id") id: String,
    ): ApiEnvelope<ShareCodeResponseDto>

    @GET("characters/{id}/export")
    suspend fun exportCharacter(
        @Path("id") id: String,
    ): ResponseBody

    @GET("characters/{id}/export-png")
    suspend fun exportCharacterPng(
        @Path("id") id: String,
    ): ResponseBody

    @POST("characters/block")
    suspend fun blockCharacter(
        @Body request: CharacterBlockRequestDto,
    ): ApiEnvelope<Map<String, Boolean>>

    @POST("characters/follow")
    suspend fun followCharacter(
        @Body request: CharacterFollowRequestDto,
    ): ApiEnvelope<CharacterFollowResponseDto>
}
