package com.stproject.client.android.core.network

import okhttp3.Cookie
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentCookieJarTest {
    private class InMemoryCookieStorage : CookieStorage {
        var cookies: List<CookieSnapshot> = emptyList()
            private set

        override fun read(): List<CookieSnapshot> = cookies

        override fun write(cookies: List<CookieSnapshot>) {
            this.cookies = cookies
        }

        override fun clear() {
            cookies = emptyList()
        }
    }

    @Test
    fun `persists only persistent cookies`() {
        val storage = InMemoryCookieStorage()
        val jar = PersistentCookieJar(storage)
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("example.com")
                .addPathSegment("api")
                .build()
        val now = System.currentTimeMillis()
        val persistent =
            Cookie.Builder()
                .name("refresh_token")
                .value("rt")
                .domain("example.com")
                .path("/")
                .expiresAt(now + 60_000)
                .build()
        val session =
            Cookie.Builder()
                .name("session")
                .value("s1")
                .domain("example.com")
                .path("/")
                .build()

        jar.saveFromResponse(url, listOf(persistent, session))

        assertEquals(1, storage.cookies.size)
        val restored = PersistentCookieJar(storage).loadForRequest(url)
        assertEquals(1, restored.size)
        assertEquals("refresh_token", restored.first().name)
    }

    @Test
    fun `drops expired cookies`() {
        val storage = InMemoryCookieStorage()
        val jar = PersistentCookieJar(storage)
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("example.com")
                .addPathSegment("api")
                .build()
        val expired =
            Cookie.Builder()
                .name("expired")
                .value("gone")
                .domain("example.com")
                .path("/")
                .expiresAt(System.currentTimeMillis() - 1_000)
                .build()

        jar.saveFromResponse(url, listOf(expired))
        val cookies = jar.loadForRequest(url)

        assertTrue(cookies.isEmpty())
    }
}
