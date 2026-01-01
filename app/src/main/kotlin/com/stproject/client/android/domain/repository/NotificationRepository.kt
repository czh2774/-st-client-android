package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.NotificationItem

data class NotificationListResult(
    val items: List<NotificationItem>,
    val total: Int,
    val hasMore: Boolean,
)

data class UnreadCounts(
    val system: Int,
    val follow: Int,
    val like: Int,
    val comment: Int,
    val total: Int,
)

interface NotificationRepository {
    suspend fun listNotifications(
        pageNum: Int,
        pageSize: Int,
    ): NotificationListResult

    suspend fun markAsRead(
        ids: List<String>,
        markAll: Boolean = false,
    )

    suspend fun getUnreadCounts(): UnreadCounts
}
