package com.stproject.client.android.core.a2ui

import com.google.gson.JsonElement
import com.google.gson.JsonObject

data class A2UIMessage(
    val surfaceUpdate: A2UISurfaceUpdate? = null,
    val dataModelUpdate: A2UIDataModelUpdate? = null,
    val beginRendering: A2UIBeginRendering? = null,
    val deleteSurface: A2UIDeleteSurface? = null,
)

data class A2UISurfaceUpdate(
    val surfaceId: String? = null,
    val components: List<A2UIComponentDefinition>? = null,
)

data class A2UIComponentDefinition(
    val id: String? = null,
    val component: JsonObject? = null,
)

data class A2UIDataModelUpdate(
    val surfaceId: String? = null,
    val path: String? = null,
    val contents: List<A2UIDataEntry>? = null,
)

data class A2UIDataEntry(
    val key: String? = null,
    val valueString: String? = null,
    val valueNumber: Double? = null,
    val valueBoolean: Boolean? = null,
    val valueMap: List<A2UIDataEntry>? = null,
    val valueList: List<JsonElement>? = null,
)

data class A2UIBeginRendering(
    val surfaceId: String? = null,
    val root: String? = null,
    val catalogId: String? = null,
)

data class A2UIDeleteSurface(
    val surfaceId: String? = null,
)
