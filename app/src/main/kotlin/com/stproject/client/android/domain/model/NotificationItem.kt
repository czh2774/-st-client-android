package com.stproject.client.android.domain.model

data class NotificationItem(
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: String?,
)
