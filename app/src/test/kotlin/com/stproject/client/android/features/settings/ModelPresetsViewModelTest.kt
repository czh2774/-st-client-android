package com.stproject.client.android.features.settings

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.preferences.UserPreferencesStore
import com.stproject.client.android.core.theme.ThemeMode
import com.stproject.client.android.domain.model.ModelPreset
import com.stproject.client.android.domain.repository.PresetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModelPresetsViewModelTest : BaseUnitTest() {
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
                )

            viewModel.selectPreset("preset-9")

            assertEquals("preset-9", prefs.getModelPresetId())
            assertEquals("preset-9", viewModel.uiState.value.selectedPresetId)
        }
}
