package com.stproject.client.android.domain.model

data class WalletBalance(
    val balanceCredits: Int,
    val currency: String,
    val diamonds: Int,
)

data class WalletTransaction(
    val id: String,
    val type: String,
    val amountCredits: Int,
    val reason: String?,
    val createdAt: String?,
)
