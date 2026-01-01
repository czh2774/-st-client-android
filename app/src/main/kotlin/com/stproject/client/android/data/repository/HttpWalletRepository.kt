package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.StWalletApi
import com.stproject.client.android.core.network.WalletBalanceDto
import com.stproject.client.android.core.network.WalletTransactionDto
import com.stproject.client.android.domain.model.WalletBalance
import com.stproject.client.android.domain.model.WalletTransaction
import com.stproject.client.android.domain.repository.WalletRepository
import com.stproject.client.android.domain.repository.WalletTransactionsResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpWalletRepository
    @Inject
    constructor(
        private val api: StWalletApi,
        private val apiClient: ApiClient,
    ) : WalletRepository {
        override suspend fun getBalance(): WalletBalance {
            val dto = apiClient.call { api.getBalance() }
            return dto.toDomain()
        }

        override suspend fun listTransactions(
            pageNum: Int,
            pageSize: Int,
        ): WalletTransactionsResult {
            val resp = apiClient.call { api.getTransactions(pageNum = pageNum, pageSize = pageSize) }
            val items = resp.items.orEmpty().mapNotNull { it.toDomain() }
            return WalletTransactionsResult(
                items = items,
                total = resp.total ?: items.size,
                hasMore = resp.hasMore ?: false,
                pageNum = resp.pageNum ?: pageNum,
                pageSize = resp.pageSize ?: pageSize,
            )
        }

        private fun WalletBalanceDto.toDomain(): WalletBalance =
            WalletBalance(
                balanceCredits = balanceCredits ?: 0,
                currency = currency?.trim()?.takeIf { it.isNotEmpty() } ?: "USD",
                diamonds = diamonds ?: 0,
            )

        private fun WalletTransactionDto.toDomain(): WalletTransaction? {
            val idValue = id?.trim().orEmpty()
            if (idValue.isEmpty()) return null
            return WalletTransaction(
                id = idValue,
                type = type?.trim().orEmpty(),
                amountCredits = amountCredits ?: 0,
                reason = reason?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = createdAt?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }
