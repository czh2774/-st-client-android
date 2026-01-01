package com.stproject.client.android.core.network

import com.google.gson.Gson
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatCompletionRequestDtoTest {
    @Test
    fun `serializes client message ids`() {
        val dto =
            ChatCompletionRequestDto(
                message = "hello",
                stream = true,
                clientMessageId = "local-user-1",
                clientAssistantMessageId = "local-assistant-1",
            )

        val json = Gson().toJson(dto)

        assertTrue(json.contains("\"clientMessageId\":\"local-user-1\""))
        assertTrue(json.contains("\"clientAssistantMessageId\":\"local-assistant-1\""))
    }
}
