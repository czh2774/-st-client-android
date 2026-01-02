package com.stproject.client.android.core.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface StCardApi {
    @POST("cards")
    suspend fun createCard(
        @Body request: Map<String, Any>,
    ): ApiEnvelope<CardCreateResponseDto>

    @PUT("cards/{id}")
    suspend fun updateCard(
        @Path("id") id: String,
        @Body request: Map<String, Any>,
    ): ApiEnvelope<CardCreateResponseDto>

    @POST("cards/parse-text")
    suspend fun parseText(
        @Body request: CardParseTextRequestDto,
    ): ApiEnvelope<CardParseResponseDto>

    @Multipart
    @POST("cards/parse-file")
    suspend fun parseFile(
        @Part file: MultipartBody.Part,
    ): ApiEnvelope<CardParseResponseDto>

    @GET("cards/template")
    suspend fun getTemplate(): ResponseBody
}
