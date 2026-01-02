package com.stproject.client.android.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import com.stproject.client.android.domain.model.ModelPreset

@Composable
fun ModelPresetsScreen(
    viewModel: ModelPresetsViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.common_back))
                }
                Text(
                    text = stringResource(R.string.model_presets_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                val selectedPresetId = uiState.selectedPresetId
                if (selectedPresetId != null) {
                    Button(onClick = { viewModel.selectPreset(null) }) {
                        Text(stringResource(R.string.model_presets_use_default))
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            if (uiState.isLoading && uiState.items.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            if (uiState.error != null) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = uiState.items, key = { it.id }) { item ->
                    ModelPresetRow(
                        item = item,
                        isSelected =
                            uiState.selectedPresetId == item.id ||
                                (uiState.selectedPresetId == null && item.isDefault),
                        onSelect = { viewModel.selectPreset(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelPresetRow(
    item: ModelPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(text = item.displayName, style = MaterialTheme.typography.titleSmall)
                if (!item.subtitle.isNullOrBlank()) {
                    Text(text = item.subtitle ?: "", style = MaterialTheme.typography.bodySmall)
                }
                if (item.isDefault) {
                    Text(
                        text = stringResource(R.string.model_presets_default),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (isSelected) {
                Button(onClick = {}, enabled = false) {
                    Text(stringResource(R.string.model_presets_selected))
                }
            } else {
                Button(onClick = onSelect) {
                    Text(stringResource(R.string.common_apply))
                }
            }
        }
        if (!item.description.isNullOrBlank()) {
            Text(text = item.description ?: "", style = MaterialTheme.typography.bodySmall)
        }
        val provider = item.provider ?: stringResource(R.string.model_presets_unknown_provider)
        val modelName = item.modelName ?: stringResource(R.string.model_presets_unknown_model)
        Text(
            text = stringResource(R.string.model_presets_provider_model, provider, modelName),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
