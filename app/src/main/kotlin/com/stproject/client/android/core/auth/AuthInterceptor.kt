package com.stproject.client.android.core.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenStore: AuthTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (path.contains("/auth/")) {
            return chain.proceed(request)
        }
        val token = tokenStore.getAccessToken()
        if (token.isNullOrBlank()) return chain.proceed(request)
        if (!request.header("Authorization").isNullOrBlank()) {
            return chain.proceed(request)
        }

        val req =
            request
                .newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        return chain.proceed(req)
    }
}
