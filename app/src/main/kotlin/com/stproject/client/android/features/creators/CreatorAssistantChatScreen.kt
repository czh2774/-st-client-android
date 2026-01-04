package com.stproject.client.android.features.creators

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
import com.stproject.client.android.core.compliance.RestrictedContentNotice
import com.stproject.client.android.domain.model.CreatorAssistantDraft
import com.stproject.client.android.domain.model.CreatorAssistantMessage
import com.stproject.client.android.features.chat.ModerationViewModel
import com.stproject.client.android.features.chat.ReportDialog

@Composable
fun CreatorAssistantChatScreen(
    sessionId: String,
    viewModel: CreatorAssistantChatViewModel,
    moderationViewModel: ModerationViewModel,
    onBack: () -> Unit,
    contentGate: ContentGate,
) {
    val uiState by viewModel.uiState.collectAsState()
    val moderationState by moderationViewModel.uiState.collectAsState()
    var reportOpen by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
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
                    text = stringResource(R.string.assistant_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (contentGate.nsfwAllowed) {
                    RestrictedContentNotice(
                        onReport = {
                            reportOpen = true
                            moderationViewModel.loadReasonsIfNeeded()
                        },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = viewModel::generateDraft,
                        enabled = !uiState.isDrafting,
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isDrafting) {
                                    R.string.assistant_generating
                                } else {
                                    R.string.assistant_generate_draft
                                },
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = viewModel::publishDraft,
                        enabled = uiState.draftResult != null && !uiState.isPublishing,
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isPublishing) {
                                    R.string.assistant_publishing
                                } else {
                                    R.string.assistant_publish
                                },
                            ),
                        )
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

            if (uiState.draftReady && uiState.draftResult == null) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    text = stringResource(R.string.assistant_draft_ready),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            val draft = uiState.draftResult?.draft ?: uiState.currentDraft
            if (draft != null) {
                DraftPreview(draft = draft, draftMeta = uiState.draftResult)
            }

            if (uiState.publishResult != null) {
                val result = uiState.publishResult
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    text =
                        stringResource(
                            R.string.assistant_published,
                            result?.name ?: result?.characterId.orEmpty(),
                        ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = uiState.messages, key = { it.messageId }) { msg ->
                    CreatorAssistantMessageRow(msg)
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
                    enabled = !uiState.isSending,
                    label = { Text(stringResource(R.string.common_message)) },
                )
                Button(
                    onClick = viewModel::sendMessage,
                    enabled = !uiState.isSending,
                ) {
                    Text(
                        stringResource(
                            if (uiState.isSending) {
                                R.string.assistant_sending
                            } else {
                                R.string.common_send
                            },
                        ),
                    )
                }
            }
        }
    }

    if (reportOpen) {
        ReportDialog(
            state = moderationState,
            onDismiss = { reportOpen = false },
            onSubmit = { reasons, detail ->
                val targetId =
                    uiState.publishResult?.characterId
                        ?: uiState.draftResult?.draft?.parentCharacterId
                        ?: uiState.currentDraft?.parentCharacterId
                if (!targetId.isNullOrBlank()) {
                    moderationViewModel.submitReportForCharacter(targetId, reasons, detail)
                } else {
                    moderationViewModel.submitReport(reasons, detail)
                }
            },
        )
    }

    LaunchedEffect(moderationState.lastReportSubmitted) {
        if (moderationState.lastReportSubmitted) {
            reportOpen = false
        }
    }
}

@Composable
private fun CreatorAssistantMessageRow(msg: CreatorAssistantMessage) {
    val label =
        if (msg.role == "user") {
            stringResource(R.string.assistant_label_user)
        } else {
            stringResource(R.string.assistant_label_assistant)
        }
    Text(text = "[$label] ${msg.content}")
}

@Composable
private fun DraftPreview(
    draft: CreatorAssistantDraft,
    draftMeta: com.stproject.client.android.domain.model.CreatorAssistantDraftResult?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = stringResource(R.string.assistant_draft_preview), style = MaterialTheme.typography.titleSmall)
        if (!draft.name.isNullOrBlank()) {
            Text(text = stringResource(R.string.assistant_draft_name, draft.name))
        }
        if (!draft.description.isNullOrBlank()) {
            Text(text = stringResource(R.string.assistant_draft_description, draft.description))
        }
        if (!draft.greeting.isNullOrBlank()) {
            Text(text = stringResource(R.string.assistant_draft_greeting, draft.greeting))
        }
        if (!draft.personality.isNullOrBlank()) {
            Text(text = stringResource(R.string.assistant_draft_personality, draft.personality))
        }
        if (!draft.scenario.isNullOrBlank()) {
            Text(text = stringResource(R.string.assistant_draft_scenario, draft.scenario))
        }
        if (draft.tags.isNotEmpty()) {
            Text(text = stringResource(R.string.assistant_draft_tags, draft.tags.joinToString()))
        }
        val nsfwLabel = draft.isNsfw?.toString() ?: "unknown"
        Text(text = stringResource(R.string.assistant_draft_nsfw, nsfwLabel))
        if (draftMeta != null) {
            Text(text = stringResource(R.string.assistant_draft_confidence, draftMeta.confidence))
            if (draftMeta.missingFields.isNotEmpty()) {
                Text(text = stringResource(R.string.assistant_draft_missing, draftMeta.missingFields.joinToString()))
            }
        }
    }
}
