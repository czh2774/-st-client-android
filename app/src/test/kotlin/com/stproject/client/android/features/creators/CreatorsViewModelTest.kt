package com.stproject.client.android.features.creators

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.CreatorCharactersResult
import com.stproject.client.android.domain.repository.CreatorListResult
import com.stproject.client.android.domain.repository.CreatorRepository
import com.stproject.client.android.domain.repository.SocialListResult
import com.stproject.client.android.domain.repository.SocialRepository
import com.stproject.client.android.domain.usecase.FollowUserUseCase
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
class CreatorsViewModelTest : BaseUnitTest() {
    private class FakeCreatorRepository : CreatorRepository {
        var listCalls = 0

        override suspend fun listCreators(
            limit: Int,
            cursor: String?,
            sortBy: String?,
            searchKeyword: String?,
        ): CreatorListResult {
            listCalls += 1
            return CreatorListResult(items = emptyList(), hasMore = false, nextCursor = null)
        }

        override suspend fun listCreatorCharacters(
            creatorId: String,
            pageNum: Int,
            pageSize: Int,
        ): CreatorCharactersResult {
            return CreatorCharactersResult(items = emptyList(), total = 0, hasMore = false)
        }
    }

    private class FakeSocialRepository : SocialRepository {
        override suspend fun followUser(
            userId: String,
            value: Boolean,
        ) = Unit

        override suspend fun blockUser(
            userId: String,
            value: Boolean,
        ) = Unit

        override suspend fun listFollowers(
            pageNum: Int,
            pageSize: Int,
            userId: String?,
        ): SocialListResult {
            return SocialListResult(items = emptyList(), total = 0, hasMore = false)
        }

        override suspend fun listFollowing(
            pageNum: Int,
            pageSize: Int,
            userId: String?,
        ): SocialListResult {
            return SocialListResult(items = emptyList(), total = 0, hasMore = false)
        }

        override suspend fun listBlocked(
            pageNum: Int,
            pageSize: Int,
        ): SocialListResult {
            return SocialListResult(items = emptyList(), total = 0, hasMore = false)
        }
    }

    private class DenyAccessUseCase :
        ResolveContentAccessUseCase(
            accessManager = ChatViewModelTest.AllowAllAccessManager(),
            characterRepository =
                object : CharacterRepository {
                    override suspend fun queryCharacters(
                        cursor: String?,
                        limit: Int?,
                        sortBy: String?,
                        isNsfw: Boolean?,
                    ): List<CharacterSummary> = emptyList()

                    override suspend fun getCharacterDetail(characterId: String): Nothing {
                        throw IllegalStateException("unused")
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
                    ): CharacterFollowResult =
                        CharacterFollowResult(
                            totalFollowers = 0,
                            isFollowed = false,
                        )
                },
        ) {
        override suspend fun execute(
            memberId: String?,
            isNsfwHint: Boolean?,
        ): ContentAccessDecision {
            return ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED)
        }
    }

    @Test
    fun `load sets error when access denied`() =
        runTest(mainDispatcherRule.dispatcher) {
            val creatorRepository = FakeCreatorRepository()
            val socialRepository = FakeSocialRepository()
            val vm =
                CreatorsViewModel(
                    creatorRepository = creatorRepository,
                    socialRepository = socialRepository,
                    resolveContentAccess = DenyAccessUseCase(),
                    followUserUseCase =
                        FollowUserUseCase(
                            socialRepository = socialRepository,
                            resolveContentAccess =
                                ResolveContentAccessUseCase(
                                    accessManager = ChatViewModelTest.AllowAllAccessManager(),
                                    characterRepository =
                                        object : CharacterRepository {
                                            override suspend fun queryCharacters(
                                                cursor: String?,
                                                limit: Int?,
                                                sortBy: String?,
                                                isNsfw: Boolean?,
                                            ): List<CharacterSummary> = emptyList()

                                            override suspend fun getCharacterDetail(characterId: String): Nothing {
                                                throw IllegalStateException("unused")
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
                                            ): CharacterFollowResult =
                                                CharacterFollowResult(
                                                    totalFollowers = 0,
                                                    isFollowed = false,
                                                )
                                        },
                                ),
                        ),
                )
            val collectJob = backgroundScope.launch { vm.uiState.collect() }

            vm.load()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.error?.contains("mature") == true)
            assertEquals(0, creatorRepository.listCalls)
            collectJob.cancel()
        }
}
