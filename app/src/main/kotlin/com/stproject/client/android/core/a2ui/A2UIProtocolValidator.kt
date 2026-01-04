package com.stproject.client.android.core.a2ui

import com.stproject.client.android.domain.model.A2UIAction

internal data class A2UIValidationResult(
    val isValid: Boolean,
    val reason: String,
)

internal object A2UIProtocolValidator {
    fun validateMessage(message: A2UIMessage): A2UIValidationResult {
        val actionCount =
            listOf(
                message.beginRendering,
                message.surfaceUpdate,
                message.dataModelUpdate,
                message.deleteSurface,
            ).count { it != null }
        if (actionCount != 1) {
            return invalid("message must contain exactly one action")
        }

        message.beginRendering?.let { begin ->
            if (begin.surfaceId.isNullOrBlank() || begin.root.isNullOrBlank()) {
                return invalid("beginRendering requires surfaceId and root")
            }
        }

        message.surfaceUpdate?.let { update ->
            if (update.surfaceId.isNullOrBlank()) {
                return invalid("surfaceUpdate requires surfaceId")
            }
            val components = update.components
            if (components.isNullOrEmpty()) {
                return invalid("surfaceUpdate requires components")
            }
            if (components.any { it.id.isNullOrBlank() || it.component == null }) {
                return invalid("surfaceUpdate contains invalid component definitions")
            }
        }

        message.dataModelUpdate?.let { update ->
            if (update.surfaceId.isNullOrBlank()) {
                return invalid("dataModelUpdate requires surfaceId")
            }
            val contents = update.contents
            if (contents.isNullOrEmpty()) {
                return invalid("dataModelUpdate requires contents")
            }
            if (!contents.all { isValidEntry(it) }) {
                return invalid("dataModelUpdate entries must contain exactly one value")
            }
        }

        message.deleteSurface?.let { delete ->
            if (delete.surfaceId.isNullOrBlank()) {
                return invalid("deleteSurface requires surfaceId")
            }
        }

        return valid()
    }

    fun validateAction(action: A2UIAction): A2UIValidationResult {
        val name = action.name.trim()
        if (name.isEmpty()) {
            return invalid("missing_action")
        }
        if (!A2UICatalog.supportsAction(name)) {
            return invalid("action_not_supported")
        }
        if (action.surfaceId.isNullOrBlank()) {
            return invalid("missing_surface_id")
        }
        if (action.sourceComponentId.isNullOrBlank()) {
            return invalid("missing_source_component_id")
        }
        return valid()
    }

    private fun isValidEntry(entry: A2UIDataEntry): Boolean {
        if (entry.key.isNullOrBlank()) return false
        val valueCount =
            listOf(
                entry.valueString,
                entry.valueNumber,
                entry.valueBoolean,
                entry.valueMap,
                entry.valueList,
            ).count { it != null }
        if (valueCount != 1) return false
        entry.valueMap?.let { mapEntries ->
            if (!mapEntries.all { isValidEntry(it) }) return false
        }
        return true
    }

    private fun invalid(reason: String) = A2UIValidationResult(isValid = false, reason = reason)

    private fun valid() = A2UIValidationResult(isValid = true, reason = "")
}
