package com.stproject.client.android.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["sessionId", "createdAt"]),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val serverId: String? = null,
    val role: String,
    val content: String,
    val createdAt: Long,
    val isStreaming: Boolean,
    val swipeId: Int? = null,
    val swipesJson: String? = null,
    val metadataJson: String? = null,
)
