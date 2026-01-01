package com.stproject.client.android.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingManager
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : PurchasesUpdatedListener {
        private val billingClient =
            BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build()

        private val _purchaseUpdates =
            MutableSharedFlow<BillingPurchaseUpdate>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val purchaseUpdates: SharedFlow<BillingPurchaseUpdate> = _purchaseUpdates

        fun isReady(): Boolean = billingClient.isReady

        suspend fun connect(): Boolean {
            if (billingClient.isReady) return true
            return suspendCancellableCoroutine { cont ->
                billingClient.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(result: BillingResult) {
                            cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                        }

                        override fun onBillingServiceDisconnected() {
                            if (!cont.isCompleted) {
                                cont.resume(false)
                            }
                        }
                    },
                )
            }
        }

        suspend fun queryProductDetails(products: List<BillingProductQuery>): List<ProductDetails> {
            if (products.isEmpty()) return emptyList()
            if (!billingClient.isReady) {
                val connected = connect()
                if (!connected) return emptyList()
            }
            val params =
                QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        products.map { item ->
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(item.productId)
                                .setProductType(item.productType)
                                .build()
                        },
                    )
                    .build()
            return suspendCancellableCoroutine { cont ->
                billingClient.queryProductDetailsAsync(params) { result, details ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(details)
                    } else {
                        cont.resume(emptyList())
                    }
                }
            }
        }

        fun launchPurchase(
            activity: Activity,
            productDetails: ProductDetails,
        ): BillingResult {
            val params =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build(),
                        ),
                    )
                    .build()
            return billingClient.launchBillingFlow(activity, params)
        }

        suspend fun acknowledgePurchase(purchaseToken: String): Boolean {
            val token = purchaseToken.trim()
            if (token.isEmpty()) return false
            if (!billingClient.isReady) {
                val connected = connect()
                if (!connected) return false
            }
            val params =
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(token)
                    .build()
            return suspendCancellableCoroutine { cont ->
                billingClient.acknowledgePurchase(params) { result ->
                    cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }
            }
        }

        suspend fun consumePurchase(purchaseToken: String): Boolean {
            val token = purchaseToken.trim()
            if (token.isEmpty()) return false
            if (!billingClient.isReady) {
                val connected = connect()
                if (!connected) return false
            }
            val params =
                ConsumeParams.newBuilder()
                    .setPurchaseToken(token)
                    .build()
            return suspendCancellableCoroutine { cont ->
                billingClient.consumeAsync(params) { result, _ ->
                    cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }
            }
        }

        suspend fun queryPurchases(productType: String): List<Purchase> {
            if (!billingClient.isReady) {
                val connected = connect()
                if (!connected) return emptyList()
            }
            val params =
                QueryPurchasesParams.newBuilder()
                    .setProductType(productType)
                    .build()
            return suspendCancellableCoroutine { cont ->
                billingClient.queryPurchasesAsync(params) { result, purchases ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(purchases)
                    } else {
                        cont.resume(emptyList())
                    }
                }
            }
        }

        override fun onPurchasesUpdated(
            result: BillingResult,
            purchases: MutableList<Purchase>?,
        ) {
            _purchaseUpdates.tryEmit(BillingPurchaseUpdate(result, purchases.orEmpty()))
        }
    }

data class BillingProductQuery(
    val productId: String,
    val productType: String,
)

data class BillingPurchaseUpdate(
    val result: BillingResult,
    val purchases: List<Purchase>,
)
