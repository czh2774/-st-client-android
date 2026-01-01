package com.stproject.client.android.core.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StCreatorApi {
    @GET("creators/list")
    suspend fun listCreators(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("searchKeyword") searchKeyword: String? = null,
    ): ApiEnvelope<QueryCreatorsResponseDto>

    @GET("creators/{id}/characters")
    suspend fun listCreatorCharacters(
        @Path("id") creatorId: String,
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int,
    ): ApiEnvelope<CreatorCharactersResponseDto>
}
