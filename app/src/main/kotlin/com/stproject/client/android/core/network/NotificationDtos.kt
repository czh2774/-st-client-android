package com.stproject.client.android.core.network

data class NotificationDto(
    val id: String? = null,
    val userId: String? = null,
    val type: String? = null,
    val title: String? = null,
    val content: String? = null,
    val data: NotificationDataDto? = null,
    val isRead: Boolean? = null,
    val createdAt: String? = null,
)

data class NotificationDataDto(
    val content: NotificationContentDto? = null,
)

data class NotificationContentDto(
    val characterId: String? = null,
    val isNsfw: Boolean? = null,
    val moderationAgeRating: String? = null,
    val tags: List<String>? = null,
    val visibility: String? = null,
)

data class NotificationsResponseDto(
    val items: List<NotificationDto>? = null,
    val total: Int? = null,
    val hasMore: Boolean? = null,
)

data class UnreadCountsDto(
    val system: Int? = null,
    val follow: Int? = null,
    val like: Int? = null,
    val comment: Int? = null,
    val total: Int? = null,
)

data class MarkReadRequestDto(
    val ids: List<String>? = null,
    val markAll: Boolean? = null,
)

data class MarkReadResponseDto(
    val ok: Boolean? = null,
)
