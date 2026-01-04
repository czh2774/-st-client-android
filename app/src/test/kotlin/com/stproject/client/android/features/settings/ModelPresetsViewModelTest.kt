package com.stproject.client.android.features.settings

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.preferences.UserPreferencesStore
import com.stproject.client.android.core.theme.ThemeMode
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.ModelPreset
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.PresetRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModelPresetsViewModelTest : BaseUnitTest() {
    private class FakeAccessManager(gate: ContentGate) : ContentAccessManager {
        private val state = MutableStateFlow(gate)
        override val gate: StateFlow<ContentGate> = state

        override fun updateGate(gate: ContentGate) {
            state.value = gate
        }

        override fun decideAccess(isNsfw: Boolean?): ContentAccessDecision {
            return state.value.decideAccess(isNsfw)
        }
    }

    private class FakeCharacterRepository : CharacterRepository {
        override suspend fun queryCharacters(
            cursor: String?,
            limit: Int?,
            sortBy: String?,
            isNsfw: Boolean?,
        ): List<CharacterSummary> = emptyList()

        override suspend fun getCharacterDetail(characterId: String): CharacterDetail {
            return CharacterDetail(
                id = characterId,
                name = "Test",
                description = "",
                tags = emptyList(),
                creatorName = null,
                isNsfw = false,
                totalFollowers = 0,
                isFollowed = false,
            )
        }

        override suspend fun resolveShareCode(shareCode: String): String? = null

        override suspend fun generateShareCode(characterId: String): ShareCodeInfo? = null

        override suspend fun blockCharacter(
            characterId: String,
            value: Boolean,
        ) = Unit

        override suspend fun followCharacter(
            characterId: String,
            value: Boolean,
        ): CharacterFollowResult {
            return CharacterFollowResult(totalFollowers = 0, isFollowed = false)
        }
    }

    private class FakePresetRepository(
        private val presets: List<ModelPreset>,
    ) : PresetRepository {
        override suspend fun listPresets(seriesId: String?): List<ModelPreset> = presets
    }

    private class FakeUserPreferencesStore : UserPreferencesStore {
        private var nsfwAllowed = false
        private var themeMode = ThemeMode.System
        private var languageTag: String? = null
        private var modelPresetId: String? = null
        private var globalVariables: Map<String, Any?> = emptyMap()
        private val presetVariables = mutableMapOf<String, Map<String, Any?>>()

        override fun isNsfwAllowed(): Boolean = nsfwAllowed

        override fun setNsfwAllowed(value: Boolean) {
            nsfwAllowed = value
        }

        override fun getThemeMode(): ThemeMode = themeMode

        override fun setThemeMode(mode: ThemeMode) {
            themeMode = mode
        }

        override fun getLanguageTag(): String? = languageTag

        override fun setLanguageTag(tag: String?) {
            languageTag = tag
        }

        override fun getModelPresetId(): String? = modelPresetId

        override fun setModelPresetId(presetId: String?) {
            modelPresetId = presetId
        }

        override fun getGlobalVariables(): Map<String, Any?> = globalVariables

        override fun setGlobalVariables(variables: Map<String, Any?>) {
            globalVariables = variables
        }

        override fun getPresetVariables(presetId: String): Map<String, Any?> {
            return presetVariables[presetId] ?: emptyMap()
        }

        override fun setPresetVariables(
            presetId: String,
            variables: Map<String, Any?>,
        ) {
            presetVariables[presetId] = variables
        }
    }

    private fun allowResolveContentAccess(): ResolveContentAccessUseCase {
        val gate =
            ContentGate(
                consentLoaded = true,
                consentRequired = false,
                ageVerified = true,
                allowNsfwPreference = false,
            )
        return ResolveContentAccessUseCase(
            accessManager = FakeAccessManager(gate),
            characterRepository = FakeCharacterRepository(),
        )
    }

    @Test
    fun `load filters disabled presets and keeps selection`() =
        runTest(mainDispatcherRule.dispatcher) {
            val enabledPreset =
                ModelPreset(
                    id = "preset-1",
                    displayName = "Default",
                    subtitle = null,
                    description = null,
                    provider = "openai",
                    modelName = "gpt-4o",
                    isDefault = true,
                    isEnabled = true,
                    sortOrder = 1,
                    tags = emptyList(),
                )
            val disabledPreset =
                enabledPreset.copy(
                    id = "preset-2",
                    displayName = "Disabled",
                    isEnabled = false,
                )
            val prefs = FakeUserPreferencesStore().apply { setModelPresetId("preset-1") }
            val viewModel =
                ModelPresetsViewModel(
                    presetRepository = FakePresetRepository(listOf(enabledPreset, disabledPreset)),
                    userPreferencesStore = prefs,
                    resolveContentAccess = allowResolveContentAccess(),
                )

            viewModel.load()
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.items.size)
            assertEquals("preset-1", viewModel.uiState.value.selectedPresetId)
        }

    @Test
    fun `selectPreset updates preferences and state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val prefs = FakeUserPreferencesStore()
            val viewModel =
                ModelPresetsViewModel(
                    presetRepository = FakePresetRepository(emptyList()),
                    userPreferencesStore = prefs,
                    resolveContentAccess = allowResolveContentAccess(),
                )

            viewModel.selectPreset("preset-9")

            assertEquals("preset-9", prefs.getModelPresetId())
            assertEquals("preset-9", viewModel.uiState.value.selectedPresetId)
        }
}
