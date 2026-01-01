package com.stproject.client.android.core.logging

import android.util.Log
import timber.log.Timber

/**
 * Safe default for release: log only WARN/ERROR, no stack traces unless provided,
 * and avoid leaking PII by keeping messages minimal.
 *
 * Replace with crash reporting integration once available.
 */
class ReleaseTree : Timber.Tree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (priority < Log.WARN) return

        // Keep it conservative; platform logging can still be captured on rooted devices.
        val safeTag = tag ?: "ST"
        if (t != null) {
            Log.println(priority, safeTag, "$message (${t.javaClass.simpleName})")
        } else {
            Log.println(priority, safeTag, message)
        }
    }
}
