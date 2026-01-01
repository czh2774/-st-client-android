package com.stproject.client.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChatMessageEntity?

    @Query("SELECT * FROM chat_messages WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET content = :content, isStreaming = :isStreaming WHERE id = :id")
    suspend fun updateContent(
        id: String,
        content: String,
        isStreaming: Boolean,
    )

    @Query("UPDATE chat_messages SET content = :content, isStreaming = :isStreaming WHERE serverId = :serverId")
    suspend fun updateContentByServerId(
        serverId: String,
        content: String,
        isStreaming: Boolean,
    )

    @Query("UPDATE chat_messages SET serverId = :serverId WHERE id = :id")
    suspend fun updateServerId(
        id: String,
        serverId: String?,
    )

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE sessionId IN (:sessionIds)")
    suspend fun deleteBySessionIds(sessionIds: List<String>)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
