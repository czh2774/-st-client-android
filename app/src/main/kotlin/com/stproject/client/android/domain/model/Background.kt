package com.stproject.client.android.domain.model

data class BackgroundConfig(
    val width: Int,
    val height: Int,
)

data class BackgroundItem(
    val name: String,
    val url: String,
)

data class BackgroundList(
    val items: List<BackgroundItem>,
    val config: BackgroundConfig?,
)
