package com.stproject.client.android.core.compliance

import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.domain.model.isAdultContent

fun resolveNsfwHint(
    isNsfw: Boolean?,
    ageRating: AgeRating?,
): Boolean? {
    if (ageRating?.isAdultContent() == true) return true
    if (isNsfw != null) return isNsfw
    return when (ageRating) {
        null -> null
        AgeRating.Unknown -> null
        else -> ageRating.isAdultContent()
    }
}
