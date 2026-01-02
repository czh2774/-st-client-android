package com.stproject.client.android.features.worldinfo

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import com.stproject.client.android.domain.model.WorldInfoEntry

@Composable
fun WorldInfoScreen(
    viewModel: WorldInfoViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var deleteTarget by remember { mutableStateOf<WorldInfoEntry?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.common_back))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = stringResource(R.string.worldinfo_title), style = MaterialTheme.typography.titleMedium)
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.scopeCharacterId,
                    onValueChange = viewModel::setScopeCharacterId,
                    label = { Text(stringResource(R.string.worldinfo_scope_character_id)) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.worldinfo_include_global))
                    Switch(
                        checked = uiState.includeGlobal,
                        onCheckedChange = viewModel::setIncludeGlobal,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = viewModel::applyScope, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.common_apply))
                    }
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.worldinfo_editor_title), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.editor.characterId,
                    onValueChange = viewModel::updateEditorCharacterId,
                    label = { Text(stringResource(R.string.worldinfo_editor_character_id)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.editor.keys,
                    onValueChange = viewModel::updateEditorKeys,
                    label = { Text(stringResource(R.string.worldinfo_editor_keys)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.editor.content,
                    onValueChange = viewModel::updateEditorContent,
                    label = { Text(stringResource(R.string.worldinfo_editor_content)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.editor.comment,
                    onValueChange = viewModel::updateEditorComment,
                    label = { Text(stringResource(R.string.worldinfo_editor_comment)) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.worldinfo_editor_enabled))
                    Switch(
                        checked = uiState.editor.enabled,
                        onCheckedChange = viewModel::updateEditorEnabled,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = viewModel::clearEditor, enabled = !uiState.isSubmitting) {
                        Text(stringResource(R.string.common_clear))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = viewModel::submitEntry, enabled = !uiState.isSubmitting) {
                        Text(
                            stringResource(
                                if (uiState.editor.isEditing) {
                                    R.string.common_update
                                } else {
                                    R.string.common_add
                                },
                            ),
                        )
                    }
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
                    WorldInfoRow(
                        item = item,
                        onEdit = { viewModel.selectEntry(item) },
                        onDelete = { deleteTarget = item },
                    )
                }
            }
        }
    }

    if (deleteTarget != null) {
        val entry = deleteTarget
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.worldinfo_delete_title)) },
            text = { Text(stringResource(R.string.worldinfo_delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        entry?.let { viewModel.deleteEntry(it.id) }
                        deleteTarget = null
                    },
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun WorldInfoRow(
    item: WorldInfoEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = item.keys.joinToString(", "), style = MaterialTheme.typography.titleSmall)
        if (item.characterId != null) {
            Text(text = stringResource(R.string.worldinfo_item_character_id, item.characterId))
        } else {
            Text(text = stringResource(R.string.worldinfo_item_global))
        }
        Text(text = item.content, style = MaterialTheme.typography.bodyMedium)
        if (!item.comment.isNullOrBlank()) {
            Text(text = item.comment ?: "", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text =
                stringResource(
                    if (item.enabled) {
                        R.string.worldinfo_item_enabled
                    } else {
                        R.string.worldinfo_item_disabled
                    },
                ),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.common_edit))
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.common_delete))
            }
        }
    }
}
