package com.stproject.client.android.core.network

import com.google.gson.Gson
import com.stproject.client.android.BuildConfig
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
            private const val A2UI_CATALOG_ID = "https://stproject.ai/a2ui/v0.8/catalogs/st-mobile-safe.json"
            private val A2UI_COMPONENTS =
                listOf(
                    "Text",
                    "Image",
                    "Button",
                    "ChoiceButtons",
                    "Form",
                    "Sheet",
                    "PurchaseCTA",
                    "OpenSettings",
                    "Column",
                    "Row",
                    "Group",
                )
            private val A2UI_ACTIONS =
                listOf(
                    "sendMessage",
                    "cancel",
                    "continue",
                    "regenerate",
                    "setVariable",
                    "navigate",
                    "purchase",
                    "deleteMessage",
                )
            private val A2UI_CAPABILITIES: String by lazy {
                val payload =
                    mapOf(
                        "schemaVersion" to 1,
                        "supportedCatalogIds" to listOf(A2UI_CATALOG_ID),
                        "components" to A2UI_COMPONENTS,
                        "actions" to A2UI_ACTIONS,
                        "platform" to "android",
                        "mode" to "full",
                        "clientVersion" to BuildConfig.VERSION_NAME,
                        "appBuild" to BuildConfig.VERSION_CODE.toString(),
                    )
                Gson().toJson(payload)
            }
        }
    }
