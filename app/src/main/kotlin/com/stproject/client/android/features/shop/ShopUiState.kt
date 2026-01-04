package com.stproject.client.android.features.shop

import com.stproject.client.android.core.network.AppError

data class ShopProduct(
    val productId: String,
    val displayName: String,
    val description: String?,
    val kind: String,
    val price: String?,
    val enabled: Boolean,
)

data class ShopUiState(
    val isLoading: Boolean = false,
    val products: List<ShopProduct> = emptyList(),
    val error: AppError? = null,
    val purchaseEnabled: Boolean = false,
    val purchaseDisabledReason: String? = null,
    val isRestoring: Boolean = false,
)
