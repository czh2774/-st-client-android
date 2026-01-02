package com.stproject.client.android.features.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.compliance.RestrictedContentNotice
import com.stproject.client.android.domain.model.A2UIAction
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatRole

private const val SEND_BUTTON_TEST_TAG = "chat.send"

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    moderationViewModel: ModerationViewModel,
    onBackToList: () -> Unit,
    contentGate: ContentGate,
    onOpenWallet: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenShop: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val variablesState by viewModel.variablesUiState.collectAsState()
    val a2uiState by viewModel.a2uiState.collectAsState()
    val moderationState by moderationViewModel.uiState.collectAsState()
    var reportOpen by remember { mutableStateOf(false) }
    var blockConfirmOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var deleteAfter by remember { mutableStateOf(false) }
    var variablesOpen by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val lastAssistant = uiState.messages.lastOrNull { it.role == ChatRole.Assistant }
    val canActOnLast =
        lastAssistant?.serverId != null &&
            !uiState.isActionRunning &&
            !uiState.isSending &&
            lastAssistant?.isStreaming != true

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
                TextButton(onClick = onBackToList) {
                    Text(stringResource(R.string.chat_nav_chats))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { variablesOpen = true },
                        enabled = !uiState.isActionRunning && !variablesState.isSaving,
                    ) {
                        Text(stringResource(R.string.chat_action_variables))
                    }
                    TextButton(
                        onClick = viewModel::requestShareCode,
                        enabled = !uiState.isActionRunning && !uiState.isSending,
                    ) {
                        Text(stringResource(R.string.chat_action_share))
                    }
                    TextButton(
                        onClick = {
                            reportOpen = true
                            moderationViewModel.loadReasonsIfNeeded()
                        },
                        enabled = !moderationState.isSubmitting,
                    ) {
                        Text(stringResource(R.string.chat_action_report))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { blockConfirmOpen = true },
                        enabled = !moderationState.isSubmitting,
                    ) {
                        Text(stringResource(R.string.chat_action_block))
                    }
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
            if (moderationState.error != null) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    text = moderationState.error ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            if (a2uiState != null) {
                A2UISurfacesPanel(
                    state = a2uiState,
                    isBusy = uiState.isSending || uiState.isActionRunning,
                    onAction = { action ->
                        viewModel.onA2UIAction(action)
                        handleA2UINavigation(action, onOpenWallet, onOpenSettings, onOpenShop)
                    },
                )
            }
            if (uiState.activeCharacterIsNsfw == true && contentGate.nsfwAllowed) {
                RestrictedContentNotice(
                    onReport = {
                        reportOpen = true
                        moderationViewModel.loadReasonsIfNeeded()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = uiState.messages, key = { it.id }) { msg ->
                    ChatMessageRow(
                        msg = msg,
                        isBusy = uiState.isActionRunning || uiState.isSending,
                        onCopy = { message ->
                            clipboard.setText(AnnotatedString(message.content))
                        },
                        onDelete = { message ->
                            deleteTarget = message
                            deleteAfter = false
                        },
                        onDeleteAfter = { message ->
                            deleteTarget = message
                            deleteAfter = true
                        },
                        onSwipePrev = { message ->
                            val swipeId = message.swipeId ?: return@ChatMessageRow
                            viewModel.setActiveSwipe(message, swipeId - 1)
                        },
                        onSwipeNext = { message ->
                            val swipeId = message.swipeId ?: return@ChatMessageRow
                            viewModel.setActiveSwipe(message, swipeId + 1)
                        },
                        onDeleteSwipe = { message ->
                            viewModel.deleteSwipe(message, null)
                        },
                    )
                }
            }

            if (lastAssistant != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { viewModel.regenerateMessage(lastAssistant) },
                        enabled = canActOnLast,
                    ) {
                        Text(stringResource(R.string.chat_regenerate))
                    }
                    Button(
                        onClick = { viewModel.continueMessage(lastAssistant) },
                        enabled = canActOnLast,
                    ) {
                        Text(stringResource(R.string.chat_continue))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = uiState.input,
                    onValueChange = viewModel::onInputChanged,
                    enabled = !uiState.isSending && !uiState.isActionRunning,
                    label = { Text(stringResource(R.string.chat_input_label)) },
                )
                Button(
                    modifier = Modifier.testTag(SEND_BUTTON_TEST_TAG),
                    onClick = viewModel::onSendClicked,
                    enabled = !uiState.isSending && !uiState.isActionRunning,
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(stringResource(R.string.chat_send))
                }
            }
        }
    }

    if (reportOpen) {
        ReportDialog(
            state = moderationState,
            onDismiss = { reportOpen = false },
            onSubmit = { reasons, detail ->
                moderationViewModel.submitReport(reasons, detail)
            },
        )
    }

    LaunchedEffect(moderationState.lastReportSubmitted) {
        if (moderationState.lastReportSubmitted) {
            reportOpen = false
        }
    }

    if (blockConfirmOpen) {
        AlertDialog(
            onDismissRequest = { blockConfirmOpen = false },
            title = { Text(stringResource(R.string.chat_block_title)) },
            text = { Text(stringResource(R.string.chat_block_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        blockConfirmOpen = false
                        moderationViewModel.blockDefaultCharacter()
                    },
                ) {
                    Text(stringResource(R.string.chat_block_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { blockConfirmOpen = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    LaunchedEffect(variablesOpen) {
        if (variablesOpen) {
            viewModel.loadVariables()
        }
    }

    if (variablesOpen) {
        AlertDialog(
            onDismissRequest = { variablesOpen = false },
            title = { Text(stringResource(R.string.chat_variables_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (variablesState.isLoading) {
                        Text(stringResource(R.string.chat_variables_loading))
                    }
                    variablesState.error?.let { err ->
                        Text(
                            text =
                                if (err == "invalid json") {
                                    stringResource(R.string.chat_variables_invalid_json)
                                } else {
                                    err
                                },
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = variablesState.text,
                        onValueChange = viewModel::updateVariablesText,
                        label = { Text(stringResource(R.string.chat_variables_label)) },
                        enabled = !variablesState.isLoading && !variablesState.isSaving,
                        minLines = 6,
                        maxLines = 12,
                    )
                    Text(
                        text = stringResource(R.string.chat_variables_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::saveVariables,
                    enabled =
                        variablesState.isDirty &&
                            !variablesState.isLoading &&
                            !variablesState.isSaving,
                ) {
                    if (variablesState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(stringResource(R.string.chat_variables_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { variablesOpen = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (uiState.shareInfo != null) {
        val shareInfo = uiState.shareInfo
        val shareCode = shareInfo?.shareCode?.trim().orEmpty()
        val appLink = shareCode.takeIf { it.isNotEmpty() }?.let { "stproject://share/c/$it" }
        val shareText =
            shareInfo?.shareUrl?.trim().takeIf { !it.isNullOrEmpty() }
                ?: appLink
                ?: shareCode
        AlertDialog(
            onDismissRequest = viewModel::clearShareInfo,
            title = { Text(stringResource(R.string.chat_share_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (shareInfo?.shareUrl?.isNullOrBlank() == false) {
                            stringResource(R.string.chat_share_link_label, shareText)
                        } else {
                            stringResource(R.string.chat_share_code_label, shareText)
                        },
                    )
                    if (appLink != null && shareText != appLink) {
                        Text(stringResource(R.string.chat_app_link_label, appLink))
                        TextButton(onClick = { clipboard.setText(AnnotatedString(appLink)) }) {
                            Text(stringResource(R.string.chat_copy_app_link))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(shareText))
                        viewModel.clearShareInfo()
                    },
                ) {
                    Text(stringResource(R.string.common_copy))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearShareInfo) {
                    Text(stringResource(R.string.common_close))
                }
            },
        )
    }

    if (deleteTarget != null) {
        val target = deleteTarget
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text(
                    stringResource(
                        if (deleteAfter) {
                            R.string.chat_delete_after_title
                        } else {
                            R.string.chat_delete_title
                        },
                    ),
                )
            },
            text = {
                Text(
                    if (deleteAfter) {
                        stringResource(R.string.chat_delete_after_body)
                    } else {
                        stringResource(R.string.chat_delete_body)
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteTarget = null
                        if (target != null) {
                            viewModel.deleteMessage(target, deleteAfter)
                        }
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
@OptIn(ExperimentalFoundationApi::class)
private fun ChatMessageRow(
    msg: ChatMessage,
    isBusy: Boolean,
    onCopy: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit,
    onDeleteAfter: (ChatMessage) -> Unit,
    onSwipePrev: (ChatMessage) -> Unit,
    onSwipeNext: (ChatMessage) -> Unit,
    onDeleteSwipe: (ChatMessage) -> Unit,
) {
    val prefix =
        when (msg.role) {
            ChatRole.System -> stringResource(R.string.chat_label_system)
            ChatRole.User -> stringResource(R.string.chat_label_user)
            ChatRole.Assistant -> stringResource(R.string.chat_label_assistant)
        }
    val canDelete = msg.serverId != null && !isBusy && !msg.isStreaming
    var menuOpen by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { menuOpen = true },
                        ),
                text = "[$prefix] ${msg.content}",
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.common_copy)) },
                    onClick = {
                        menuOpen = false
                        onCopy(msg)
                    },
                )
                if (canDelete) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete(msg)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_delete_after)) },
                        onClick = {
                            menuOpen = false
                            onDeleteAfter(msg)
                        },
                    )
                }
            }
        }
        val swipeCount = msg.swipes.size
        val swipeId = msg.swipeId
        if (swipeId != null && swipeCount > 1 && msg.role == ChatRole.Assistant) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.chat_swipe_label, swipeId + 1, swipeCount),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(
                    onClick = { onSwipePrev(msg) },
                    enabled = !isBusy && swipeId > 0,
                ) {
                    Text(stringResource(R.string.chat_swipe_prev))
                }
                TextButton(
                    onClick = { onSwipeNext(msg) },
                    enabled = !isBusy && swipeId < swipeCount - 1,
                ) {
                    Text(stringResource(R.string.chat_swipe_next))
                }
                TextButton(
                    onClick = { onDeleteSwipe(msg) },
                    enabled = !isBusy && swipeCount > 1,
                ) {
                    Text(stringResource(R.string.chat_swipe_delete))
                }
            }
        }
        if (canDelete) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onDelete(msg) }) {
                    Text(stringResource(R.string.chat_delete))
                }
                TextButton(onClick = { onDeleteAfter(msg) }) {
                    Text(stringResource(R.string.chat_delete_after))
                }
            }
        }
    }
}

private fun handleA2UINavigation(
    action: A2UIAction,
    onOpenWallet: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenShop: () -> Unit,
) {
    when (action.normalizedName) {
        "purchase" -> onOpenShop()
        "navigate" -> {
            when (action.contextString("destination")?.lowercase()) {
                "wallet" -> onOpenWallet()
                "settings" -> onOpenSettings()
                "shop" -> onOpenShop()
            }
        }
    }
}
