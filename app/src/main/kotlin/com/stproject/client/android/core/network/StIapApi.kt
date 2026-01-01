package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StIapApi {
    @GET("iap/products")
    suspend fun getProducts(): ApiEnvelope<IapProductsResponseDto>

    @POST("iap/transactions")
    suspend fun submitTransaction(
        @Body request: IapSubmitTransactionRequestDto,
    ): ApiEnvelope<IapSubmitTransactionResponseDto>

    @POST("iap/restore")
    suspend fun restore(
        @Body request: IapRestoreRequestDto,
    ): ApiEnvelope<IapRestoreResponseDto>
}
