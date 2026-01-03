package com.stproject.client.android.features.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stproject.client.android.R
import com.stproject.client.android.domain.model.BackgroundItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun BackgroundsScreen(
    viewModel: BackgroundsViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var renameTarget by remember { mutableStateOf<BackgroundItem?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<BackgroundItem?>(null) }
    val uploadLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val bytes = readBytesFromUri(context, uri)
                val fileName = queryDisplayName(context, uri) ?: "background"
                if (bytes == null) {
                    viewModel.setError("upload failed")
                } else {
                    viewModel.upload(fileName, bytes)
                }
            }
        }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val isBusy = uiState.isUploading || uiState.isRenaming || uiState.isDeleting

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
                    text = stringResource(R.string.backgrounds_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = viewModel::load, enabled = !uiState.isLoading) {
                    Text(stringResource(R.string.common_refresh))
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.backgrounds_description), style = MaterialTheme.typography.bodySmall)
                val config = uiState.config
                if (config != null) {
                    Text(
                        text =
                            stringResource(
                                R.string.backgrounds_size_hint,
                                config.width,
                                config.height,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = { uploadLauncher.launch(arrayOf("image/*")) },
                        enabled = !isBusy,
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isUploading) {
                                    R.string.common_loading
                                } else {
                                    R.string.backgrounds_upload
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
                if (uiState.items.isEmpty() && !uiState.isLoading) {
                    item(key = "empty") {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(stringResource(R.string.backgrounds_empty))
                            Text(
                                text = stringResource(R.string.backgrounds_empty_hint),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                items(items = uiState.items, key = { it.name }) { item ->
                    BackgroundRow(
                        item = item,
                        isBusy = isBusy,
                        onCopy = {
                            clipboard.setText(AnnotatedString(item.url))
                        },
                        onRename = {
                            renameTarget = item
                            renameValue = item.name
                        },
                        onDelete = {
                            deleteTarget = item
                        },
                    )
                }
            }
        }
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.backgrounds_rename_title)) },
            text = {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text(stringResource(R.string.backgrounds_rename_hint)) },
                )
            },
            confirmButton = {
                Button(onClick = {
                    val target = renameTarget
                    if (target != null) {
                        val next = renameValue.trim()
                        if (next.isNotEmpty() && next != target.name) {
                            viewModel.rename(target.name, next)
                        }
                    }
                    renameTarget = null
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.backgrounds_delete_title)) },
            text = { Text(stringResource(R.string.backgrounds_delete_hint)) },
            confirmButton = {
                Button(onClick = {
                    val target = deleteTarget
                    if (target != null) {
                        viewModel.delete(target.name)
                    }
                    deleteTarget = null
                }) {
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
private fun BackgroundRow(
    item: BackgroundItem,
    isBusy: Boolean,
    onCopy: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
                .testTag("backgrounds.item.${item.name}"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(96.dp)
                        .clip(MaterialTheme.shapes.medium),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleSmall)
                Text(text = item.url, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCopy, enabled = !isBusy) {
                Text(stringResource(R.string.backgrounds_copy_link))
            }
            TextButton(onClick = onRename, enabled = !isBusy) {
                Text(stringResource(R.string.common_edit))
            }
            TextButton(onClick = onDelete, enabled = !isBusy) {
                Text(stringResource(R.string.common_delete))
            }
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
