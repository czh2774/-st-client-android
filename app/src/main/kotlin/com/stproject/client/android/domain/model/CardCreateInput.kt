package com.stproject.client.android.domain.model

enum class CardVisibility(val apiValue: String) {
    Public("public"),
    Private("private"),
    ShareOnly("share_only"),
}

enum class CardCharacterType(val apiValue: String) {
    Single("single"),
    Multi("multi"),
    Simulator("simulator"),
    Extension("extension"),
}

data class CardCreateInput(
    val name: String,
    val description: String?,
    val personality: String?,
    val scenario: String?,
    val firstMessage: String?,
    val messageExample: String?,
    val systemPrompt: String?,
    val postHistoryInstructions: String?,
    val tags: List<String>,
    val isNsfw: Boolean,
    val visibility: CardVisibility,
    val characterType: CardCharacterType,
    val avatarUrl: String?,
)

data class CardCreateResult(
    val characterId: String,
    val name: String?,
)
