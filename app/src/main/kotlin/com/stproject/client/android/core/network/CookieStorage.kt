package com.stproject.client.android.core.network

import android.content.SharedPreferences
import com.google.gson.Gson
import okhttp3.Cookie
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

interface CookieStorage {
    fun read(): List<CookieSnapshot>

    fun write(cookies: List<CookieSnapshot>)

    fun clear()
}

interface CookieCleaner {
    fun clearCookies()
}

data class CookieSnapshot(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean,
    val persistent: Boolean,
) {
    fun toCookie(): Cookie {
        val builder =
            Cookie.Builder()
                .name(name)
                .value(value)
                .path(path)
                .expiresAt(expiresAt)
        if (hostOnly) {
            builder.hostOnlyDomain(domain)
        } else {
            builder.domain(domain)
        }
        if (secure) builder.secure()
        if (httpOnly) builder.httpOnly()
        return builder.build()
    }

    companion object {
        fun fromCookie(cookie: Cookie): CookieSnapshot {
            return CookieSnapshot(
                name = cookie.name,
                value = cookie.value,
                expiresAt = cookie.expiresAt,
                domain = cookie.domain,
                path = cookie.path,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly,
                hostOnly = cookie.hostOnly,
                persistent = cookie.persistent,
            )
        }
    }
}

@Singleton
class SharedPreferencesCookieStorage
    @Inject
    constructor(
        @Named("cookie_prefs") private val prefs: SharedPreferences,
    ) : CookieStorage {
        private val gson = Gson()

        override fun read(): List<CookieSnapshot> {
            val raw = prefs.getString(KEY_COOKIES, null) ?: return emptyList()
            return runCatching {
                gson.fromJson(raw, Array<CookieSnapshot>::class.java).toList()
            }.getOrElse {
                clear()
                emptyList()
            }
        }

        override fun write(cookies: List<CookieSnapshot>) {
            val json = gson.toJson(cookies)
            prefs.edit().putString(KEY_COOKIES, json).apply()
        }

        override fun clear() {
            prefs.edit().remove(KEY_COOKIES).apply()
        }

        private companion object {
            private const val KEY_COOKIES = "cookies.snapshot"
        }
    }
