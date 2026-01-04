package com.stproject.client.android.domain.usecase

import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.compliance.resolveNsfwHint
import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.domain.repository.CharacterRepository
import javax.inject.Inject

open class ResolveContentAccessUseCase
    @Inject
    constructor(
        private val accessManager: ContentAccessManager,
        private val characterRepository: CharacterRepository,
    ) {
        open suspend fun execute(
            memberId: String?,
            isNsfwHint: Boolean?,
            ageRatingHint: AgeRating? = null,
            tags: List<String>? = null,
            requireMetadata: Boolean = false,
        ): ContentAccessDecision {
            val gate = accessManager.gate.value
            if (gate.isTagBlocked(tags)) {
                return ContentAccessDecision.Blocked(ContentBlockReason.TAGS_BLOCKED)
            }
            val resolvedHint = resolveNsfwHint(isNsfwHint, ageRatingHint)
            val decision = accessManager.decideAccess(resolvedHint)
            val hasHint = isNsfwHint != null || ageRatingHint != null
            val hasTags = !tags.isNullOrEmpty()
            val metadataMissing = !hasHint && !hasTags
            if (requireMetadata && metadataMissing && memberId.isNullOrBlank()) {
                return ContentAccessDecision.Blocked(ContentBlockReason.TAGS_BLOCKED)
            }
            val shouldResolveTags =
                !memberId.isNullOrBlank() &&
                    gate.blockedTags.isNotEmpty() &&
                    (tags == null || tags.isEmpty()) &&
                    (decision is ContentAccessDecision.Allowed ||
                        (decision is ContentAccessDecision.Blocked &&
                            decision.reason == ContentBlockReason.NSFW_DISABLED))
            val shouldResolveNsfw =
                decision is ContentAccessDecision.Blocked &&
                    decision.reason == ContentBlockReason.NSFW_DISABLED &&
                    resolvedHint == null &&
                    !memberId.isNullOrBlank()
            val shouldResolveMetadata =
                requireMetadata && metadataMissing && !memberId.isNullOrBlank()
            if (shouldResolveTags || shouldResolveNsfw || shouldResolveMetadata) {
                val detail =
                    runCatching {
                        characterRepository.getCharacterDetail(memberId)
                    }.getOrNull()
                if (detail == null) {
                    return if (requireMetadata || shouldResolveTags) {
                        ContentAccessDecision.Blocked(ContentBlockReason.TAGS_BLOCKED)
                    } else {
                        decision
                    }
                }
                if (gate.isTagBlocked(detail.tags)) {
                    return ContentAccessDecision.Blocked(ContentBlockReason.TAGS_BLOCKED)
                }
                val resolvedNsfw = resolveNsfwHint(detail.isNsfw, detail.moderationAgeRating)
                return accessManager.decideAccess(resolvedNsfw)
            }
            return decision
        }
    }
