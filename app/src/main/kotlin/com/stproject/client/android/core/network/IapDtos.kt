package com.stproject.client.android.core.network

data class IapProductDto(
    val productId: String? = null,
    val kind: String? = null,
    val displayName: String? = null,
    val description: String? = null,
    val enabled: Boolean? = null,
    val grantCredits: Int? = null,
    val vipType: String? = null,
    val durationMonths: Int? = null,
    val tags: List<String>? = null,
)

data class IapProductsResponseDto(
    val schemaVersion: Int? = null,
    val environment: String? = null,
    val catalogVersion: String? = null,
    val updatedAtMs: Long? = null,
    val products: List<IapProductDto>? = null,
)

data class IapSubmitTransactionRequestDto(
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

data class IapSubmitTransactionResponseDto(
    val ok: Boolean? = null,
    val status: String? = null,
    val serverTimeMs: Long? = null,
)

data class IapRestoreTransactionItemDto(
    val transactionId: String,
    val productId: String? = null,
    val purchaseToken: String? = null,
    val purchaseData: String? = null,
    val purchaseSignature: String? = null,
    val orderId: String? = null,
)

data class IapRestoreRequestDto(
    val platform: String,
    val environment: String,
    val transactions: List<IapRestoreTransactionItemDto>,
)

data class IapRestoreResponseDto(
    val serverTimeMs: Long? = null,
)
