package com.stproject.client.android.core.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistentCookieJar
    @Inject
    constructor(
        private val storage: CookieStorage,
    ) : CookieJar, CookieCleaner {
        private val lock = Any()
        private var cache: MutableList<Cookie> = mutableListOf()

        init {
            val now = nowMillis()
            cache =
                storage.read()
                    .filter { it.expiresAt > now }
                    .map { it.toCookie() }
                    .toMutableList()
        }

        override fun saveFromResponse(
            url: HttpUrl,
            cookies: List<Cookie>,
        ) {
            synchronized(lock) {
                val now = nowMillis()
                cache.removeAll { existing -> cookies.any { matchesKey(existing, it) } }
                val fresh = cookies.filter { !isExpired(it, now) }
                cache.addAll(fresh)
                persistLocked()
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            synchronized(lock) {
                val now = nowMillis()
                val valid = cache.filter { !isExpired(it, now) }
                if (valid.size != cache.size) {
                    cache = valid.toMutableList()
                    persistLocked()
                }
                return valid.filter { it.matches(url) }
            }
        }

        override fun clearCookies() {
            synchronized(lock) {
                cache.clear()
                storage.clear()
            }
        }

        private fun persistLocked() {
            val snapshots = cache.filter { it.persistent }.map { CookieSnapshot.fromCookie(it) }
            storage.write(snapshots)
        }

        private fun nowMillis(): Long = System.currentTimeMillis()

        private fun isExpired(
            cookie: Cookie,
            nowMillis: Long,
        ): Boolean = cookie.expiresAt <= nowMillis

        private fun matchesKey(
            first: Cookie,
            second: Cookie,
        ): Boolean {
            return first.name == second.name && first.domain == second.domain && first.path == second.path
        }
    }
