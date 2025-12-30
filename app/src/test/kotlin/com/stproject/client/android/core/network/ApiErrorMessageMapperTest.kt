package com.stproject.client.android.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorMessageMapperTest {
    @Test
    fun `maps error detail code`() {
        val msg = ApiErrorMessageMapper.toUserMessage(
            httpStatus = 400,
            apiCode = 123,
            errorDetailCode = "INSUFFICIENT_BALANCE",
            fallback = "oops"
        )
        assertEquals("insufficient balance", msg)
    }

    @Test
    fun `maps http status`() {
        val msg = ApiErrorMessageMapper.toUserMessage(
            httpStatus = 401,
            apiCode = null,
            errorDetailCode = null,
            fallback = null
        )
        assertEquals("unauthorized", msg)
    }

    @Test
    fun `uses fallback when available`() {
        val msg = ApiErrorMessageMapper.toUserMessage(
            httpStatus = null,
            apiCode = null,
            errorDetailCode = null,
            fallback = "custom"
        )
        assertEquals("custom", msg)
    }
}
