package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface StUserApi {
    @GET("users/me")
    suspend fun getMe(): ApiEnvelope<UserMeDto>

    @DELETE("users/me")
    suspend fun deleteMe(): ApiEnvelope<Map<String, Boolean>>

    @POST("users/accept-tos")
    suspend fun acceptTos(
        @Body request: AcceptTosRequestDto,
    ): ApiEnvelope<AcceptTosResponseDto>

    @GET("users/config")
    suspend fun getUserConfig(): ApiEnvelope<UserConfigDto>

    @PUT("users/config")
    suspend fun updateUserConfig(
        @Body request: UpdateUserConfigRequestDto,
    ): ApiEnvelope<UserConfigDto>

    @GET("users/decorations")
    suspend fun listDecorations(): ApiEnvelope<DecorationListResponseDto>

    @POST("users/decorations/equip")
    suspend fun equipDecoration(
        @Body request: EquipDecorationRequestDto,
    ): ApiEnvelope<OkResponseDto>
}
