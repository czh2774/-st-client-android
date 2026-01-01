package com.stproject.client.android.domain.usecase

import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentBlockReason
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
        ): ContentAccessDecision {
            val decision = accessManager.decideAccess(isNsfwHint)
            if (decision is ContentAccessDecision.Allowed) return decision
            if (decision is ContentAccessDecision.Blocked &&
                decision.reason == ContentBlockReason.NSFW_DISABLED &&
                isNsfwHint == null &&
                !memberId.isNullOrBlank()
            ) {
                val resolvedNsfw =
                    runCatching {
                        characterRepository.getCharacterDetail(memberId).isNsfw
                    }.getOrNull()
                return accessManager.decideAccess(resolvedNsfw)
            }
            return decision
        }
    }
