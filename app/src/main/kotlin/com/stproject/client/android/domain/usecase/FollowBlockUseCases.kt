package com.stproject.client.android.domain.usecase

import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.SocialRepository
import javax.inject.Inject

class FollowUserUseCase
    @Inject
    constructor(
        private val socialRepository: SocialRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) {
        suspend fun execute(
            userId: String,
            value: Boolean,
        ): GuardedActionResult<Unit> {
            val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
            if (access is ContentAccessDecision.Blocked) {
                return GuardedActionResult.Blocked(access)
            }
            socialRepository.followUser(userId, value)
            return GuardedActionResult.Allowed(Unit)
        }
    }

class BlockUserUseCase
    @Inject
    constructor(
        private val socialRepository: SocialRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) {
        suspend fun execute(
            userId: String,
            value: Boolean,
        ): GuardedActionResult<Unit> {
            val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
            if (access is ContentAccessDecision.Blocked) {
                return GuardedActionResult.Blocked(access)
            }
            socialRepository.blockUser(userId, value)
            return GuardedActionResult.Allowed(Unit)
        }
    }

class FollowCharacterUseCase
    @Inject
    constructor(
        private val characterRepository: CharacterRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) {
    suspend fun execute(
        characterId: String,
        isNsfwHint: Boolean?,
        value: Boolean,
        ageRatingHint: AgeRating? = null,
    ): GuardedActionResult<com.stproject.client.android.domain.model.CharacterFollowResult> {
        val access =
            resolveContentAccess.execute(
                memberId = characterId,
                isNsfwHint = isNsfwHint,
                ageRatingHint = ageRatingHint,
            )
        if (access is ContentAccessDecision.Blocked) {
            return GuardedActionResult.Blocked(access)
        }
            val result = characterRepository.followCharacter(characterId, value)
            return GuardedActionResult.Allowed(result)
        }
    }

class BlockCharacterUseCase
    @Inject
    constructor(
        private val characterRepository: CharacterRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) {
    suspend fun execute(
        characterId: String,
        isNsfwHint: Boolean?,
        value: Boolean,
        ageRatingHint: AgeRating? = null,
    ): GuardedActionResult<Unit> {
        val access =
            resolveContentAccess.execute(
                memberId = characterId,
                isNsfwHint = isNsfwHint,
                ageRatingHint = ageRatingHint,
            )
        if (access is ContentAccessDecision.Blocked) {
            return GuardedActionResult.Blocked(access)
        }
            characterRepository.blockCharacter(characterId, value)
            return GuardedActionResult.Allowed(Unit)
        }
    }
