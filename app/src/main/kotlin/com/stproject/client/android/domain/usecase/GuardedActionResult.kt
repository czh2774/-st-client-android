package com.stproject.client.android.domain.usecase

import com.stproject.client.android.core.compliance.ContentAccessDecision

sealed class GuardedActionResult<out T> {
    data class Allowed<T>(val value: T) : GuardedActionResult<T>()

    data class Blocked(val decision: ContentAccessDecision.Blocked) : GuardedActionResult<Nothing>()
}
