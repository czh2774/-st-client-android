package com.stproject.client.android.core.a2ui

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object A2UIBindingResolver {
    fun resolveValue(
        value: JsonElement?,
        dataModel: Map<String, Any?>,
    ): Any? {
        if (value == null || value.isJsonNull) return null
        if (value.isJsonPrimitive) {
            val primitive = value.asJsonPrimitive
            return when {
                primitive.isBoolean -> primitive.asBoolean
                primitive.isNumber -> primitive.asNumber.toDouble()
                primitive.isString -> primitive.asString
                else -> null
            }
        }
        if (value.isJsonArray) {
            return value.asJsonArray.map { resolveValue(it, dataModel) }
        }
        if (value.isJsonObject) {
            val obj = value.asJsonObject
            val path = obj.readString("path")?.trim().orEmpty()
            if (path.isNotEmpty()) {
                val literal = extractLiteralValue(obj)
                if (literal != null) {
                    initializePathValue(dataModel, path, literal)
                }
                return resolvePath(path, dataModel) ?: literal
            }
            extractLiteralValue(obj)?.let { return it }
        }
        return null
    }

    fun resolveString(
        value: JsonElement?,
        dataModel: Map<String, Any?>,
    ): String? {
        val resolved = resolveValue(value, dataModel)
        return when (resolved) {
            is String -> resolved.trim().takeIf { it.isNotEmpty() }
            is Number -> resolved.toString()
            is Boolean -> resolved.toString()
            else -> null
        }
    }

    fun resolveBoolean(
        value: JsonElement?,
        dataModel: Map<String, Any?>,
    ): Boolean? {
        val resolved = resolveValue(value, dataModel)
        return when (resolved) {
            is Boolean -> resolved
            is String ->
                when (resolved.trim().lowercase()) {
                    "1", "true", "yes" -> true
                    "0", "false", "no" -> false
                    else -> null
                }
            else -> null
        }
    }

    fun resolveNumber(
        value: JsonElement?,
        dataModel: Map<String, Any?>,
    ): Double? {
        val resolved = resolveValue(value, dataModel)
        return when (resolved) {
            is Number -> resolved.toDouble()
            is String -> resolved.trim().toDoubleOrNull()
            else -> null
        }
    }

    private fun resolvePath(
        rawPath: String,
        dataModel: Map<String, Any?>,
    ): Any? {
        val segments = parsePathSegments(rawPath)
        if (segments.isEmpty()) return null
        var current: Any? = dataModel
        for (segment in segments) {
            current =
                when (current) {
                    is Map<*, *> -> current[segment]
                    is List<*> -> {
                        val idx = segment.toIntOrNull() ?: return null
                        current.getOrNull(idx)
                    }
                    else -> return null
                }
        }
        return current
    }

    private fun extractLiteralValue(obj: JsonObject): Any? {
        obj.readString("literalString")?.let { return it }
        obj.readString("valueString")?.let { return it }
        obj.readDouble("literalNumber")?.let { return it }
        obj.readDouble("valueNumber")?.let { return it }
        obj.readBoolean("literalBoolean")?.let { return it }
        obj.readBoolean("valueBoolean")?.let { return it }
        return null
    }

    private fun initializePathValue(
        dataModel: Map<String, Any?>,
        path: String,
        value: Any,
    ) {
        val root = dataModel as? MutableMap<String, Any?> ?: return
        val segments = parsePathSegments(path)
        if (segments.isEmpty()) return
        val leaf = segments.last()
        var current: MutableMap<String, Any?> = root
        for (segment in segments.dropLast(1)) {
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
        current[leaf] = value
    }

    private fun parsePathSegments(rawPath: String): List<String> =
        rawPath.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}

private fun JsonObject.readString(key: String): String? =
    runCatching { get(key)?.takeIf { !it.isJsonNull }?.asString }.getOrNull()

private fun JsonObject.readDouble(key: String): Double? =
    runCatching { get(key)?.takeIf { !it.isJsonNull }?.asDouble }.getOrNull()

private fun JsonObject.readBoolean(key: String): Boolean? =
    runCatching { get(key)?.takeIf { !it.isJsonNull }?.asBoolean }.getOrNull()
