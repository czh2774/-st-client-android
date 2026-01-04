package com.stproject.client.android.core.compliance

import com.stproject.client.android.domain.model.AgeRating
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentGateTest {
    @Test
    fun `decideAccess blocks when consent not loaded`() {
        val gate =
            ContentGate(
                consentLoaded = false,
                consentRequired = true,
                ageVerified = false,
                allowNsfwPreference = false,
            )
        val decision = gate.decideAccess(isNsfw = false)
        assertEquals(ContentAccessDecision.Blocked(ContentBlockReason.CONSENT_PENDING), decision)
    }

    @Test
    fun `decideAccess blocks when consent required`() {
        val gate =
            ContentGate(
                consentLoaded = true,
                consentRequired = true,
                ageVerified = true,
                allowNsfwPreference = true,
            )
        val decision = gate.decideAccess(isNsfw = false)
        assertEquals(ContentAccessDecision.Blocked(ContentBlockReason.CONSENT_REQUIRED), decision)
    }

    @Test
    fun `decideAccess blocks when age required`() {
        val gate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = false,
                allowNsfwPreference = true,
            )
        val decision = gate.decideAccess(isNsfw = false)
        assertEquals(ContentAccessDecision.Blocked(ContentBlockReason.AGE_REQUIRED), decision)
    }

    @Test
    fun `decideAccess blocks nsfw when disabled`() {
        val gate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = false,
            )
        val decision = gate.decideAccess(isNsfw = true)
        assertEquals(ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED), decision)
    }

    @Test
    fun `decideAccess allows safe content when nsfw disabled`() {
        val gate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = false,
            )
        val decision = gate.decideAccess(isNsfw = false)
        assertEquals(ContentAccessDecision.Allowed, decision)
    }

    @Test
    fun `decideAccess blocks when age rating is adult`() {
        val gate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = false,
            )
        val decision = gate.decideAccess(isNsfw = null, ageRating = AgeRating.Age18)
        assertEquals(ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED), decision)
    }

    @Test
    fun `decideAccess blocks when age rating is adult even if nsfw is false`() {
        val gate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = false,
            )
        val decision = gate.decideAccess(isNsfw = false, ageRating = AgeRating.Age18)
        assertEquals(ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED), decision)
    }

    @Test
    fun `decideAccess allows non-adult age rating when nsfw disabled`() {
        val gate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = false,
            )
        val decision = gate.decideAccess(isNsfw = null, ageRating = AgeRating.Age16)
        assertEquals(ContentAccessDecision.Allowed, decision)
    }

    @Test
    fun `isTagBlocked returns true for matching blocked tag ignoring case`() {
        val gate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = true,
                blockedTags = listOf("Violence", "gore"),
            )

        assertEquals(true, gate.isTagBlocked(listOf("romance", "GORE")))
    }

    @Test
    fun `isTagBlocked returns false when tags are empty or not matching`() {
        val gate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = true,
                blockedTags = listOf("violence"),
            )

        assertEquals(false, gate.isTagBlocked(null))
        assertEquals(false, gate.isTagBlocked(emptyList()))
        assertEquals(false, gate.isTagBlocked(listOf("romance")))
    }

    @Test
    fun `blockKind returns tag block before access checks`() {
        val gate =
            ContentGate(
                consentLoaded = false,
                consentRequired = true,
                ageVerified = false,
                allowNsfwPreference = false,
                blockedTags = listOf("gore"),
            )

        assertEquals(
            ContentGateBlockKind.TAGS_BLOCKED,
            gate.blockKind(isNsfw = true, ageRating = AgeRating.Age18, tags = listOf("GORE")),
        )
    }

    @Test
    fun `blockKind returns nsfw disabled when blocked`() {
        val gate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = false,
            )

        assertEquals(
            ContentGateBlockKind.NSFW_DISABLED,
            gate.blockKind(isNsfw = true, ageRating = AgeRating.Age18, tags = emptyList()),
        )
    }
}
