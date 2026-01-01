package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.IapProductDto
import com.stproject.client.android.core.network.IapRestoreRequestDto
import com.stproject.client.android.core.network.IapRestoreTransactionItemDto
import com.stproject.client.android.core.network.IapSubmitTransactionRequestDto
import com.stproject.client.android.core.network.StIapApi
import com.stproject.client.android.domain.model.IapProduct
import com.stproject.client.android.domain.repository.IapCatalog
import com.stproject.client.android.domain.repository.IapRepository
import com.stproject.client.android.domain.repository.IapRestoreRequest
import com.stproject.client.android.domain.repository.IapRestoreResult
import com.stproject.client.android.domain.repository.IapRestoreTransactionItem
import com.stproject.client.android.domain.repository.IapTransactionRequest
import com.stproject.client.android.domain.repository.IapTransactionResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpIapRepository
    @Inject
    constructor(
        private val api: StIapApi,
        private val apiClient: ApiClient,
    ) : IapRepository {
        override suspend fun getCatalog(): IapCatalog {
            val resp = apiClient.call { api.getProducts() }
            val products = resp.products ?: emptyList()
            return IapCatalog(
                environment = resp.environment?.trim()?.takeIf { it.isNotEmpty() },
                products = products.mapNotNull { it.toDomain() },
            )
        }

        override suspend fun submitTransaction(request: IapTransactionRequest): IapTransactionResult {
            val resp = apiClient.call { api.submitTransaction(request.toDto()) }
            return IapTransactionResult(
                ok = resp.ok,
                status = resp.status,
                serverTimeMs = resp.serverTimeMs,
            )
        }

        override suspend fun restore(request: IapRestoreRequest): IapRestoreResult {
            val resp = apiClient.call { api.restore(request.toDto()) }
            return IapRestoreResult(serverTimeMs = resp.serverTimeMs)
        }

        private fun IapProductDto.toDomain(): IapProduct? {
            val idValue = productId?.trim().orEmpty()
            if (idValue.isEmpty()) return null
            val name = displayName?.trim().takeIf { !it.isNullOrEmpty() } ?: idValue
            val kindValue = kind?.trim()?.lowercase().orEmpty()
            return IapProduct(
                productId = idValue,
                kind = kindValue,
                displayName = name,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                enabled = enabled ?: false,
                grantCredits = grantCredits,
                vipType = vipType?.trim()?.takeIf { it.isNotEmpty() },
                durationMonths = durationMonths,
            )
        }

        private fun IapTransactionRequest.toDto(): IapSubmitTransactionRequestDto =
            IapSubmitTransactionRequestDto(
                platform = platform,
                kind = kind,
                productId = productId,
                environment = environment,
                transactionId = transactionId,
                purchaseToken = purchaseToken,
                packageName = packageName,
                orderId = orderId,
                purchaseTimeMs = purchaseTimeMs,
                purchaseState = purchaseState,
                purchaseData = purchaseData,
                purchaseSignature = purchaseSignature,
                idempotencyKey = idempotencyKey,
                clientTimeMs = clientTimeMs,
            )

        private fun IapRestoreRequest.toDto(): IapRestoreRequestDto =
            IapRestoreRequestDto(
                platform = platform,
                environment = environment,
                transactions = transactions.map { it.toDto() },
            )

        private fun IapRestoreTransactionItem.toDto(): IapRestoreTransactionItemDto =
            IapRestoreTransactionItemDto(
                transactionId = transactionId,
                productId = productId,
                purchaseToken = purchaseToken,
                purchaseData = purchaseData,
                purchaseSignature = purchaseSignature,
                orderId = orderId,
            )
    }
