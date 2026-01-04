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
import androidx.compose.foundation.lazy.LazyRow
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
import com.stproject.client.android.domain.model.CreatorSummary
import com.stproject.client.android.features.chat.ModerationViewModel
import com.stproject.client.android.features.chat.ReportDialog

private data class CreatorSortOption(val key: String, val labelRes: Int)

@Composable
fun CreatorsScreen(
    viewModel: CreatorsViewModel,
    moderationViewModel: ModerationViewModel,
    onOpenCreator: (String) -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenCreateRole: () -> Unit,
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
                    text = stringResource(R.string.creators_title),
                    style = MaterialTheme.typography.titleMedium,
                )
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
                Button(onClick = onOpenAssistant) {
                    Text(stringResource(R.string.creators_assistant))
                }
                Button(onClick = onOpenCreateRole) {
                    Text(stringResource(R.string.creators_create_role))
                }
                val sortOptions =
                    listOf(
                        CreatorSortOption("recommend", R.string.creators_sort_recommend),
                        CreatorSortOption("new", R.string.creators_sort_new),
                        CreatorSortOption("followers", R.string.creators_sort_followers),
                        CreatorSortOption("interaction", R.string.creators_sort_interaction),
                        CreatorSortOption("followed", R.string.creators_sort_followed),
                    )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = sortOptions, key = { it.key }) { option ->
                        val selected = option.key == uiState.sortBy
                        if (selected) {
                            Button(
                                onClick = { viewModel.setSortBy(option.key) },
                                enabled = !uiState.isLoading,
                            ) {
                                Text(stringResource(option.labelRes))
                            }
                        } else {
                            TextButton(
                                onClick = { viewModel.setSortBy(option.key) },
                                enabled = !uiState.isLoading,
                            ) {
                                Text(stringResource(option.labelRes))
                            }
                        }
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.searchKeyword,
                    onValueChange = viewModel::onSearchChanged,
                    label = { Text(stringResource(R.string.creators_search_label)) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = viewModel::submitSearch, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.common_search))
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
                    CreatorRow(
                        item = item,
                        onOpenCreator = onOpenCreator,
                        onFollowToggle = { creatorId, follow ->
                            viewModel.followCreator(creatorId, follow)
                        },
                        onReport = { creatorId ->
                            reportTargetId = creatorId
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
private fun CreatorRow(
    item: CreatorSummary,
    onOpenCreator: (String) -> Unit,
    onFollowToggle: (String, Boolean) -> Unit,
    onReport: (String) -> Unit,
) {
    val isFollowing = item.followStatus == 1 || item.followStatus == 3
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = item.displayName, style = MaterialTheme.typography.titleMedium)
        if (!item.bio.isNullOrBlank()) {
            Text(text = item.bio ?: "", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text =
                stringResource(
                    R.string.creators_followers_interactions,
                    item.followerCount,
                    item.interactionCount,
                ),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onReport(item.id) }) {
                Text(stringResource(R.string.common_report))
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = { onFollowToggle(item.id, !isFollowing) },
                enabled = !item.isBlocked,
            ) {
                Text(
                    stringResource(
                        if (isFollowing) {
                            R.string.common_unfollow
                        } else {
                            R.string.common_follow
                        },
                    ),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { onOpenCreator(item.id) }) {
                Text(stringResource(R.string.creators_characters))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onOpenCreator(item.id) }) {
                Text(stringResource(R.string.common_open))
            }
        }
    }
}
