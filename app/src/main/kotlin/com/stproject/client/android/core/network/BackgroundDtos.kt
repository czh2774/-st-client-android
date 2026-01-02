package com.stproject.client.android.core.network

data class BackgroundConfigDto(
    val width: Int? = null,
    val height: Int? = null,
)

data class BackgroundListResponseDto(
    val images: List<String>? = null,
    val config: BackgroundConfigDto? = null,
)

data class BackgroundRenameRequestDto(
    val oldName: String,
    val newName: String,
)

data class BackgroundDeleteRequestDto(
    val name: String,
)
