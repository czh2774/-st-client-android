package com.stproject.client.android.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientHeadersInterceptor
    @Inject
    constructor() : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val builder = request.newBuilder()
            if (request.header(AUTH_CLIENT_HEADER).isNullOrBlank()) {
                builder.header(AUTH_CLIENT_HEADER, AUTH_CLIENT_VALUE)
            }
            return chain.proceed(builder.build())
        }

        private companion object {
            private const val AUTH_CLIENT_HEADER = "X-Auth-Client"
            private const val AUTH_CLIENT_VALUE = "android"
        }
    }
