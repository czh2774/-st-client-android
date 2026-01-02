package com.stproject.client.android.features.creators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.flags.PlayFeatureFlags
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.CardCharacterType
import com.stproject.client.android.domain.model.CardVisibility
import com.stproject.client.android.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CardEditorMode {
    Basic,
    Raw,
}

data class CreateRoleUiState(
    val editCharacterId: String = "",
    val activeCharacterId: String? = null,
    val name: String = "",
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val firstMessage: String = "",
    val messageExample: String = "",
    val systemPrompt: String = "",
    val postHistoryInstructions: String = "",
    val creatorNotes: String = "",
    val characterVersion: String = "",
    val alternateGreetings: String = "",
    val groupOnlyGreetings: String = "",
    val tags: String = "",
    val avatarUrl: String = "",
    val extensionsJson: String = "",
    val characterBookJson: String = "",
    val isNsfw: Boolean = false,
    val visibility: CardVisibility = CardVisibility.Private,
    val characterType: CardCharacterType = CardCharacterType.Single,
    val editorMode: CardEditorMode = CardEditorMode.Basic,
    val rawJson: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdCharacterId: String? = null,
) {
    val isEditing: Boolean = !activeCharacterId.isNullOrBlank()
}

@HiltViewModel
class CreateRoleViewModel
    @Inject
    constructor(
        private val cardRepository: CardRepository,
    ) : ViewModel() {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        private val _uiState = MutableStateFlow(CreateRoleUiState())
        val uiState: StateFlow<CreateRoleUiState> = _uiState
        private var wrapperDraft: MutableMap<String, Any>? = null

        fun updateEditCharacterId(value: String) {
            _uiState.update { it.copy(editCharacterId = value) }
        }

        fun updateName(value: String) {
            _uiState.update { it.copy(name = value) }
        }

        fun updateDescription(value: String) {
            _uiState.update { it.copy(description = value) }
        }

        fun updatePersonality(value: String) {
            _uiState.update { it.copy(personality = value) }
        }

        fun updateScenario(value: String) {
            _uiState.update { it.copy(scenario = value) }
        }

        fun updateFirstMessage(value: String) {
            _uiState.update { it.copy(firstMessage = value) }
        }

        fun updateMessageExample(value: String) {
            _uiState.update { it.copy(messageExample = value) }
        }

        fun updateSystemPrompt(value: String) {
            _uiState.update { it.copy(systemPrompt = value) }
        }

        fun updatePostHistoryInstructions(value: String) {
            _uiState.update { it.copy(postHistoryInstructions = value) }
        }

        fun updateCreatorNotes(value: String) {
            _uiState.update { it.copy(creatorNotes = value) }
        }

        fun updateCharacterVersion(value: String) {
            _uiState.update { it.copy(characterVersion = value) }
        }

        fun updateAlternateGreetings(value: String) {
            _uiState.update { it.copy(alternateGreetings = value) }
        }

        fun updateGroupOnlyGreetings(value: String) {
            _uiState.update { it.copy(groupOnlyGreetings = value) }
        }

        fun updateTags(value: String) {
            _uiState.update { it.copy(tags = value) }
        }

        fun updateAvatarUrl(value: String) {
            _uiState.update { it.copy(avatarUrl = value) }
        }

        fun updateExtensionsJson(value: String) {
            _uiState.update { it.copy(extensionsJson = value) }
        }

        fun updateCharacterBookJson(value: String) {
            _uiState.update { it.copy(characterBookJson = value) }
        }

        fun updateIsNsfw(value: Boolean) {
            _uiState.update { it.copy(isNsfw = value) }
        }

        fun updateVisibility(value: CardVisibility) {
            _uiState.update { it.copy(visibility = value) }
        }

        fun updateCharacterType(value: CardCharacterType) {
            _uiState.update { it.copy(characterType = value) }
        }

        fun updateEditorMode(mode: CardEditorMode) {
            _uiState.update { it.copy(editorMode = mode) }
        }

        fun updateRawJson(value: String) {
            _uiState.update { it.copy(rawJson = value) }
        }

        fun setError(message: String?) {
            _uiState.update { it.copy(error = message) }
        }

        fun clear() {
            wrapperDraft = null
            _uiState.value = CreateRoleUiState()
        }

        fun loadForEdit() {
            val id = _uiState.value.editCharacterId.trim()
            if (id.isEmpty()) {
                _uiState.update { it.copy(error = "character id required") }
                return
            }
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null, createdCharacterId = null) }
            viewModelScope.launch {
                try {
                    val wrapper = cardRepository.fetchCardWrapper(id)
                    wrapperDraft = wrapper.toMutableMap()
                    val json = gson.toJson(wrapper)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeCharacterId = id,
                            rawJson = json,
                        )
                    }
                    applyWrapperToFields(wrapper)
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun importRawJson(text: String) {
            val parsed =
                parseWrapper(text) ?: run {
                    _uiState.update { it.copy(error = "invalid json") }
                    return
                }
            wrapperDraft = parsed.toMutableMap()
            _uiState.update {
                it.copy(
                    rawJson = gson.toJson(parsed),
                    activeCharacterId = null,
                    createdCharacterId = null,
                    error = null,
                )
            }
            applyWrapperToFields(parsed)
        }

        fun importFromFile(
            fileName: String,
            bytes: ByteArray,
        ) {
            if (_uiState.value.isLoading || _uiState.value.isSubmitting) return
            _uiState.update { it.copy(isLoading = true, error = null, createdCharacterId = null) }
            viewModelScope.launch {
                try {
                    val wrapper = cardRepository.parseCardFile(fileName, bytes)
                    wrapperDraft = wrapper.toMutableMap()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeCharacterId = null,
                            rawJson = gson.toJson(wrapper),
                        )
                    }
                    applyWrapperToFields(wrapper)
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun loadTemplate() {
            if (_uiState.value.isLoading || _uiState.value.isSubmitting) return
            _uiState.update { it.copy(isLoading = true, error = null, createdCharacterId = null) }
            viewModelScope.launch {
                try {
                    val wrapper = cardRepository.fetchTemplate()
                    wrapperDraft = wrapper.toMutableMap()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeCharacterId = null,
                            rawJson = gson.toJson(wrapper),
                        )
                    }
                    applyWrapperToFields(wrapper)
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun parseTextContent(
            content: String,
            fileName: String?,
        ) {
            val trimmed = content.trim()
            if (trimmed.isEmpty()) {
                _uiState.update { it.copy(error = "content required") }
                return
            }
            if (_uiState.value.isLoading || _uiState.value.isSubmitting) return
            _uiState.update { it.copy(isLoading = true, error = null, createdCharacterId = null) }
            viewModelScope.launch {
                try {
                    val wrapper = cardRepository.parseCardText(trimmed, fileName?.trim()?.takeIf { it.isNotEmpty() })
                    wrapperDraft = wrapper.toMutableMap()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeCharacterId = null,
                            rawJson = gson.toJson(wrapper),
                        )
                    }
                    applyWrapperToFields(wrapper)
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        suspend fun fetchExportPng(allowNsfw: Boolean): ByteArray? {
            val state = _uiState.value
            val id = state.activeCharacterId?.trim()?.takeIf { it.isNotEmpty() }
            if (id == null) {
                _uiState.update { it.copy(error = "character id required") }
                return null
            }
            if (state.isSubmitting) return null
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            return try {
                if (state.isNsfw && !allowNsfw) {
                    _uiState.update { it.copy(error = "mature content disabled") }
                    null
                } else {
                    cardRepository.fetchExportPng(id)
                }
            } catch (e: ApiException) {
                _uiState.update { it.copy(error = e.userMessage ?: e.message) }
                null
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                _uiState.update { it.copy(error = "unexpected error") }
                null
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }

        fun buildExportJson(allowNsfw: Boolean): String? {
            val wrapper = buildWrapperForSubmit(allowNsfw) ?: return null
            return gson.toJson(wrapper)
        }

        fun submit(allowNsfw: Boolean) {
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(isSubmitting = true, error = null, createdCharacterId = null) }
            viewModelScope.launch {
                try {
                    val wrapper =
                        buildWrapperForSubmit(allowNsfw) ?: run {
                            _uiState.update { it.copy(isSubmitting = false) }
                            return@launch
                        }
                    val activeId = _uiState.value.activeCharacterId
                    val result =
                        if (!activeId.isNullOrBlank()) {
                            cardRepository.updateCardFromWrapper(activeId, wrapper)
                        } else {
                            cardRepository.createCardFromWrapper(wrapper)
                        }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            createdCharacterId = result.characterId,
                            error = null,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSubmitting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSubmitting = false, error = "unexpected error") }
                }
            }
        }

        private fun buildWrapperForSubmit(allowNsfw: Boolean): Map<String, Any>? {
            val state = _uiState.value
            val mode = state.editorMode
            if (mode == CardEditorMode.Basic && state.name.trim().isEmpty()) {
                _uiState.update { it.copy(error = "name is required") }
                return null
            }
            if (mode == CardEditorMode.Raw && state.rawJson.isBlank()) {
                _uiState.update { it.copy(error = "json required") }
                return null
            }
            val wrapper =
                if (mode == CardEditorMode.Raw && state.rawJson.isNotBlank()) {
                    parseWrapper(state.rawJson) ?: run {
                        _uiState.update { it.copy(error = "invalid json") }
                        return null
                    }
                } else {
                    val base = wrapperDraft?.toMutableMap() ?: mutableMapOf()
                    if (!applyFieldsToWrapper(base)) {
                        return null
                    }
                    base
                }
            normalizeWrapper(wrapper)
            if (!PlayFeatureFlags.extensionsEnabled && hasExtensions(wrapper)) {
                _uiState.update { it.copy(error = "extensions disabled for Play builds") }
                return null
            }
            val name = resolveName(wrapper)
            if (name.isBlank()) {
                _uiState.update { it.copy(error = "name is required") }
                return null
            }
            val isNsfw = resolveIsNsfw(wrapper)
            if (isNsfw && !allowNsfw) {
                _uiState.update { it.copy(error = "mature content disabled") }
                return null
            }
            return wrapper
        }

        private fun resolveIsNsfw(wrapper: Map<String, Any>): Boolean {
            val data = wrapper["data"] as? Map<*, *> ?: return false
            val value = data["isNsfw"]
            return value as? Boolean ?: false
        }

        private fun hasExtensions(wrapper: Map<String, Any>): Boolean {
            val data = wrapper["data"] as? Map<*, *> ?: return false
            if (data["extensions"] != null) return true
            val type = parseCharacterType(asString(data["characterType"]))
            return type == CardCharacterType.Extension
        }

        private fun resolveName(wrapper: Map<String, Any>): String {
            val data = wrapper["data"] as? Map<*, *> ?: return ""
            return asString(data["name"]).trim()
        }

        private fun parseWrapper(text: String): MutableMap<String, Any>? {
            return try {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val parsed = gson.fromJson<Map<String, Any>>(text, type) ?: return null
                parsed.toMutableMap()
            } catch (_: Exception) {
                null
            }
        }

        private fun applyWrapperToFields(wrapper: Map<String, Any>) {
            val data = wrapper["data"] as? Map<*, *> ?: emptyMap<String, Any>()
            val extensions = data["extensions"]
            val characterBook = data["character_book"] ?: data["characterBook"]
            _uiState.update {
                it.copy(
                    name = asString(data["name"]),
                    description = asString(data["description"]),
                    personality = asString(data["personality"]),
                    scenario = asString(data["scenario"]),
                    firstMessage = asString(data["first_mes"]),
                    messageExample = asString(data["mes_example"]),
                    systemPrompt = asString(data["system_prompt"]),
                    postHistoryInstructions = asString(data["post_history_instructions"]),
                    creatorNotes = asString(data["creator_notes"]),
                    characterVersion = asString(data["character_version"]),
                    alternateGreetings = asStringList(data["alternate_greetings"]).joinToString("\n"),
                    groupOnlyGreetings = asStringList(data["group_only_greetings"]).joinToString("\n"),
                    tags = asStringList(data["tags"]).joinToString(", "),
                    avatarUrl = asString(data["avatar"]).takeIf { it != "none" } ?: "",
                    extensionsJson = encodeJsonOrEmpty(extensions),
                    characterBookJson = encodeJsonOrEmpty(characterBook),
                    isNsfw = asBoolean(data["isNsfw"]),
                    visibility = parseVisibility(asString(data["visibility"])),
                    characterType = parseCharacterType(asString(data["characterType"])),
                )
            }
        }

        private fun applyFieldsToWrapper(wrapper: MutableMap<String, Any>): Boolean {
            val data = mutableMapOf<String, Any>()
            val existing = wrapper["data"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
            for ((key, value) in existing) {
                if (key != null && value != null) {
                    data[key.toString()] = value
                }
            }
            wrapper["data"] = data
            val state = _uiState.value
            val name = state.name.trim()
            if (name.isNotEmpty()) {
                data["name"] = name
            }
            data["description"] = state.description.trim()
            data["personality"] = state.personality.trim()
            data["scenario"] = state.scenario.trim()
            data["first_mes"] = state.firstMessage.trim()
            data["mes_example"] = state.messageExample.trim()
            data["system_prompt"] = state.systemPrompt.trim()
            data["post_history_instructions"] = state.postHistoryInstructions.trim()
            data["creator_notes"] = state.creatorNotes.trim()
            data["character_version"] = state.characterVersion.trim()
            val alternateGreetings = parseLines(state.alternateGreetings)
            if (alternateGreetings.isNotEmpty()) {
                data["alternate_greetings"] = alternateGreetings
            }
            val groupOnlyGreetings = parseLines(state.groupOnlyGreetings)
            if (groupOnlyGreetings.isNotEmpty()) {
                data["group_only_greetings"] = groupOnlyGreetings
            }
            val tags = parseTags(state.tags)
            if (tags.isNotEmpty()) {
                data["tags"] = tags
            }
            data["visibility"] = state.visibility.apiValue
            data["isNsfw"] = state.isNsfw
            data["characterType"] = state.characterType.apiValue
            val avatar = state.avatarUrl.trim()
            if (avatar.isNotEmpty()) {
                data["avatar"] = avatar
            }
            val extensions = parseJsonObject(state.extensionsJson)
            if (extensions == null && state.extensionsJson.isNotBlank()) {
                _uiState.update { it.copy(error = "invalid extensions json") }
                return false
            }
            if (extensions != null) {
                data["extensions"] = extensions
            }
            val characterBook = parseJsonObject(state.characterBookJson)
            if (characterBook == null && state.characterBookJson.isNotBlank()) {
                _uiState.update { it.copy(error = "invalid lorebook json") }
                return false
            }
            if (characterBook != null) {
                data["character_book"] = characterBook
            }
            return true
        }

        private fun normalizeWrapper(wrapper: MutableMap<String, Any>) {
            wrapper["spec"] = "chara_card_v3"
            wrapper["spec_version"] = "3.0"
            if (wrapper["data"] !is Map<*, *>) {
                wrapper["data"] = mutableMapOf<String, Any>()
            }
        }

        private fun parseTags(raw: String): List<String> {
            return raw.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

        private fun parseLines(raw: String): List<String> {
            return raw.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

        private fun asString(value: Any?): String {
            return when (value) {
                is String -> value
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> ""
            }
        }

        private fun asBoolean(value: Any?): Boolean {
            return when (value) {
                is Boolean -> value
                is String -> value.equals("true", ignoreCase = true)
                is Number -> value.toInt() != 0
                else -> false
            }
        }

        private fun asStringList(value: Any?): List<String> {
            if (value is List<*>) {
                return value.mapNotNull { it?.toString()?.trim()?.takeIf { v -> v.isNotEmpty() } }
            }
            return emptyList()
        }

        private fun encodeJsonOrEmpty(value: Any?): String {
            if (value == null) return ""
            return runCatching { gson.toJson(value) }.getOrDefault("")
        }

        private fun parseJsonObject(raw: String): Map<String, Any>? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            return try {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(trimmed, type)
            } catch (_: Exception) {
                null
            }
        }

        private fun parseVisibility(raw: String): CardVisibility {
            return CardVisibility.values().firstOrNull { it.apiValue == raw } ?: CardVisibility.Private
        }

        private fun parseCharacterType(raw: String): CardCharacterType {
            return CardCharacterType.values().firstOrNull { it.apiValue == raw } ?: CardCharacterType.Single
        }
    }
