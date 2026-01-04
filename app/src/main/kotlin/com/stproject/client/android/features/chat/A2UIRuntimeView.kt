package com.stproject.client.android.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.stproject.client.android.core.a2ui.A2UIBindingResolver
import com.stproject.client.android.core.a2ui.A2UICatalog
import com.stproject.client.android.core.a2ui.A2UIComponent
import com.stproject.client.android.core.a2ui.A2UIRuntimeState
import com.stproject.client.android.core.a2ui.A2UISurfaceState
import com.stproject.client.android.domain.model.A2UIAction
import timber.log.Timber

private const val DEFAULT_MAX_DEPTH = 8

@Composable
fun A2UISurfacesPanel(
    state: A2UIRuntimeState?,
    isBusy: Boolean,
    onAction: (A2UIAction) -> Unit,
    maxDepth: Int = DEFAULT_MAX_DEPTH,
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
                maxDepth = maxDepth,
            )
        }
    }
}

@Composable
private fun A2UISurfaceCard(
    surface: A2UISurfaceState,
    isBusy: Boolean,
    onAction: (A2UIAction) -> Unit,
    maxDepth: Int,
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
                maxDepth = maxDepth,
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
    maxDepth: Int,
    visited: MutableSet<String>,
) {
    if (depth > maxDepth) {
        Timber.w(
            "A2UI render depth exceeded (surface=%s component=%s depth=%d max=%d)",
            surface.surfaceId,
            componentId,
            depth,
            maxDepth,
        )
        return
    }
    if (!visited.add(componentId)) return
    val component = surface.components[componentId] ?: return

    when (component.type) {
        A2UICatalog.Components.TEXT -> renderText(component, surface)
        A2UICatalog.Components.IMAGE -> renderImage(component, surface)
        A2UICatalog.Components.BUTTON -> renderButton(component, surface, onAction, isBusy, depth, maxDepth, visited)
        A2UICatalog.Components.CHOICE_BUTTONS -> renderChoiceButtons(component, surface, onAction, isBusy)
        A2UICatalog.Components.FORM -> renderForm(component, surface, onAction, isBusy)
        A2UICatalog.Components.SHEET -> renderSheet(component, surface)
        A2UICatalog.Components.PURCHASE_CTA -> renderPurchaseCTA(component, surface, onAction, isBusy)
        A2UICatalog.Components.OPEN_SETTINGS -> renderOpenSettings(component, surface, onAction, isBusy)
        A2UICatalog.Components.COLUMN -> renderColumn(component, surface, onAction, isBusy, depth, maxDepth, visited)
        A2UICatalog.Components.ROW -> renderRow(component, surface, onAction, isBusy, depth, maxDepth, visited)
        A2UICatalog.Components.GROUP -> renderColumn(component, surface, onAction, isBusy, depth, maxDepth, visited)
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
    val styleSpec = resolveStyle(component, surface)
    val hint = component.props.readString("usageHint")
    val baseStyle =
        when (hint) {
            "h1" -> MaterialTheme.typography.headlineLarge
            "h2" -> MaterialTheme.typography.headlineMedium
            "h3" -> MaterialTheme.typography.headlineSmall
            "h4" -> MaterialTheme.typography.titleLarge
            "h5" -> MaterialTheme.typography.titleMedium
            "caption" -> MaterialTheme.typography.labelSmall
            else -> MaterialTheme.typography.bodyMedium
        }
    val style = applyTextStyle(baseStyle, styleSpec)
    val modifier = styleModifier(styleSpec)
    val color = styleSpec?.textColor ?: Color.Unspecified
    Text(text = text, style = style, color = color, modifier = modifier)
}

@Composable
private fun renderImage(
    component: A2UIComponent,
    surface: A2UISurfaceState,
) {
    val url = A2UIBindingResolver.resolveString(component.props.get("url"), surface.dataModel) ?: return
    val styleSpec = resolveStyle(component, surface)
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
    val baseModifier = styleModifier(styleSpec, clipContent = styleSpec?.cornerRadius != null)
    val sizedModifier =
        if (size != null) {
            Modifier.size(size)
        } else {
            Modifier.fillMaxWidth()
        }
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier =
            sizedModifier.then(baseModifier),
    )
}

@Composable
private fun renderButton(
    component: A2UIComponent,
    surface: A2UISurfaceState,
    onAction: (A2UIAction) -> Unit,
    isBusy: Boolean,
    depth: Int,
    maxDepth: Int,
    visited: MutableSet<String>,
) {
    val childId = component.props.readString("child")?.trim().orEmpty()
    val actionSpec = parseActionSpec(component.props.getAsJsonObject("action"))
    val styleSpec = resolveStyle(component, surface)
    val modifier = styleModifier(styleSpec, includeBackground = false)
    val colors = buttonColors(styleSpec)
    val shape = buttonShape(styleSpec)
    Button(
        onClick = {
            actionSpec?.let { spec ->
                val action = buildAction(spec, surface, component.id)
                onAction(action)
            }
        },
        enabled = !isBusy && actionSpec != null,
        modifier = modifier,
        colors = colors ?: ButtonDefaults.buttonColors(),
        shape = shape ?: MaterialTheme.shapes.small,
    ) {
        if (childId.isNotEmpty()) {
            A2UINodeView(
                componentId = childId,
                surface = surface,
                onAction = onAction,
                isBusy = isBusy,
                depth = depth + 1,
                maxDepth = maxDepth,
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
    val styleSpec = resolveStyle(component, surface)
    val spacing = styleSpacing(styleSpec, 6.dp)
    Column(
        modifier = styleModifier(styleSpec),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        if (!prompt.isNullOrBlank()) {
            Text(text = prompt, style = MaterialTheme.typography.labelSmall)
        }
        choices.forEach { choice ->
            Button(
                onClick = { onAction(choice.action) },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                colors = buttonColors(styleSpec) ?: ButtonDefaults.buttonColors(),
                shape = buttonShape(styleSpec) ?: MaterialTheme.shapes.small,
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

    val styleSpec = resolveStyle(component, surface)
    val spacing = styleSpacing(styleSpec, 8.dp)
    Column(
        modifier = styleModifier(styleSpec),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
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
                colors = buttonColors(styleSpec) ?: ButtonDefaults.buttonColors(),
                shape = buttonShape(styleSpec) ?: MaterialTheme.shapes.small,
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
    val styleSpec = resolveStyle(component, surface)
    val spacing = styleSpacing(styleSpec, 4.dp)
    Column(
        modifier = styleModifier(styleSpec),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
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
    val styleSpec = resolveStyle(component, surface)
    Button(
        onClick = {
            onAction(
                A2UIAction(
                    name = A2UICatalog.Actions.PURCHASE,
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
        modifier = Modifier.fillMaxWidth().then(styleModifier(styleSpec, includeBackground = false)),
        colors = buttonColors(styleSpec) ?: ButtonDefaults.buttonColors(),
        shape = buttonShape(styleSpec) ?: MaterialTheme.shapes.small,
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
    val styleSpec = resolveStyle(component, surface)
    Button(
        onClick = {
            onAction(
                A2UIAction(
                    name = A2UICatalog.Actions.NAVIGATE,
                    surfaceId = surface.surfaceId,
                    sourceComponentId = component.id,
                    context = mapOf("destination" to destination),
                ),
            )
        },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().then(styleModifier(styleSpec, includeBackground = false)),
        colors = buttonColors(styleSpec) ?: ButtonDefaults.buttonColors(),
        shape = buttonShape(styleSpec) ?: MaterialTheme.shapes.small,
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
    maxDepth: Int,
    visited: MutableSet<String>,
) {
    val childrenSpec = parseChildrenSpec(component.props) ?: return
    val styleSpec = resolveStyle(component, surface)
    val spacing = styleSpacing(styleSpec, 8.dp)
    Column(
        modifier = styleModifier(styleSpec),
        verticalArrangement = Arrangement.spacedBy(spacing),
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
                        maxDepth = maxDepth,
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
                        dataModelOverride = templateItemDataModel(item, surface.dataModel),
                        onAction = onAction,
                        isBusy = isBusy,
                        depth = depth + 1,
                        maxDepth = maxDepth,
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
    maxDepth: Int,
    visited: MutableSet<String>,
) {
    val childrenSpec = parseChildrenSpec(component.props) ?: return
    val styleSpec = resolveStyle(component, surface)
    val spacing = styleSpacing(styleSpec, 8.dp)
    Row(
        modifier = styleModifier(styleSpec),
        horizontalArrangement = Arrangement.spacedBy(spacing),
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
                        maxDepth = maxDepth,
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
                        dataModelOverride = templateItemDataModel(item, surface.dataModel),
                        onAction = onAction,
                        isBusy = isBusy,
                        depth = depth + 1,
                        maxDepth = maxDepth,
                        visited = visited,
                    )
                }
            }
        }
    }
}

internal data class A2UIStyle(
    val padding: PaddingValues? = null,
    val backgroundColor: Color? = null,
    val textColor: Color? = null,
    val fontSize: TextUnit? = null,
    val fontWeight: FontWeight? = null,
    val cornerRadius: Dp? = null,
    val spacing: Dp? = null,
)

internal fun resolveStyle(
    component: A2UIComponent,
    surface: A2UISurfaceState,
): A2UIStyle? {
    val element = component.props.get("style") ?: return null
    return parseStyleElement(element, surface.styles)
}

private fun parseStyleElement(
    element: JsonElement,
    styles: JsonObject?,
): A2UIStyle? {
    if (element.isJsonPrimitive) {
        val ref = runCatching { element.asString.trim() }.getOrNull().orEmpty()
        return styleFromRef(ref, styles)
    }
    if (element.isJsonArray) {
        var merged: A2UIStyle? = null
        element.asJsonArray.forEach { item ->
            merged = mergeStyles(merged, parseStyleElement(item, styles))
        }
        return merged
    }
    if (element.isJsonObject) {
        val obj = element.asJsonObject
        val ref = obj.readString("ref")?.trim().orEmpty()
        val base = if (ref.isNotEmpty()) styleFromRef(ref, styles) else null
        return mergeStyles(base, parseStyleDefinition(obj))
    }
    return null
}

private fun styleFromRef(
    ref: String,
    styles: JsonObject?,
): A2UIStyle? {
    if (ref.isEmpty()) return null
    val obj = styles?.getAsJsonObject(ref) ?: return null
    return parseStyleDefinition(obj)
}

private fun parseStyleDefinition(obj: JsonObject): A2UIStyle? {
    val padding = parsePadding(obj)
    val backgroundColor = parseColor(obj.readString("backgroundColor"))
    val textColor = parseColor(obj.readString("textColor"))
    val fontSize = obj.readDouble("fontSize")?.toFloat()?.sp
    val fontWeight = parseFontWeight(obj.readString("fontWeight"))
    val cornerRadius = obj.readDouble("cornerRadius")?.dp
    val spacing = obj.readDouble("spacing")?.takeIf { it >= 0.0 }?.dp
    if (
        padding == null &&
        backgroundColor == null &&
        textColor == null &&
        fontSize == null &&
        fontWeight == null &&
        cornerRadius == null &&
        spacing == null
    ) {
        return null
    }
    return A2UIStyle(
        padding = padding,
        backgroundColor = backgroundColor,
        textColor = textColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        cornerRadius = cornerRadius,
        spacing = spacing,
    )
}

private fun parsePadding(obj: JsonObject): PaddingValues? {
    val all = obj.readDouble("padding")
    val horizontal = obj.readDouble("paddingHorizontal")
    val vertical = obj.readDouble("paddingVertical")
    if (all == null && horizontal == null && vertical == null) return null
    val resolvedHorizontal = (horizontal ?: all ?: 0.0).dp
    val resolvedVertical = (vertical ?: all ?: 0.0).dp
    return PaddingValues(horizontal = resolvedHorizontal, vertical = resolvedVertical)
}

private fun mergeStyles(
    base: A2UIStyle?,
    override: A2UIStyle?,
): A2UIStyle? {
    if (base == null) return override
    if (override == null) return base
    return A2UIStyle(
        padding = override.padding ?: base.padding,
        backgroundColor = override.backgroundColor ?: base.backgroundColor,
        textColor = override.textColor ?: base.textColor,
        fontSize = override.fontSize ?: base.fontSize,
        fontWeight = override.fontWeight ?: base.fontWeight,
        cornerRadius = override.cornerRadius ?: base.cornerRadius,
        spacing = override.spacing ?: base.spacing,
    )
}

private fun styleModifier(
    style: A2UIStyle?,
    includeBackground: Boolean = true,
    clipContent: Boolean = false,
): Modifier {
    if (style == null) return Modifier
    var modifier: Modifier = Modifier
    style.padding?.let { modifier = modifier.padding(it) }
    val shape = style.cornerRadius?.let { RoundedCornerShape(it) }
    if (clipContent && shape != null) {
        modifier = modifier.clip(shape)
    }
    if (includeBackground) {
        val color = style.backgroundColor
        if (color != null) {
            modifier =
                if (shape != null) {
                    modifier.background(color, shape)
                } else {
                    modifier.background(color)
                }
        }
    }
    return modifier
}

private fun applyTextStyle(
    base: TextStyle,
    style: A2UIStyle?,
): TextStyle {
    if (style == null) return base
    return base.copy(
        fontSize = style.fontSize ?: base.fontSize,
        fontWeight = style.fontWeight ?: base.fontWeight,
    )
}

@Composable
private fun buttonColors(style: A2UIStyle?) =
    if (style == null || (style.backgroundColor == null && style.textColor == null)) {
        null
    } else {
        ButtonDefaults.buttonColors(
            containerColor = style.backgroundColor ?: MaterialTheme.colorScheme.primary,
            contentColor = style.textColor ?: MaterialTheme.colorScheme.onPrimary,
        )
    }

private fun buttonShape(style: A2UIStyle?): RoundedCornerShape? = style?.cornerRadius?.let { RoundedCornerShape(it) }

private fun styleSpacing(
    style: A2UIStyle?,
    defaultSpacing: Dp,
): Dp = style?.spacing ?: defaultSpacing

private fun parseFontWeight(raw: String?): FontWeight? {
    return when (raw?.trim()?.lowercase()) {
        "light" -> FontWeight.Light
        "normal", "regular" -> FontWeight.Normal
        "medium" -> FontWeight.Medium
        "semibold", "semi-bold" -> FontWeight.SemiBold
        "bold" -> FontWeight.Bold
        else -> null
    }
}

private fun parseColor(raw: String?): Color? {
    val cleaned = raw?.trim()?.removePrefix("#") ?: return null
    val value = cleaned.toLongOrNull(16) ?: return null
    val argb =
        when (cleaned.length) {
            6 -> (0xFF000000 or value).toInt()
            8 -> value.toInt()
            else -> return null
        }
    val a = ((argb shr 24) and 0xFF) / 255f
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return Color(r, g, b, a)
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

internal sealed class ChildrenSpec {
    data class Explicit(val ids: List<String>) : ChildrenSpec()

    data class Template(
        val dataBinding: String,
        val componentId: String,
    ) : ChildrenSpec()
}

internal fun parseChildrenSpec(props: JsonObject): ChildrenSpec? {
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

internal fun resolveTemplateItems(
    spec: ChildrenSpec.Template,
    dataModel: Map<String, Any?>,
): List<Any?> {
    val binding = JsonObject().apply { addProperty("path", spec.dataBinding) }
    val resolved = A2UIBindingResolver.resolveValue(binding, dataModel)
    val list = resolved as? List<*> ?: return emptyList()
    return list.filterNotNull()
}

internal fun templateItemDataModel(
    item: Any?,
    parent: Map<String, Any?>,
): Map<String, Any?> {
    val itemData =
        when (item) {
            is Map<*, *> ->
                item.entries.associate { it.key.toString() to it.value }.toMutableMap()
            is List<*> -> item
            else -> mutableMapOf("value" to item)
        }
    return parent.toMutableMap().apply { this[A2UIBindingResolver.TEMPLATE_ITEM_KEY] = itemData }
}

internal fun componentWeight(
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
    maxDepth: Int,
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
                maxDepth = maxDepth,
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
            maxDepth = maxDepth,
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
    maxDepth: Int,
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
                maxDepth = maxDepth,
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
            maxDepth = maxDepth,
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

private fun JsonObject.readDouble(key: String): Double? =
    runCatching { get(key)?.takeIf { !it.isJsonNull }?.asDouble }.getOrNull()
