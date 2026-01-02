package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.ModelPreset

interface PresetRepository {
    suspend fun listPresets(seriesId: String? = null): List<ModelPreset>
}
