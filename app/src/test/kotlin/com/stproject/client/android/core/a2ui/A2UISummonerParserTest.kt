package com.stproject.client.android.core.a2ui

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class A2UISummonerParserTest {
    private val gson = Gson()

    @Test
    fun `parses gold and actions from dataModelUpdate contents`() {
        val raw =
            """
            {
              "surfaceId": "chat:panel:status",
              "contents": [
                {
                  "key": "summoner",
                  "valueMap": [
                    { "key": "gold", "valueNumber": 120 },
                    {
                      "key": "actions",
                      "valueMap": [
                        { "key": "a0", "valueString": "Explore" },
                        { "key": "a1", "valueString": "Rest" }
                      ]
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
        val json = gson.fromJson(raw, JsonObject::class.java)
        val state = A2UISummonerParser.fromDataModel(json)
        assertNotNull(state)
        assertEquals("chat:panel:status", state?.surfaceId)
        assertEquals(120, state?.gold)
        assertEquals(listOf("Explore", "Rest"), state?.actions)
    }
}
