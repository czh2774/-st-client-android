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
}
