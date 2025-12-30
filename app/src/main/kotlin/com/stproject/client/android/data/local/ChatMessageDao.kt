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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET content = :content, isStreaming = :isStreaming WHERE id = :id")
    suspend fun updateContent(id: String, content: String, isStreaming: Boolean)

    @Query("UPDATE chat_messages SET serverId = :serverId WHERE id = :id")
    suspend fun updateServerId(id: String, serverId: String?)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
