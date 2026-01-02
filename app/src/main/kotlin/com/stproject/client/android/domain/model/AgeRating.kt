package com.stproject.client.android.domain.model

enum class AgeRating(val raw: String) {
    All("all"),
    Age13("13+"),
    Age16("16+"),
    Age18("18+"),
    Unknown("unknown"),
    ;

    companion object {
        fun from(raw: String?): AgeRating? {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            if (normalized.isEmpty()) return null
            return when (normalized) {
                "all" -> All
                "13+", "13" -> Age13
                "16+", "16" -> Age16
                "18+", "18" -> Age18
                "unknown" -> Unknown
                else -> null
            }
        }
    }
}

fun AgeRating?.isAdultContent(): Boolean = this == AgeRating.Age18
