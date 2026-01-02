package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface StWorldInfoApi {
    @GET("worldinfo")
    suspend fun listEntries(
        @Query("characterId") characterId: String? = null,
        @Query("global") global: Boolean? = null,
        @Query("includeGlobal") includeGlobal: Boolean? = null,
    ): ApiEnvelope<WorldInfoListResponseDto>

    @POST("worldinfo")
    suspend fun createEntry(
        @Body request: WorldInfoUpsertRequestDto,
    ): ApiEnvelope<WorldInfoEntryDto>

    @PUT("worldinfo/{id}")
    suspend fun updateEntry(
        @Path("id") id: String,
        @Body request: WorldInfoUpsertRequestDto,
    ): ApiEnvelope<WorldInfoEntryDto>

    @DELETE("worldinfo/{id}")
    suspend fun deleteEntry(
        @Path("id") id: String,
    ): ApiEnvelope<WorldInfoDeleteResponseDto>
}
