package com.stproject.client.android.core.network

import com.stproject.client.android.BuildConfig

/**
 * Centralized base URL provider.
 *
 * Best practice: read from build config / flavors. For now this is a placeholder so
 * call sites don't hardcode URLs.
 */
class StBaseUrlProvider {
    fun baseUrl(): String {
        val raw = BuildConfig.API_BASE_URL.trim()
        // Retrofit requires baseUrl ends with "/"
        return if (raw.endsWith("/")) raw else "$raw/"
    }
}


