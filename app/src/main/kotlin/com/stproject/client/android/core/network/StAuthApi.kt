package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.POST

interface StAuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: AuthLoginRequestDto): ApiEnvelope<AuthLoginResponseDto>

    @POST("auth/logout")
    suspend fun logout(@Body request: AuthLogoutRequestDto): ApiEnvelope<Map<String, String>>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: AuthRefreshRequestDto): ApiEnvelope<AuthRefreshResponseDto>
}
