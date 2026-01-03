package com.stproject.client.android.features.chat

import com.google.gson.JsonParser
import com.stproject.client.android.core.a2ui.A2UIComponent
import com.stproject.client.android.core.a2ui.A2UIBindingResolver
import com.stproject.client.android.core.a2ui.A2UISurfaceState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class A2UIRuntimeViewTest {
    @Test
    fun `parseChildrenSpec handles explicit list`() {
        val props = JsonParser.parseString("""{"children":{"explicitList":["a","b"]}}""").asJsonObject

        val spec = parseChildrenSpec(props)

        assertTrue(spec is ChildrenSpec.Explicit)
        assertEquals(listOf("a", "b"), (spec as ChildrenSpec.Explicit).ids)
    }

    @Test
    fun `parseChildrenSpec handles template`() {
        val props =
            JsonParser.parseString(
                """{"children":{"template":{"dataBinding":"/items","componentId":"item"}}}""",
            ).asJsonObject

        val spec = parseChildrenSpec(props)

        assertTrue(spec is ChildrenSpec.Template)
        val template = spec as ChildrenSpec.Template
        assertEquals("/items", template.dataBinding)
        assertEquals("item", template.componentId)
    }

    @Test
    fun `resolveTemplateItems returns bound list`() {
        val spec = ChildrenSpec.Template(dataBinding = "/items", componentId = "item")
        val dataModel =
            mapOf(
                "items" to listOf(mapOf("name" to "A"), "B"),
            )

        val items = resolveTemplateItems(spec, dataModel)

        assertEquals(listOf(mapOf("name" to "A"), "B"), items)
    }

    @Test
    fun `templateItemDataModel stores item data under template key`() {
        val parent = mapOf("name" to "Parent")
        val item = mapOf("name" to "Child")

        val result = templateItemDataModel(item, parent)

        assertEquals("Parent", result["name"])
        val itemData = result[A2UIBindingResolver.TEMPLATE_ITEM_KEY] as? Map<*, *>
        assertEquals("Child", itemData?.get("name"))
    }

    @Test
    fun `componentWeight returns positive weights only`() {
        val component =
            A2UIComponent(
                id = "c1",
                type = "Text",
                props = JsonParser.parseString("""{"text":{"literalString":"hi"}}""").asJsonObject,
                weight = 2.0,
            )
        val surface = A2UISurfaceState(surfaceId = "s1", components = mapOf("c1" to component))

        assertEquals(2.0f, componentWeight(surface, "c1"))
        assertNull(componentWeight(surface, "missing"))
    }

    @Test
    fun `resolveStyle merges referenced styles and inline overrides`() {
        val styles =
            JsonParser.parseString(
                """{"base":{"fontSize":12,"spacing":4},"accent":{"fontWeight":"bold"}}""",
            ).asJsonObject
        val props =
            JsonParser.parseString(
                """{"style":["base",{"ref":"accent","fontSize":14}],"text":{"literalString":"Hi"}}""",
            ).asJsonObject
        val component =
            A2UIComponent(
                id = "t1",
                type = "Text",
                props = props,
            )
        val surface = A2UISurfaceState(surfaceId = "s1", styles = styles)

        val style = resolveStyle(component, surface)

        assertEquals(14.sp, style?.fontSize)
        assertEquals(4.dp, style?.spacing)
        assertEquals(FontWeight.Bold, style?.fontWeight)
    }
}
