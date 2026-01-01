package com.stproject.client.android.core.network

data class WalletBalanceDto(
    val balanceCredits: Int? = null,
    val currency: String? = null,
    val diamonds: Int? = null,
)

data class WalletTransactionDto(
    val id: String? = null,
    val type: String? = null,
    val amountCredits: Int? = null,
    val reason: String? = null,
    val createdAt: String? = null,
)

data class WalletTransactionsResponseDto(
    val items: List<WalletTransactionDto>? = null,
    val total: Int? = null,
    val hasMore: Boolean? = null,
    val pageNum: Int? = null,
    val pageSize: Int? = null,
)
