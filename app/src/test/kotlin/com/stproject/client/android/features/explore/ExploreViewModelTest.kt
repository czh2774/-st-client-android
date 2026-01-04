package com.stproject.client.android.features.explore

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.usecase.FollowCharacterUseCase
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import com.stproject.client.android.features.chat.ChatViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest : BaseUnitTest() {
    private open class FakeCharacterRepository : CharacterRepository {
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

        override suspend fun generateShareCode(characterId: String) = null

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

    private class CapturingCharacterRepository : FakeCharacterRepository() {
        var lastSort: String? = null
        var lastTags: List<String>? = null
        var lastSearch: String? = null

        override suspend fun queryCharactersFiltered(
            cursor: String?,
            limit: Int?,
            sortBy: String?,
            isNsfw: Boolean?,
            tags: List<String>?,
            searchKeyword: String?,
            gender: String?,
        ): List<CharacterSummary> {
            lastSort = sortBy
            lastTags = tags
            lastSearch = searchKeyword
            return emptyList()
        }
    }

    private class DenyAccessUseCase(
        accessManager: ChatViewModelTest.AllowAllAccessManager,
    ) : ResolveContentAccessUseCase(
            accessManager = accessManager,
            characterRepository = FakeCharacterRepository(),
        ) {
        override suspend fun execute(
            memberId: String?,
            isNsfwHint: Boolean?,
            ageRatingHint: com.stproject.client.android.domain.model.AgeRating?,
            tags: List<String>?,
            requireMetadata: Boolean,
        ): ContentAccessDecision {
            return ContentAccessDecision.Blocked(
                com.stproject.client.android.core.compliance.ContentBlockReason.NSFW_DISABLED,
            )
        }
    }

    @Test
    fun `load sets access error when blocked`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm =
                ExploreViewModel(
                    characterRepository = FakeCharacterRepository(),
                    resolveContentAccess = DenyAccessUseCase(ChatViewModelTest.AllowAllAccessManager()),
                    followCharacterUseCase =
                        FollowCharacterUseCase(
                            characterRepository = FakeCharacterRepository(),
                            resolveContentAccess =
                                ResolveContentAccessUseCase(
                                    accessManager = ChatViewModelTest.AllowAllAccessManager(),
                                    characterRepository = FakeCharacterRepository(),
                                ),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.uiState.collect() }

            vm.load(force = true)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.accessError?.contains("mature") == true)
            collectJob.cancel()
        }

    @Test
    fun `resolveShareCode sets access error when blocked`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                object : FakeCharacterRepository() {
                    override suspend fun resolveShareCode(shareCode: String): String? = "char-1"
                }
            val vm =
                ExploreViewModel(
                    characterRepository = repo,
                    resolveContentAccess = DenyAccessUseCase(ChatViewModelTest.AllowAllAccessManager()),
                    followCharacterUseCase =
                        FollowCharacterUseCase(
                            characterRepository = repo,
                            resolveContentAccess =
                                ResolveContentAccessUseCase(
                                    accessManager = ChatViewModelTest.AllowAllAccessManager(),
                                    characterRepository = repo,
                                ),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.uiState.collect() }

            vm.onShareCodeChanged("code")
            vm.resolveShareCode()
            advanceUntilIdle()

            assertEquals(true, vm.uiState.value.accessError?.isNotBlank())
            collectJob.cancel()
        }

    @Test
    fun `applyFilters sends sort and filters to repository`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = CapturingCharacterRepository()
            val vm =
                ExploreViewModel(
                    characterRepository = repo,
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = ChatViewModelTest.AllowAllAccessManager(),
                            characterRepository = repo,
                        ),
                    followCharacterUseCase =
                        FollowCharacterUseCase(
                            characterRepository = repo,
                            resolveContentAccess =
                                ResolveContentAccessUseCase(
                                    accessManager = ChatViewModelTest.AllowAllAccessManager(),
                                    characterRepository = repo,
                                ),
                        ),
                )

            vm.onSearchChanged("hero")
            vm.onTagsChanged("fantasy, romance, fantasy")
            vm.setSortBy("new")
            advanceUntilIdle()

            assertEquals("new", repo.lastSort)
            assertEquals(listOf("fantasy", "romance"), repo.lastTags)
            assertEquals("hero", repo.lastSearch)
        }
}
