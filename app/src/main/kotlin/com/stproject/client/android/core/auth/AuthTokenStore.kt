package com.stproject.client.android.core.auth

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtEpochSeconds: Long?
)

interface AuthTokenStore {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getExpiresAtEpochSeconds(): Long?
    fun getTokens(): AuthTokens?
    fun updateTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long?)
    fun clear()
}

@Singleton
class EncryptedAuthTokenStore @Inject constructor(
    @Named("auth_prefs") private val prefs: SharedPreferences
) : AuthTokenStore {
    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    override fun getExpiresAtEpochSeconds(): Long? {
        val raw = prefs.getLong(KEY_EXPIRES_AT, -1)
        return raw.takeIf { it > 0 }
    }

    override fun getTokens(): AuthTokens? {
        val access = getAccessToken() ?: return null
        return AuthTokens(
            accessToken = access,
            refreshToken = getRefreshToken(),
            expiresAtEpochSeconds = getExpiresAtEpochSeconds()
        )
    }

    override fun updateTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long?) {
        val cleanAccess = normalize(accessToken)
        val cleanRefresh = normalize(refreshToken)
        val editor = prefs.edit()
        if (cleanAccess == null) {
            editor.remove(KEY_ACCESS_TOKEN)
        } else {
            editor.putString(KEY_ACCESS_TOKEN, cleanAccess)
        }
        if (cleanRefresh == null) {
            editor.remove(KEY_REFRESH_TOKEN)
        } else {
            editor.putString(KEY_REFRESH_TOKEN, cleanRefresh)
        }
        if (expiresInSeconds != null && expiresInSeconds > 0) {
            editor.putLong(KEY_EXPIRES_AT, nowEpochSeconds() + expiresInSeconds)
        } else {
            editor.remove(KEY_EXPIRES_AT)
        }
        editor.apply()
    }

    override fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    private fun normalize(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

    private fun nowEpochSeconds(): Long = System.currentTimeMillis() / 1000

    private companion object {
        private const val KEY_ACCESS_TOKEN = "auth.access_token"
        private const val KEY_REFRESH_TOKEN = "auth.refresh_token"
        private const val KEY_EXPIRES_AT = "auth.expires_at"
    }
}

