package com.stproject.client.android.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_sessions",
    indices = [
        Index(value = ["updatedAtMs"]),
    ],
)
data class ChatSessionEntity(
    @PrimaryKey val sessionId: String,
    val primaryMemberId: String?,
    val displayName: String,
    val updatedAt: String?,
    val updatedAtMs: Long,
)
