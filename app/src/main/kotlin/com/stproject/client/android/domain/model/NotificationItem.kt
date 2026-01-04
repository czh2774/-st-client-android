package com.stproject.client.android.domain.model

data class NotificationItem(
    val id: String,
    val userId: String? = null,
    val type: String,
    val title: String,
    val content: String,
    val contentMeta: ContentSummary? = null,
    val isRead: Boolean,
    val createdAt: String?,
)
