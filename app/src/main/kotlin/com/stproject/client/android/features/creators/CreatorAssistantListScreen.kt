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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.compliance.RestrictedContentNotice
import com.stproject.client.android.domain.model.CreatorAssistantSessionSummary

@Composable
fun CreatorAssistantListScreen(
    viewModel: CreatorAssistantListViewModel,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    contentGate: ContentGate,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.newSessionId) {
        val sessionId = uiState.newSessionId
        if (!sessionId.isNullOrBlank()) {
            onOpenSession(sessionId)
            viewModel.consumeNewSession()
        }
    }

    LaunchedEffect(uiState.openSessionId) {
        val sessionId = uiState.openSessionId
        if (!sessionId.isNullOrBlank()) {
            onOpenSession(sessionId)
            viewModel.consumeOpenSession()
        }
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
                    RestrictedContentNotice(onReport = null)
                }
                Button(onClick = viewModel::startSession, enabled = !uiState.isLoading) {
                    Text(stringResource(R.string.assistant_new_session))
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
                items(items = uiState.items, key = { it.sessionId }) { item ->
                    CreatorAssistantSessionRow(
                        item = item,
                        onOpen = { viewModel.requestOpenSession(item.sessionId) },
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
}

@Composable
private fun CreatorAssistantSessionRow(
    item: CreatorAssistantSessionSummary,
    onOpen: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = item.draftName ?: stringResource(R.string.assistant_untitled_session),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text =
                stringResource(
                    R.string.assistant_status,
                    item.status ?: stringResource(R.string.assistant_status_unknown),
                ),
            style = MaterialTheme.typography.bodySmall,
        )
        if (!item.updatedAt.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.assistant_updated, item.updatedAt),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onOpen) {
                Text(stringResource(R.string.common_continue))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onOpen) {
                Text(stringResource(R.string.common_open))
            }
        }
    }
}
