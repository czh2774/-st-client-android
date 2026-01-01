package com.stproject.client.android.domain.model

data class IapProduct(
    val productId: String,
    val kind: String,
    val displayName: String,
    val description: String?,
    val enabled: Boolean,
    val grantCredits: Int?,
    val vipType: String?,
    val durationMonths: Int?,
)
