package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.IapProduct

data class IapCatalog(
    val environment: String?,
    val products: List<IapProduct>,
)

data class IapTransactionRequest(
    val platform: String,
    val kind: String,
    val productId: String,
    val environment: String,
    val transactionId: String,
    val purchaseToken: String? = null,
    val packageName: String? = null,
    val orderId: String? = null,
    val purchaseTimeMs: Long? = null,
    val purchaseState: Int? = null,
    val purchaseData: String? = null,
    val purchaseSignature: String? = null,
    val idempotencyKey: String? = null,
    val clientTimeMs: Long? = null,
)

data class IapTransactionResult(
    val ok: Boolean?,
    val status: String?,
    val serverTimeMs: Long?,
)

data class IapRestoreTransactionItem(
    val transactionId: String,
    val productId: String? = null,
    val purchaseToken: String? = null,
    val purchaseData: String? = null,
    val purchaseSignature: String? = null,
    val orderId: String? = null,
)

data class IapRestoreRequest(
    val platform: String,
    val environment: String,
    val transactions: List<IapRestoreTransactionItem>,
)

data class IapRestoreResult(
    val serverTimeMs: Long?,
)

interface IapRepository {
    suspend fun getCatalog(): IapCatalog

    suspend fun submitTransaction(request: IapTransactionRequest): IapTransactionResult

    suspend fun restore(request: IapRestoreRequest): IapRestoreResult
}
