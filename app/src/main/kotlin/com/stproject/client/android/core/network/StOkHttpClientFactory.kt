package com.stproject.client.android.core.network

import okhttp3.Authenticator
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class StOkHttpClientFactory {
    fun create(
        cookieJar: CookieJar,
        additionalInterceptors: List<Interceptor> = emptyList(),
        authenticator: Authenticator? = null,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .apply {
                if (authenticator != null) {
                    authenticator(authenticator)
                }
            }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .apply {
                additionalInterceptors.forEach { addInterceptor(it) }
            }
            .build()
    }
}
