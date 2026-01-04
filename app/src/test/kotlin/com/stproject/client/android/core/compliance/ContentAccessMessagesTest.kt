package com.stproject.client.android.core.compliance

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentAccessMessagesTest {
    @Test
    fun `blocked userMessage maps reasons`() {
        assertEquals(
            "content blocked by filters",
            ContentAccessDecision.Blocked(ContentBlockReason.TAGS_BLOCKED).userMessage(),
        )
        assertEquals(
            "mature content disabled",
            ContentAccessDecision.Blocked(ContentBlockReason.NSFW_DISABLED).userMessage(),
        )
        assertEquals(
            "age verification required",
            ContentAccessDecision.Blocked(ContentBlockReason.AGE_REQUIRED).userMessage(),
        )
        assertEquals(
            "terms acceptance required",
            ContentAccessDecision.Blocked(ContentBlockReason.CONSENT_REQUIRED).userMessage(),
        )
        assertEquals(
            "compliance not loaded",
            ContentAccessDecision.Blocked(ContentBlockReason.CONSENT_PENDING).userMessage(),
        )
    }
}
