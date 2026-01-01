package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.WalletBalance
import com.stproject.client.android.domain.model.WalletTransaction

data class WalletTransactionsResult(
    val items: List<WalletTransaction>,
    val total: Int,
    val hasMore: Boolean,
    val pageNum: Int,
    val pageSize: Int,
)

interface WalletRepository {
    suspend fun getBalance(): WalletBalance

    suspend fun listTransactions(
        pageNum: Int,
        pageSize: Int,
    ): WalletTransactionsResult
}
