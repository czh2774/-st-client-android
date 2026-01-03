package com.stproject.client.android.core.a2ui

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import timber.log.Timber

data class A2UIComponent(
    val id: String,
    val type: String,
    val props: JsonObject,
    val weight: Double? = null,
)

data class A2UISurfaceState(
    val surfaceId: String,
    val rootId: String? = null,
    val catalogId: String? = null,
    val styles: JsonObject? = null,
    val components: Map<String, A2UIComponent> = emptyMap(),
    val dataModel: Map<String, Any?> = emptyMap(),
)

data class A2UIRuntimeState(
    val surfaces: Map<String, A2UISurfaceState> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = surfaces.isEmpty()

    fun asList(): List<A2UISurfaceState> = surfaces.values.sortedBy { it.surfaceId }
}

object A2UIRuntimeReducer {
    fun reduce(
        state: A2UIRuntimeState,
        message: A2UIMessage,
    ): A2UIRuntimeState {
        val actionCount = countActions(message)
        if (actionCount != 1) {
            Timber.w("A2UI message must contain exactly one action, got %d", actionCount)
            return state
        }
        val surfaces = state.surfaces.toMutableMap()

        message.deleteSurface?.surfaceId?.trim()?.takeIf { it.isNotEmpty() }?.let { surfaceId ->
            surfaces.remove(surfaceId)
        }

        message.surfaceUpdate?.let { update ->
            val surfaceId = update.surfaceId?.trim().orEmpty()
            if (surfaceId.isNotEmpty()) {
                val next =
                    updateSurfaceComponents(
                        current = surfaces[surfaceId],
                        surfaceId = surfaceId,
                        components = update.components.orEmpty(),
                    )
                surfaces[surfaceId] = next
            }
        }

        message.beginRendering?.let { begin ->
            val surfaceId = begin.surfaceId?.trim().orEmpty()
            if (surfaceId.isNotEmpty()) {
                val current = surfaces[surfaceId] ?: A2UISurfaceState(surfaceId = surfaceId)
                logCatalogIdIfNeeded(current, begin)
                logStylesIfNeeded(current, begin)
                surfaces[surfaceId] =
                    current.copy(
                        rootId = begin.root?.trim()?.takeIf { it.isNotEmpty() },
                        catalogId = begin.catalogId?.trim()?.takeIf { it.isNotEmpty() },
                        styles = begin.styles?.takeIf { it.entrySet().isNotEmpty() },
                    )
            }
        }

        message.dataModelUpdate?.let { update ->
            val surfaceId = update.surfaceId?.trim().orEmpty()
            if (surfaceId.isNotEmpty()) {
                val current = surfaces[surfaceId] ?: A2UISurfaceState(surfaceId = surfaceId)
                surfaces[surfaceId] = applyDataModelUpdate(current, update)
            }
        }

        return A2UIRuntimeState(surfaces = surfaces)
    }

    private fun updateSurfaceComponents(
        current: A2UISurfaceState?,
        surfaceId: String,
        components: List<A2UIComponentDefinition>,
    ): A2UISurfaceState {
        val existing = current ?: A2UISurfaceState(surfaceId = surfaceId)
        if (components.isEmpty()) return existing
        val nextComponents = existing.components.toMutableMap()
        for (definition in components) {
            val componentId = definition.id?.trim().orEmpty()
            val componentObj = definition.component ?: continue
            val entries = componentObj.entrySet()
            if (componentId.isEmpty() || entries.isEmpty()) continue
            if (entries.size != 1) {
                Timber.w(
                    "A2UI component must contain exactly one type key (id=%s size=%d)",
                    componentId,
                    entries.size,
                )
                continue
            }
            val entry = entries.first()
            val type = entry.key
            val props = entry.value.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
            nextComponents[componentId] =
                A2UIComponent(
                    id = componentId,
                    type = type,
                    props = props,
                    weight = definition.weight,
                )
        }
        return existing.copy(components = nextComponents)
    }

    private fun applyDataModelUpdate(
        current: A2UISurfaceState,
        update: A2UIDataModelUpdate,
    ): A2UISurfaceState {
        val patch = entriesToMap(update.contents.orEmpty())
        val path = update.path?.trim().orEmpty()
        if (path.isEmpty() || path == "/") {
            return current.copy(dataModel = patch.toMutableMap())
        }
        if (patch.isEmpty()) return current
        val root = current.dataModel.toMutableMap()
        val target = ensurePath(root, path) ?: return current
        target.putAll(patch)
        return current.copy(dataModel = root)
    }

    private fun entriesToMap(entries: List<A2UIDataEntry>): Map<String, Any?> {
        if (entries.isEmpty()) return emptyMap()
        val out = mutableMapOf<String, Any?>()
        for (entry in entries) {
            val key = entry.key?.trim().orEmpty()
            if (key.isEmpty()) continue
            val value = parseEntryValue(entry)
            if (value != null) {
                out[key] = value
            }
        }
        return out
    }

    private fun parseEntryValue(entry: A2UIDataEntry): Any? {
        entry.valueString?.let { return it }
        entry.valueNumber?.let { return it }
        entry.valueBoolean?.let { return it }
        entry.valueMap?.let { return entriesToMap(it) }
        entry.valueList?.let { return parseValueList(it) }
        return null
    }

    private fun parseValueList(entries: List<JsonElement>): List<Any?> {
        val out = ArrayList<Any?>(entries.size)
        for (entry in entries) {
            out.add(parseJsonElement(entry))
        }
        return out
    }

    private fun parseJsonElement(element: JsonElement?): Any? {
        if (element == null || element.isJsonNull) return null
        if (element.isJsonPrimitive) {
            val primitive = element.asJsonPrimitive
            return when {
                primitive.isBoolean -> primitive.asBoolean
                primitive.isNumber -> primitive.asNumber.toDouble()
                primitive.isString -> primitive.asString
                else -> null
            }
        }
        if (element.isJsonArray) {
            return element.asJsonArray.map { parseJsonElement(it) }
        }
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            val maybeEntry = runCatching { obj.get("key")?.asString }.getOrNull()
            if (!maybeEntry.isNullOrBlank()) {
                val entry =
                    A2UIDataEntry(
                        key = maybeEntry,
                        valueString = obj.readString("valueString"),
                        valueNumber = obj.readDouble("valueNumber"),
                        valueBoolean = obj.readBoolean("valueBoolean"),
                        valueMap = obj.readEntryList("valueMap"),
                        valueList = obj.readElementList("valueList"),
                    )
                return parseEntryValue(entry)
            }
            val mapped = mutableMapOf<String, Any?>()
            for ((k, v) in obj.entrySet()) {
                mapped[k] = parseJsonElement(v)
            }
            return mapped
        }
        return null
    }

    private fun ensurePath(
        root: MutableMap<String, Any?>,
        path: String,
    ): MutableMap<String, Any?>? {
        val segments =
            path.split("/")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        if (segments.isEmpty()) return root
        var current: MutableMap<String, Any?> = root
        for (segment in segments) {
            val existing = current[segment]
            val next =
                when (existing) {
                    is MutableMap<*, *> -> existing.entries.associate { it.key.toString() to it.value }.toMutableMap()
                    is Map<*, *> -> existing.entries.associate { it.key.toString() to it.value }.toMutableMap()
                    else -> null
                }
            if (next == null) {
                val fresh = mutableMapOf<String, Any?>()
                current[segment] = fresh
                current = fresh
            } else {
                if (existing !is MutableMap<*, *>) {
                    current[segment] = next
                }
                current = next
            }
        }
        return current
    }

    private fun countActions(message: A2UIMessage): Int {
        var count = 0
        if (message.beginRendering != null) count++
        if (message.surfaceUpdate != null) count++
        if (message.dataModelUpdate != null) count++
        if (message.deleteSurface != null) count++
        return count
    }

    private fun logCatalogIdIfNeeded(
        current: A2UISurfaceState,
        begin: A2UIBeginRendering,
    ) {
        val nextCatalog = begin.catalogId?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val currentCatalog = current.catalogId?.trim().orEmpty()
        if (nextCatalog != currentCatalog) {
            Timber.i("A2UI catalogId=%s", nextCatalog)
        }
    }

    private fun logStylesIfNeeded(
        current: A2UISurfaceState,
        begin: A2UIBeginRendering,
    ) {
        if (begin.styles?.entrySet()?.isNotEmpty() != true) return
        if (current.styles == null || current.styles.entrySet().isEmpty()) {
            Timber.i("A2UI styles received (%d entries)", begin.styles.entrySet().size)
        }
    }
}

private fun JsonObject.readString(key: String): String? =
    runCatching { get(key)?.takeIf { !it.isJsonNull }?.asString }.getOrNull()

private fun JsonObject.readDouble(key: String): Double? =
    runCatching { get(key)?.takeIf { !it.isJsonNull }?.asDouble }.getOrNull()

private fun JsonObject.readBoolean(key: String): Boolean? =
    runCatching { get(key)?.takeIf { !it.isJsonNull }?.asBoolean }.getOrNull()

private fun JsonObject.readEntryList(key: String): List<A2UIDataEntry>? =
    runCatching {
        getAsJsonArray(key)?.mapNotNull { element ->
            runCatching { element.asJsonObject }.getOrNull()
        }?.map { obj ->
            A2UIDataEntry(
                key = obj.readString("key"),
                valueString = obj.readString("valueString"),
                valueNumber = obj.readDouble("valueNumber"),
                valueBoolean = obj.readBoolean("valueBoolean"),
                valueMap = obj.readEntryList("valueMap"),
                valueList = obj.readElementList("valueList"),
            )
        }
    }.getOrNull()

private fun JsonObject.readElementList(key: String): List<JsonElement>? =
    runCatching { getAsJsonArray(key)?.toList() }.getOrNull()
