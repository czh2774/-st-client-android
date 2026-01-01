package com.stproject.client.android.features.notifications

import com.stproject.client.android.domain.model.NotificationItem
import com.stproject.client.android.domain.repository.UnreadCounts

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val items: List<NotificationItem> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = false,
    val pageNum: Int = 1,
    val unreadCounts: UnreadCounts = UnreadCounts(0, 0, 0, 0, 0),
)
