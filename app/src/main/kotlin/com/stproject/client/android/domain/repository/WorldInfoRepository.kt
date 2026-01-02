package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.WorldInfoEntry
import com.stproject.client.android.domain.model.WorldInfoEntryInput

interface WorldInfoRepository {
    suspend fun listEntries(
        characterId: String?,
        includeGlobal: Boolean,
    ): List<WorldInfoEntry>

    suspend fun createEntry(input: WorldInfoEntryInput): WorldInfoEntry

    suspend fun updateEntry(
        id: String,
        input: WorldInfoEntryInput,
    ): WorldInfoEntry

    suspend fun deleteEntry(id: String): Boolean
}
