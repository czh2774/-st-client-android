package com.stproject.client.android.features.personas

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.stproject.client.android.domain.model.Persona

@Composable
fun PersonasScreen(
    viewModel: PersonasViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var editorOpen by remember { mutableStateOf(false) }
    var editingPersona by remember { mutableStateOf<Persona?>(null) }
    var deleteTarget by remember { mutableStateOf<Persona?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.common_back))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.personas_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Button(
                    onClick = {
                        editingPersona = null
                        editorOpen = true
                    },
                    enabled = !uiState.isSaving,
                ) {
                    Text(stringResource(R.string.personas_create_button))
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

            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                Text(stringResource(R.string.personas_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = uiState.items, key = { it.id }) { persona ->
                        PersonaRow(
                            persona = persona,
                            onEdit = {
                                editingPersona = persona
                                editorOpen = true
                            },
                            onDelete = { deleteTarget = persona },
                            onSetDefault = { viewModel.setDefault(persona) },
                            isBusy = uiState.isSaving || uiState.isDeleting,
                        )
                    }
                }
            }
        }
    }

    if (editorOpen) {
        val persona = editingPersona
        PersonaEditorDialog(
            title =
                stringResource(
                    if (persona == null) {
                        R.string.personas_create_title
                    } else {
                        R.string.personas_edit_title
                    },
                ),
            initialName = persona?.name ?: "",
            initialDescription = persona?.description ?: "",
            initialAvatarUrl = persona?.avatarUrl ?: "",
            initialIsDefault = persona?.isDefault ?: false,
            isSaving = uiState.isSaving,
            onDismiss = {
                editorOpen = false
                editingPersona = null
            },
            onSubmit = { name, description, avatarUrl, isDefault ->
                if (persona == null) {
                    viewModel.createPersona(name, description, avatarUrl, isDefault)
                } else {
                    viewModel.updatePersona(persona, name, description, avatarUrl, isDefault)
                }
            },
        )
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.personas_delete_title)) },
            text = { Text(stringResource(R.string.personas_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget?.let { viewModel.deletePersona(it) }
                        deleteTarget = null
                    },
                    enabled = !uiState.isDeleting,
                ) {
                    Text(stringResource(R.string.common_delete))
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
private fun PersonaRow(
    persona: Persona,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    isBusy: Boolean,
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
            Text(text = persona.name, style = MaterialTheme.typography.titleMedium)
            if (persona.isDefault) {
                Text(
                    text = stringResource(R.string.personas_default_badge),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        if (persona.description.isNotBlank()) {
            Text(
                text = persona.description,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!persona.isDefault) {
                TextButton(onClick = onSetDefault, enabled = !isBusy) {
                    Text(stringResource(R.string.personas_set_default))
                }
            }
            TextButton(onClick = onEdit, enabled = !isBusy) {
                Text(stringResource(R.string.common_edit))
            }
            TextButton(onClick = onDelete, enabled = !isBusy) {
                Text(stringResource(R.string.common_delete))
            }
        }
    }
}

@Composable
private fun PersonaEditorDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    initialAvatarUrl: String,
    initialIsDefault: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String?, String?, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var avatarUrl by remember { mutableStateOf(initialAvatarUrl) }
    var isDefault by remember { mutableStateOf(initialIsDefault) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.personas_name_label)) },
                    placeholder = { Text(stringResource(R.string.personas_name_placeholder)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.personas_description_label)) },
                    placeholder = { Text(stringResource(R.string.personas_description_placeholder)) },
                )
                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    label = { Text(stringResource(R.string.personas_avatar_label)) },
                    placeholder = { Text(stringResource(R.string.personas_avatar_placeholder)) },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                    )
                    Text(stringResource(R.string.personas_set_default))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        onSubmit(
                            trimmed,
                            description.trim().takeIf { it.isNotEmpty() },
                            avatarUrl.trim().takeIf { it.isNotEmpty() },
                            isDefault,
                        )
                    }
                },
                enabled = name.trim().isNotEmpty() && !isSaving,
            ) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
