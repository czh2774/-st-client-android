package com.stproject.client.android.core.flags

import com.stproject.client.android.BuildConfig

object PlayFeatureFlags {
    val extensionsEnabled: Boolean = false
    val badgesEnabled: Boolean = BuildConfig.BADGES_ENABLED
}
