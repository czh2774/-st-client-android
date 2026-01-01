package com.stproject.client.android.core.network

import retrofit2.http.GET
import retrofit2.http.Query

interface StWalletApi {
    @GET("wallet/balance")
    suspend fun getBalance(): ApiEnvelope<WalletBalanceDto>

    @GET("wallet/transactions")
    suspend fun getTransactions(
        @Query("pageNum") pageNum: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
    ): ApiEnvelope<WalletTransactionsResponseDto>
}
