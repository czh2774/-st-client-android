package com.stproject.client.android.features.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stproject.client.android.R
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.DecorationItem
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.DecorationRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DecorationsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun filtersByTypeAndEquipsItem() {
        val repo =
            FakeDecorationRepository(
                items =
                    mutableListOf(
                        DecorationItem(
                            id = "d1",
                            name = "Frame 1",
                            description = "desc",
                            type = "avatar_frame",
                            imageUrl = null,
                            priceCredits = 100,
                            owned = true,
                            equipped = false,
                        ),
                        DecorationItem(
                            id = "d2",
                            name = "Bubble 1",
                            description = null,
                            type = "bubble",
                            imageUrl = null,
                            priceCredits = null,
                            owned = true,
                            equipped = false,
                        ),
                    ),
            )
        val viewModel = DecorationsViewModel(repo, allowAllUseCase())
        val bubbleLabel = composeRule.activity.getString(R.string.decoration_type_bubble)
        val equipLabel = composeRule.activity.getString(R.string.decorations_equip)
        val equippedLabel = composeRule.activity.getString(R.string.decorations_equipped)
        val unequipLabel = composeRule.activity.getString(R.string.decorations_unequip)

        composeRule.setContent {
            DecorationsScreen(
                viewModel = viewModel,
                onBack = {},
                onOpenShop = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Frame 1").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithText("Bubble 1").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithText(bubbleLabel).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Bubble 1").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(equipLabel).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(equippedLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(unequipLabel).assertIsDisplayed()
    }

    @Test
    fun equipTogglesEquippedState() {
        val repo =
            FakeDecorationRepository(
                items =
                    mutableListOf(
                        DecorationItem(
                            id = "d1",
                            name = "Frame 1",
                            description = "desc",
                            type = "avatar_frame",
                            imageUrl = null,
                            priceCredits = 100,
                            owned = true,
                            equipped = false,
                        ),
                    ),
            )
        val viewModel = DecorationsViewModel(repo, allowAllUseCase())
        val equipLabel = composeRule.activity.getString(R.string.decorations_equip)
        val unequipLabel = composeRule.activity.getString(R.string.decorations_unequip)
        val equippedLabel = composeRule.activity.getString(R.string.decorations_equipped)

        composeRule.setContent {
            DecorationsScreen(
                viewModel = viewModel,
                onBack = {},
                onOpenShop = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Frame 1").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithText(equippedLabel).fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText(equipLabel).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(equippedLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(unequipLabel).assertIsDisplayed()

        composeRule.onNodeWithText(unequipLabel).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(equipLabel).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun typeSwitchShowsPriceAndDescription() {
        val repo =
            FakeDecorationRepository(
                items =
                    mutableListOf(
                        DecorationItem(
                            id = "d1",
                            name = "Frame 1",
                            description = "desc",
                            type = "avatar_frame",
                            imageUrl = null,
                            priceCredits = 100,
                            owned = true,
                            equipped = false,
                        ),
                        DecorationItem(
                            id = "d2",
                            name = "Bubble 1",
                            description = null,
                            type = "bubble",
                            imageUrl = null,
                            priceCredits = null,
                            owned = true,
                            equipped = false,
                        ),
                    ),
            )
        val viewModel = DecorationsViewModel(repo, allowAllUseCase())
        val bubbleLabel = composeRule.activity.getString(R.string.decoration_type_bubble)
        val priceLabel = composeRule.activity.getString(R.string.decorations_price_credits, 100)

        composeRule.setContent {
            DecorationsScreen(
                viewModel = viewModel,
                onBack = {},
                onOpenShop = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Frame 1").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("desc").assertIsDisplayed()
        composeRule.onNodeWithText(priceLabel).assertIsDisplayed()

        composeRule.onNodeWithText(bubbleLabel).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Bubble 1").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithText("Frame 1").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Bubble 1").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("desc").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText(priceLabel).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun emptyTypeShowsShopEntry() {
        val repo =
            FakeDecorationRepository(items = mutableListOf())
        val viewModel = DecorationsViewModel(repo, allowAllUseCase())
        val emptyLabel = composeRule.activity.getString(R.string.decorations_empty)
        val shopLabel = composeRule.activity.getString(R.string.shop_title)
        var shopClicks = 0

        composeRule.setContent {
            DecorationsScreen(
                viewModel = viewModel,
                onBack = {},
                onOpenShop = { shopClicks += 1 },
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(emptyLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(shopLabel).performClick()
        assertEquals(1, shopClicks)
    }

    @Test
    fun notOwnedDisablesEquip() {
        val repo =
            FakeDecorationRepository(
                items =
                    mutableListOf(
                        DecorationItem(
                            id = "d1",
                            name = "Frame 1",
                            description = null,
                            type = "avatar_frame",
                            imageUrl = null,
                            priceCredits = null,
                            owned = false,
                            equipped = false,
                        ),
                    ),
            )
        val viewModel = DecorationsViewModel(repo, allowAllUseCase())
        val equipLabel = composeRule.activity.getString(R.string.decorations_equip)
        val notOwnedLabel = composeRule.activity.getString(R.string.decorations_not_owned)

        composeRule.setContent {
            DecorationsScreen(
                viewModel = viewModel,
                onBack = {},
                onOpenShop = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Frame 1").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(notOwnedLabel).assertIsDisplayed()
        composeRule.onNodeWithText(equipLabel).assertIsNotEnabled()
    }

    private fun allowAllUseCase(): ResolveContentAccessUseCase {
        return ResolveContentAccessUseCase(
            accessManager = AllowAllAccessManager(),
            characterRepository = FakeCharacterRepository(),
        )
    }

    private class FakeDecorationRepository(
        private val items: MutableList<DecorationItem>,
    ) : DecorationRepository {
        override suspend fun listDecorations(): List<DecorationItem> = items.toList()

        override suspend fun setDecorationEquipped(
            decorationId: String,
            equip: Boolean,
        ) {
            val index = items.indexOfFirst { it.id == decorationId }
            if (index >= 0) {
                val current = items[index]
                items[index] = current.copy(equipped = equip)
            }
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
            return CharacterFollowResult(totalFollowers = 0, isFollowed = value)
        }
    }

    private class AllowAllAccessManager : ContentAccessManager {
        override val gate: StateFlow<ContentGate> =
            MutableStateFlow(
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = true,
                ),
            )

        override fun updateGate(gate: ContentGate) = Unit

        override fun decideAccess(isNsfw: Boolean?): ContentAccessDecision {
            return ContentAccessDecision.Allowed
        }
    }
}
