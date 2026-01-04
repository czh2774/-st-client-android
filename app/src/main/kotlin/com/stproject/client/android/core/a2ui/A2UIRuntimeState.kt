package com.stproject.client.android.core.a2ui

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import timber.log.Timber
import java.util.ArrayDeque

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
    private data class BoundDefault(
        val path: String,
        val value: Any,
    )

    private data class TemplateBinding(
        val componentId: String,
        val dataBinding: String,
    )

    private data class ComponentNode(
        val explicitChildren: List<String>,
        val template: TemplateBinding?,
    )

    private data class ComponentDefaults(
        val absolute: List<BoundDefault>,
        val relative: List<BoundDefault>,
    ) {
        val isEmpty: Boolean
            get() = absolute.isEmpty() && relative.isEmpty()
    }

    fun reduce(
        state: A2UIRuntimeState,
        message: A2UIMessage,
    ): A2UIRuntimeState {
        val validation = A2UIProtocolValidator.validateMessage(message)
        if (!validation.isValid) {
            Timber.w("A2UI message rejected: %s", validation.reason)
            return state
        }
        val surfaces = state.surfaces.toMutableMap()

        message.deleteSurface?.surfaceId?.trim()?.takeIf { it.isNotEmpty() }?.let { surfaceId ->
            surfaces.remove(surfaceId)
        }

        message.surfaceUpdate?.let { update ->
            val surfaceId = update.surfaceId?.trim().orEmpty()
            if (surfaceId.isNotEmpty()) {
                var next =
                    updateSurfaceComponents(
                        current = surfaces[surfaceId],
                        surfaceId = surfaceId,
                        components = update.components.orEmpty(),
                    )
                next = applyBoundDefaults(next)
                surfaces[surfaceId] = next
            }
        }

        message.beginRendering?.let { begin ->
            val surfaceId = begin.surfaceId?.trim().orEmpty()
            if (surfaceId.isNotEmpty()) {
                val current = surfaces[surfaceId] ?: A2UISurfaceState(surfaceId = surfaceId)
                logCatalogIdIfNeeded(current, begin)
                logStylesIfNeeded(current, begin)
                var next =
                    current.copy(
                        rootId = begin.root?.trim()?.takeIf { it.isNotEmpty() },
                        catalogId = begin.catalogId?.trim()?.takeIf { it.isNotEmpty() },
                        styles = begin.styles?.takeIf { it.entrySet().isNotEmpty() },
                    )
                next = applyBoundDefaults(next)
                surfaces[surfaceId] = next
            }
        }

        message.dataModelUpdate?.let { update ->
            val surfaceId = update.surfaceId?.trim().orEmpty()
            if (surfaceId.isNotEmpty()) {
                val current = surfaces[surfaceId] ?: A2UISurfaceState(surfaceId = surfaceId)
                var next = applyDataModelUpdate(current, update)
                next = applyBoundDefaults(next)
                surfaces[surfaceId] = next
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
            if (!A2UICatalog.supportsComponent(type)) {
                Timber.w("A2UI unsupported component type=%s id=%s", type, componentId)
                continue
            }
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

    private fun applyBoundDefaults(surface: A2UISurfaceState): A2UISurfaceState {
        if (surface.components.isEmpty()) return surface
        val defaultsByComponent = mutableMapOf<String, ComponentDefaults>()
        val graph = buildComponentGraph(surface.components)
        val rootReachable = collectExplicitReachable(surface.rootId, graph)
        val templateBindings = collectTemplateBindings(surface.rootId, graph)

        for ((id, component) in surface.components) {
            val defaults = collectBoundDefaults(component.props)
            if (!defaults.isEmpty) {
                defaultsByComponent[id] = defaults
            }
        }

        if (defaultsByComponent.isEmpty()) return surface
        val root = surface.dataModel.toMutableMap()
        var mutated = false

        for (defaults in defaultsByComponent.values) {
            if (applyDefaultsToRoot(root, defaults.absolute)) {
                mutated = true
            }
        }

        for (id in rootReachable) {
            val defaults = defaultsByComponent[id] ?: continue
            if (defaults.relative.isEmpty()) continue
            if (applyDefaultsToRoot(root, defaults.relative)) {
                mutated = true
            }
        }

        for (binding in templateBindings) {
            val templateReachable = collectExplicitReachable(binding.componentId, graph)
            if (templateReachable.isEmpty()) continue
            val templateDefaults =
                templateReachable.flatMap { id ->
                    defaultsByComponent[id]?.relative.orEmpty()
                }
            if (templateDefaults.isEmpty()) continue
            if (applyTemplateDefaults(root, binding.dataBinding, templateDefaults)) {
                mutated = true
            }
        }

        return if (mutated) surface.copy(dataModel = root) else surface
    }

    private fun collectBoundDefaults(props: JsonObject): ComponentDefaults {
        val absolute = mutableListOf<BoundDefault>()
        val relative = mutableListOf<BoundDefault>()

        fun visit(element: JsonElement?) {
            if (element == null || element.isJsonNull) return
            if (element.isJsonArray) {
                element.asJsonArray.forEach { visit(it) }
                return
            }
            if (!element.isJsonObject) return
            val obj = element.asJsonObject
            val path = obj.readString("path")?.trim().orEmpty()
            if (path.isNotEmpty()) {
                val literal = extractLiteralValue(obj)
                if (literal != null) {
                    val target = if (path.startsWith("/")) absolute else relative
                    target.add(BoundDefault(path = path, value = literal))
                    return
                }
            }
            for ((_, value) in obj.entrySet()) {
                visit(value)
            }
        }

        visit(props)
        return ComponentDefaults(absolute = absolute, relative = relative)
    }

    private fun extractTemplateBinding(props: JsonObject): TemplateBinding? {
        val children = props.getAsJsonObject("children") ?: return null
        val template = children.getAsJsonObject("template") ?: return null
        val dataBinding = template.readString("dataBinding")?.trim().orEmpty()
        val componentId = template.readString("componentId")?.trim().orEmpty()
        if (dataBinding.isEmpty() || componentId.isEmpty()) return null
        return TemplateBinding(componentId = componentId, dataBinding = dataBinding)
    }

    private fun extractExplicitChildren(props: JsonObject): List<String> {
        val children = props.getAsJsonObject("children") ?: return emptyList()
        val explicitList = children.getAsJsonArray("explicitList") ?: return emptyList()
        return explicitList.mapNotNull { runCatching { it.asString.trim() }.getOrNull() }
            .filter { it.isNotEmpty() }
    }

    private fun buildComponentGraph(
        components: Map<String, A2UIComponent>,
    ): Map<String, ComponentNode> {
        val graph = mutableMapOf<String, ComponentNode>()
        for ((id, component) in components) {
            val explicit = extractExplicitChildren(component.props)
            val template = if (explicit.isEmpty()) extractTemplateBinding(component.props) else null
            graph[id] = ComponentNode(explicitChildren = explicit, template = template)
        }
        return graph
    }

    private fun collectExplicitReachable(
        startId: String?,
        graph: Map<String, ComponentNode>,
    ): Set<String> {
        val start = startId?.trim()?.takeIf { it.isNotEmpty() } ?: return emptySet()
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<String>()
        stack.add(start)
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (!visited.add(id)) continue
            val children = graph[id]?.explicitChildren.orEmpty()
            for (child in children) {
                val trimmed = child.trim()
                if (trimmed.isNotEmpty()) {
                    stack.add(trimmed)
                }
            }
        }
        return visited
    }

    private fun collectTemplateBindings(
        startId: String?,
        graph: Map<String, ComponentNode>,
    ): List<TemplateBinding> {
        val start = startId?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<String>()
        val bindings = mutableListOf<TemplateBinding>()
        stack.add(start)
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (!visited.add(id)) continue
            val node = graph[id] ?: continue
            node.template?.let { bindings.add(it) }
            for (child in node.explicitChildren) {
                val trimmed = child.trim()
                if (trimmed.isNotEmpty()) {
                    stack.add(trimmed)
                }
            }
        }
        return bindings
    }

    private fun applyDefaultsToRoot(
        root: MutableMap<String, Any?>,
        defaults: List<BoundDefault>,
    ): Boolean {
        if (defaults.isEmpty()) return false
        var mutated = false
        for (defaultValue in defaults) {
            if (setDefaultAtPath(root, defaultValue.path, defaultValue.value)) {
                mutated = true
            }
        }
        return mutated
    }

    private fun applyTemplateDefaults(
        root: MutableMap<String, Any?>,
        dataBinding: String,
        defaults: List<BoundDefault>,
    ): Boolean {
        if (defaults.isEmpty()) return false
        val resolved = A2UIJsonPointer.resolve(dataBinding, root) as? List<*> ?: return false
        val mutableList = resolved.toMutableList()
        var itemMutated = false

        for (idx in mutableList.indices) {
            val item = mutableList[idx]
            val rootItem =
                when (item) {
                    is Map<*, *> -> toMutableStringMap(item)
                    is List<*> -> item.toMutableList()
                    else -> mutableMapOf("value" to item)
                } ?: continue
            val changed = applyDefaultsToContainer(rootItem, defaults)
            if (changed) {
                mutableList[idx] = rootItem
                itemMutated = true
            }
        }

        if (!itemMutated) return false
        return setValueAtPath(root, dataBinding, mutableList)
    }

    private fun applyDefaultsToContainer(
        root: Any,
        defaults: List<BoundDefault>,
    ): Boolean {
        if (defaults.isEmpty()) return false
        var mutated = false
        for (defaultValue in defaults) {
            if (setDefaultAtPath(root, defaultValue.path, defaultValue.value)) {
                mutated = true
            }
        }
        return mutated
    }

    private fun setDefaultAtPath(
        root: Any,
        pointer: String,
        value: Any?,
    ): Boolean {
        if (pointer.isBlank()) return false
        if (A2UIJsonPointer.resolve(pointer, root) != null) return false
        return setValueAtPath(root, pointer, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun setValueAtPath(
        root: Any,
        pointer: String,
        value: Any?,
    ): Boolean {
        val segments = A2UIJsonPointer.parse(pointer)
        if (segments.isEmpty()) return false
        var current: Any? = root
        var parentMap: MutableMap<String, Any?>? = null
        var parentList: MutableList<Any?>? = null
        var parentKey: String? = null
        var parentIndex: Int? = null

        fun attach(newValue: Any) {
            when {
                parentMap != null && parentKey != null -> parentMap!![parentKey!!] = newValue
                parentList != null && parentIndex != null -> parentList!![parentIndex!!] = newValue
            }
            current = newValue
        }

        for (i in segments.indices) {
            val segment = segments[i]
            val isLast = i == segments.lastIndex
            when (current) {
                is Map<*, *> -> {
                    val map =
                        if (current === root) {
                            root as? MutableMap<String, Any?> ?: return false
                        } else {
                            toMutableStringMap(current) ?: return false
                        }
                    if (current !== map) {
                        if (parentMap == null && parentList == null) return false
                        attach(map)
                    }
                    if (isLast) {
                        map[segment] = value
                        return true
                    }
                    val nextSegment = segments[i + 1]
                    val nextIsIndex = nextSegment.toIntOrNull() != null
                    val existing = map[segment]
                    val next =
                        when (existing) {
                            null -> if (nextIsIndex) mutableListOf<Any?>() else mutableMapOf<String, Any?>()
                            is Map<*, *> -> toMutableStringMap(existing) ?: return false
                            is List<*> -> toMutableList(existing) ?: return false
                            else -> return false
                        }
                    map[segment] = next
                    parentMap = map
                    parentKey = segment
                    parentList = null
                    parentIndex = null
                    current = next
                }
                is List<*> -> {
                    val list =
                        if (current === root) {
                            root as? MutableList<Any?> ?: return false
                        } else {
                            toMutableList(current) ?: return false
                        }
                    if (current !== list) {
                        if (parentMap == null && parentList == null) return false
                        attach(list)
                    }
                    val idx = segment.toIntOrNull() ?: return false
                    if (idx < 0) return false
                    while (list.size <= idx) {
                        list.add(null)
                    }
                    if (isLast) {
                        list[idx] = value
                        return true
                    }
                    val nextSegment = segments[i + 1]
                    val nextIsIndex = nextSegment.toIntOrNull() != null
                    val existing = list[idx]
                    val next =
                        when (existing) {
                            null -> if (nextIsIndex) mutableListOf<Any?>() else mutableMapOf<String, Any?>()
                            is Map<*, *> -> toMutableStringMap(existing) ?: return false
                            is List<*> -> toMutableList(existing) ?: return false
                            else -> return false
                        }
                    list[idx] = next
                    parentList = list
                    parentIndex = idx
                    parentMap = null
                    parentKey = null
                    current = next
                }
                else -> return false
            }
        }
        return false
    }

    private fun extractLiteralValue(obj: JsonObject): Any? {
        obj.readString("literalString")?.let { return it }
        obj.readString("valueString")?.let { return it }
        obj.readDouble("literalNumber")?.let { return it }
        obj.readDouble("valueNumber")?.let { return it }
        obj.readBoolean("literalBoolean")?.let { return it }
        obj.readBoolean("valueBoolean")?.let { return it }
        obj.getAsJsonArray("literalArray")?.let { array ->
            return array.map { parseLiteralElement(it) }
        }
        obj.getAsJsonArray("valueList")?.let { array ->
            return array.map { parseLiteralElement(it) }
        }
        return null
    }

    private fun parseLiteralElement(element: JsonElement?): Any? {
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
            return element.asJsonArray.map { parseLiteralElement(it) }
        }
        if (element.isJsonObject) {
            val mapped = mutableMapOf<String, Any?>()
            for ((k, v) in element.asJsonObject.entrySet()) {
                mapped[k] = parseLiteralElement(v)
            }
            return mapped
        }
        return null
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
        val target = ensureMapAtPath(root, path) ?: return current
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

    private fun ensureMapAtPath(
        root: MutableMap<String, Any?>,
        path: String,
    ): MutableMap<String, Any?>? {
        val segments = A2UIJsonPointer.parse(path)
        if (segments.isEmpty()) return root
        var current: Any? = root
        var parentMap: MutableMap<String, Any?>? = null
        var parentList: MutableList<Any?>? = null
        var parentKey: String? = null
        var parentIndex: Int? = null

        fun attach(newValue: Any) {
            when {
                parentMap != null && parentKey != null -> parentMap!![parentKey!!] = newValue
                parentList != null && parentIndex != null -> parentList!![parentIndex!!] = newValue
            }
            current = newValue
        }

        for (i in segments.indices) {
            val segment = segments[i]
            val isLast = i == segments.lastIndex
            when (current) {
                is Map<*, *> -> {
                    val map = if (current === root) root else toMutableStringMap(current) ?: return null
                    if (current !== map) {
                        attach(map)
                    }
                    val existing = map[segment]
                    if (isLast) {
                        val target = toMutableStringMap(existing) ?: mutableMapOf()
                        map[segment] = target
                        return target
                    }
                    val nextSegment = segments[i + 1]
                    val nextIsIndex = nextSegment.toIntOrNull() != null
                    val next =
                        when (existing) {
                            null -> if (nextIsIndex) mutableListOf<Any?>() else mutableMapOf<String, Any?>()
                            is Map<*, *> -> toMutableStringMap(existing)!!
                            is List<*> -> toMutableList(existing)!!
                            else -> return null
                        }
                    map[segment] = next
                    parentMap = map
                    parentKey = segment
                    parentList = null
                    parentIndex = null
                    current = next
                }
                is List<*> -> {
                    val list = toMutableList(current) ?: return null
                    attach(list)
                    val idx = segment.toIntOrNull() ?: return null
                    if (idx < 0) return null
                    while (list.size <= idx) {
                        list.add(null)
                    }
                    val existing = list[idx]
                    if (isLast) {
                        val target = toMutableStringMap(existing) ?: mutableMapOf()
                        list[idx] = target
                        return target
                    }
                    val nextSegment = segments[i + 1]
                    val nextIsIndex = nextSegment.toIntOrNull() != null
                    val next =
                        when (existing) {
                            null -> if (nextIsIndex) mutableListOf<Any?>() else mutableMapOf<String, Any?>()
                            is Map<*, *> -> toMutableStringMap(existing)!!
                            is List<*> -> toMutableList(existing)!!
                            else -> return null
                        }
                    list[idx] = next
                    parentList = list
                    parentIndex = idx
                    parentMap = null
                    parentKey = null
                    current = next
                }
                else -> return null
            }
        }
        return null
    }

    private fun toMutableStringMap(value: Any?): MutableMap<String, Any?>? {
        return when (value) {
            is Map<*, *> -> value.entries.associate { it.key.toString() to it.value }.toMutableMap()
            else -> null
        }
    }

    private fun toMutableList(value: Any?): MutableList<Any?>? {
        return when (value) {
            is List<*> -> value.toMutableList()
            else -> null
        }
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
