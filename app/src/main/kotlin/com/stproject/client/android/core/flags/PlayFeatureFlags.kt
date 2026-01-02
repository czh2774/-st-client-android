package com.stproject.client.android.core.flags

import com.stproject.client.android.BuildConfig

object PlayFeatureFlags {
    val extensionsEnabled: Boolean = !BuildConfig.PLAY_BUILD
    val badgesEnabled: Boolean = false
    val redPacketEnabled: Boolean = false
}
