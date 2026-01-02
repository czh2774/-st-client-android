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
    ): ContentAccessDecision {
        val resolvedHint = resolveNsfwHint(isNsfwHint, ageRatingHint)
        val decision = accessManager.decideAccess(resolvedHint)
        if (decision is ContentAccessDecision.Allowed) return decision
        if (decision is ContentAccessDecision.Blocked &&
            decision.reason == ContentBlockReason.NSFW_DISABLED &&
            resolvedHint == null &&
            !memberId.isNullOrBlank()
        ) {
            val resolvedNsfw =
                runCatching {
                    val detail = characterRepository.getCharacterDetail(memberId)
                    resolveNsfwHint(detail.isNsfw, detail.moderationAgeRating)
                }.getOrNull()
            return accessManager.decideAccess(resolvedNsfw)
        }
        return decision
        }
    }
