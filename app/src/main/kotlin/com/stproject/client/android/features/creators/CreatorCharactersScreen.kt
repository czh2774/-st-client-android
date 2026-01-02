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
import com.stproject.client.android.core.compliance.RestrictedContentNotice
import com.stproject.client.android.core.compliance.resolveNsfwHint
import com.stproject.client.android.domain.model.CreatorCharacter

@Composable
fun CreatorCharactersScreen(
    creatorId: String,
    viewModel: CreatorCharactersViewModel,
    onBack: () -> Unit,
    onStartChat: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    contentGate: ContentGate,
) {
    val uiState by viewModel.uiState.collectAsState()
    var nsfwBlockedOpen by remember { mutableStateOf(false) }

    LaunchedEffect(creatorId) {
        viewModel.load(creatorId)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.common_back))
                }
                Text(
                    text = stringResource(R.string.creator_characters_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (contentGate.nsfwAllowed) {
                    RestrictedContentNotice(onReport = null)
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = uiState.items, key = { it.id }) { item ->
                    CreatorCharacterRow(
                        item = item,
                        onStartChat = { character ->
                            if (contentGate.isRestricted(character.isNsfw, character.moderationAgeRating)) {
                                if (contentGate.isNsfwBlocked(character.isNsfw, character.moderationAgeRating)) {
                                    nsfwBlockedOpen = true
                                }
                                return@CreatorCharacterRow
                            }
                            onStartChat(character.id)
                        },
                        onOpenDetail = { character ->
                            if (contentGate.isRestricted(character.isNsfw, character.moderationAgeRating)) {
                                if (contentGate.isNsfwBlocked(character.isNsfw, character.moderationAgeRating)) {
                                    nsfwBlockedOpen = true
                                }
                                return@CreatorCharacterRow
                            }
                            onOpenDetail(character.id)
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

    NsfwBlockedDialog(
        open = nsfwBlockedOpen,
        onDismiss = { nsfwBlockedOpen = false },
    )
}

@Composable
private fun CreatorCharacterRow(
    item: CreatorCharacter,
    onStartChat: (CreatorCharacter) -> Unit,
    onOpenDetail: (CreatorCharacter) -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onOpenDetail(item) }) {
                Text(stringResource(R.string.common_details))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onStartChat(item) }) {
                Text(stringResource(R.string.common_chat))
            }
        }
    }
}
