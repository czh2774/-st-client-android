package com.stproject.client.android.features.creators

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.CreatorAssistantChatResult
import com.stproject.client.android.domain.model.CreatorAssistantDraft
import com.stproject.client.android.domain.model.CreatorAssistantDraftResult
import com.stproject.client.android.domain.model.CreatorAssistantPublishResult
import com.stproject.client.android.domain.model.CreatorAssistantSessionHistory
import com.stproject.client.android.domain.model.CreatorAssistantSessionSummary
import com.stproject.client.android.domain.model.CreatorAssistantSubCharacter
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.CreatorAssistantRepository
import com.stproject.client.android.domain.repository.CreatorAssistantSessionsResult
import com.stproject.client.android.domain.repository.CreatorAssistantStartResult
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreatorAssistantListScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeCreatorAssistantRepository : CreatorAssistantRepository {
        override suspend fun listSessions(
            pageNum: Int,
            pageSize: Int,
            status: String?,
        ): CreatorAssistantSessionsResult {
            return CreatorAssistantSessionsResult(
                items =
                    listOf(
                        CreatorAssistantSessionSummary(
                            sessionId = "session-1",
                            characterType = null,
                            status = null,
                            draftName = null,
                            messageCount = 0,
                            createdAt = null,
                            updatedAt = null,
                        ),
                    ),
                total = 1,
                hasMore = false,
            )
        }

        override suspend fun startSession(
            characterType: String?,
            parentCharacterId: String?,
            initialPrompt: String?,
        ): CreatorAssistantStartResult {
            return CreatorAssistantStartResult(
                sessionId = "session-1",
                characterType = null,
                greeting = null,
                suggestions = emptyList(),
            )
        }

        override suspend fun getSessionHistory(sessionId: String): CreatorAssistantSessionHistory {
            return CreatorAssistantSessionHistory(
                sessionId = sessionId,
                characterType = null,
                status = null,
                messages = emptyList(),
                currentDraft = null,
                createdAt = null,
                updatedAt = null,
            )
        }

        override suspend fun chat(
            sessionId: String,
            content: String,
        ): CreatorAssistantChatResult {
            return CreatorAssistantChatResult(
                messageId = "m1",
                content = "ok",
                suggestions = emptyList(),
                draftReady = false,
            )
        }

        override suspend fun generateDraft(sessionId: String): CreatorAssistantDraftResult {
            return CreatorAssistantDraftResult(
                draftId = "d1",
                draft = buildDraft(),
                confidence = 0.0,
                missingFields = emptyList(),
            )
        }

        override suspend fun updateDraft(
            sessionId: String,
            draftId: String,
            updates: Map<String, Any>,
        ): CreatorAssistantDraftResult {
            return CreatorAssistantDraftResult(
                draftId = draftId,
                draft = buildDraft(),
                confidence = 0.0,
                missingFields = emptyList(),
            )
        }

        override suspend fun publish(
            sessionId: String,
            draftId: String,
            isPublic: Boolean,
        ): CreatorAssistantPublishResult {
            return CreatorAssistantPublishResult(
                characterId = "c1",
                name = null,
                avatarUrl = null,
                backgroundUrl = null,
                shareCode = null,
            )
        }

        override suspend fun abandon(sessionId: String): Boolean = true

        private fun buildDraft(): CreatorAssistantDraft {
            return CreatorAssistantDraft(
                name = null,
                description = null,
                greeting = null,
                personality = null,
                scenario = null,
                exampleDialogs = null,
                tags = emptyList(),
                gender = 0,
                isNsfw = false,
                characterType = null,
                parentCharacterId = null,
                subCharacters =
                    listOf(
                        CreatorAssistantSubCharacter(
                            name = null,
                            description = null,
                            personality = null,
                            avatarBase64 = null,
                        ),
                    ),
            )
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

    private class ToggleAccessUseCase(
        accessManager: ContentAccessManager,
        characterRepository: CharacterRepository,
    ) : ResolveContentAccessUseCase(
            accessManager = accessManager,
            characterRepository = characterRepository,
        ) {
        private var calls = 0

        override suspend fun execute(
            memberId: String?,
            isNsfwHint: Boolean?,
        ): ContentAccessDecision {
            calls += 1
            return if (calls == 1) {
                ContentAccessDecision.Allowed
            } else {
                ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED)
            }
        }
    }

    @Test
    fun openSessionBlockedShowsErrorAndNoNavigate() {
        val viewModel =
            CreatorAssistantListViewModel(
                repository = FakeCreatorAssistantRepository(),
                resolveContentAccess =
                    ToggleAccessUseCase(
                        accessManager = AllowAllAccessManager(),
                        characterRepository = FakeCharacterRepository(),
                    ),
            )
        var openedSessionId: String? = null

        composeRule.setContent {
            CreatorAssistantListScreen(
                viewModel = viewModel,
                onBack = {},
                onOpenSession = { openedSessionId = it },
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Open").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Open").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("mature content disabled").fetchSemanticsNodes().isNotEmpty()
        }

        assertEquals(null, openedSessionId)
    }
}
