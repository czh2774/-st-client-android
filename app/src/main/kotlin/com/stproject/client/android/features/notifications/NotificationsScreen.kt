package com.stproject.client.android.features.notifications

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
import com.stproject.client.android.domain.model.NotificationItem
import com.stproject.client.android.features.chat.ModerationViewModel
import com.stproject.client.android.features.chat.ReportDialog

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    moderationViewModel: ModerationViewModel,
    contentGate: ContentGate,
) {
    val uiState by viewModel.uiState.collectAsState()
    val moderationState by moderationViewModel.uiState.collectAsState()
    var reportTargetId by remember { mutableStateOf<String?>(null) }

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
                Text(
                    text = stringResource(R.string.notifications_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (contentGate.nsfwAllowed) {
                    RestrictedContentNotice(
                        onReport = {
                            val targetId = uiState.items.firstOrNull { !it.userId.isNullOrBlank() }?.userId
                            if (!targetId.isNullOrBlank()) {
                                reportTargetId = targetId
                                moderationViewModel.loadReasonsIfNeeded()
                            }
                        },
                    )
                }
                Text(
                    text = stringResource(R.string.notifications_unread, uiState.unreadCounts.total),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = viewModel::markAllRead, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.notifications_mark_all_read))
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
                    NotificationRow(
                        item = item,
                        onMarkRead = viewModel::markRead,
                        onReport = { userId ->
                            reportTargetId = userId
                            moderationViewModel.loadReasonsIfNeeded()
                        },
                    )
                }
                if (uiState.hasMore) {
                    item(key = "load-more") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(onClick = viewModel::loadMore, enabled = !uiState.isLoading) {
                                Text(
                                    stringResource(
                                        if (uiState.isLoading) {
                                            R.string.common_loading
                                        } else {
                                            R.string.common_load_more
                                        },
                                    ),
                                )
                            }
                        }
                    }
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
                moderationViewModel.submitReportForUser(targetId, reasons, detail)
            },
        )
    }

    LaunchedEffect(moderationState.lastReportSubmitted) {
        if (moderationState.lastReportSubmitted) {
            reportTargetId = null
        }
    }
}

@Composable
private fun NotificationRow(
    item: NotificationItem,
    onMarkRead: (String) -> Unit,
    onReport: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = item.title, style = MaterialTheme.typography.titleSmall)
        if (item.content.isNotBlank()) {
            Text(text = item.content, style = MaterialTheme.typography.bodyMedium)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.type.ifBlank { stringResource(R.string.notifications_type_general) },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!item.userId.isNullOrBlank()) {
                    TextButton(onClick = { onReport(item.userId) }) {
                        Text(stringResource(R.string.common_report))
                    }
                }
            }
            if (!item.isRead) {
                TextButton(onClick = { onMarkRead(item.id) }) {
                    Text(stringResource(R.string.notifications_mark_read))
                }
            } else {
                Text(text = stringResource(R.string.notifications_read), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
