package com.stproject.client.android.core.network

import okhttp3.Interceptor
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class StOkHttpClientFactory {
    fun create(
        cookieJar: CookieJar,
        additionalInterceptors: List<Interceptor> = emptyList()
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .apply {
                additionalInterceptors.forEach { addInterceptor(it) }
            }
            .build()
    }
}


