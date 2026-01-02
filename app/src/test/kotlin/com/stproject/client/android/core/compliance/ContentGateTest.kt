package com.stproject.client.android.core.compliance

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
}
