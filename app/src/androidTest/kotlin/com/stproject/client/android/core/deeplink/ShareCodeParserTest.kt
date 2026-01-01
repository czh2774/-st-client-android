package com.stproject.client.android.core.deeplink

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareCodeParserTest {
    @Test
    fun extractsShareCodeFromCustomScheme() {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("stproject://share/c/abc123"),
            )
        assertEquals("abc123", ShareCodeParser.extractShareCode(intent))
    }

    @Test
    fun extractsShareCodeFromPath() {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://example.com/share/c/xyz"),
            )
        assertEquals("xyz", ShareCodeParser.extractShareCode(intent))
    }

    @Test
    fun extractsShareCodeFromQuery() {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://example.com/invite?shareCode=qwe"),
            )
        assertEquals("qwe", ShareCodeParser.extractShareCode(intent))
    }

    @Test
    fun returnsNullWhenMissing() {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://example.com/"),
            )
        assertNull(ShareCodeParser.extractShareCode(intent))
    }
}
