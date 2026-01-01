package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.MarkReadRequestDto
import com.stproject.client.android.core.network.NotificationDto
import com.stproject.client.android.core.network.StNotificationApi
import com.stproject.client.android.domain.model.NotificationItem
import com.stproject.client.android.domain.repository.NotificationListResult
import com.stproject.client.android.domain.repository.NotificationRepository
import com.stproject.client.android.domain.repository.UnreadCounts
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpNotificationRepository
    @Inject
    constructor(
        private val api: StNotificationApi,
        private val apiClient: ApiClient,
    ) : NotificationRepository {
        override suspend fun listNotifications(
            pageNum: Int,
            pageSize: Int,
        ): NotificationListResult {
            val resp =
                apiClient.call {
                    api.listNotifications(
                        pageNum = pageNum,
                        pageSize = pageSize,
                        types = null,
                        excludeTypes = null,
                        unreadOnly = null,
                    )
                }
            val items = resp.items ?: emptyList()
            return NotificationListResult(
                items = items.mapNotNull { it.toDomain() },
                total = resp.total ?: items.size,
                hasMore = resp.hasMore ?: false,
            )
        }

        override suspend fun markAsRead(
            ids: List<String>,
            markAll: Boolean,
        ) {
            apiClient.call {
                api.markAsRead(
                    MarkReadRequestDto(
                        ids = ids.takeIf { it.isNotEmpty() },
                        markAll = if (markAll) true else null,
                    ),
                )
            }
        }

        override suspend fun getUnreadCounts(): UnreadCounts {
            val resp = apiClient.call { api.getUnreadCounts() }
            return UnreadCounts(
                system = resp.system ?: 0,
                follow = resp.follow ?: 0,
                like = resp.like ?: 0,
                comment = resp.comment ?: 0,
                total = resp.total ?: 0,
            )
        }

        private fun NotificationDto.toDomain(): NotificationItem? {
            val idValue = id?.trim().orEmpty()
            if (idValue.isEmpty()) return null
            return NotificationItem(
                id = idValue,
                type = type?.trim().orEmpty(),
                title = title?.trim().orEmpty(),
                content = content?.trim().orEmpty(),
                isRead = isRead ?: false,
                createdAt = createdAt?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }
