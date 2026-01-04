package com.stproject.client.android.domain.usecase

import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveContentAccessUseCaseTest {
    private class FakeAccessManager(
        private var currentGate: ContentGate,
    ) : ContentAccessManager {
        private val state = MutableStateFlow(currentGate)
        override val gate: StateFlow<ContentGate> = state

        override fun updateGate(gate: ContentGate) {
            currentGate = gate
            state.value = gate
        }

        override fun decideAccess(isNsfw: Boolean?): ContentAccessDecision {
            return currentGate.decideAccess(isNsfw)
        }
    }

    private class FakeCharacterRepository(
        private val isNsfw: Boolean?,
        private val ageRating: AgeRating? = null,
        private val tags: List<String> = emptyList(),
    ) : CharacterRepository {
        var detailCalls = 0

        override suspend fun queryCharacters(
            cursor: String?,
            limit: Int?,
            sortBy: String?,
            isNsfw: Boolean?,
        ) = emptyList<CharacterSummary>()

        override suspend fun getCharacterDetail(characterId: String): CharacterDetail {
            detailCalls += 1
            return CharacterDetail(
                id = characterId,
                name = "Test",
                description = "",
                tags = tags,
                creatorName = null,
                isNsfw = isNsfw,
                moderationAgeRating = ageRating,
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

    @Test
    fun `resolves access for unknown nsfw by fetching detail`() =
        runTest {
            val gate =
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = false,
                )
            val repo = FakeCharacterRepository(isNsfw = false)
            val useCase =
                ResolveContentAccessUseCase(
                    accessManager = FakeAccessManager(gate),
                    characterRepository = repo,
                )
            val result = useCase.execute(memberId = "char-1", isNsfwHint = null)

            assertEquals(ContentAccessDecision.Allowed, result)
            assertEquals(1, repo.detailCalls)
        }

    @Test
    fun `blocks when resolved character is nsfw`() =
        runTest {
            val gate =
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = false,
                )
            val repo = FakeCharacterRepository(isNsfw = true)
            val useCase =
                ResolveContentAccessUseCase(
                    accessManager = FakeAccessManager(gate),
                    characterRepository = repo,
                )
            val result = useCase.execute(memberId = "char-1", isNsfwHint = null)

            assertEquals(
                ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED),
                result,
            )
            assertEquals(1, repo.detailCalls)
        }

    @Test
    fun `blocks when resolved character is adult rating`() =
        runTest {
            val gate =
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = false,
                )
            val repo = FakeCharacterRepository(isNsfw = false, ageRating = AgeRating.Age18)
            val useCase =
                ResolveContentAccessUseCase(
                    accessManager = FakeAccessManager(gate),
                    characterRepository = repo,
                )
            val result = useCase.execute(memberId = "char-1", isNsfwHint = null)

            assertEquals(
                ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED),
                result,
            )
            assertEquals(1, repo.detailCalls)
        }

    @Test
    fun `blocks when tags are blocked without fetching detail`() =
        runTest {
            val gate =
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = true,
                    blockedTags = listOf("spoilers"),
                )
            val repo = FakeCharacterRepository(isNsfw = false, tags = emptyList())
            val useCase =
                ResolveContentAccessUseCase(
                    accessManager = FakeAccessManager(gate),
                    characterRepository = repo,
                )
            val result = useCase.execute(memberId = "char-1", isNsfwHint = false, tags = listOf("Spoilers"))

            assertEquals(
                ContentAccessDecision.Blocked(ContentBlockReason.TAGS_BLOCKED),
                result,
            )
            assertEquals(0, repo.detailCalls)
        }

    @Test
    fun `blocks when blocked tags resolved from detail`() =
        runTest {
            val gate =
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = true,
                    blockedTags = listOf("gore"),
                )
            val repo = FakeCharacterRepository(isNsfw = false, tags = listOf("gore"))
            val useCase =
                ResolveContentAccessUseCase(
                    accessManager = FakeAccessManager(gate),
                    characterRepository = repo,
                )
            val result = useCase.execute(memberId = "char-1", isNsfwHint = false)

            assertEquals(
                ContentAccessDecision.Blocked(ContentBlockReason.TAGS_BLOCKED),
                result,
            )
            assertEquals(1, repo.detailCalls)
        }

    @Test
    fun `fails closed when blocked tags need resolution and detail fetch fails`() =
        runTest {
            val gate =
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = true,
                    blockedTags = listOf("violence"),
                )
            val repo =
                object : CharacterRepository {
                    override suspend fun queryCharacters(
                        cursor: String?,
                        limit: Int?,
                        sortBy: String?,
                        isNsfw: Boolean?,
                    ) = emptyList<CharacterSummary>()

                    override suspend fun getCharacterDetail(characterId: String): CharacterDetail {
                        throw IllegalStateException("boom")
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
            val useCase =
                ResolveContentAccessUseCase(
                    accessManager = FakeAccessManager(gate),
                    characterRepository = repo,
                )
            val result = useCase.execute(memberId = "char-1", isNsfwHint = false)

            assertEquals(
                ContentAccessDecision.Blocked(ContentBlockReason.TAGS_BLOCKED),
                result,
            )
        }
}
