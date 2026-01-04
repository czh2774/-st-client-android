package com.stproject.client.android.domain.repository

interface CharacterRepository {
    suspend fun queryCharacters(
        cursor: String? = null,
        limit: Int? = null,
        sortBy: String? = null,
        isNsfw: Boolean? = null,
    ): List<com.stproject.client.android.domain.model.CharacterSummary>

    suspend fun queryCharactersFiltered(
        cursor: String? = null,
        limit: Int? = null,
        sortBy: String? = null,
        isNsfw: Boolean? = null,
        tags: List<String>? = null,
        searchKeyword: String? = null,
        gender: String? = null,
    ): List<com.stproject.client.android.domain.model.CharacterSummary> {
        return queryCharacters(cursor, limit, sortBy, isNsfw)
    }

    suspend fun getCharacterDetail(characterId: String): com.stproject.client.android.domain.model.CharacterDetail

    suspend fun resolveShareCode(shareCode: String): String?

    suspend fun generateShareCode(characterId: String): com.stproject.client.android.domain.model.ShareCodeInfo?

    suspend fun blockCharacter(
        characterId: String,
        value: Boolean,
    )

    suspend fun followCharacter(
        characterId: String,
        value: Boolean,
    ): com.stproject.client.android.domain.model.CharacterFollowResult
}
