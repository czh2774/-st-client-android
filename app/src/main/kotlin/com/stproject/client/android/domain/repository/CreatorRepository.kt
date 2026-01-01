package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.CreatorCharacter
import com.stproject.client.android.domain.model.CreatorSummary

data class CreatorListResult(
    val items: List<CreatorSummary>,
    val hasMore: Boolean,
    val nextCursor: String?,
)

data class CreatorCharactersResult(
    val items: List<CreatorCharacter>,
    val total: Int,
    val hasMore: Boolean,
)

interface CreatorRepository {
    suspend fun listCreators(
        limit: Int,
        cursor: String? = null,
        sortBy: String? = null,
        searchKeyword: String? = null,
    ): CreatorListResult

    suspend fun listCreatorCharacters(
        creatorId: String,
        pageNum: Int,
        pageSize: Int,
    ): CreatorCharactersResult
}
