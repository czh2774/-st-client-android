package com.stproject.client.android.core.network

data class CreatorAssistantStartRequestDto(
    val characterType: String? = null,
    val parentCharacterId: String? = null,
    val initialPrompt: String? = null,
)

data class CreatorAssistantStartResponseDto(
    val sessionId: String? = null,
    val characterType: String? = null,
    val greeting: String? = null,
    val suggestions: List<String>? = null,
)

data class CreatorAssistantChatRequestDto(
    val sessionId: String,
    val content: String,
)

data class CreatorAssistantChatResponseDto(
    val messageId: String? = null,
    val content: String? = null,
    val suggestions: List<String>? = null,
    val draftReady: Boolean? = null,
)

data class CreatorAssistantGenerateDraftRequestDto(
    val sessionId: String,
)

data class CreatorAssistantUpdateDraftRequestDto(
    val sessionId: String,
    val draftId: String,
    val updates: Map<String, Any>,
)

data class CreatorAssistantPublishRequestDto(
    val sessionId: String,
    val draftId: String,
    val isPublic: Boolean,
    val avatarBase64: String? = null,
    val backgroundBase64: String? = null,
)

data class CreatorAssistantPublishResponseDto(
    val characterId: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val backgroundUrl: String? = null,
    val shareCode: String? = null,
)

data class CreatorAssistantSessionSummaryDto(
    val sessionId: String? = null,
    val characterType: String? = null,
    val status: String? = null,
    val draftName: String? = null,
    val draftIsNsfw: Boolean? = null,
    val draftTags: List<String>? = null,
    val draftModerationAgeRating: String? = null,
    val messageCount: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class CreatorAssistantSessionsResponseDto(
    val items: List<CreatorAssistantSessionSummaryDto>? = null,
    val total: Int? = null,
    val hasMore: Boolean? = null,
)

data class CreatorAssistantMessageDto(
    val messageId: String? = null,
    val role: String? = null,
    val content: String? = null,
    val createdAt: String? = null,
)

data class CreatorAssistantDraftDto(
    val name: String? = null,
    val description: String? = null,
    val greeting: String? = null,
    val personality: String? = null,
    val scenario: String? = null,
    val exampleDialogs: String? = null,
    val tags: List<String>? = null,
    val gender: Int? = null,
    val isNsfw: Boolean? = null,
    val characterType: String? = null,
    val parentCharacterId: String? = null,
    val subCharacters: List<CreatorAssistantSubCharacterDto>? = null,
)

data class CreatorAssistantSubCharacterDto(
    val name: String? = null,
    val description: String? = null,
    val personality: String? = null,
    val avatarBase64: String? = null,
)

data class CreatorAssistantDraftResponseDto(
    val draftId: String? = null,
    val draft: CreatorAssistantDraftDto? = null,
    val confidence: Double? = null,
    val missingFields: List<String>? = null,
)

data class CreatorAssistantSessionHistoryDto(
    val sessionId: String? = null,
    val characterType: String? = null,
    val status: String? = null,
    val messages: List<CreatorAssistantMessageDto>? = null,
    val currentDraft: CreatorAssistantDraftDto? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
