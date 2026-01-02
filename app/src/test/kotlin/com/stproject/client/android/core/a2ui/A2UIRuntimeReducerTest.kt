package com.stproject.client.android.core.a2ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
