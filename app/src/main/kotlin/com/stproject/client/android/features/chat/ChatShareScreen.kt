package com.stproject.client.android.features.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import com.stproject.client.android.core.compliance.ContentFilterBlockedDialog
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.network.userMessage

@Composable
fun ChatShareScreen(
    shareCode: String?,
    viewModel: ChatShareViewModel,
    chatViewModel: ChatViewModel,
    onBackToExplore: () -> Unit,
    onOpenChat: () -> Unit,
    onShareCodeConsumed: () -> Unit,
    contentGate: ContentGate,
) {
    val uiState by viewModel.uiState.collectAsState()
    val chatState by chatViewModel.uiState.collectAsState()
    var startRequested by remember { mutableStateOf(false) }
    var tagBlockedOpen by remember { mutableStateOf(false) }

    LaunchedEffect(shareCode) {
        startRequested = false
        viewModel.resolveShareCode(shareCode)
        if (!shareCode.isNullOrBlank()) {
            onShareCodeConsumed()
        }
    }

    LaunchedEffect(uiState.resolvedMemberId, uiState.shareCode) {
        val memberId = uiState.resolvedMemberId
        val code = uiState.shareCode
        if (!memberId.isNullOrBlank() && !code.isNullOrBlank() && !startRequested) {
            if (contentGate.isTagBlocked(uiState.resolvedTags)) {
                tagBlockedOpen = true
                viewModel.consumeResolvedMemberId()
                return@LaunchedEffect
            }
            startRequested = true
            chatViewModel.startNewChat(
                memberId = memberId,
                shareCode = code,
                onSuccess = onOpenChat,
            )
            viewModel.consumeResolvedMemberId()
        }
    }

    val errorText = uiState.error?.userMessage() ?: if (startRequested) chatState.error else null

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (errorText != null) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("chat.share.error"),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.chat_share_invalid_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    modifier = Modifier.padding(top = 16.dp),
                    onClick = onBackToExplore,
                ) {
                    Text(stringResource(R.string.chat_share_back_to_explore))
                }
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("chat.share.loading"),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = stringResource(R.string.chat_share_resolving_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    modifier = Modifier.padding(top = 6.dp),
                    text = stringResource(R.string.chat_share_resolving_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    ContentFilterBlockedDialog(
        open = tagBlockedOpen,
        onDismiss = { tagBlockedOpen = false },
    )
}
