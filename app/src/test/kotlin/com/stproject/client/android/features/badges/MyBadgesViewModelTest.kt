package com.stproject.client.android.features.badges

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.FanBadge
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.FanBadgeListResult
import com.stproject.client.android.domain.repository.FanBadgePurchaseResult
import com.stproject.client.android.domain.repository.FanBadgeRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyBadgesViewModelTest : BaseUnitTest() {
    private class FakeAccessManager(gate: ContentGate) : ContentAccessManager {
        private val state = MutableStateFlow(gate)
        override val gate: StateFlow<ContentGate> = state

        override fun updateGate(gate: ContentGate) {
            state.value = gate
        }

        override fun decideAccess(isNsfw: Boolean?): ContentAccessDecision {
            return state.value.decideAccess(isNsfw)
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

    private class FakeFanBadgeRepository(
        private val items: List<FanBadge>,
    ) : FanBadgeRepository {
        override suspend fun listCreatorBadges(
            creatorId: String,
            pageNum: Int,
            pageSize: Int,
        ): FanBadgeListResult {
            return FanBadgeListResult(
                items = items,
                total = items.size,
                hasMore = false,
                pageNum = pageNum,
                pageSize = pageSize,
            )
        }

        override suspend fun listPurchasedBadges(): FanBadgeListResult {
            return FanBadgeListResult(
                items = items,
                total = items.size,
                hasMore = false,
                pageNum = 1,
                pageSize = items.size,
            )
        }

        override suspend fun purchaseBadge(badgeId: String): FanBadgePurchaseResult {
            return FanBadgePurchaseResult(ok = true, userBadgeId = null)
        }

        override suspend fun equipBadge(
            badgeId: String,
            equip: Boolean,
        ) = Unit
    }

    private fun buildBadge(
        id: String,
        owned: Boolean,
    ): FanBadge {
        return FanBadge(
            id = id,
            userBadgeId = if (owned) "ub-$id" else null,
            creatorId = "creator-1",
            creatorName = "Creator",
            name = "Badge $id",
            description = "desc",
            imageUrl = null,
            priceDiamonds = 100,
            level = 1,
            maxLevel = 5,
            owned = owned,
            equipped = false,
            experience = 0,
            experienceToNextLevel = 100,
            isMaxLevel = false,
        )
    }

    @Test
    fun `load blocked sets error and clears items`() =
        runTest(mainDispatcherRule.dispatcher) {
            val gate =
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = false,
                    allowNsfwPreference = false,
                )
            val resolveContentAccess =
                ResolveContentAccessUseCase(
                    accessManager = FakeAccessManager(gate),
                    characterRepository = FakeCharacterRepository(),
                )
            val repo = FakeFanBadgeRepository(listOf(buildBadge("1", owned = true)))
            val viewModel = MyBadgesViewModel(repo, resolveContentAccess)

            viewModel.load()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("age verification required", state.error)
            assertEquals(emptyList<FanBadge>(), state.items)
        }

    @Test
    fun `load allowed returns badges`() =
        runTest(mainDispatcherRule.dispatcher) {
            val gate =
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = false,
                )
            val resolveContentAccess =
                ResolveContentAccessUseCase(
                    accessManager = FakeAccessManager(gate),
                    characterRepository = FakeCharacterRepository(),
                )
            val badge = buildBadge("1", owned = true)
            val repo = FakeFanBadgeRepository(listOf(badge))
            val viewModel = MyBadgesViewModel(repo, resolveContentAccess)

            viewModel.load()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(badge), state.items)
        }
}
