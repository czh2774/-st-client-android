package com.stproject.client.android.core.a2ui

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class A2UIRuntimeReducerTest {
    @Test
    fun `replaces data model when path omitted`() {
        val state =
            A2UIRuntimeState(
                surfaces =
                    mapOf(
                        "s1" to
                            A2UISurfaceState(
                                surfaceId = "s1",
                                dataModel = mutableMapOf("keep" to "old"),
                            ),
                    ),
            )
        val message =
            A2UIMessage(
                dataModelUpdate =
                    A2UIDataModelUpdate(
                        surfaceId = "s1",
                        contents = listOf(A2UIDataEntry(key = "fresh", valueString = "new")),
                    ),
            )

        val next = A2UIRuntimeReducer.reduce(state, message)
        val dataModel = next.surfaces["s1"]?.dataModel ?: emptyMap()

        assertEquals(mapOf("fresh" to "new"), dataModel)
        assertFalse(dataModel.containsKey("keep"))
    }

    @Test
    fun `replaces data model when path is root slash`() {
        val state =
            A2UIRuntimeState(
                surfaces =
                    mapOf(
                        "s1" to
                            A2UISurfaceState(
                                surfaceId = "s1",
                                dataModel = mutableMapOf("keep" to "old"),
                            ),
                    ),
            )
        val message =
            A2UIMessage(
                dataModelUpdate =
                    A2UIDataModelUpdate(
                        surfaceId = "s1",
                        path = "/",
                        contents = listOf(A2UIDataEntry(key = "fresh", valueString = "new")),
                    ),
            )

        val next = A2UIRuntimeReducer.reduce(state, message)
        val dataModel = next.surfaces["s1"]?.dataModel ?: emptyMap()

        assertEquals(mapOf("fresh" to "new"), dataModel)
        assertFalse(dataModel.containsKey("keep"))
    }

    @Test
    fun `updates list item when path targets array index`() {
        val state =
            A2UIRuntimeState(
                surfaces =
                    mapOf(
                        "s1" to
                            A2UISurfaceState(
                                surfaceId = "s1",
                                dataModel = mapOf("items" to listOf(mapOf("name" to "Old"))),
                            ),
                    ),
            )
        val message =
            A2UIMessage(
                dataModelUpdate =
                    A2UIDataModelUpdate(
                        surfaceId = "s1",
                        path = "/items/0",
                        contents = listOf(A2UIDataEntry(key = "name", valueString = "New")),
                    ),
            )

        val next = A2UIRuntimeReducer.reduce(state, message)
        val items = next.surfaces["s1"]?.dataModel?.get("items") as? List<*>
        val first = items?.firstOrNull() as? Map<*, *>

        assertEquals("New", first?.get("name"))
    }

    @Test
    fun `ignores messages with multiple actions`() {
        val state = A2UIRuntimeState()
        val message =
            A2UIMessage(
                beginRendering = A2UIBeginRendering(surfaceId = "s1", root = "root"),
                surfaceUpdate = A2UISurfaceUpdate(surfaceId = "s1", components = emptyList()),
            )

        val next = A2UIRuntimeReducer.reduce(state, message)

        assertEquals(state, next)
    }

    @Test
    fun `skips components with multiple type keys`() {
        val component =
            JsonParser.parseString(
                """{"Text":{"text":{"literalString":"Hi"}},"Image":{"url":{"literalString":"x"}}}""",
            ).asJsonObject
        val message =
            A2UIMessage(
                surfaceUpdate =
                    A2UISurfaceUpdate(
                        surfaceId = "s1",
                        components = listOf(A2UIComponentDefinition(id = "c1", component = component)),
                    ),
            )

        val next = A2UIRuntimeReducer.reduce(A2UIRuntimeState(), message)
        val components = next.surfaces["s1"]?.components ?: emptyMap()

        assertTrue(components.isEmpty())
    }

    @Test
    fun `applies bound defaults from components`() {
        val component =
            JsonParser.parseString(
                """{"Text":{"text":{"path":"/user/name","literalString":"Guest"}}}""",
            ).asJsonObject
        val message =
            A2UIMessage(
                surfaceUpdate =
                    A2UISurfaceUpdate(
                        surfaceId = "s1",
                        components = listOf(A2UIComponentDefinition(id = "c1", component = component)),
                    ),
            )

        val next = A2UIRuntimeReducer.reduce(A2UIRuntimeState(), message)
        val user = next.surfaces["s1"]?.dataModel?.get("user") as? Map<*, *>

        assertEquals("Guest", user?.get("name"))
    }

    @Test
    fun `applies template defaults after data model update`() {
        val parentComponent =
            JsonParser.parseString(
                """{"Column":{"children":{"template":{"dataBinding":"/items","componentId":"item"}}}}""",
            ).asJsonObject
        val itemComponent =
            JsonParser.parseString(
                """{"Text":{"text":{"path":"title","literalString":"Default"}}}""",
            ).asJsonObject
        val surfaceMessage =
            A2UIMessage(
                surfaceUpdate =
                    A2UISurfaceUpdate(
                        surfaceId = "s1",
                        components =
                            listOf(
                                A2UIComponentDefinition(id = "parent", component = parentComponent),
                                A2UIComponentDefinition(id = "item", component = itemComponent),
                            ),
                    ),
            )
        val stateWithComponents = A2UIRuntimeReducer.reduce(A2UIRuntimeState(), surfaceMessage)
        val stateWithRoot =
            A2UIRuntimeReducer.reduce(
                stateWithComponents,
                A2UIMessage(beginRendering = A2UIBeginRendering(surfaceId = "s1", root = "parent")),
            )
        val itemsArray = JsonParser.parseString("""[{"title":"Existing"},{}]""").asJsonArray
        val dataMessage =
            A2UIMessage(
                dataModelUpdate =
                    A2UIDataModelUpdate(
                        surfaceId = "s1",
                        contents =
                            listOf(
                                A2UIDataEntry(
                                    key = "items",
                                    valueList = itemsArray.toList(),
                                ),
                            ),
                    ),
            )

        val next = A2UIRuntimeReducer.reduce(stateWithRoot, dataMessage)
        val items = next.surfaces["s1"]?.dataModel?.get("items") as? List<*>
        val first = items?.getOrNull(0) as? Map<*, *>
        val second = items?.getOrNull(1) as? Map<*, *>

        assertEquals("Existing", first?.get("title"))
        assertEquals("Default", second?.get("title"))
    }

    @Test
    fun `applies template defaults from explicit children`() {
        val parentComponent =
            JsonParser.parseString(
                """{"Column":{"children":{"template":{"dataBinding":"/items","componentId":"item"}}}}""",
            ).asJsonObject
        val itemComponent =
            JsonParser.parseString(
                """{"Column":{"children":{"explicitList":["title"]}}}""",
            ).asJsonObject
        val titleComponent =
            JsonParser.parseString(
                """{"Text":{"text":{"path":"title","literalString":"Default"}}}""",
            ).asJsonObject
        val surfaceMessage =
            A2UIMessage(
                surfaceUpdate =
                    A2UISurfaceUpdate(
                        surfaceId = "s1",
                        components =
                            listOf(
                                A2UIComponentDefinition(id = "parent", component = parentComponent),
                                A2UIComponentDefinition(id = "item", component = itemComponent),
                                A2UIComponentDefinition(id = "title", component = titleComponent),
                            ),
                    ),
            )
        val stateWithComponents = A2UIRuntimeReducer.reduce(A2UIRuntimeState(), surfaceMessage)
        val stateWithRoot =
            A2UIRuntimeReducer.reduce(
                stateWithComponents,
                A2UIMessage(beginRendering = A2UIBeginRendering(surfaceId = "s1", root = "parent")),
            )
        val itemsArray = JsonParser.parseString("""[{},{}]""").asJsonArray
        val dataMessage =
            A2UIMessage(
                dataModelUpdate =
                    A2UIDataModelUpdate(
                        surfaceId = "s1",
                        contents =
                            listOf(
                                A2UIDataEntry(
                                    key = "items",
                                    valueList = itemsArray.toList(),
                                ),
                            ),
                    ),
            )

        val next = A2UIRuntimeReducer.reduce(stateWithRoot, dataMessage)
        val items = next.surfaces["s1"]?.dataModel?.get("items") as? List<*>
        val first = items?.getOrNull(0) as? Map<*, *>
        val second = items?.getOrNull(1) as? Map<*, *>

        assertEquals("Default", first?.get("title"))
        assertEquals("Default", second?.get("title"))
        assertFalse(next.surfaces["s1"]?.dataModel?.containsKey("title") ?: false)
    }
}
