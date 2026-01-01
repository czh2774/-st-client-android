package com.stproject.client.android.core.logging

import com.stproject.client.android.BuildConfig
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Debug-only network logging. Keep this conservative: redact sensitive headers and
 * never enable in release builds.
 */
fun createHttpLoggingInterceptor(): HttpLoggingInterceptor? {
    if (!BuildConfig.DEBUG) return null

    return HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
        redactHeader("X-Csrf-Token")
    }
}
