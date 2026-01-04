package com.stproject.client.android.core.a2ui

import com.stproject.client.android.domain.model.A2UIAction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class A2UIProtocolValidatorTest {
    @Test
    fun `rejects actions without surface id`() {
        val action =
            A2UIAction(
                name = A2UICatalog.Actions.SEND_MESSAGE,
                surfaceId = null,
                sourceComponentId = "c1",
            )

        val result = A2UIProtocolValidator.validateAction(action)

        assertFalse(result.isValid)
    }

    @Test
    fun `rejects data model entries with multiple values`() {
        val message =
            A2UIMessage(
                dataModelUpdate =
                    A2UIDataModelUpdate(
                        surfaceId = "s1",
                        contents = listOf(A2UIDataEntry(key = "x", valueString = "a", valueNumber = 1.0)),
                    ),
            )

        val result = A2UIProtocolValidator.validateMessage(message)

        assertFalse(result.isValid)
    }

    @Test
    fun `accepts basic beginRendering messages`() {
        val message =
            A2UIMessage(
                beginRendering = A2UIBeginRendering(surfaceId = "s1", root = "root"),
            )

        val result = A2UIProtocolValidator.validateMessage(message)

        assertTrue(result.isValid)
    }
}
