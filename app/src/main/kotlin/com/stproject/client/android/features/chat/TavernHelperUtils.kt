package com.stproject.client.android.features.chat

internal fun asStringKeyMap(value: Any?): Map<String, Any?> {
    if (value !is Map<*, *>) return emptyMap()
    val out = mutableMapOf<String, Any?>()
    for ((key, v) in value) {
        if (key is String) {
            out[key] = v
        }
    }
    return out
}

internal fun asMutableStringKeyMap(value: Any?): MutableMap<String, Any?> {
    val out = mutableMapOf<String, Any?>()
    if (value !is Map<*, *>) return out
    for ((key, v) in value) {
        if (key is String) {
            out[key] = v
        }
    }
    return out
}

internal fun parseTavernHelperSettings(value: Any?): Map<String, Any?>? {
    if (value is Map<*, *>) return asStringKeyMap(value)
    if (value !is List<*>) return null
    val out = mutableMapOf<String, Any?>()
    for (entry in value) {
        if (entry !is List<*> || entry.size < 2) continue
        val key = entry[0] as? String ?: continue
        val trimmed = key.trim()
        if (trimmed.isEmpty()) continue
        out[trimmed] = entry[1]
    }
    return out.takeIf { it.isNotEmpty() }
}

private fun mergeTavernHelperExtensions(
    extensions: Map<String, Any?>,
    inlineTavernHelper: Any?,
    legacyVariables: Any?,
): Map<String, Any?> {
    val needsInline = inlineTavernHelper != null && !extensions.containsKey("tavern_helper")
    val needsLegacy =
        legacyVariables != null && !extensions.containsKey("TavernHelper_characterScriptVariables")
    if (!needsInline && !needsLegacy) return extensions
    val next = extensions.toMutableMap()
    if (needsInline) {
        next["tavern_helper"] = inlineTavernHelper
    }
    if (needsLegacy) {
        next["TavernHelper_characterScriptVariables"] = legacyVariables
    }
    return next
}

internal fun extractTavernHelperVariables(wrapper: Map<String, Any>): Map<String, Any?> {
    val data = asStringKeyMap(wrapper["data"])
    val extensions = asStringKeyMap(data["extensions"])
    val merged =
        mergeTavernHelperExtensions(
            extensions,
            if (data.containsKey("tavern_helper")) data["tavern_helper"] else null,
            if (data.containsKey("TavernHelper_characterScriptVariables")) {
                data["TavernHelper_characterScriptVariables"]
            } else {
                null
            },
        )
    val tavernHelper = parseTavernHelperSettings(merged["tavern_helper"])
    if (tavernHelper != null) {
        val vars =
            tavernHelper["variables"]
                ?: tavernHelper["character_variables"]
                ?: tavernHelper["characterScriptVariables"]
        return asStringKeyMap(vars)
    }
    return asStringKeyMap(merged["TavernHelper_characterScriptVariables"])
}

internal fun updateTavernHelperWrapper(
    wrapper: Map<String, Any>,
    nextVars: Map<String, Any?>,
): Map<String, Any> {
    val data = asMutableStringKeyMap(wrapper["data"])
    val extensions = asMutableStringKeyMap(data["extensions"])
    val next = updateTavernHelperVariables(extensions["tavern_helper"], nextVars)
    extensions["tavern_helper"] = next
    data["extensions"] = extensions
    return wrapper.toMutableMap().apply { this["data"] = data }
}

internal fun updateTavernHelperVariables(
    raw: Any?,
    nextVars: Map<String, Any?>,
): Any {
    if (raw is List<*>) {
        var hasVars = false
        val next =
            raw.map { entry ->
                if (entry is List<*> && entry.size >= 2) {
                    val key = entry[0] as? String
                    val trimmed = key?.trim().orEmpty()
                    if (trimmed == "variables" ||
                        trimmed == "character_variables" ||
                        trimmed == "characterScriptVariables"
                    ) {
                        hasVars = true
                        listOf(entry[0], nextVars)
                    } else {
                        entry
                    }
                } else {
                    entry
                }
            }.toMutableList()
        if (!hasVars) {
            next.add(listOf("variables", nextVars))
        }
        return next
    }
    val base = asMutableStringKeyMap(raw)
    var updated = false
    if (base.containsKey("variables")) {
        base["variables"] = nextVars
        updated = true
    }
    if (base.containsKey("character_variables")) {
        base["character_variables"] = nextVars
        updated = true
    }
    if (base.containsKey("characterScriptVariables")) {
        base["characterScriptVariables"] = nextVars
        updated = true
    }
    if (!updated) {
        base["variables"] = nextVars
    }
    return base
}
