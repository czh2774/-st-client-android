package com.stproject.client.android.core.a2ui

import com.google.gson.Gson
import com.stproject.client.android.BuildConfig

data class A2UIClientCapabilities(
    val schemaVersion: Int,
    val supportedCatalogIds: List<String>,
    val components: List<String>,
    val actions: List<String>,
    val platform: String,
    val mode: String,
    val clientVersion: String,
    val appBuild: String,
)

object A2UIClientCapabilitiesProvider {
    val capabilities: A2UIClientCapabilities by lazy {
        A2UIClientCapabilities(
            schemaVersion = 1,
            supportedCatalogIds = listOf(A2UICatalog.CATALOG_ID),
            components = A2UICatalog.COMPONENTS,
            actions = A2UICatalog.ACTIONS,
            platform = "android",
            mode = "full",
            clientVersion = BuildConfig.VERSION_NAME,
            appBuild = BuildConfig.VERSION_CODE.toString(),
        )
    }

    fun asJson(gson: Gson = Gson()): String = gson.toJson(capabilities)
}
