package com.stproject.client.android.features.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.stproject.client.android.core.a2ui.A2UIBindingResolver
import com.stproject.client.android.core.a2ui.A2UIComponent
import com.stproject.client.android.core.a2ui.A2UIRuntimeState
import com.stproject.client.android.core.a2ui.A2UISurfaceState
import com.stproject.client.android.domain.model.A2UIAction

private const val MAX_DEPTH = 8

@Composable
fun A2UISurfacesPanel(
    state: A2UIRuntimeState?,
    isBusy: Boolean,
    onAction: (A2UIAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaces = state?.asList().orEmpty()
    if (surfaces.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (surface in surfaces) {
            A2UISurfaceCard(
                surface = surface,
                isBusy = isBusy,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun A2UISurfaceCard(
    surface: A2UISurfaceState,
    isBusy: Boolean,
    onAction: (A2UIAction) -> Unit,
) {
    val rootId = surface.rootId?.trim().orEmpty()
    if (rootId.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            A2UINodeView(
                componentId = rootId,
                surface = surface,
                onAction = onAction,
                isBusy = isBusy,
                depth = 0,
                visited = mutableSetOf(),
            )
        }
    }
}

@Composable
private fun A2UINodeView(
    componentId: String,
    surface: A2UISurfaceState,
    onAction: (A2UIAction) -> Unit,
    isBusy: Boolean,
    depth: Int,
    visited: MutableSet<String>,
) {
    if (depth > MAX_DEPTH) return
    if (!visited.add(componentId)) return
    val component = surface.components[componentId] ?: return

    when (component.type) {
        "Text" -> renderText(component, surface)
        "Image" -> renderImage(component, surface)
        "Button" -> renderButton(component, surface, onAction, isBusy, depth, visited)
        "ChoiceButtons" -> renderChoiceButtons(component, surface, onAction, isBusy)
        "Form" -> renderForm(component, surface, onAction, isBusy)
        "Sheet" -> renderSheet(component, surface)
        "PurchaseCTA" -> renderPurchaseCTA(component, surface, onAction, isBusy)
        "OpenSettings" -> renderOpenSettings(component, surface, onAction, isBusy)
        "Column" -> renderColumn(component, surface, onAction, isBusy, depth, visited)
        "Row" -> renderRow(component, surface, onAction, isBusy, depth, visited)
        "Group" -> renderColumn(component, surface, onAction, isBusy, depth, visited)
        else -> Spacer(modifier = Modifier.height(0.dp))
    }

    visited.remove(componentId)
}

@Composable
private fun renderText(
    component: A2UIComponent,
    surface: A2UISurfaceState,
) {
    val text = A2UIBindingResolver.resolveString(component.props.get("text"), surface.dataModel).orEmpty()
    if (text.isBlank()) return
    val hint = component.props.readString("usageHint")
    val style =
        when (hint) {
            "h1" -> MaterialTheme.typography.headlineLarge
            "h2" -> MaterialTheme.typography.headlineMedium
            "h3" -> MaterialTheme.typography.headlineSmall
            "h4" -> MaterialTheme.typography.titleLarge
            "h5" -> MaterialTheme.typography.titleMedium
            "caption" -> MaterialTheme.typography.labelSmall
            else -> MaterialTheme.typography.bodyMedium
        }
    Text(text = text, style = style)
}

@Composable
private fun renderImage(
    component: A2UIComponent,
    surface: A2UISurfaceState,
) {
    val url = A2UIBindingResolver.resolveString(component.props.get("url"), surface.dataModel) ?: return
    val hint = component.props.readString("usageHint")
    val fit = component.props.readString("fit")
    val contentScale =
        when (fit) {
            "contain" -> ContentScale.Fit
            "cover" -> ContentScale.Crop
            "fill" -> ContentScale.FillBounds
            "none" -> ContentScale.Inside
            "scale-down" -> ContentScale.Inside
            else -> ContentScale.Fit
        }
    val size = imageSizeForHint(hint)
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier =
            if (size != null) {
                Modifier.size(size)
            } else {
                Modifier.fillMaxWidth()
            },
    )
}

@Composable
private fun renderButton(
    component: A2UIComponent,
    surface: A2UISurfaceState,
    onAction: (A2UIAction) -> Unit,
    isBusy: Boolean,
    depth: Int,
    visited: MutableSet<String>,
) {
    val childId = component.props.readString("child")?.trim().orEmpty()
    val actionSpec = parseActionSpec(component.props.getAsJsonObject("action"))
    Button(
        onClick = {
            actionSpec?.let { spec ->
                val action = buildAction(spec, surface, component.id)
                onAction(action)
            }
        },
        enabled = !isBusy && actionSpec != null,
    ) {
        if (childId.isNotEmpty()) {
            A2UINodeView(
                componentId = childId,
                surface = surface,
                onAction = onAction,
                isBusy = isBusy,
                depth = depth + 1,
                visited = visited,
            )
        }
    }
}

@Composable
private fun renderChoiceButtons(
    component: A2UIComponent,
    surface: A2UISurfaceState,
    onAction: (A2UIAction) -> Unit,
    isBusy: Boolean,
) {
    val prompt = A2UIBindingResolver.resolveString(component.props.get("prompt"), surface.dataModel)
    val choices = parseChoices(component.props.get("choices"), surface, component.id)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!prompt.isNullOrBlank()) {
            Text(text = prompt, style = MaterialTheme.typography.labelSmall)
        }
        choices.forEach { choice ->
            Button(
                onClick = { onAction(choice.action) },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(choice.label)
            }
        }
    }
}

@Composable
private fun renderForm(
    component: A2UIComponent,
    surface: A2UISurfaceState,
    onAction: (A2UIAction) -> Unit,
    isBusy: Boolean,
) {
    val fields = parseFormFields(component.props)
    if (fields.isEmpty()) return
    val textValues = remember(fields.map { it.id }) { mutableStateMapOf<String, String>() }
    val toggleValues = remember(fields.map { it.id }) { mutableStateMapOf<String, Boolean>() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        fields.forEach { field ->
            when (field.type) {
                FormFieldType.Toggle -> {
                    val checked = toggleValues[field.id] ?: (field.defaultValue as? Boolean ?: false)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = field.label, modifier = Modifier.weight(1f))
                        Switch(
                            checked = checked,
                            onCheckedChange = { toggleValues[field.id] = it },
                        )
                    }
                }
                FormFieldType.Number, FormFieldType.Text -> {
                    val initial =
                        textValues[field.id]
                            ?: field.defaultValue?.toString().orEmpty().also { textValues[field.id] = it }
                    val keyboardType =
                        if (field.type == FormFieldType.Number) {
                            KeyboardType.Decimal
                        } else {
                            KeyboardType.Text
                        }
                    OutlinedTextField(
                        value = initial,
                        onValueChange = { textValues[field.id] = it },
                        label = { Text(field.label) },
                        placeholder = field.placeholder?.let { { Text(it) } },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = keyboardType,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        val actionSpec = parseActionSpec(component.props.getAsJsonObject("action"))
        if (actionSpec != null) {
            val label =
                A2UIBindingResolver.resolveString(component.props.get("submitLabel"), surface.dataModel)
                    ?: "Submit"
            Button(
                onClick = {
                    val context = buildActionContext(actionSpec, surface)
                    context["formValues"] = buildFormValues(fields, textValues, toggleValues)
                    onAction(
                        A2UIAction(
                            name = actionSpec.name,
                            surfaceId = surface.surfaceId,
                            sourceComponentId = component.id,
                            context = context,
                        ),
                    )
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun renderSheet(
    component: A2UIComponent,
    surface: A2UISurfaceState,
) {
    val title = A2UIBindingResolver.resolveString(component.props.get("title"), surface.dataModel).orEmpty()
    val body = A2UIBindingResolver.resolveString(component.props.get("body"), surface.dataModel).orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (title.isNotBlank()) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
        }
        if (body.isNotBlank()) {
            Text(text = body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun renderPurchaseCTA(
    component: A2UIComponent,
    surface: A2UISurfaceState,
    onAction: (A2UIAction) -> Unit,
    isBusy: Boolean,
) {
    val label = A2UIBindingResolver.resolveString(component.props.get("label"), surface.dataModel) ?: "Purchase"
    val productId = component.props.readString("productId")?.trim().orEmpty()
    val kind = component.props.readString("kind")?.trim().orEmpty()
    Button(
        onClick = {
            onAction(
                A2UIAction(
                    name = "purchase",
                    surfaceId = surface.surfaceId,
                    sourceComponentId = component.id,
                    context =
                        mapOf(
                            "productId" to productId,
                            "kind" to kind,
                        ),
                ),
            )
        },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
    }
}

@Composable
private fun renderOpenSettings(
    component: A2UIComponent,
    surface: A2UISurfaceState,
    onAction: (A2UIAction) -> Unit,
    isBusy: Boolean,
) {
    val label = A2UIBindingResolver.resolveString(component.props.get("label"), surface.dataModel) ?: "Open"
    val destination = component.props.readString("destination")?.trim().orEmpty()
    Button(
        onClick = {
            onAction(
                A2UIAction(
                    name = "navigate",
                    surfaceId = surface.surfaceId,
                    sourceComponentId = component.id,
                    context = mapOf("destination" to destination),
                ),
            )
        },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
    }
}

@Composable
private fun renderColumn(
    component: A2UIComponent,
    surface: A2UISurfaceState,
    onAction: (A2UIAction) -> Unit,
    isBusy: Boolean,
    depth: Int,
    visited: MutableSet<String>,
) {
    val childrenSpec = parseChildrenSpec(component.props) ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (childrenSpec) {
            is ChildrenSpec.Explicit -> {
                childrenSpec.ids.forEach { id ->
                    renderChildWithWeight(
                        componentId = id,
                        surface = surface,
                        dataModelOverride = null,
                        onAction = onAction,
                        isBusy = isBusy,
                        depth = depth + 1,
                        visited = visited,
                    )
                }
            }
            is ChildrenSpec.Template -> {
                val items = resolveTemplateItems(childrenSpec, surface.dataModel)
                items.forEach { item ->
                    renderChildWithWeight(
                        componentId = childrenSpec.componentId,
                        surface = surface,
                        dataModelOverride = templateItemDataModel(item),
                        onAction = onAction,
                        isBusy = isBusy,
                        depth = depth + 1,
                        visited = visited,
                    )
                }
            }
        }
    }
}

@Composable
private fun renderRow(
    component: A2UIComponent,
    surface: A2UISurfaceState,
    onAction: (A2UIAction) -> Unit,
    isBusy: Boolean,
    depth: Int,
    visited: MutableSet<String>,
) {
    val childrenSpec = parseChildrenSpec(component.props) ?: return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (childrenSpec) {
            is ChildrenSpec.Explicit -> {
                childrenSpec.ids.forEach { id ->
                    renderChildWithWeight(
                        componentId = id,
                        surface = surface,
                        dataModelOverride = null,
                        onAction = onAction,
                        isBusy = isBusy,
                        depth = depth + 1,
                        visited = visited,
                    )
                }
            }
            is ChildrenSpec.Template -> {
                val items = resolveTemplateItems(childrenSpec, surface.dataModel)
                items.forEach { item ->
                    renderChildWithWeight(
                        componentId = childrenSpec.componentId,
                        surface = surface,
                        dataModelOverride = templateItemDataModel(item),
                        onAction = onAction,
                        isBusy = isBusy,
                        depth = depth + 1,
                        visited = visited,
                    )
                }
            }
        }
    }
}

private data class ActionContextSpec(
    val key: String,
    val value: JsonElement?,
)

private data class ActionSpec(
    val name: String,
    val context: List<ActionContextSpec>,
)

private fun parseActionSpec(obj: JsonObject?): ActionSpec? {
    if (obj == null) return null
    val name = obj.readString("name")?.trim().orEmpty()
    if (name.isEmpty()) return null
    val context =
        obj.getAsJsonArray("context")?.mapNotNull { entry ->
            val asObj = runCatching { entry.asJsonObject }.getOrNull() ?: return@mapNotNull null
            val key = asObj.readString("key")?.trim().orEmpty()
            if (key.isEmpty()) return@mapNotNull null
            ActionContextSpec(key = key, value = asObj.get("value"))
        } ?: emptyList()
    return ActionSpec(name = name, context = context)
}

private fun buildAction(
    spec: ActionSpec,
    surface: A2UISurfaceState,
    sourceComponentId: String,
): A2UIAction {
    val context = buildActionContext(spec, surface)
    return A2UIAction(
        name = spec.name,
        surfaceId = surface.surfaceId,
        sourceComponentId = sourceComponentId,
        context = context,
    )
}

private fun buildActionContext(
    spec: ActionSpec,
    surface: A2UISurfaceState,
): MutableMap<String, Any?> {
    val context = mutableMapOf<String, Any?>()
    for (entry in spec.context) {
        val resolved = A2UIBindingResolver.resolveValue(entry.value, surface.dataModel)
        if (resolved != null) {
            context[entry.key] = resolved
        }
    }
    return context
}

private sealed class ChildrenSpec {
    data class Explicit(val ids: List<String>) : ChildrenSpec()

    data class Template(
        val dataBinding: String,
        val componentId: String,
    ) : ChildrenSpec()
}

private fun parseChildrenSpec(props: JsonObject): ChildrenSpec? {
    val children = props.getAsJsonObject("children") ?: return null
    val explicit = parseExplicitChildren(children)
    if (explicit.isNotEmpty()) {
        return ChildrenSpec.Explicit(explicit)
    }
    val template = children.getAsJsonObject("template") ?: return null
    val dataBinding = template.readString("dataBinding")?.trim().orEmpty()
    val componentId = template.readString("componentId")?.trim().orEmpty()
    if (dataBinding.isEmpty() || componentId.isEmpty()) return null
    return ChildrenSpec.Template(dataBinding = dataBinding, componentId = componentId)
}

private fun parseExplicitChildren(children: JsonObject): List<String> {
    val list = children.getAsJsonArray("explicitList") ?: return emptyList()
    return list.mapNotNull { runCatching { it.asString.trim() }.getOrNull() }
        .filter { it.isNotEmpty() }
}

private fun resolveTemplateItems(
    spec: ChildrenSpec.Template,
    dataModel: Map<String, Any?>,
): List<Any?> {
    val binding = JsonObject().apply { addProperty("path", spec.dataBinding) }
    val resolved = A2UIBindingResolver.resolveValue(binding, dataModel)
    val list = resolved as? List<*> ?: return emptyList()
    return list.filterNotNull()
}

private fun templateItemDataModel(item: Any?): Map<String, Any?> {
    return when (item) {
        is Map<*, *> -> item.entries.associate { it.key.toString() to it.value }
        else -> mapOf("value" to item)
    }
}

private fun componentWeight(
    surface: A2UISurfaceState,
    componentId: String,
): Float? {
    val weight = surface.components[componentId]?.weight ?: return null
    val floatWeight = weight.toFloat()
    return floatWeight.takeIf { it > 0f }
}

@Composable
private fun ColumnScope.renderChildWithWeight(
    componentId: String,
    surface: A2UISurfaceState,
    dataModelOverride: Map<String, Any?>?,
    onAction: (A2UIAction) -> Unit,
    isBusy: Boolean,
    depth: Int,
    visited: MutableSet<String>,
) {
    val weight = componentWeight(surface, componentId)
    val childSurface = dataModelOverride?.let { surface.copy(dataModel = it) } ?: surface
    if (weight != null) {
        Box(modifier = Modifier.weight(weight)) {
            A2UINodeView(
                componentId = componentId,
                surface = childSurface,
                onAction = onAction,
                isBusy = isBusy,
                depth = depth,
                visited = visited,
            )
        }
    } else {
        A2UINodeView(
            componentId = componentId,
            surface = childSurface,
            onAction = onAction,
            isBusy = isBusy,
            depth = depth,
            visited = visited,
        )
    }
}

@Composable
private fun RowScope.renderChildWithWeight(
    componentId: String,
    surface: A2UISurfaceState,
    dataModelOverride: Map<String, Any?>?,
    onAction: (A2UIAction) -> Unit,
    isBusy: Boolean,
    depth: Int,
    visited: MutableSet<String>,
) {
    val weight = componentWeight(surface, componentId)
    val childSurface = dataModelOverride?.let { surface.copy(dataModel = it) } ?: surface
    if (weight != null) {
        Box(modifier = Modifier.weight(weight)) {
            A2UINodeView(
                componentId = componentId,
                surface = childSurface,
                onAction = onAction,
                isBusy = isBusy,
                depth = depth,
                visited = visited,
            )
        }
    } else {
        A2UINodeView(
            componentId = componentId,
            surface = childSurface,
            onAction = onAction,
            isBusy = isBusy,
            depth = depth,
            visited = visited,
        )
    }
}

private data class Choice(
    val label: String,
    val action: A2UIAction,
)

private fun parseChoices(
    element: JsonElement?,
    surface: A2UISurfaceState,
    sourceComponentId: String,
): List<Choice> {
    val list = element?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
    return list.mapNotNull { item ->
        val obj = runCatching { item.asJsonObject }.getOrNull() ?: return@mapNotNull null
        val label = A2UIBindingResolver.resolveString(obj.get("label"), surface.dataModel)?.trim().orEmpty()
        if (label.isEmpty()) return@mapNotNull null
        val actionSpec = parseActionSpec(obj.getAsJsonObject("action")) ?: return@mapNotNull null
        val action =
            A2UIAction(
                name = actionSpec.name,
                surfaceId = surface.surfaceId,
                sourceComponentId = sourceComponentId,
                context =
                    buildActionContext(actionSpec, surface),
            )
        Choice(label = label, action = action)
    }
}

private enum class FormFieldType {
    Text,
    Number,
    Toggle,
}

private data class FormField(
    val id: String,
    val label: String,
    val type: FormFieldType,
    val placeholder: String?,
    val defaultValue: Any?,
)

private fun parseFormFields(props: JsonObject): List<FormField> {
    val fields = props.getAsJsonArray("fields") ?: return emptyList()
    return fields.mapNotNull { item ->
        val obj = runCatching { item.asJsonObject }.getOrNull() ?: return@mapNotNull null
        val id = obj.readString("id")?.trim().orEmpty()
        val label = obj.readString("label")?.trim().orEmpty()
        val typeRaw = obj.readString("type")?.trim().orEmpty()
        if (id.isEmpty() || label.isEmpty()) return@mapNotNull null
        val type =
            when (typeRaw.lowercase()) {
                "number" -> FormFieldType.Number
                "toggle" -> FormFieldType.Toggle
                else -> FormFieldType.Text
            }
        val placeholder = obj.readString("placeholder")
        val defaultValue = obj.get("defaultValue")?.let { parseJsonPrimitive(it) }
        FormField(id = id, label = label, type = type, placeholder = placeholder, defaultValue = defaultValue)
    }
}

private fun buildFormValues(
    fields: List<FormField>,
    textValues: Map<String, String>,
    toggleValues: Map<String, Boolean>,
): Map<String, Any?> {
    val out = mutableMapOf<String, Any?>()
    for (field in fields) {
        when (field.type) {
            FormFieldType.Toggle -> out[field.id] = toggleValues[field.id] ?: false
            FormFieldType.Number -> {
                val raw = textValues[field.id]?.trim().orEmpty()
                out[field.id] = raw.toDoubleOrNull() ?: raw
            }
            FormFieldType.Text -> out[field.id] = textValues[field.id]?.trim().orEmpty()
        }
    }
    return out
}

private fun parseJsonPrimitive(element: JsonElement): Any? {
    if (element.isJsonPrimitive) {
        val primitive = element.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isNumber -> primitive.asNumber.toDouble()
            primitive.isString -> primitive.asString
            else -> null
        }
    }
    return null
}

private fun imageSizeForHint(hint: String?): androidx.compose.ui.unit.Dp? {
    return when (hint) {
        "icon" -> 24.dp
        "avatar" -> 40.dp
        "smallFeature" -> 96.dp
        "mediumFeature" -> 140.dp
        "largeFeature" -> 200.dp
        else -> null
    }
}

private fun JsonObject.readString(key: String): String? =
    runCatching { get(key)?.takeIf { !it.isJsonNull }?.asString }.getOrNull()
