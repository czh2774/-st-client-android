package com.stproject.client.android.core.compliance

import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.features.settings.ComplianceUiState

data class ContentGate(
    val consentLoaded: Boolean,
    val consentRequired: Boolean,
    val ageVerified: Boolean,
    val allowNsfwPreference: Boolean,
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
            )
    }
}

enum class ContentBlockReason {
    CONSENT_PENDING,
    CONSENT_REQUIRED,
    AGE_REQUIRED,
    NSFW_DISABLED,
}

sealed class ContentAccessDecision {
    data object Allowed : ContentAccessDecision()

    data class Blocked(val reason: ContentBlockReason) : ContentAccessDecision()
}
