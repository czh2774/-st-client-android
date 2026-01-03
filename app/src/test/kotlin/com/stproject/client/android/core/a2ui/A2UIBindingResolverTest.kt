package com.stproject.client.android.core.a2ui

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class A2UIBindingResolverTest {
    @Test
    fun `initializes data model when path and literal provided`() {
        val dataModel = mutableMapOf<String, Any?>()
        val value = JsonParser.parseString("""{"path":"/user/name","literalString":"Guest"}""")

        val resolved = A2UIBindingResolver.resolveValue(value, dataModel)

        val user = dataModel["user"] as? Map<*, *>
        assertEquals("Guest", user?.get("name"))
        assertEquals("Guest", resolved)
    }

    @Test
    fun `resolves relative paths against template item`() {
        val dataModel =
            mutableMapOf<String, Any?>(
                "name" to "Parent",
                A2UIBindingResolver.TEMPLATE_ITEM_KEY to mapOf("name" to "Item"),
            )
        val relative = JsonParser.parseString("""{"path":"name"}""")
        val absolute = JsonParser.parseString("""{"path":"/name"}""")

        val relativeValue = A2UIBindingResolver.resolveString(relative, dataModel)
        val absoluteValue = A2UIBindingResolver.resolveString(absolute, dataModel)

        assertEquals("Item", relativeValue)
        assertEquals("Parent", absoluteValue)
    }

    @Test
    fun `parses literalArray bound values`() {
        val dataModel = emptyMap<String, Any?>()
        val value = JsonParser.parseString("""{"literalArray":[1,"a",true]}""")

        val resolved = A2UIBindingResolver.resolveValue(value, dataModel)

        assertEquals(listOf(1.0, "a", true), resolved)
    }
}
