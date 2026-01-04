package com.stproject.client.android.features.shop

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.stproject.client.android.R
import com.stproject.client.android.core.billing.BillingManager
import com.stproject.client.android.core.billing.BillingPurchaseUpdate
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.IapProduct
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.IapCatalog
import com.stproject.client.android.domain.repository.IapRepository
import com.stproject.client.android.domain.repository.IapRestoreRequest
import com.stproject.client.android.domain.repository.IapRestoreResult
import com.stproject.client.android.domain.repository.IapTransactionRequest
import com.stproject.client.android.domain.repository.IapTransactionResult
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ShopScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeIapRepository : IapRepository {
        val restoreStarted = CompletableDeferred<IapRestoreRequest>()
        val restoreGate = CompletableDeferred<Unit>()

        override suspend fun getCatalog(): IapCatalog {
            return IapCatalog(
                environment = "Sandbox",
                products =
                    listOf(
                        IapProduct(
                            productId = "prod-1",
                            kind = "credits",
                            displayName = "Test",
                            description = null,
                            enabled = true,
                            grantCredits = 100,
                            vipType = null,
                            durationMonths = null,
                        ),
                    ),
            )
        }

        override suspend fun submitTransaction(request: IapTransactionRequest): IapTransactionResult {
            return IapTransactionResult(ok = true, status = "processed", serverTimeMs = 0)
        }

        override suspend fun restore(request: IapRestoreRequest): IapRestoreResult {
            restoreStarted.complete(request)
            restoreGate.await()
            return IapRestoreResult(serverTimeMs = 0)
        }
    }

    private class AllowAllAccessManager : ContentAccessManager {
        private val _gate =
            MutableStateFlow(
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = true,
                ),
            )

        override val gate: StateFlow<ContentGate> = _gate

        override fun updateGate(gate: ContentGate) {
            _gate.value = gate
        }

        override fun decideAccess(isNsfw: Boolean?): ContentAccessDecision {
            return ContentAccessDecision.Allowed
        }
    }

    private class FakeCharacterRepository : CharacterRepository {
        override suspend fun queryCharacters(
            cursor: String?,
            limit: Int?,
            sortBy: String?,
            isNsfw: Boolean?,
        ): List<CharacterSummary> = emptyList()

        override suspend fun getCharacterDetail(characterId: String): CharacterDetail {
            return CharacterDetail(
                id = characterId,
                name = "Test",
                description = "",
                tags = emptyList(),
                creatorName = null,
                isNsfw = false,
                totalFollowers = 0,
                isFollowed = false,
            )
        }

        override suspend fun resolveShareCode(shareCode: String): String? = null

        override suspend fun generateShareCode(characterId: String): ShareCodeInfo? = null

        override suspend fun blockCharacter(
            characterId: String,
            value: Boolean,
        ) = Unit

        override suspend fun followCharacter(
            characterId: String,
            value: Boolean,
        ): CharacterFollowResult {
            return CharacterFollowResult(totalFollowers = 0, isFollowed = false)
        }
    }

    private fun createViewModel(
        repo: IapRepository,
        billingManager: BillingManager,
    ): ShopViewModel {
        val resolveContentAccess =
            ResolveContentAccessUseCase(
                accessManager = AllowAllAccessManager(),
                characterRepository = FakeCharacterRepository(),
            )
        return ShopViewModel(repo, billingManager, resolveContentAccess)
    }

    @Test
    fun restorePurchasesShowsRestoringAndSubmitsRequest() {
        val repo = FakeIapRepository()
        val updates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 1)
        val billingManager = mockk<BillingManager>()
        every { billingManager.purchaseUpdates } returns updates
        coEvery { billingManager.queryProductDetails(any()) } returns emptyList()
        coEvery { billingManager.connect() } returns true
        coEvery { billingManager.consumePurchase(any()) } returns true
        coEvery { billingManager.acknowledgePurchase(any()) } returns true

        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchase.purchaseToken } returns "token-1"
        every { purchase.products } returns listOf("prod-1")
        every { purchase.orderId } returns "order-1"
        every { purchase.purchaseTime } returns 123L
        every { purchase.originalJson } returns "{\"productId\":\"prod-1\"}"
        every { purchase.signature } returns "sig"
        every { purchase.isAcknowledged } returns false

        coEvery { billingManager.queryPurchases(BillingClient.ProductType.INAPP) } returns listOf(purchase)
        coEvery { billingManager.queryPurchases(BillingClient.ProductType.SUBS) } returns emptyList()

        val viewModel = createViewModel(repo, billingManager)
        composeRule.setContent {
            ShopScreen(viewModel = viewModel)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.purchaseEnabled
        }

        composeRule.onNodeWithText("Restore Purchases").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            repo.restoreStarted.isCompleted
        }

        composeRule.onNodeWithText("Restoring...").assertIsDisplayed()
        val request = repo.restoreStarted.getCompleted()
        assertEquals("android", request.platform)

        repo.restoreGate.complete(Unit)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.isRestoring
        }
    }

    @Test
    fun launchPurchaseShowsBillingError() {
        val repo = FakeIapRepository()
        val updates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 1)
        val billingManager = mockk<BillingManager>()
        every { billingManager.purchaseUpdates } returns updates

        val productDetails = mockk<ProductDetails>(relaxed = true)
        every { productDetails.productId } returns "prod-1"
        every { productDetails.productType } returns BillingClient.ProductType.INAPP
        coEvery { billingManager.queryProductDetails(any()) } returns listOf(productDetails)
        coEvery { billingManager.connect() } returns true
        every { billingManager.launchPurchase(any(), any()) } returns
            BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
                .setDebugMessage("service down")
                .build()

        val viewModel = createViewModel(repo, billingManager)
        composeRule.setContent {
            ShopScreen(viewModel = viewModel)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.purchaseEnabled
        }

        val buyLabel = composeRule.activity.getString(R.string.shop_buy)
        composeRule.onNodeWithText(buyLabel).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.error?.message?.contains("billing error") == true
        }
        composeRule.onNodeWithText("billing error: service down").assertIsDisplayed()
    }

    @Test
    fun restorePurchasesShowsErrorWhenEmpty() {
        val repo = FakeIapRepository()
        val updates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 1)
        val billingManager = mockk<BillingManager>()
        every { billingManager.purchaseUpdates } returns updates
        coEvery { billingManager.queryProductDetails(any()) } returns emptyList()
        coEvery { billingManager.connect() } returns true
        coEvery { billingManager.queryPurchases(BillingClient.ProductType.INAPP) } returns emptyList()
        coEvery { billingManager.queryPurchases(BillingClient.ProductType.SUBS) } returns emptyList()

        val viewModel = createViewModel(repo, billingManager)
        composeRule.setContent {
            ShopScreen(viewModel = viewModel)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.purchaseEnabled
        }

        composeRule.onNodeWithText("Restore Purchases").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.error != null
        }

        composeRule.onNodeWithText("No purchases to restore.").assertIsDisplayed()
    }

    @Test
    fun restorePurchasesShowsErrorWhenBillingUnavailable() {
        val repo = FakeIapRepository()
        val updates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 1)
        val billingManager = mockk<BillingManager>()
        every { billingManager.purchaseUpdates } returns updates
        coEvery { billingManager.queryProductDetails(any()) } returns emptyList()
        coEvery { billingManager.connect() } returnsMany listOf(true, false)
        coEvery { billingManager.queryPurchases(BillingClient.ProductType.INAPP) } returns emptyList()
        coEvery { billingManager.queryPurchases(BillingClient.ProductType.SUBS) } returns emptyList()

        val viewModel = createViewModel(repo, billingManager)
        composeRule.setContent {
            ShopScreen(viewModel = viewModel)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.purchaseEnabled
        }

        composeRule.onNodeWithText("Restore Purchases").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.error != null
        }

        composeRule.onNodeWithText("Google Play billing not available.").assertIsDisplayed()
    }

    @Test
    fun purchaseAcknowledgementFailureShowsError() {
        val repo = FakeIapRepository()
        val updates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 1)
        val billingManager = mockk<BillingManager>()
        every { billingManager.purchaseUpdates } returns updates
        coEvery { billingManager.queryProductDetails(any()) } returns emptyList()
        coEvery { billingManager.connect() } returns true
        coEvery { billingManager.consumePurchase(any()) } returns false
        coEvery { billingManager.acknowledgePurchase(any()) } returns false

        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchase.purchaseToken } returns "token-ack"
        every { purchase.products } returns listOf("prod-1")
        every { purchase.orderId } returns "order-ack"
        every { purchase.purchaseTime } returns 123L
        every { purchase.originalJson } returns "{\"productId\":\"prod-1\"}"
        every { purchase.signature } returns "sig"
        every { purchase.isAcknowledged } returns false

        val viewModel = createViewModel(repo, billingManager)
        composeRule.setContent {
            ShopScreen(viewModel = viewModel)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.purchaseEnabled
        }

        val result =
            BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.OK)
                .build()
        updates.tryEmit(BillingPurchaseUpdate(result, listOf(purchase)))

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.error != null
        }

        composeRule.onNodeWithText("purchase acknowledgement failed").assertIsDisplayed()
    }
}
