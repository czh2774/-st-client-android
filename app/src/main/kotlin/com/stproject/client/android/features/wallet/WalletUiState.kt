package com.stproject.client.android.features.wallet

import com.stproject.client.android.core.network.AppError
import com.stproject.client.android.domain.model.WalletBalance
import com.stproject.client.android.domain.model.WalletTransaction

data class WalletUiState(
    val isLoading: Boolean = false,
    val balance: WalletBalance? = null,
    val transactions: List<WalletTransaction> = emptyList(),
    val hasMore: Boolean = false,
    val pageNum: Int = 1,
    val pageSize: Int = 20,
    val error: AppError? = null,
)
