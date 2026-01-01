package com.stproject.client.android.features.shop

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.stproject.client.android.BuildConfig
import com.stproject.client.android.core.billing.BillingManager
import com.stproject.client.android.core.billing.BillingProductQuery
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.IapProduct
import com.stproject.client.android.domain.repository.IapRepository
import com.stproject.client.android.domain.repository.IapRestoreRequest
import com.stproject.client.android.domain.repository.IapRestoreTransactionItem
import com.stproject.client.android.domain.repository.IapTransactionRequest
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShopViewModel
    @Inject
    constructor(
        private val iapRepository: IapRepository,
        private val billingManager: BillingManager,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ShopUiState())
        val uiState: StateFlow<ShopUiState> = _uiState

        private val productDetailsMap = mutableMapOf<String, ProductDetails>()
        private val catalogProducts = mutableMapOf<String, IapProduct>()
        private var catalogEnvironment: String? = null

        init {
            viewModelScope.launch {
                billingManager.purchaseUpdates.collect { update ->
                    handlePurchaseUpdate(update.result, update.purchases)
                }
            }
        }

        fun load() {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(isLoading = false, error = accessErrorMessage(access)) }
                        return@launch
                    }
                    val catalog = iapRepository.getCatalog()
                    val enabledProducts = catalog.products.filter { it.enabled }
                    catalogProducts.clear()
                    catalogProducts.putAll(enabledProducts.associateBy { it.productId })
                    catalogEnvironment = catalog.environment?.trim()?.takeIf { it.isNotEmpty() }
                    val details =
                        billingManager.queryProductDetails(
                            enabledProducts.mapNotNull { it.toQuery() },
                        )
                    productDetailsMap.clear()
                    details.forEach { productDetailsMap[it.productId] = it }
                    val shopProducts = enabledProducts.map { it.toShopProduct(productDetailsMap[it.productId]) }
                    val billingReady = billingManager.connect()
                    val purchaseEnabled = billingReady && shopProducts.isNotEmpty()
                    val purchaseDisabledReason =
                        if (purchaseEnabled) {
                            null
                        } else if (!billingReady) {
                            "Google Play billing not available."
                        } else {
                            "No purchasable products available."
                        }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            products = shopProducts,
                            purchaseEnabled = purchaseEnabled,
                            purchaseDisabledReason = purchaseDisabledReason,
                            isRestoring = false,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun launchPurchase(
            activity: Activity,
            productId: String,
        ) {
            if (!_uiState.value.purchaseEnabled) {
                _uiState.update {
                    it.copy(error = it.purchaseDisabledReason ?: "purchases disabled")
                }
                return
            }
            val details =
                productDetailsMap[productId] ?: run {
                    _uiState.update { it.copy(error = "product not available") }
                    return
                }
            viewModelScope.launch {
                val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                if (access is ContentAccessDecision.Blocked) {
                    _uiState.update { it.copy(error = accessErrorMessage(access)) }
                    return@launch
                }
                val result = billingManager.launchPurchase(activity, details)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    _uiState.update { it.copy(error = "billing error: ${result.debugMessage}") }
                }
            }
        }

        fun restorePurchases() {
            if (_uiState.value.isRestoring) return
            _uiState.update { it.copy(isRestoring = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(isRestoring = false, error = accessErrorMessage(access)) }
                        return@launch
                    }
                    val ready = billingManager.connect()
                    if (!ready) {
                        _uiState.update {
                            it.copy(isRestoring = false, error = "Google Play billing not available.")
                        }
                        return@launch
                    }
                    val inApps = billingManager.queryPurchases(BillingClient.ProductType.INAPP)
                    val subs = billingManager.queryPurchases(BillingClient.ProductType.SUBS)
                    val purchases =
                        (inApps + subs)
                            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                            .distinctBy { it.purchaseToken }
                    if (purchases.isEmpty()) {
                        _uiState.update { it.copy(isRestoring = false, error = "No purchases to restore.") }
                        return@launch
                    }
                    val environment = catalogEnvironment ?: "Production"
                    val items =
                        purchases.flatMap { purchase ->
                            val token = purchase.purchaseToken.trim()
                            if (token.isEmpty()) return@flatMap emptyList()
                            val products = purchase.products
                            if (products.isEmpty()) return@flatMap emptyList()
                            products.map { productId ->
                                IapRestoreTransactionItem(
                                    transactionId = token,
                                    productId = productId,
                                    purchaseToken = token,
                                    purchaseData = purchase.originalJson,
                                    purchaseSignature = purchase.signature,
                                    orderId = purchase.orderId,
                                )
                            }
                        }
                    if (items.isEmpty()) {
                        _uiState.update { it.copy(isRestoring = false, error = "No valid purchases found.") }
                        return@launch
                    }
                    iapRepository.restore(
                        IapRestoreRequest(
                            platform = "android",
                            environment = environment,
                            transactions = items,
                        ),
                    )
                    purchases.forEach { purchase ->
                        finalizePurchase(purchase)
                    }
                    _uiState.update { it.copy(isRestoring = false) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isRestoring = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isRestoring = false, error = "unexpected error") }
                }
            }
        }

        private suspend fun handlePurchaseUpdate(
            result: com.android.billingclient.api.BillingResult,
            purchases: List<Purchase>,
        ) {
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) return
                _uiState.update { it.copy(error = "billing error: ${result.debugMessage}") }
                return
            }
            if (purchases.isEmpty()) return
            val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
            if (access is ContentAccessDecision.Blocked) {
                _uiState.update { it.copy(error = accessErrorMessage(access)) }
                return
            }
            val environment = catalogEnvironment ?: "Production"
            for (purchase in purchases) {
                if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
                val token = purchase.purchaseToken.trim()
                if (token.isEmpty()) continue
                val products = purchase.products
                if (products.isEmpty()) continue
                try {
                    for (productId in products) {
                        val product = catalogProducts[productId] ?: continue
                        iapRepository.submitTransaction(
                            IapTransactionRequest(
                                platform = "android",
                                kind = product.kind,
                                productId = productId,
                                environment = environment,
                                transactionId = token,
                                purchaseToken = token,
                                packageName = BuildConfig.APPLICATION_ID,
                                orderId = purchase.orderId,
                                purchaseTimeMs = purchase.purchaseTime,
                                purchaseState = purchase.purchaseState,
                                purchaseData = purchase.originalJson,
                                purchaseSignature = purchase.signature,
                                idempotencyKey = "android:$token",
                                clientTimeMs = System.currentTimeMillis(),
                            ),
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(error = e.userMessage ?: e.message) }
                    continue
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(error = "unexpected error") }
                    continue
                }

                if (!purchase.isAcknowledged) {
                    finalizePurchase(purchase)
                }
            }
        }

        private suspend fun finalizePurchase(purchase: Purchase) {
            if (purchase.isAcknowledged) return
            val token = purchase.purchaseToken.trim()
            if (token.isEmpty()) return
            val products = purchase.products
            if (products.isEmpty()) return
            val hasSubscription =
                products.any { productId ->
                    catalogProducts[productId]?.kind?.lowercase() == "subscription"
                }
            val acknowledged =
                if (hasSubscription) {
                    billingManager.acknowledgePurchase(token)
                } else {
                    billingManager.consumePurchase(token)
                }
            if (!acknowledged) {
                _uiState.update { it.copy(error = "purchase acknowledgement failed") }
            }
        }

        private fun IapProduct.toQuery(): BillingProductQuery? {
            val type =
                when (kind.lowercase()) {
                    "subscription" -> BillingClient.ProductType.SUBS
                    "credits" -> BillingClient.ProductType.INAPP
                    else -> BillingClient.ProductType.INAPP
                }
            return BillingProductQuery(productId = productId, productType = type)
        }

        private fun IapProduct.toShopProduct(details: ProductDetails?): ShopProduct {
            val price =
                when (details?.productType) {
                    BillingClient.ProductType.INAPP -> details.oneTimePurchaseOfferDetails?.formattedPrice
                    BillingClient.ProductType.SUBS -> {
                        details.subscriptionOfferDetails
                            ?.firstOrNull()
                            ?.pricingPhases
                            ?.pricingPhaseList
                            ?.firstOrNull()
                            ?.formattedPrice
                    }
                    else -> null
                }
            return ShopProduct(
                productId = productId,
                displayName = displayName,
                description = description,
                kind = kind,
                price = price,
                enabled = enabled,
            )
        }

        private fun accessErrorMessage(access: ContentAccessDecision.Blocked): String {
            return when (access.reason) {
                ContentBlockReason.NSFW_DISABLED -> "mature content disabled"
                ContentBlockReason.AGE_REQUIRED -> "age verification required"
                ContentBlockReason.CONSENT_REQUIRED -> "terms acceptance required"
                ContentBlockReason.CONSENT_PENDING -> "compliance not loaded"
            }
        }
    }
