package com.stproject.client.android.domain.model

data class CreatorAssistantSessionSummary(
    val sessionId: String,
    val characterType: String?,
    val status: String?,
    val draftName: String?,
    val messageCount: Int,
    val createdAt: String?,
    val updatedAt: String?,
)

data class CreatorAssistantMessage(
    val messageId: String,
    val role: String,
    val content: String,
    val createdAt: String?,
)

data class CreatorAssistantDraft(
    val name: String?,
    val description: String?,
    val greeting: String?,
    val personality: String?,
    val scenario: String?,
    val exampleDialogs: String?,
    val tags: List<String>,
    val gender: Int,
    val isNsfw: Boolean,
    val characterType: String?,
    val parentCharacterId: String?,
    val subCharacters: List<CreatorAssistantSubCharacter>,
)

data class CreatorAssistantSubCharacter(
    val name: String?,
    val description: String?,
    val personality: String?,
    val avatarBase64: String?,
)

data class CreatorAssistantDraftResult(
    val draftId: String,
    val draft: CreatorAssistantDraft,
    val confidence: Double,
    val missingFields: List<String>,
)

data class CreatorAssistantSessionHistory(
    val sessionId: String,
    val characterType: String?,
    val status: String?,
    val messages: List<CreatorAssistantMessage>,
    val currentDraft: CreatorAssistantDraft?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class CreatorAssistantChatResult(
    val messageId: String,
    val content: String,
    val suggestions: List<String>,
    val draftReady: Boolean,
)

data class CreatorAssistantPublishResult(
    val characterId: String,
    val name: String?,
    val avatarUrl: String?,
    val backgroundUrl: String?,
    val shareCode: String?,
)
