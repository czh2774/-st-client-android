package com.stproject.client.android.features.creators

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import com.stproject.client.android.domain.model.CardCharacterType
import com.stproject.client.android.domain.model.CardVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CreateRoleScreen(
    viewModel: CreateRoleViewModel,
    allowNsfw: Boolean,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var parseText by remember { mutableStateOf("") }
    var parseFileName by remember { mutableStateOf("") }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val bytes = readBytesFromUri(context, uri)
                val fileName = queryDisplayName(context, uri) ?: "card"
                if (bytes == null) {
                    viewModel.setError("import failed")
                } else {
                    viewModel.importFromFile(fileName, bytes)
                }
            }
        }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val json = viewModel.buildExportJson(allowNsfw)
                if (json.isNullOrBlank()) {
                    viewModel.setError("export failed")
                    return@launch
                }
                val ok = writeTextToUri(context, uri, json)
                if (!ok) {
                    viewModel.setError("export failed")
                }
            }
        }
    val exportPngLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val bytes = viewModel.fetchExportPng(allowNsfw)
                if (bytes == null) return@launch
                val ok = writeBytesToUri(context, uri, bytes)
                if (!ok) {
                    viewModel.setError("export failed")
                }
            }
        }

    LaunchedEffect(allowNsfw) {
        if (!allowNsfw && uiState.isNsfw) {
            viewModel.updateIsNsfw(false)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.common_back))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = stringResource(R.string.create_role_title), style = MaterialTheme.typography.titleLarge)
            }

            if (uiState.isLoading || uiState.isSubmitting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

            if (!uiState.createdCharacterId.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.create_role_created, uiState.createdCharacterId ?: ""),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.editCharacterId,
                onValueChange = viewModel::updateEditCharacterId,
                label = { Text(stringResource(R.string.create_role_edit_id)) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = viewModel::loadForEdit, enabled = !uiState.isLoading) {
                    Text(stringResource(R.string.create_role_load))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = viewModel::clear, enabled = !uiState.isSubmitting) {
                    Text(stringResource(R.string.common_clear))
                }
            }

            Text(stringResource(R.string.create_role_editor_mode), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeButton(
                    label = stringResource(R.string.create_role_editor_basic),
                    selected = uiState.editorMode == CardEditorMode.Basic,
                    onClick = { viewModel.updateEditorMode(CardEditorMode.Basic) },
                )
                ModeButton(
                    label = stringResource(R.string.create_role_editor_raw),
                    selected = uiState.editorMode == CardEditorMode.Raw,
                    onClick = { viewModel.updateEditorMode(CardEditorMode.Raw) },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { viewModel.loadTemplate() }) {
                    Text(stringResource(R.string.create_role_template))
                }
                TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                    Text(stringResource(R.string.create_role_import))
                }
                TextButton(onClick = { exportLauncher.launch("${uiState.name.ifBlank { "character" }}.json") }) {
                    Text(stringResource(R.string.create_role_export))
                }
                TextButton(
                    onClick = {
                        if (uiState.activeCharacterId.isNullOrBlank()) {
                            viewModel.setError("character id required")
                        } else {
                            exportPngLauncher.launch("${uiState.name.ifBlank { "character" }}.png")
                        }
                    },
                ) {
                    Text(stringResource(R.string.create_role_export_png))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.create_role_parse_text_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = parseFileName,
                    onValueChange = { parseFileName = it },
                    label = { Text(stringResource(R.string.create_role_parse_text_filename)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = parseText,
                    onValueChange = { parseText = it },
                    label = { Text(stringResource(R.string.create_role_parse_text_content)) },
                    minLines = 5,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = {
                            viewModel.parseTextContent(parseText, parseFileName)
                        },
                        enabled = !uiState.isLoading && !uiState.isSubmitting,
                    ) {
                        Text(stringResource(R.string.create_role_parse_text_action))
                    }
                }
            }

            if (uiState.editorMode == CardEditorMode.Raw) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.rawJson,
                    onValueChange = viewModel::updateRawJson,
                    label = { Text(stringResource(R.string.create_role_raw_json)) },
                    minLines = 8,
                )
            } else {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text(stringResource(R.string.create_role_name)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text(stringResource(R.string.create_role_description)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.personality,
                    onValueChange = viewModel::updatePersonality,
                    label = { Text(stringResource(R.string.create_role_personality)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.scenario,
                    onValueChange = viewModel::updateScenario,
                    label = { Text(stringResource(R.string.create_role_scenario)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.firstMessage,
                    onValueChange = viewModel::updateFirstMessage,
                    label = { Text(stringResource(R.string.create_role_first_message)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.messageExample,
                    onValueChange = viewModel::updateMessageExample,
                    label = { Text(stringResource(R.string.create_role_message_example)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.systemPrompt,
                    onValueChange = viewModel::updateSystemPrompt,
                    label = { Text(stringResource(R.string.create_role_system_prompt)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.postHistoryInstructions,
                    onValueChange = viewModel::updatePostHistoryInstructions,
                    label = { Text(stringResource(R.string.create_role_post_history)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.creatorNotes,
                    onValueChange = viewModel::updateCreatorNotes,
                    label = { Text(stringResource(R.string.create_role_creator_notes)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.characterVersion,
                    onValueChange = viewModel::updateCharacterVersion,
                    label = { Text(stringResource(R.string.create_role_character_version)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.tags,
                    onValueChange = viewModel::updateTags,
                    label = { Text(stringResource(R.string.create_role_tags)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.alternateGreetings,
                    onValueChange = viewModel::updateAlternateGreetings,
                    label = { Text(stringResource(R.string.create_role_alternate_greetings)) },
                    minLines = 2,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.groupOnlyGreetings,
                    onValueChange = viewModel::updateGroupOnlyGreetings,
                    label = { Text(stringResource(R.string.create_role_group_only_greetings)) },
                    minLines = 2,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.avatarUrl,
                    onValueChange = viewModel::updateAvatarUrl,
                    label = { Text(stringResource(R.string.create_role_avatar_url)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.extensionsJson,
                    onValueChange = viewModel::updateExtensionsJson,
                    label = { Text(stringResource(R.string.create_role_extensions_json)) },
                    minLines = 4,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.characterBookJson,
                    onValueChange = viewModel::updateCharacterBookJson,
                    label = { Text(stringResource(R.string.create_role_character_book_json)) },
                    minLines = 4,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.create_role_nsfw))
                    Switch(
                        checked = uiState.isNsfw,
                        onCheckedChange = viewModel::updateIsNsfw,
                        enabled = allowNsfw,
                    )
                }
                if (!allowNsfw) {
                    Text(
                        text = stringResource(R.string.content_mature_disabled_inline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(stringResource(R.string.create_role_visibility), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisibilityButton(
                        label = stringResource(R.string.create_role_visibility_public),
                        selected = uiState.visibility == CardVisibility.Public,
                        onClick = { viewModel.updateVisibility(CardVisibility.Public) },
                    )
                    VisibilityButton(
                        label = stringResource(R.string.create_role_visibility_private),
                        selected = uiState.visibility == CardVisibility.Private,
                        onClick = { viewModel.updateVisibility(CardVisibility.Private) },
                    )
                    VisibilityButton(
                        label = stringResource(R.string.create_role_visibility_share_only),
                        selected = uiState.visibility == CardVisibility.ShareOnly,
                        onClick = { viewModel.updateVisibility(CardVisibility.ShareOnly) },
                    )
                }
                Text(stringResource(R.string.create_role_character_type), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeButton(
                        label = stringResource(R.string.create_role_type_single),
                        selected = uiState.characterType == CardCharacterType.Single,
                        onClick = { viewModel.updateCharacterType(CardCharacterType.Single) },
                    )
                    TypeButton(
                        label = stringResource(R.string.create_role_type_multi),
                        selected = uiState.characterType == CardCharacterType.Multi,
                        onClick = { viewModel.updateCharacterType(CardCharacterType.Multi) },
                    )
                    TypeButton(
                        label = stringResource(R.string.create_role_type_simulator),
                        selected = uiState.characterType == CardCharacterType.Simulator,
                        onClick = { viewModel.updateCharacterType(CardCharacterType.Simulator) },
                    )
                    TypeButton(
                        label = stringResource(R.string.create_role_type_extension),
                        selected = uiState.characterType == CardCharacterType.Extension,
                        onClick = { viewModel.updateCharacterType(CardCharacterType.Extension) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(onClick = { viewModel.submit(allowNsfw) }, enabled = !uiState.isSubmitting) {
                    Text(
                        stringResource(
                            if (uiState.isEditing) {
                                R.string.create_role_save
                            } else {
                                R.string.create_role_submit
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun VisibilityButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(label)
        }
    } else {
        TextButton(onClick = onClick) {
            Text(label)
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(label)
        }
    } else {
        TextButton(onClick = onClick) {
            Text(label)
        }
    }
}

@Composable
private fun TypeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(label)
        }
    } else {
        TextButton(onClick = onClick) {
            Text(label)
        }
    }
}

private fun readBytesFromUri(
    context: android.content.Context,
    uri: android.net.Uri,
): ByteArray? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()
}

private fun queryDisplayName(
    context: android.content.Context,
    uri: android.net.Uri,
): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)
            } else {
                null
            }
        }
    }.getOrNull()
}

private fun writeTextToUri(
    context: android.content.Context,
    uri: android.net.Uri,
    content: String,
): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
        true
    }.getOrDefault(false)
}

private fun writeBytesToUri(
    context: android.content.Context,
    uri: android.net.Uri,
    bytes: ByteArray,
): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        true
    }.getOrDefault(false)
}
