package com.stproject.client.android.core.compliance

import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.features.settings.ComplianceUiState

data class ContentGate(
    val consentLoaded: Boolean,
    val consentRequired: Boolean,
    val ageVerified: Boolean,
    val allowNsfwPreference: Boolean,
    val blockedTags: List<String> = emptyList(),
) {
    val contentAllowed: Boolean =
        consentLoaded && !consentRequired && ageVerified
    val nsfwAllowed: Boolean =
        contentAllowed && allowNsfwPreference

    fun decideAccess(
        isNsfw: Boolean?,
        ageRating: AgeRating? = null,
    ): ContentAccessDecision {
        val resolvedNsfw = resolveNsfwHint(isNsfw, ageRating)
        if (!consentLoaded) return ContentAccessDecision.Blocked(ContentBlockReason.CONSENT_PENDING)
        if (consentRequired) return ContentAccessDecision.Blocked(ContentBlockReason.CONSENT_REQUIRED)
        if (!ageVerified) return ContentAccessDecision.Blocked(ContentBlockReason.AGE_REQUIRED)
        if (!allowNsfwPreference && resolvedNsfw != false) {
            return ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED)
        }
        return ContentAccessDecision.Allowed
    }

    fun isRestricted(
        isNsfw: Boolean?,
        ageRating: AgeRating? = null,
    ): Boolean {
        return decideAccess(isNsfw, ageRating) is ContentAccessDecision.Blocked
    }

    fun isTagBlocked(tags: List<String>?): Boolean {
        if (tags.isNullOrEmpty() || blockedTags.isEmpty()) return false
        return tags.any { tag -> blockedTags.any { blocked -> blocked.equals(tag, ignoreCase = true) } }
    }

    fun blockKind(
        isNsfw: Boolean?,
        ageRating: AgeRating? = null,
        tags: List<String>? = null,
    ): ContentGateBlockKind? {
        if (isTagBlocked(tags)) return ContentGateBlockKind.TAGS_BLOCKED
        return when (val decision = decideAccess(isNsfw, ageRating)) {
            is ContentAccessDecision.Allowed -> null
            is ContentAccessDecision.Blocked ->
                when (decision.reason) {
                    ContentBlockReason.TAGS_BLOCKED -> ContentGateBlockKind.TAGS_BLOCKED
                    ContentBlockReason.CONSENT_PENDING -> ContentGateBlockKind.CONSENT_PENDING
                    ContentBlockReason.CONSENT_REQUIRED -> ContentGateBlockKind.CONSENT_REQUIRED
                    ContentBlockReason.AGE_REQUIRED -> ContentGateBlockKind.AGE_REQUIRED
                    ContentBlockReason.NSFW_DISABLED -> ContentGateBlockKind.NSFW_DISABLED
                }
        }
    }

    fun isNsfwBlocked(
        isNsfw: Boolean?,
        ageRating: AgeRating? = null,
    ): Boolean {
        val decision = decideAccess(isNsfw, ageRating)
        return decision is ContentAccessDecision.Blocked &&
            decision.reason == ContentBlockReason.NSFW_DISABLED
    }

    companion object {
        fun from(state: ComplianceUiState): ContentGate =
            ContentGate(
                consentLoaded = state.consentLoaded,
                consentRequired = state.consentRequired,
                ageVerified = state.ageVerified,
                allowNsfwPreference = state.allowNsfw,
                blockedTags = state.blockedTags,
            )
    }
}

enum class ContentBlockReason {
    TAGS_BLOCKED,
    CONSENT_PENDING,
    CONSENT_REQUIRED,
    AGE_REQUIRED,
    NSFW_DISABLED,
}

enum class ContentGateBlockKind {
    TAGS_BLOCKED,
    CONSENT_PENDING,
    CONSENT_REQUIRED,
    AGE_REQUIRED,
    NSFW_DISABLED,
}

sealed class ContentAccessDecision {
    data object Allowed : ContentAccessDecision()

    data class Blocked(val reason: ContentBlockReason) : ContentAccessDecision()
}
