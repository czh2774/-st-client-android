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
    const val CATALOG_ID = "https://stproject.ai/a2ui/v0.8/catalogs/st-mobile-safe.json"
    private val COMPONENTS =
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
    private val ACTIONS =
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

    val capabilities: A2UIClientCapabilities by lazy {
        A2UIClientCapabilities(
            schemaVersion = 1,
            supportedCatalogIds = listOf(CATALOG_ID),
            components = COMPONENTS,
            actions = ACTIONS,
            platform = "android",
            mode = "full",
            clientVersion = BuildConfig.VERSION_NAME,
            appBuild = BuildConfig.VERSION_CODE.toString(),
        )
    }

    fun asJson(gson: Gson = Gson()): String = gson.toJson(capabilities)
}
