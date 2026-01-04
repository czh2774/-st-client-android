package com.stproject.client.android.features.social

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.compliance.RestrictedContentNotice
import com.stproject.client.android.domain.model.SocialUserSummary
import com.stproject.client.android.features.chat.ModerationViewModel
import com.stproject.client.android.features.chat.ReportDialog

@Composable
fun SocialScreen(
    viewModel: SocialViewModel,
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
                Text(text = stringResource(R.string.social_title), style = MaterialTheme.typography.titleMedium)
                if (contentGate.nsfwAllowed) {
                    RestrictedContentNotice(
                        onReport = {
                            val targetId = uiState.items.firstOrNull()?.id
                            if (!targetId.isNullOrBlank()) {
                                reportTargetId = targetId
                                moderationViewModel.loadReasonsIfNeeded()
                            }
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { viewModel.setTab(SocialTab.Followers) }) {
                        Text(stringResource(R.string.social_tab_followers))
                    }
                    TextButton(onClick = { viewModel.setTab(SocialTab.Following) }) {
                        Text(stringResource(R.string.social_tab_following))
                    }
                    TextButton(onClick = { viewModel.setTab(SocialTab.Blocked) }) {
                        Text(stringResource(R.string.social_tab_blocked))
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.targetUserId,
                    onValueChange = viewModel::onTargetUserIdChanged,
                    label = { Text(stringResource(R.string.social_target_user)) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = viewModel::load, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.common_refresh))
                    }
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.social_follow_block_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.actionUserId,
                    onValueChange = viewModel::onActionUserIdChanged,
                    label = { Text(stringResource(R.string.social_user_id)) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = { viewModel.followUser(true) }, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.common_follow))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.followUser(false) }, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.common_unfollow))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.blockUser(true) }, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.common_block))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.blockUser(false) }, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.common_unblock))
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
                    SocialUserRow(
                        item = item,
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
private fun SocialUserRow(
    item: SocialUserSummary,
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
        Text(text = item.displayName, style = MaterialTheme.typography.titleSmall)
        if (!item.bio.isNullOrBlank()) {
            Text(text = item.bio ?: "", style = MaterialTheme.typography.bodyMedium)
        }
        val followInfo =
            item.followerCount?.let {
                stringResource(R.string.social_followers_count, it)
            } ?: stringResource(R.string.social_followers_unknown)
        Text(text = followInfo, style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { onReport(item.id) }) {
                Text(stringResource(R.string.common_report))
            }
        }
    }
}
