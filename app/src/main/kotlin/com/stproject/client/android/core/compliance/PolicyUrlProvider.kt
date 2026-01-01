package com.stproject.client.android.core.compliance

import com.stproject.client.android.BuildConfig

class PolicyUrlProvider {
    fun privacyUrl(): String = validateUrl(BuildConfig.PRIVACY_URL, "privacy")

    fun termsUrl(): String = validateUrl(BuildConfig.TERMS_URL, "terms")

    private fun validateUrl(
        raw: String,
        label: String,
    ): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            error("$label url is empty.")
        }
        if (!BuildConfig.DEBUG) {
            if (trimmed == "__SET_ME__") {
                error("Release $label url must be set (BuildConfig.${label.uppercase()}_URL).")
            }
            if (!trimmed.startsWith("https://")) {
                error("Release $label url must use HTTPS.")
            }
        }
        return trimmed
    }
}
