package com.stproject.client.android.features.comments

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.Comment
import com.stproject.client.android.domain.model.CommentLikeResult
import com.stproject.client.android.domain.model.CommentListResult
import com.stproject.client.android.domain.model.CommentSort
import com.stproject.client.android.domain.model.CommentUser
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.model.UserConfig
import com.stproject.client.android.domain.model.UserConfigUpdate
import com.stproject.client.android.domain.model.UserProfile
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.CommentRepository
import com.stproject.client.android.domain.repository.UserRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommentsViewModelTest : BaseUnitTest() {
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

    private class FakeCommentRepository : CommentRepository {
        var createCalls = 0
        private var created: Comment? = null
        private val baseComment =
            Comment(
                id = "c-1",
                characterId = "char-1",
                userId = "user-2",
                content = "hello",
                likesCount = 0,
                isLiked = false,
                createdAt = "2025-01-01T00:00:00Z",
                parentId = null,
                user = CommentUser(id = "user-2", username = "User2", avatarUrl = null),
                replies = emptyList(),
            )

        override suspend fun listComments(
            characterId: String,
            sort: CommentSort,
            pageNum: Int,
            pageSize: Int,
        ): CommentListResult {
            val items = listOfNotNull(created ?: baseComment)
            return CommentListResult(items = items, total = items.size, hasMore = false)
        }

        override suspend fun createComment(
            characterId: String,
            content: String,
            parentId: String?,
        ): Comment {
            createCalls += 1
            val comment =
                Comment(
                    id = "c-new",
                    characterId = characterId,
                    userId = "user-1",
                    content = content,
                    likesCount = 0,
                    isLiked = false,
                    createdAt = "2025-01-02T00:00:00Z",
                    parentId = parentId,
                    user = CommentUser(id = "user-1", username = "User1", avatarUrl = null),
                    replies = emptyList(),
                )
            created = comment
            return comment
        }

        override suspend fun deleteComment(commentId: String) = Unit

        override suspend fun likeComment(
            commentId: String,
            value: Boolean,
        ): CommentLikeResult {
            return CommentLikeResult(likesCount = 1, isLiked = value)
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
                name = "Character",
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

    private class FakeUserRepository : UserRepository {
        override suspend fun getMe(): UserProfile {
            return UserProfile(id = "user-1", email = null, tosVersion = null, tosAcceptedAt = null)
        }

        override suspend fun acceptTos(version: String?): UserProfile = getMe()

        override suspend fun getUserConfig(): UserConfig {
            return UserConfig(
                blockedTags = emptyList(),
                ageVerified = true,
                birthDate = null,
            )
        }

        override suspend fun updateUserConfig(update: UserConfigUpdate): UserConfig = getUserConfig()

        override suspend fun deleteMe() = Unit
    }

    @Test
    fun `load populates comments`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm =
                CommentsViewModel(
                    commentRepository = FakeCommentRepository(),
                    characterRepository = FakeCharacterRepository(),
                    userRepository = FakeUserRepository(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )

            vm.load("char-1")
            advanceUntilIdle()

            assertEquals("char-1", vm.uiState.value.characterId)
            assertEquals(1, vm.uiState.value.items.size)
            assertEquals("user-1", vm.uiState.value.currentUserId)
        }

    @Test
    fun `submit comment clears input and reloads`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeCommentRepository()
            val vm =
                CommentsViewModel(
                    commentRepository = repo,
                    characterRepository = FakeCharacterRepository(),
                    userRepository = FakeUserRepository(),
                    resolveContentAccess =
                        ResolveContentAccessUseCase(
                            accessManager = AllowAllAccessManager(),
                            characterRepository = FakeCharacterRepository(),
                        ),
                )

            vm.load("char-1")
            advanceUntilIdle()

            vm.onInputChanged("new comment")
            vm.submitComment()
            advanceUntilIdle()

            assertEquals(1, repo.createCalls)
            assertEquals("", vm.uiState.value.input)
            assertEquals(1, vm.uiState.value.items.size)
        }
}
