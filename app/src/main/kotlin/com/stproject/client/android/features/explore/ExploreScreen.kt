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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import com.stproject.client.android.core.compliance.ContentFilterBlockedDialog
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.compliance.ContentGateBlockKind
import com.stproject.client.android.core.compliance.NsfwBlockedDialog
import com.stproject.client.android.core.compliance.RestrictedContentNotice
import com.stproject.client.android.core.compliance.resolveNsfwHint
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.features.chat.ModerationViewModel
import com.stproject.client.android.features.chat.ReportDialog

private const val SHARE_CODE_INPUT_TEST_TAG = "explore.share_code_input"

private data class ExploreSortOption(val key: String, val labelRes: Int)

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
    var tagBlockedOpen by remember { mutableStateOf(false) }
    val allowNsfw = contentGate.nsfwAllowed
    val visibleItems = uiState.items.filterNot { contentGate.isTagBlocked(it.tags) }

    fun handleGateForCharacter(character: CharacterSummary): Boolean {
        return when (
            contentGate.blockKind(
                character.isNsfw,
                character.moderationAgeRating,
                character.tags,
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

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(
        uiState.resolvedMemberId,
        uiState.resolvedShareCode,
        uiState.resolvedIsNsfw,
        uiState.resolvedAgeRating,
        allowNsfw,
    ) {
        val memberId = uiState.resolvedMemberId
        val shareCode = uiState.resolvedShareCode
        if (!memberId.isNullOrBlank() && !shareCode.isNullOrBlank()) {
            when (
                contentGate.blockKind(
                    uiState.resolvedIsNsfw,
                    uiState.resolvedAgeRating,
                    uiState.resolvedTags,
                )
            ) {
                ContentGateBlockKind.TAGS_BLOCKED -> {
                    tagBlockedOpen = true
                    viewModel.consumeResolvedShareCode()
                    return@LaunchedEffect
                }
                ContentGateBlockKind.NSFW_DISABLED -> {
                    nsfwBlockedOpen = true
                    viewModel.consumeResolvedShareCode()
                    return@LaunchedEffect
                }
                null -> Unit
                else -> {
                    viewModel.consumeResolvedShareCode()
                    return@LaunchedEffect
                }
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
                val hasMatureItems =
                    visibleItems.any {
                        resolveNsfwHint(it.isNsfw, it.moderationAgeRating) == true
                    }
                if (contentGate.nsfwAllowed && hasMatureItems) {
                    RestrictedContentNotice(
                        onReport = {
                            val targetId = visibleItems.firstOrNull()?.id
                            if (!targetId.isNullOrBlank()) {
                                reportTargetId = targetId
                                moderationViewModel.loadReasonsIfNeeded()
                            }
                        },
                    )
                }
                OutlinedTextField(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(SHARE_CODE_INPUT_TEST_TAG),
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
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.explore_filters_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                val sortOptions =
                    listOf(
                        ExploreSortOption("homepage", R.string.explore_sort_homepage),
                        ExploreSortOption("new", R.string.explore_sort_new),
                        ExploreSortOption("recommend", R.string.explore_sort_recommend),
                        ExploreSortOption("trending", R.string.explore_sort_trending),
                        ExploreSortOption("all", R.string.explore_sort_all),
                        ExploreSortOption("star", R.string.explore_sort_followed),
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
                    enabled = !uiState.isLoading,
                    label = { Text(stringResource(R.string.explore_search_label)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.tagsInput,
                    onValueChange = viewModel::onTagsChanged,
                    enabled = !uiState.isLoading,
                    label = { Text(stringResource(R.string.explore_tags_label)) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = viewModel::clearFilters, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.common_clear))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = viewModel::applyFilters, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.common_apply))
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
                items(items = visibleItems, key = { it.id }) { item ->
                    CharacterRow(
                        item = item,
                        onStartChat = { character ->
                            if (handleGateForCharacter(character)) return@CharacterRow
                            onStartChat(character.id, null)
                        },
                        onOpenDetail = { character ->
                            if (handleGateForCharacter(character)) return@CharacterRow
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
    ContentFilterBlockedDialog(
        open = tagBlockedOpen,
        onDismiss = { tagBlockedOpen = false },
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
        if (resolveNsfwHint(item.isNsfw, item.moderationAgeRating) == true) {
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
