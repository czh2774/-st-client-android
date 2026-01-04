package com.stproject.client.android.core.a2ui

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class A2UIBindingResolverTest {
    @Test
    fun `returns literal when path is missing`() {
        val dataModel = mapOf<String, Any?>()
        val value = JsonParser.parseString("""{"path":"/user/name","literalString":"Guest"}""")

        val resolved = A2UIBindingResolver.resolveValue(value, dataModel)

        assertEquals("Guest", resolved)
    }

    @Test
    fun `prefers data model value over literal`() {
        val dataModel = mapOf("user" to mapOf("name" to "Alice"))
        val value = JsonParser.parseString("""{"path":"/user/name","literalString":"Guest"}""")

        val resolved = A2UIBindingResolver.resolveValue(value, dataModel)

        assertEquals("Alice", resolved)
    }

    @Test
    fun `resolves relative template paths against item scope and absolute against root`() {
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
    fun `resolves absolute paths against root when no template`() {
        val dataModel = mapOf("name" to "Root")
        val value = JsonParser.parseString("""{"path":"/name"}""")

        val resolved = A2UIBindingResolver.resolveString(value, dataModel)

        assertEquals("Root", resolved)
    }

    @Test
    fun `decodes json pointer escape sequences`() {
        val dataModel =
            mapOf(
                "a/b" to "slash",
                "tilde~key" to "tilde",
            )

        val slash = JsonParser.parseString("""{"path":"/a~1b"}""")
        val tilde = JsonParser.parseString("""{"path":"/tilde~0key"}""")

        val slashValue = A2UIBindingResolver.resolveString(slash, dataModel)
        val tildeValue = A2UIBindingResolver.resolveString(tilde, dataModel)

        assertEquals("slash", slashValue)
        assertEquals("tilde", tildeValue)
    }

    @Test
    fun `parses literalArray bound values`() {
        val dataModel = emptyMap<String, Any?>()
        val value = JsonParser.parseString("""{"literalArray":[1,"a",true]}""")

        val resolved = A2UIBindingResolver.resolveValue(value, dataModel)

        assertEquals(listOf(1.0, "a", true), resolved)
    }
}
