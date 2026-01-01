package com.stproject.client.android.features.shop

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
    val error: String? = null,
    val purchaseEnabled: Boolean = false,
    val purchaseDisabledReason: String? = null,
    val isRestoring: Boolean = false,
)
