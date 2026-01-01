package com.stproject.client.android.core.deeplink

import android.content.Intent
import android.net.Uri

object ShareCodeParser {
    fun extractShareCode(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        return parseShareCodeUri(uri)
            ?: uri.getQueryParameter("shareCode")?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun parseShareCodeUri(uri: Uri): String? {
        val segments = uri.pathSegments ?: emptyList()
        if (segments.isEmpty()) return null
        if (uri.scheme == "stproject" && uri.host == "share") {
            if (segments.size >= 2 && segments[0].equals("c", ignoreCase = true)) {
                return segments[1].trim().takeIf { it.isNotEmpty() }
            }
        }
        val shareIndex = segments.indexOfFirst { it.equals("share", ignoreCase = true) }
        if (shareIndex >= 0 && shareIndex + 2 < segments.size) {
            if (segments[shareIndex + 1].equals("c", ignoreCase = true)) {
                return segments[shareIndex + 2].trim().takeIf { it.isNotEmpty() }
            }
        }
        return null
    }
}
