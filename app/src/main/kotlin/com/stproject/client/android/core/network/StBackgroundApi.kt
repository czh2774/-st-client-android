package com.stproject.client.android.core.network

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface StBackgroundApi {
    @POST("backgrounds/all")
    suspend fun listBackgrounds(): ApiEnvelope<BackgroundListResponseDto>

    @Multipart
    @POST("backgrounds/upload")
    suspend fun uploadBackground(
        @Part file: MultipartBody.Part,
    ): ApiEnvelope<String>

    @POST("backgrounds/rename")
    suspend fun renameBackground(
        @Body request: BackgroundRenameRequestDto,
    ): ApiEnvelope<OkResponseDto>

    @POST("backgrounds/delete")
    suspend fun deleteBackground(
        @Body request: BackgroundDeleteRequestDto,
    ): ApiEnvelope<OkResponseDto>
}
