package com.stproject.client.android.features.chat

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatRole

private const val SEND_BUTTON_TEST_TAG = "chat.send"

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.error != null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = uiState.messages, key = { it.id }) { msg ->
                    ChatMessageRow(msg)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = uiState.input,
                    onValueChange = viewModel::onInputChanged,
                    enabled = !uiState.isSending,
                    label = { Text("Message") }
                )
                Button(
                    modifier = Modifier.testTag(SEND_BUTTON_TEST_TAG),
                    onClick = viewModel::onSendClicked,
                    enabled = !uiState.isSending
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun ChatMessageRow(msg: ChatMessage) {
    val prefix = when (msg.role) {
        ChatRole.System -> "SYS"
        ChatRole.User -> "YOU"
        ChatRole.Assistant -> "BOT"
    }
    Text(text = "[$prefix] ${msg.content}")
}


