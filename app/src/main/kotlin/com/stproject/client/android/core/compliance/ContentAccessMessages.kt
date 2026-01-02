package com.stproject.client.android.core.compliance

fun ContentAccessDecision.Blocked.userMessage(): String {
    return when (reason) {
        ContentBlockReason.NSFW_DISABLED -> "mature content disabled"
        ContentBlockReason.AGE_REQUIRED -> "age verification required"
        ContentBlockReason.CONSENT_REQUIRED -> "terms acceptance required"
        ContentBlockReason.CONSENT_PENDING -> "compliance not loaded"
    }
}
