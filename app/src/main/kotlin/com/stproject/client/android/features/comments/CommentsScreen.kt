package com.stproject.client.android.features.comments

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
import com.stproject.client.android.core.compliance.resolveNsfwHint
import com.stproject.client.android.domain.model.Comment
import com.stproject.client.android.domain.model.CommentSort
import com.stproject.client.android.features.chat.ModerationViewModel
import com.stproject.client.android.features.chat.ReportDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CommentsScreen(
    characterId: String,
    viewModel: CommentsViewModel,
    moderationViewModel: ModerationViewModel,
    contentGate: ContentGate,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val moderationState by moderationViewModel.uiState.collectAsState()
    var reportOpen by remember { mutableStateOf(false) }
    var reportTargetCommentId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(characterId) {
        viewModel.load(characterId)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.common_back))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = stringResource(R.string.comments_title), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.weight(1f))
                if (uiState.total > 0) {
                    Text(
                        text = stringResource(R.string.comments_total_count, uiState.total),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(
                    onClick = {
                        reportTargetCommentId = null
                        reportOpen = true
                        moderationViewModel.loadReasonsIfNeeded()
                    },
                ) {
                    Text(stringResource(R.string.common_report))
                }
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

            if (contentGate.nsfwAllowed &&
                resolveNsfwHint(uiState.characterIsNsfw, uiState.characterAgeRating) == true
            ) {
                RestrictedContentNotice(onReport = null)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SortChip(
                    label = stringResource(R.string.comments_sort_hot),
                    selected = uiState.sort == CommentSort.Hot,
                    onClick = { viewModel.setSort(CommentSort.Hot) },
                )
                SortChip(
                    label = stringResource(R.string.comments_sort_new),
                    selected = uiState.sort == CommentSort.New,
                    onClick = { viewModel.setSort(CommentSort.New) },
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::refresh, enabled = !uiState.isLoading) {
                    Text(stringResource(R.string.common_refresh))
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

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!uiState.isLoading && uiState.items.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.comments_empty),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    items(items = uiState.items, key = { it.id }) { comment ->
                        CommentThread(
                            comment = comment,
                            indentLevel = 0,
                            currentUserId = uiState.currentUserId,
                            onLike = { viewModel.toggleLike(it) },
                            onReply = { viewModel.setReplyTarget(it) },
                            onDelete = { viewModel.deleteComment(it.id) },
                            onReport = {
                                reportTargetCommentId = it.id
                                reportOpen = true
                                moderationViewModel.loadReasonsIfNeeded()
                            },
                        )
                    }
                    if (uiState.hasMore) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Button(
                                    onClick = viewModel::loadMore,
                                    enabled = !uiState.isLoadingMore,
                                ) {
                                    Text(stringResource(R.string.common_load_more))
                                }
                            }
                        }
                    }
                    if (uiState.isLoadingMore) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }

            if (uiState.replyTarget != null) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.comments_replying_to,
                                uiState.replyTarget?.username ?: "",
                            ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = viewModel::clearReplyTarget) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = uiState.input,
                    onValueChange = viewModel::onInputChanged,
                    enabled = !uiState.isSubmitting,
                    label = { Text(stringResource(R.string.comments_placeholder)) },
                )
                Button(
                    onClick = viewModel::submitComment,
                    enabled = !uiState.isSubmitting,
                ) {
                    Text(stringResource(R.string.common_send))
                }
            }
        }
    }

    if (reportOpen) {
        ReportDialog(
            state = moderationState,
            onDismiss = {
                reportOpen = false
                reportTargetCommentId = null
            },
            onSubmit = { reasons, detail ->
                val commentId = reportTargetCommentId
                if (!commentId.isNullOrBlank()) {
                    moderationViewModel.submitReportForComment(commentId, reasons, detail)
                } else {
                    moderationViewModel.submitReportForCharacter(characterId, reasons, detail)
                }
            },
        )
    }

    LaunchedEffect(moderationState.lastReportSubmitted) {
        if (moderationState.lastReportSubmitted) {
            reportOpen = false
            reportTargetCommentId = null
        }
    }
}

@Composable
private fun SortChip(
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
private fun CommentThread(
    comment: Comment,
    indentLevel: Int,
    currentUserId: String?,
    onLike: (Comment) -> Unit,
    onReply: (Comment) -> Unit,
    onDelete: (Comment) -> Unit,
    onReport: (Comment) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = (indentLevel * 16).dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val username = comment.user?.username?.takeIf { it.isNotBlank() } ?: "Anonymous"
            Text(text = username, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.width(8.dp))
            val timestamp = formatTimestamp(comment.createdAt)
            if (timestamp.isNotEmpty()) {
                Text(text = timestamp, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(text = comment.content, style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val likeLabel =
                if (comment.likesCount > 0) {
                    "${stringResource(R.string.comments_like)} (${comment.likesCount})"
                } else {
                    stringResource(R.string.comments_like)
                }
            TextButton(onClick = { onLike(comment) }) {
                Text(
                    text = likeLabel,
                    color =
                        if (comment.isLiked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
            TextButton(onClick = { onReply(comment) }) {
                Text(stringResource(R.string.comments_reply))
            }
            TextButton(onClick = { onReport(comment) }) {
                Text(stringResource(R.string.common_report))
            }
            if (!currentUserId.isNullOrBlank() && comment.userId == currentUserId) {
                TextButton(onClick = { onDelete(comment) }) {
                    Text(stringResource(R.string.common_delete))
                }
            }
        }
        if (comment.replies.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                comment.replies.forEach { reply ->
                    CommentThread(
                        comment = reply,
                        indentLevel = indentLevel + 1,
                        currentUserId = currentUserId,
                        onLike = onLike,
                        onReply = onReply,
                        onDelete = onDelete,
                        onReport = onReport,
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return try {
        val instant = Instant.parse(trimmed)
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    } catch (_: Exception) {
        trimmed
    }
}
