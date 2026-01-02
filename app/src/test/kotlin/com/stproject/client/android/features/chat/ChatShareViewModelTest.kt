package com.stproject.client.android.features.chat

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatShareViewModelTest : BaseUnitTest() {
    private class FakeCharacterRepository(
        private val resolvedMemberId: String? = "char-1",
    ) : CharacterRepository {
        override suspend fun queryCharacters(
            cursor: String?,
            limit: Int?,
            sortBy: String?,
            isNsfw: Boolean?,
        ) = emptyList<CharacterSummary>()

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

        override suspend fun resolveShareCode(shareCode: String): String? = resolvedMemberId

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

    private class FakeAccessManager : ContentAccessManager {
        private val _gate =
            MutableStateFlow(
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = false,
                ),
            )

        override val gate: StateFlow<ContentGate> = _gate

        override fun updateGate(gate: ContentGate) {
            _gate.value = gate
        }

        override fun decideAccess(isNsfw: Boolean?): ContentAccessDecision {
            return _gate.value.decideAccess(isNsfw)
        }
    }

    private class AllowAccessUseCase(
        accessManager: ContentAccessManager,
        characterRepository: CharacterRepository,
    ) : ResolveContentAccessUseCase(
            accessManager = accessManager,
            characterRepository = characterRepository,
        ) {
        override suspend fun execute(
            memberId: String?,
            isNsfwHint: Boolean?,
            ageRatingHint: com.stproject.client.android.domain.model.AgeRating?,
        ): ContentAccessDecision {
            return ContentAccessDecision.Allowed
        }
    }

    private class DenyAccessUseCase(
        accessManager: ContentAccessManager,
        characterRepository: CharacterRepository,
    ) : ResolveContentAccessUseCase(
            accessManager = accessManager,
            characterRepository = characterRepository,
        ) {
        override suspend fun execute(
            memberId: String?,
            isNsfwHint: Boolean?,
            ageRatingHint: com.stproject.client.android.domain.model.AgeRating?,
        ): ContentAccessDecision {
            return ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED)
        }
    }

    @Test
    fun resolveShareCodeBlockedSetsError() =
        runTest {
            val repo = FakeCharacterRepository()
            val viewModel =
                ChatShareViewModel(
                    characterRepository = repo,
                    resolveContentAccess =
                        DenyAccessUseCase(
                            accessManager = FakeAccessManager(),
                            characterRepository = repo,
                        ),
                )

            viewModel.resolveShareCode("code-123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("mature content disabled", state.error)
            assertNull(state.resolvedMemberId)
        }

    @Test
    fun resolveShareCodeAllowedSetsMemberId() =
        runTest {
            val repo = FakeCharacterRepository()
            val viewModel =
                ChatShareViewModel(
                    characterRepository = repo,
                    resolveContentAccess =
                        AllowAccessUseCase(
                            accessManager = FakeAccessManager(),
                            characterRepository = repo,
                        ),
                )

            viewModel.resolveShareCode("code-123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("char-1", state.resolvedMemberId)
            assertNull(state.error)
        }
}
