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
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.compliance.NsfwBlockedDialog
import com.stproject.client.android.domain.model.ChatSessionSummary

@Composable
fun ChatsListScreen(
    viewModel: ChatsListViewModel,
    onOpenSession: (ChatSessionSummary) -> Unit,
    contentGate: ContentGate,
) {
    val uiState by viewModel.uiState.collectAsState()
    var nsfwBlockedOpen by remember { mutableStateOf(false) }
    val allowNsfw = contentGate.nsfwAllowed

    LaunchedEffect(allowNsfw) {
        viewModel.load(allowNsfw)
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = uiState.items, key = { it.sessionId }) { item ->
                    ChatSessionRow(
                        item = item,
                        onOpenSession = {
                            if (contentGate.isRestricted(item.primaryMemberIsNsfw)) {
                                if (contentGate.isNsfwBlocked(item.primaryMemberIsNsfw)) {
                                    nsfwBlockedOpen = true
                                }
                                return@ChatSessionRow
                            }
                            onOpenSession(item)
                        },
                    )
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
