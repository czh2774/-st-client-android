package com.stproject.client.android.core.network

import retrofit2.http.GET
import retrofit2.http.Query

interface StPresetApi {
    @GET("presets")
    suspend fun listPresets(
        @Query("seriesId") seriesId: String? = null,
    ): ApiEnvelope<PresetListResponseDto>
}
