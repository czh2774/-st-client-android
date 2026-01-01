package com.stproject.client.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAtMs DESC LIMIT :limit OFFSET :offset")
    suspend fun listSessions(
        limit: Int,
        offset: Int,
    ): List<ChatSessionEntity>

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): ChatSessionEntity?

    @Query("SELECT COUNT(*) FROM chat_sessions")
    suspend fun countSessions(): Int

    @Query("SELECT sessionId FROM chat_sessions ORDER BY updatedAtMs DESC LIMIT :limit OFFSET :offset")
    suspend fun listSessionIds(
        limit: Int,
        offset: Int,
    ): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<ChatSessionEntity>)

    @Query("DELETE FROM chat_sessions WHERE sessionId IN (:sessionIds)")
    suspend fun deleteByIds(sessionIds: List<String>)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAll()
}
