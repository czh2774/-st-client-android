package com.stproject.client.android.core.a2ui

internal object A2UIJsonPointer {
    fun parse(pointer: String): List<String> {
        val trimmed = pointer.trim()
        if (trimmed.isEmpty() || trimmed == "/") return emptyList()
        val raw = if (trimmed.startsWith("/")) trimmed.drop(1) else trimmed
        if (raw.isEmpty()) return emptyList()
        return raw.split("/")
            .filter { it.isNotEmpty() }
            .map { decodeSegment(it) }
    }

    fun resolve(
        pointer: String,
        root: Any?,
    ): Any? {
        val segments = parse(pointer)
        if (segments.isEmpty()) return root
        var current: Any? = root
        for (segment in segments) {
            current =
                when (current) {
                    is Map<*, *> -> current[segment]
                    is List<*> -> {
                        val idx = segment.toIntOrNull() ?: return null
                        current.getOrNull(idx)
                    }
                    else -> return null
                }
        }
        return current
    }

    private fun decodeSegment(segment: String): String {
        if (!segment.contains("~")) return segment
        val out = StringBuilder(segment.length)
        var i = 0
        while (i < segment.length) {
            val ch = segment[i]
            if (ch == '~' && i + 1 < segment.length) {
                when (segment[i + 1]) {
                    '0' -> out.append('~')
                    '1' -> out.append('/')
                    else -> {
                        out.append('~')
                        out.append(segment[i + 1])
                    }
                }
                i += 2
            } else {
                out.append(ch)
                i += 1
            }
        }
        return out.toString()
    }
}
