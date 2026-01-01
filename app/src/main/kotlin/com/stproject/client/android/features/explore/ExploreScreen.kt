package com.stproject.client.android.features.explore

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
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.compliance.NsfwBlockedDialog
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.features.chat.ModerationViewModel
import com.stproject.client.android.features.chat.ReportDialog

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onStartChat: (String, String?) -> Unit,
    onOpenDetail: (String) -> Unit,
    moderationViewModel: ModerationViewModel,
    contentGate: ContentGate,
) {
    val uiState by viewModel.uiState.collectAsState()
    val moderationState by moderationViewModel.uiState.collectAsState()
    var reportTargetId by remember { mutableStateOf<String?>(null) }
    var blockTargetId by remember { mutableStateOf<String?>(null) }
    var nsfwBlockedOpen by remember { mutableStateOf(false) }
    val allowNsfw = contentGate.nsfwAllowed

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.resolvedMemberId, uiState.resolvedShareCode, uiState.resolvedIsNsfw, allowNsfw) {
        val memberId = uiState.resolvedMemberId
        val shareCode = uiState.resolvedShareCode
        if (!memberId.isNullOrBlank() && !shareCode.isNullOrBlank()) {
            if (contentGate.isRestricted(uiState.resolvedIsNsfw)) {
                if (contentGate.isNsfwBlocked(uiState.resolvedIsNsfw)) {
                    nsfwBlockedOpen = true
                }
                viewModel.consumeResolvedShareCode()
                return@LaunchedEffect
            }
            onStartChat(memberId, shareCode)
            viewModel.consumeResolvedShareCode()
        }
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
                Text(
                    text = stringResource(R.string.explore_share_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.shareCodeInput,
                    onValueChange = viewModel::onShareCodeChanged,
                    enabled = !uiState.isResolvingShareCode,
                    label = { Text(stringResource(R.string.explore_share_label)) },
                )
                if (uiState.shareCodeError != null) {
                    Text(
                        text = uiState.shareCodeError ?: "",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = viewModel::resolveShareCode,
                        enabled = !uiState.isResolvingShareCode,
                    ) {
                        Text(stringResource(R.string.explore_share_join))
                    }
                }
            }
            if (uiState.isLoading) {
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
            if (uiState.accessError != null) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    text = uiState.accessError ?: "",
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = uiState.items, key = { it.id }) { item ->
                    CharacterRow(
                        item = item,
                        onStartChat = { character ->
                            if (contentGate.isRestricted(character.isNsfw)) {
                                if (contentGate.isNsfwBlocked(character.isNsfw)) {
                                    nsfwBlockedOpen = true
                                }
                                return@CharacterRow
                            }
                            onStartChat(character.id, null)
                        },
                        onOpenDetail = { character ->
                            if (contentGate.isRestricted(character.isNsfw)) {
                                if (contentGate.isNsfwBlocked(character.isNsfw)) {
                                    nsfwBlockedOpen = true
                                }
                                return@CharacterRow
                            }
                            onOpenDetail(character.id)
                        },
                        onReport = { characterId ->
                            reportTargetId = characterId
                            moderationViewModel.loadReasonsIfNeeded()
                        },
                        onBlock = { characterId ->
                            blockTargetId = characterId
                        },
                        onFollow = { characterId, follow ->
                            viewModel.followCharacter(characterId, follow)
                        },
                    )
                }
            }
        }
    }

    if (reportTargetId != null) {
        ReportDialog(
            state = moderationState,
            onDismiss = { reportTargetId = null },
            onSubmit = { reasons, detail ->
                val targetId = reportTargetId ?: return@ReportDialog
                moderationViewModel.submitReportForCharacter(targetId, reasons, detail)
            },
        )
    }

    LaunchedEffect(moderationState.lastReportSubmitted) {
        if (moderationState.lastReportSubmitted) {
            reportTargetId = null
        }
    }

    if (blockTargetId != null) {
        AlertDialog(
            onDismissRequest = { blockTargetId = null },
            title = { Text(stringResource(R.string.chat_block_title)) },
            text = { Text(stringResource(R.string.chat_block_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = blockTargetId
                        blockTargetId = null
                        if (targetId != null) {
                            moderationViewModel.blockCharacter(targetId)
                        }
                    },
                ) {
                    Text(stringResource(R.string.common_block))
                }
            },
            dismissButton = {
                TextButton(onClick = { blockTargetId = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    NsfwBlockedDialog(
        open = nsfwBlockedOpen,
        onDismiss = { nsfwBlockedOpen = false },
    )

    LaunchedEffect(moderationState.lastBlockSuccess) {
        if (moderationState.lastBlockSuccess) {
            viewModel.load(force = true)
        }
    }
}

@Composable
private fun CharacterRow(
    item: CharacterSummary,
    onStartChat: (CharacterSummary) -> Unit,
    onOpenDetail: (CharacterSummary) -> Unit,
    onReport: (String) -> Unit,
    onBlock: (String) -> Unit,
    onFollow: (String, Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = item.name, style = MaterialTheme.typography.titleMedium)
        if (item.description.isNotBlank()) {
            Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
        }
        if (item.isNsfw) {
            Text(
                text = stringResource(R.string.content_nsfw_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(R.string.explore_followers, item.totalFollowers),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onOpenDetail(item) }) {
                    Text(stringResource(R.string.common_details))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onReport(item.id) }) {
                    Text(stringResource(R.string.common_report))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onBlock(item.id) }) {
                    Text(stringResource(R.string.common_block))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onFollow(item.id, !item.isFollowed) }) {
                    Text(
                        stringResource(
                            if (item.isFollowed) {
                                R.string.common_unfollow
                            } else {
                                R.string.common_follow
                            },
                        ),
                    )
                }
            }
            Button(onClick = { onStartChat(item) }) {
                Text(stringResource(R.string.common_start_chat))
            }
        }
    }
}
