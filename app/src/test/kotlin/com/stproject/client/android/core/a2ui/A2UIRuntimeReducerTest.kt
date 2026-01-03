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
}
