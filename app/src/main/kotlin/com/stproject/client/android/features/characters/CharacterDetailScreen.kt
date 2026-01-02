package com.stproject.client.android.features.characters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.compliance.RestrictedContentNotice
import com.stproject.client.android.features.chat.ModerationViewModel
import com.stproject.client.android.features.chat.ReportDialog

@Composable
fun CharacterDetailScreen(
    characterId: String,
    viewModel: CharacterDetailViewModel,
    moderationViewModel: ModerationViewModel,
    onBack: () -> Unit,
    onStartChat: (String, String?) -> Unit,
    onOpenComments: (String) -> Unit,
    contentGate: ContentGate,
) {
    val uiState by viewModel.uiState.collectAsState()
    val moderationState by moderationViewModel.uiState.collectAsState()
    var reportOpen by remember { mutableStateOf(false) }
    var blockConfirmOpen by remember { mutableStateOf(false) }

    LaunchedEffect(characterId) {
        viewModel.load(characterId)
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
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.common_back))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            reportOpen = true
                            moderationViewModel.loadReasonsIfNeeded()
                        },
                        enabled = !moderationState.isSubmitting,
                    ) {
                        Text(stringResource(R.string.common_report))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { blockConfirmOpen = true },
                        enabled = !moderationState.isSubmitting,
                    ) {
                        Text(stringResource(R.string.common_block))
                    }
                }
            }

            if (uiState.isLoading) {
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

            val detail = uiState.detail
            val accessRestricted = contentGate.isRestricted(detail?.isNsfw)
            if (detail != null) {
                val nsfwBlocked = contentGate.isNsfwBlocked(detail.isNsfw)
                Text(text = detail.name, style = MaterialTheme.typography.headlineSmall)
                if (detail.creatorName != null) {
                    Text(
                        text = stringResource(R.string.character_creator, detail.creatorName),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.character_followers, detail.totalFollowers),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (detail.description.isNotBlank()) {
                    Text(text = detail.description, style = MaterialTheme.typography.bodyMedium)
                }
                if (detail.tags.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.character_tags, detail.tags.joinToString()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (nsfwBlocked) {
                    Text(
                        text = stringResource(R.string.content_mature_disabled_inline),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (detail.isNsfw && contentGate.nsfwAllowed) {
                    RestrictedContentNotice(
                        onReport = {
                            reportOpen = true
                            moderationViewModel.loadReasonsIfNeeded()
                        },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onStartChat(characterId, null) },
                    enabled = detail != null && !uiState.isLoading && !accessRestricted,
                ) {
                    Text(stringResource(R.string.common_start_chat))
                }
                Button(
                    onClick = { viewModel.followCharacter(characterId, detail?.isFollowed == false) },
                    enabled = detail != null && !uiState.isLoading,
                ) {
                    Text(
                        stringResource(
                            if (detail?.isFollowed == true) {
                                R.string.common_unfollow
                            } else {
                                R.string.common_follow
                            },
                        ),
                    )
                }
                Button(
                    onClick = { onOpenComments(characterId) },
                    enabled = detail != null && !uiState.isLoading && !accessRestricted,
                ) {
                    Text(stringResource(R.string.common_comments))
                }
                Button(
                    onClick = { viewModel.generateShareCode(characterId) },
                    enabled = detail != null && !uiState.isLoading && !accessRestricted,
                ) {
                    Text(stringResource(R.string.common_share))
                }
            }

            if (!uiState.shareUrl.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.chat_share_code_inline, uiState.shareUrl ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (reportOpen) {
        ReportDialog(
            state = moderationState,
            onDismiss = { reportOpen = false },
            onSubmit = { reasons, detail ->
                moderationViewModel.submitReportForCharacter(characterId, reasons, detail)
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
                        moderationViewModel.blockCharacter(characterId)
                    },
                ) {
                    Text(stringResource(R.string.common_block))
                }
            },
            dismissButton = {
                TextButton(onClick = { blockConfirmOpen = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
