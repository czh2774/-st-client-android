package com.stproject.client.android.core.network

import com.google.gson.Gson
import com.stproject.client.android.core.a2ui.A2UIClientCapabilitiesProvider
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
            val a2uiEnabled = isA2UIEnabled()
            if (request.header(A2UI_ENABLED_HEADER).isNullOrBlank()) {
                builder.header(A2UI_ENABLED_HEADER, if (a2uiEnabled) "1" else "0")
            }
            if (a2uiEnabled && request.header(A2UI_CAPABILITIES_HEADER).isNullOrBlank()) {
                builder.header(A2UI_CAPABILITIES_HEADER, A2UI_CAPABILITIES)
            }
            return chain.proceed(builder.build())
        }

        private fun isA2UIEnabled(): Boolean = true

        private companion object {
            private const val AUTH_CLIENT_HEADER = "X-Auth-Client"
            private const val AUTH_CLIENT_VALUE = "android"
            private const val A2UI_ENABLED_HEADER = "X-ST-A2UI-Enabled"
            private const val A2UI_CAPABILITIES_HEADER = "X-ST-A2UI-Capabilities"
            private val A2UI_CAPABILITIES: String by lazy {
                A2UIClientCapabilitiesProvider.asJson(Gson())
            }
        }
    }
