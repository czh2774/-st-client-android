package com.stproject.client.android.features.shop

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.billing.BillingManager
import com.stproject.client.android.core.billing.BillingPurchaseUpdate
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
import com.stproject.client.android.features.chat.ChatViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest : BaseUnitTest() {
    private class FakeIapRepository(
        private val kind: String,
        private val transactionResult: IapTransactionResult =
            IapTransactionResult(ok = true, status = "processed", serverTimeMs = 0),
    ) : IapRepository {
        val submitted = mutableListOf<IapTransactionRequest>()
        val restoreRequests = mutableListOf<IapRestoreRequest>()

        override suspend fun getCatalog(): IapCatalog {
            return IapCatalog(
                environment = "Sandbox",
                products =
                    listOf(
                        IapProduct(
                            productId = "prod-1",
                            kind = kind,
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
            submitted.add(request)
            return transactionResult
        }

        override suspend fun restore(request: IapRestoreRequest): IapRestoreResult {
            restoreRequests.add(request)
            return IapRestoreResult(serverTimeMs = 0)
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
                accessManager = ChatViewModelTest.AllowAllAccessManager(),
                characterRepository = FakeCharacterRepository(),
            )
        return ShopViewModel(repo, billingManager, resolveContentAccess)
    }

    @Test
    fun `purchase update submits transaction and consumes credits`() =
        runTest(mainDispatcherRule.dispatcher) {
            val updates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 1)
            val billingManager = mockk<BillingManager>()
            every { billingManager.purchaseUpdates } returns updates
            coEvery { billingManager.queryProductDetails(any()) } returns emptyList()
            coEvery { billingManager.connect() } returns true
            coEvery { billingManager.consumePurchase(any()) } returns true
            coEvery { billingManager.acknowledgePurchase(any()) } returns true

            val repo = FakeIapRepository(kind = "credits")
            val viewModel = createViewModel(repo, billingManager)

            viewModel.load()
            advanceUntilIdle()

            val purchase = mockk<Purchase>()
            every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { purchase.purchaseToken } returns "token-1"
            every { purchase.products } returns listOf("prod-1")
            every { purchase.orderId } returns "order-1"
            every { purchase.purchaseTime } returns 123L
            every { purchase.originalJson } returns "{\"productId\":\"prod-1\"}"
            every { purchase.signature } returns "sig"
            every { purchase.isAcknowledged } returns false

            val result =
                BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.OK)
                    .build()
            updates.emit(BillingPurchaseUpdate(result, listOf(purchase)))
            advanceUntilIdle()

            assertEquals(1, repo.submitted.size)
            assertEquals("android", repo.submitted.first().platform)
            coVerify { billingManager.consumePurchase("token-1") }
            coVerify(exactly = 0) { billingManager.acknowledgePurchase(any()) }
        }

    @Test
    fun `purchase update acknowledges subscriptions`() =
        runTest(mainDispatcherRule.dispatcher) {
            val updates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 1)
            val billingManager = mockk<BillingManager>()
            every { billingManager.purchaseUpdates } returns updates
            coEvery { billingManager.queryProductDetails(any()) } returns emptyList()
            coEvery { billingManager.connect() } returns true
            coEvery { billingManager.consumePurchase(any()) } returns true
            coEvery { billingManager.acknowledgePurchase(any()) } returns true

            val repo = FakeIapRepository(kind = "subscription")
            val viewModel = createViewModel(repo, billingManager)

            viewModel.load()
            advanceUntilIdle()

            val purchase = mockk<Purchase>()
            every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { purchase.purchaseToken } returns "token-2"
            every { purchase.products } returns listOf("prod-1")
            every { purchase.orderId } returns "order-2"
            every { purchase.purchaseTime } returns 456L
            every { purchase.originalJson } returns "{\"productId\":\"prod-1\"}"
            every { purchase.signature } returns "sig"
            every { purchase.isAcknowledged } returns false

            val result =
                BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.OK)
                    .build()
            updates.emit(BillingPurchaseUpdate(result, listOf(purchase)))
            advanceUntilIdle()

            assertEquals(1, repo.submitted.size)
            coVerify { billingManager.acknowledgePurchase("token-2") }
            coVerify(exactly = 0) { billingManager.consumePurchase(any()) }
        }

    @Test
    fun `purchase acknowledgement failure surfaces error`() =
        runTest(mainDispatcherRule.dispatcher) {
            val updates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 1)
            val billingManager = mockk<BillingManager>()
            every { billingManager.purchaseUpdates } returns updates
            coEvery { billingManager.queryProductDetails(any()) } returns emptyList()
            coEvery { billingManager.connect() } returns true
            coEvery { billingManager.consumePurchase(any()) } returns false
            coEvery { billingManager.acknowledgePurchase(any()) } returns false

            val repo = FakeIapRepository(kind = "credits")
            val viewModel = createViewModel(repo, billingManager)

            viewModel.load()
            advanceUntilIdle()

            val purchase = mockk<Purchase>()
            every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { purchase.purchaseToken } returns "token-3"
            every { purchase.products } returns listOf("prod-1")
            every { purchase.orderId } returns "order-3"
            every { purchase.purchaseTime } returns 789L
            every { purchase.originalJson } returns "{\"productId\":\"prod-1\"}"
            every { purchase.signature } returns "sig"
            every { purchase.isAcknowledged } returns false

            val result =
                BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.OK)
                    .build()
            updates.emit(BillingPurchaseUpdate(result, listOf(purchase)))
            advanceUntilIdle()

            assertEquals("purchase acknowledgement failed", viewModel.uiState.value.error?.message)
        }

    @Test
    fun `purchase verification failure skips acknowledgement`() =
        runTest(mainDispatcherRule.dispatcher) {
            val updates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 1)
            val billingManager = mockk<BillingManager>()
            every { billingManager.purchaseUpdates } returns updates
            coEvery { billingManager.queryProductDetails(any()) } returns emptyList()
            coEvery { billingManager.connect() } returns true
            coEvery { billingManager.consumePurchase(any()) } returns true
            coEvery { billingManager.acknowledgePurchase(any()) } returns true

            val repo =
                FakeIapRepository(
                    kind = "credits",
                    transactionResult =
                        IapTransactionResult(
                            ok = false,
                            status = "verification_failed",
                            serverTimeMs = 0,
                        ),
                )
            val viewModel = createViewModel(repo, billingManager)

            viewModel.load()
            advanceUntilIdle()

            val purchase = mockk<Purchase>()
            every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { purchase.purchaseToken } returns "token-4"
            every { purchase.products } returns listOf("prod-1")
            every { purchase.orderId } returns "order-4"
            every { purchase.purchaseTime } returns 321L
            every { purchase.originalJson } returns "{\"productId\":\"prod-1\"}"
            every { purchase.signature } returns "sig"
            every { purchase.isAcknowledged } returns false

            val result =
                BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.OK)
                    .build()
            updates.emit(BillingPurchaseUpdate(result, listOf(purchase)))
            advanceUntilIdle()

            assertEquals("verification_failed", viewModel.uiState.value.error?.message)
            coVerify(exactly = 0) { billingManager.consumePurchase(any()) }
            coVerify(exactly = 0) { billingManager.acknowledgePurchase(any()) }
        }

    @Test
    fun `restore purchases submits transactions and consumes in-apps`() =
        runTest(mainDispatcherRule.dispatcher) {
            val updates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 1)
            val billingManager = mockk<BillingManager>()
            every { billingManager.purchaseUpdates } returns updates
            coEvery { billingManager.queryProductDetails(any()) } returns emptyList()
            coEvery { billingManager.connect() } returns true
            coEvery { billingManager.queryPurchases(BillingClient.ProductType.SUBS) } returns emptyList()
            coEvery { billingManager.consumePurchase(any()) } returns true
            coEvery { billingManager.acknowledgePurchase(any()) } returns true

            val purchase = mockk<Purchase>()
            every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { purchase.purchaseToken } returns "restore-token"
            every { purchase.products } returns listOf("prod-1")
            every { purchase.orderId } returns "order-restore"
            every { purchase.purchaseTime } returns 1234L
            every { purchase.originalJson } returns "{\"productId\":\"prod-1\"}"
            every { purchase.signature } returns "sig"
            every { purchase.isAcknowledged } returns false
            coEvery { billingManager.queryPurchases(BillingClient.ProductType.INAPP) } returns listOf(purchase)

            val repo = FakeIapRepository(kind = "credits")
            val viewModel = createViewModel(repo, billingManager)

            viewModel.load()
            advanceUntilIdle()

            viewModel.restorePurchases()
            advanceUntilIdle()

            assertEquals(1, repo.restoreRequests.size)
            assertEquals("android", repo.restoreRequests.first().platform)
            assertEquals("Sandbox", repo.restoreRequests.first().environment)
            assertEquals(1, repo.restoreRequests.first().transactions.size)
            coVerify { billingManager.consumePurchase("restore-token") }
        }

    @Test
    fun `restore purchases handles empty history`() =
        runTest(mainDispatcherRule.dispatcher) {
            val updates = MutableSharedFlow<BillingPurchaseUpdate>(extraBufferCapacity = 1)
            val billingManager = mockk<BillingManager>()
            every { billingManager.purchaseUpdates } returns updates
            coEvery { billingManager.queryProductDetails(any()) } returns emptyList()
            coEvery { billingManager.connect() } returns true
            coEvery { billingManager.queryPurchases(BillingClient.ProductType.INAPP) } returns emptyList()
            coEvery { billingManager.queryPurchases(BillingClient.ProductType.SUBS) } returns emptyList()

            val repo = FakeIapRepository(kind = "credits")
            val viewModel = createViewModel(repo, billingManager)

            viewModel.load()
            advanceUntilIdle()

            viewModel.restorePurchases()
            advanceUntilIdle()

            assertEquals("No purchases to restore.", viewModel.uiState.value.error?.message)
        }
}
