package com.stproject.client.android.features.notifications

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.ContentSummary
import com.stproject.client.android.domain.model.NotificationItem
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.NotificationListResult
import com.stproject.client.android.domain.repository.NotificationRepository
import com.stproject.client.android.domain.repository.UnreadCounts
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest : BaseUnitTest() {
    private class AllowAllAccessManager : ContentAccessManager {
        override val gate: StateFlow<ContentGate> =
            MutableStateFlow(
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = false,
                ),
            )

        override fun updateGate(gate: ContentGate) = Unit

        override fun decideAccess(isNsfw: Boolean?): ContentAccessDecision {
            return ContentAccessDecision.Allowed
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

    private class GateOnMetadataUseCase :
        ResolveContentAccessUseCase(
            accessManager = AllowAllAccessManager(),
            characterRepository = FakeCharacterRepository(),
        ) {
        override suspend fun execute(
            memberId: String?,
            isNsfwHint: Boolean?,
            ageRatingHint: com.stproject.client.android.domain.model.AgeRating?,
            tags: List<String>?,
            requireMetadata: Boolean,
        ): ContentAccessDecision {
            return if (isNsfwHint == false) {
                ContentAccessDecision.Allowed
            } else {
                ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED)
            }
        }
    }

    private class FakeNotificationRepository(
        private val items: List<NotificationItem>,
    ) : NotificationRepository {
        override suspend fun listNotifications(
            pageNum: Int,
            pageSize: Int,
        ): NotificationListResult {
            return NotificationListResult(
                items = items,
                total = items.size,
                hasMore = false,
            )
        }

        override suspend fun markAsRead(
            ids: List<String>,
            markAll: Boolean,
        ) = Unit

        override suspend fun getUnreadCounts(): UnreadCounts {
            return UnreadCounts(system = 1, follow = 0, like = 0, comment = 0, total = 1)
        }
    }

    @Test
    fun `load filters notifications without metadata but keeps system`() =
        runTest(mainDispatcherRule.dispatcher) {
            val system =
                NotificationItem(
                    id = "n-system",
                    userId = "user-1",
                    type = "system",
                    title = "System",
                    content = "System notice",
                    contentMeta = null,
                    isRead = false,
                    createdAt = null,
                )
            val missingMeta =
                NotificationItem(
                    id = "n-missing",
                    userId = "user-1",
                    type = "like",
                    title = "Like",
                    content = "Someone liked you",
                    contentMeta = null,
                    isRead = false,
                    createdAt = null,
                )
            val safeMeta =
                NotificationItem(
                    id = "n-safe",
                    userId = "user-1",
                    type = "like",
                    title = "Like",
                    content = "Someone liked you",
                    contentMeta =
                        ContentSummary(
                            characterId = "char-1",
                            isNsfw = false,
                            moderationAgeRating = null,
                            tags = emptyList(),
                            visibility = "public",
                        ),
                    isRead = false,
                    createdAt = null,
                )

            val vm =
                NotificationsViewModel(
                    notificationRepository = FakeNotificationRepository(listOf(system, missingMeta, safeMeta)),
                    resolveContentAccess = GateOnMetadataUseCase(),
                )

            vm.load()
            advanceUntilIdle()

            val ids = vm.uiState.value.items.map { it.id }
            assertEquals(listOf("n-system", "n-safe"), ids)
            assertTrue("n-missing" !in ids)
        }
}
