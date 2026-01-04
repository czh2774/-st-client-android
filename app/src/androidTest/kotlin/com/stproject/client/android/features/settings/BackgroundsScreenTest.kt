package com.stproject.client.android.features.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stproject.client.android.R
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.domain.model.BackgroundConfig
import com.stproject.client.android.domain.model.BackgroundItem
import com.stproject.client.android.domain.model.BackgroundList
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.BackgroundRepository
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgroundsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsEmptyStateAndConfig() {
        val repo =
            FakeBackgroundRepository().apply {
                config = BackgroundConfig(width = 120, height = 90)
            }
        val viewModel = BackgroundsViewModel(repo, allowAllUseCase())
        val emptyText = composeRule.activity.getString(R.string.backgrounds_empty)
        val sizeHint = composeRule.activity.getString(R.string.backgrounds_size_hint, 120, 90)

        composeRule.setContent {
            BackgroundsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(emptyText).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(emptyText).assertIsDisplayed()
        composeRule.onNodeWithText(sizeHint).assertIsDisplayed()
    }

    @Test
    fun renameAndDeleteUpdatesList() {
        val repo =
            FakeBackgroundRepository().apply {
                items.add(BackgroundItem(name = "bg-one", url = "https://example.com/bg-one"))
            }
        val viewModel = BackgroundsViewModel(repo, allowAllUseCase())
        val editLabel = composeRule.activity.getString(R.string.common_edit)
        val confirmLabel = composeRule.activity.getString(R.string.common_confirm)
        val deleteLabel = composeRule.activity.getString(R.string.common_delete)
        val emptyText = composeRule.activity.getString(R.string.backgrounds_empty)

        composeRule.setContent {
            BackgroundsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("bg-one").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(editLabel).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("bg-two")
        composeRule.onNodeWithText(confirmLabel).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("bg-two").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithText("bg-one").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithText(deleteLabel).performClick()
        composeRule.onNodeWithText(confirmLabel).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(emptyText).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(emptyText).assertIsDisplayed()
    }

    @Test
    fun listOrderIsStable() {
        val repo =
            FakeBackgroundRepository().apply {
                items.add(BackgroundItem(name = "bg-two", url = "https://example.com/bg-two"))
                items.add(BackgroundItem(name = "bg-one", url = "https://example.com/bg-one"))
                items.add(BackgroundItem(name = "bg-three", url = "https://example.com/bg-three"))
            }
        val viewModel = BackgroundsViewModel(repo, allowAllUseCase())
        val order = listOf("bg-two", "bg-one", "bg-three")

        composeRule.setContent {
            BackgroundsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("bg-three").fetchSemanticsNodes().isNotEmpty()
        }

        val tops =
            order.map { name ->
                composeRule.onNodeWithTag("backgrounds.item.$name").fetchSemanticsNode().boundsInRoot.top
            }
        assertTrue(tops.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun deleteConfirmRefreshesList() {
        val repo =
            FakeBackgroundRepository().apply {
                items.add(BackgroundItem(name = "bg-one", url = "https://example.com/bg-one"))
                items.add(BackgroundItem(name = "bg-two", url = "https://example.com/bg-two"))
                items.add(BackgroundItem(name = "bg-three", url = "https://example.com/bg-three"))
            }
        val viewModel = BackgroundsViewModel(repo, allowAllUseCase())
        val deleteLabel = composeRule.activity.getString(R.string.common_delete)
        val confirmLabel = composeRule.activity.getString(R.string.common_confirm)

        composeRule.setContent {
            BackgroundsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("bg-three").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText(deleteLabel)[1].performClick()
        composeRule.onNodeWithText(confirmLabel).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("bg-two").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("bg-one").assertIsDisplayed()
        composeRule.onNodeWithText("bg-three").assertIsDisplayed()
        assertEquals(1, repo.deleteCalls)
        assertEquals(2, composeRule.onAllNodesWithText(deleteLabel).fetchSemanticsNodes().size)
    }

    @Test
    fun renameKeepsListOrder() {
        val repo =
            FakeBackgroundRepository().apply {
                items.add(BackgroundItem(name = "bg-one", url = "https://example.com/bg-one"))
                items.add(BackgroundItem(name = "bg-two", url = "https://example.com/bg-two"))
                items.add(BackgroundItem(name = "bg-three", url = "https://example.com/bg-three"))
            }
        val viewModel = BackgroundsViewModel(repo, allowAllUseCase())
        val editLabel = composeRule.activity.getString(R.string.common_edit)
        val confirmLabel = composeRule.activity.getString(R.string.common_confirm)

        composeRule.setContent {
            BackgroundsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("bg-three").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText(editLabel)[1].performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("bg-two-new")
        composeRule.onNodeWithText(confirmLabel).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("bg-two-new").fetchSemanticsNodes().isNotEmpty()
        }
        val order = listOf("bg-one", "bg-two-new", "bg-three")
        val tops =
            order.map { name ->
                composeRule.onNodeWithTag("backgrounds.item.$name").fetchSemanticsNode().boundsInRoot.top
            }
        assertTrue(tops.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun renameCancelKeepsName() {
        val repo =
            FakeBackgroundRepository().apply {
                items.add(BackgroundItem(name = "bg-one", url = "https://example.com/bg-one"))
            }
        val viewModel = BackgroundsViewModel(repo, allowAllUseCase())
        val editLabel = composeRule.activity.getString(R.string.common_edit)
        val cancelLabel = composeRule.activity.getString(R.string.common_cancel)

        composeRule.setContent {
            BackgroundsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("bg-one").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(editLabel).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("bg-two")
        composeRule.onNodeWithText(cancelLabel).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("bg-one").assertIsDisplayed()
        assertEquals(0, repo.renameCalls)
    }

    @Test
    fun deleteCancelKeepsItem() {
        val repo =
            FakeBackgroundRepository().apply {
                items.add(BackgroundItem(name = "bg-one", url = "https://example.com/bg-one"))
            }
        val viewModel = BackgroundsViewModel(repo, allowAllUseCase())
        val deleteLabel = composeRule.activity.getString(R.string.common_delete)
        val cancelLabel = composeRule.activity.getString(R.string.common_cancel)

        composeRule.setContent {
            BackgroundsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("bg-one").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(deleteLabel).performClick()
        composeRule.onNodeWithText(cancelLabel).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("bg-one").assertIsDisplayed()
    }

    @Test
    fun copyLinkButtonVisible() {
        val repo =
            FakeBackgroundRepository().apply {
                items.add(BackgroundItem(name = "bg-one", url = "https://example.com/bg-one"))
            }
        val viewModel = BackgroundsViewModel(repo, allowAllUseCase())
        val copyLabel = composeRule.activity.getString(R.string.backgrounds_copy_link)

        composeRule.setContent {
            BackgroundsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(copyLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(copyLabel).assertIsDisplayed()
    }

    @Test
    fun copyLinkWritesClipboard() {
        val repo =
            FakeBackgroundRepository().apply {
                items.add(BackgroundItem(name = "bg-one", url = "https://example.com/bg-one"))
            }
        val viewModel = BackgroundsViewModel(repo, allowAllUseCase())
        val copyLabel = composeRule.activity.getString(R.string.backgrounds_copy_link)
        val clipboard = FakeClipboardManager()

        composeRule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                BackgroundsScreen(
                    viewModel = viewModel,
                    onBack = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(copyLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(copyLabel).performClick()

        composeRule.waitForIdle()
        assertEquals("https://example.com/bg-one", clipboard.storedText?.text)
    }

    @Test
    fun renameIgnoresEmptyOrSameName() {
        val repo =
            FakeBackgroundRepository().apply {
                items.add(BackgroundItem(name = "bg-one", url = "https://example.com/bg-one"))
            }
        val viewModel = BackgroundsViewModel(repo, allowAllUseCase())
        val editLabel = composeRule.activity.getString(R.string.common_edit)
        val confirmLabel = composeRule.activity.getString(R.string.common_confirm)

        composeRule.setContent {
            BackgroundsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("bg-one").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(editLabel).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("bg-one")
        composeRule.onNodeWithText(confirmLabel).performClick()
        composeRule.waitForIdle()
        assertEquals(0, repo.renameCalls)

        composeRule.onNodeWithText(editLabel).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNodeWithText(confirmLabel).performClick()
        composeRule.waitForIdle()
        assertEquals(0, repo.renameCalls)
    }

    private fun allowAllUseCase(): ResolveContentAccessUseCase {
        return ResolveContentAccessUseCase(
            accessManager = AllowAllAccessManager(),
            characterRepository = FakeCharacterRepository(),
        )
    }

    private class FakeBackgroundRepository : BackgroundRepository {
        val items = mutableListOf<BackgroundItem>()
        var config: BackgroundConfig? = null
        var renameCalls = 0
        var deleteCalls = 0

        override suspend fun listBackgrounds(): BackgroundList {
            return BackgroundList(items = items.toList(), config = config)
        }

        override suspend fun uploadBackground(
            fileName: String,
            bytes: ByteArray,
        ): String {
            val name = fileName.trim().ifEmpty { "background.png" }
            items.add(BackgroundItem(name = name, url = "https://example.com/$name"))
            return name
        }

        override suspend fun renameBackground(
            oldName: String,
            newName: String,
        ) {
            renameCalls += 1
            val index = items.indexOfFirst { it.name == oldName }
            if (index >= 0) {
                items[index] = BackgroundItem(name = newName, url = "https://example.com/$newName")
            }
        }

        override suspend fun deleteBackground(name: String) {
            deleteCalls += 1
            items.removeAll { it.name == name }
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

    private class FakeClipboardManager : ClipboardManager {
        var storedText: AnnotatedString? = null
            private set

        override fun setText(annotatedString: AnnotatedString) {
            storedText = annotatedString
        }

        override fun getText(): AnnotatedString? = storedText

        override fun hasText(): Boolean = storedText != null
    }
}
