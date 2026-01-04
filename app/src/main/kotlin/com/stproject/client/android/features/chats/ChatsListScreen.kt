package com.stproject.client.android.features.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.stproject.client.android.core.compliance.ContentFilterBlockedDialog
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.compliance.ContentGateBlockKind
import com.stproject.client.android.core.compliance.NsfwBlockedDialog
import com.stproject.client.android.core.compliance.RestrictedContentNotice
import com.stproject.client.android.domain.model.ChatSessionSummary
import com.stproject.client.android.features.chat.ModerationViewModel
import com.stproject.client.android.features.chat.ReportDialog

@Composable
fun ChatsListScreen(
    viewModel: ChatsListViewModel,
    moderationViewModel: ModerationViewModel,
    onOpenSession: (ChatSessionSummary) -> Unit,
    contentGate: ContentGate,
) {
    val uiState by viewModel.uiState.collectAsState()
    val moderationState by moderationViewModel.uiState.collectAsState()
    var nsfwBlockedOpen by remember { mutableStateOf(false) }
    var tagBlockedOpen by remember { mutableStateOf(false) }
    var reportTargetId by remember { mutableStateOf<String?>(null) }
    val allowNsfw = contentGate.nsfwAllowed
    val visibleItems = uiState.items.filterNot { contentGate.isTagBlocked(it.primaryMemberTags) }
    val visibleLastSession =
        uiState.lastSession?.takeUnless { contentGate.isTagBlocked(it.primaryMemberTags) }

    fun handleGateForSession(session: ChatSessionSummary): Boolean {
        return when (
            contentGate.blockKind(
                session.primaryMemberIsNsfw,
                session.primaryMemberAgeRating,
                session.primaryMemberTags,
            )
        ) {
            ContentGateBlockKind.TAGS_BLOCKED -> {
                tagBlockedOpen = true
                true
            }
            ContentGateBlockKind.NSFW_DISABLED -> {
                nsfwBlockedOpen = true
                true
            }
            null -> false
            else -> true
        }
    }

    LaunchedEffect(allowNsfw, contentGate.blockedTags) {
        viewModel.load(allowNsfw, contentGate.blockedTags)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
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
            if (contentGate.nsfwAllowed) {
                RestrictedContentNotice(
                    onReport = {
                        val targetId =
                            visibleLastSession?.primaryMemberId
                                ?: visibleItems.firstOrNull()?.primaryMemberId
                        if (!targetId.isNullOrBlank()) {
                            reportTargetId = targetId
                            moderationViewModel.loadReasonsIfNeeded()
                        }
                    },
                )
            }
            if (visibleLastSession != null) {
                val lastSession = visibleLastSession
                LastSessionCard(
                    item = lastSession,
                    onOpenSession = {
                        if (handleGateForSession(lastSession)) {
                            return@LastSessionCard
                        }
                        onOpenSession(lastSession)
                    },
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = visibleItems, key = { it.sessionId }) { item ->
                    ChatSessionRow(
                        item = item,
                        onOpenSession = {
                            if (handleGateForSession(item)) {
                                return@ChatSessionRow
                            }
                            onOpenSession(item)
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

    NsfwBlockedDialog(
        open = nsfwBlockedOpen,
        onDismiss = { nsfwBlockedOpen = false },
    )
    ContentFilterBlockedDialog(
        open = tagBlockedOpen,
        onDismiss = { tagBlockedOpen = false },
    )
}

@Composable
private fun ChatSessionRow(
    item: ChatSessionSummary,
    onOpenSession: (ChatSessionSummary) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = item.displayName, style = MaterialTheme.typography.titleMedium)
        if (!item.updatedAt.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.chats_updated, item.updatedAt),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = { onOpenSession(item) }) {
                Text(stringResource(R.string.common_open))
            }
        }
    }
}

@Composable
private fun LastSessionCard(
    item: ChatSessionSummary?,
    onOpenSession: () -> Unit,
) {
    if (item == null) return
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.chats_resume_last), style = MaterialTheme.typography.titleSmall)
        Text(text = item.displayName, style = MaterialTheme.typography.titleMedium)
        if (!item.updatedAt.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.chats_updated, item.updatedAt),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onOpenSession) {
                Text(stringResource(R.string.common_continue))
            }
        }
    }
}
