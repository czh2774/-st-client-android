package com.stproject.client.android.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.preferences.UserPreferencesStore
import com.stproject.client.android.domain.model.ModelPreset
import com.stproject.client.android.domain.repository.PresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelPresetsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<ModelPreset> = emptyList(),
    val selectedPresetId: String? = null,
)

@HiltViewModel
class ModelPresetsViewModel
    @Inject
    constructor(
        private val presetRepository: PresetRepository,
        private val userPreferencesStore: UserPreferencesStore,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                ModelPresetsUiState(
                    selectedPresetId = userPreferencesStore.getModelPresetId(),
                ),
            )
        val uiState: StateFlow<ModelPresetsUiState> = _uiState

        fun load(seriesId: String? = null) {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val items =
                        presetRepository
                            .listPresets(seriesId)
                            .filter { it.isEnabled }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = items,
                            selectedPresetId = userPreferencesStore.getModelPresetId(),
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun selectPreset(presetId: String?) {
            userPreferencesStore.setModelPresetId(presetId)
            _uiState.update { it.copy(selectedPresetId = presetId, error = null) }
        }
    }
