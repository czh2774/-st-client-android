package com.stproject.client.android.core.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal in-memory auth token store.
 *
 * Contract tests in st-client-react use Authorization: Bearer <token>.
 * Replace with secure storage + refresh/cookie flow when auth is implemented.
 */
@Singleton
class AuthTokenStore @Inject constructor() {
    @Volatile private var token: String? = null

    fun getBearerToken(): String? = token

    fun setBearerToken(value: String?) {
        token = value?.trim()?.takeIf { it.isNotEmpty() }
    }
}


