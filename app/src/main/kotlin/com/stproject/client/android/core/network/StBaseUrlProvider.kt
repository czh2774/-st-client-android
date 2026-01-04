package com.stproject.client.android.core.network

import com.stproject.client.android.BuildConfig

/**
 * Centralized base URL provider.
 *
 * Best practice: read from build config / flavors. For now this is a placeholder so
 * call sites don't hardcode URLs.
 */
open class StBaseUrlProvider {
    open fun baseUrl(): String {
        val raw = BuildConfig.API_BASE_URL.trim()
        if (raw.isEmpty()) {
            error("API base URL is empty.")
        }
        if (!BuildConfig.DEBUG) {
            if (raw == "__SET_ME__" || raw.isEmpty()) {
                error("Release API base URL must be set (BuildConfig.API_BASE_URL).")
            }
            if (raw.startsWith("http://")) {
                error("Release API base URL must use HTTPS.")
            }
        }
        // Retrofit requires baseUrl ends with "/"
        return if (raw.endsWith("/")) raw else "$raw/"
    }
}
