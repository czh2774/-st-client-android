package com.stproject.client.android.domain.model

data class A2UIAction(
    val name: String,
    val surfaceId: String?,
    val sourceComponentId: String?,
    val context: Map<String, Any?> = emptyMap(),
) {
    val normalizedName: String
        get() = name.trim().lowercase().replace("_", "").replace("-", "")

    fun contextString(vararg keys: String): String? {
        for (key in keys) {
            val raw = context[key] ?: continue
            when (raw) {
                is String -> raw.trim().takeIf { it.isNotEmpty() }?.let { return it }
                is Number -> return raw.toString()
                is Boolean -> return raw.toString()
            }
        }
        return null
    }
}

data class A2UIActionResult(
    val accepted: Boolean,
    val reason: String? = null,
)
