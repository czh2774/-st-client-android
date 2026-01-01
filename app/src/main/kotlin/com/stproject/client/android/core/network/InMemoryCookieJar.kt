package com.stproject.client.android.core.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal CookieJar implementation to support cookie-based auth flows.
 *
 * Technical debt intentionally kept small: this is in-memory only. Replace with a persistent
 * implementation before shipping login/refresh flows.
 */
class InMemoryCookieJar : CookieJar {
    private val store = ConcurrentHashMap<String, List<Cookie>>()

    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>,
    ) {
        store[url.host] = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = store[url.host].orEmpty()
        // OkHttp will discard expired cookies on its own at serialization time, but we keep it simple.
        return cookies
    }
}
